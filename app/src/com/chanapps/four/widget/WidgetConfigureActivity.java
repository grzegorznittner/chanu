package com.chanapps.four.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.mColorPicker.ColorPickerDialog;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.FetchPopularThreadsService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/15/13
 * Time: 11:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class WidgetConfigureActivity extends FragmentActivity {

    public static final String TAG = WidgetConfigureActivity.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final long DELAY_BOARD_IMAGE_MS = 5 * 1000; // give board fetch time to finish

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetConf widgetConf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.widget_configure_layout);
        appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid app widget id received, exiting configuration");
            finish();
        }
        else {
            if (DEBUG) Log.i(TAG, "Configuring widget=" + appWidgetId);
        }
        widgetConf = BoardWidgetProvider.loadWidgetConf(this, appWidgetId);
        if (widgetConf == null)
            widgetConf = new WidgetConf(appWidgetId); // new widget or no config;
        setupSpinner();
        setupCheckboxes();
        addColorClickHandler();
        addDoneClickHandler();
        initWidgetLayoutState();
        WidgetConfigureActivity.this.setResult(Activity.RESULT_CANCELED);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    protected void setupSpinner() {
        boolean adultMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);
        int spinnerId = adultMode ? R.id.board_spinner_adult : R.id.board_spinner;
        int otherSpinnerId = adultMode ? R.id.board_spinner : R.id.board_spinner_adult;
        Spinner spinner = (Spinner)findViewById(spinnerId);
        Spinner otherSpinner = (Spinner)findViewById(otherSpinnerId);
        spinner.setVisibility(View.VISIBLE);
        otherSpinner.setVisibility(View.GONE);
        int position = 0;
        if (widgetConf.boardCode == null || widgetConf.boardCode.isEmpty()) {
            position = 0;
        }
        else {
            SpinnerAdapter spinnerAdapter = spinner.getAdapter();
            for (int i = 0; i < spinnerAdapter.getCount(); i++) {
                String boardText = (String)spinnerAdapter.getItem(i);
                if (ChanBoard.isVirtualBoard(widgetConf.boardCode)
                    && ChanBoard.WATCH_BOARD_CODE.equals(widgetConf.boardCode)
                    && boardText.matches(getString(R.string.board_watch)))
                {
                    position = i;
                    break;
                }
                if (ChanBoard.isVirtualBoard(widgetConf.boardCode)
                    && ChanBoard.POPULAR_BOARD_CODE.equals(widgetConf.boardCode)
                    && boardText.matches(getString(R.string.board_popular)))
                {
                    position = i;
                    break;
                }
                if (ChanBoard.isVirtualBoard(widgetConf.boardCode)
                    && ChanBoard.LATEST_BOARD_CODE.equals(widgetConf.boardCode)
                    && boardText.matches(getString(R.string.board_latest)))
                {
                    position = i;
                    break;
                }
                if (ChanBoard.isVirtualBoard(widgetConf.boardCode)
                    && ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(widgetConf.boardCode)
                    && boardText.matches(getString(R.string.board_latest_images)))
                {
                    position = i;
                    break;
                }
                else if (!ChanBoard.isVirtualBoard(widgetConf.boardCode)
                        && boardText.matches("/" + widgetConf.boardCode + "/.*"))
                {
                    position = i;
                    break;
                }
            }
        }
        spinner.setSelection(position, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final Context context = WidgetConfigureActivity.this.getApplicationContext();
                updateWidgetConfWithSelectedBoard((String)parent.getItemAtPosition(position));
                boolean onDisk = ChanFileStorage.isBoardCachedOnDisk(context, widgetConf.boardCode);
                boolean freshFetch;
                if (onDisk) {
                    freshFetch = false;
                }
                else {
                    if (ChanBoard.WATCH_BOARD_CODE.equals(widgetConf.boardCode)) {
                        freshFetch = false;
                    }
                    else if (ChanBoard.isVirtualBoard(widgetConf.boardCode)) {
                        freshFetch = FetchPopularThreadsService.schedulePopularFetchWithPriority(context);
                    }
                    else {
                        freshFetch = FetchChanDataService.scheduleBoardFetchWithPriority(context, widgetConf.boardCode);
                    }
                }
                if (freshFetch)
                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setBoardImages();
                        }
                    }, DELAY_BOARD_IMAGE_MS);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateWidgetConfWithSelectedBoard("");
            }
        });
    }

    protected void setupCheckboxes() {
        CheckBox roundedCorners = (CheckBox)findViewById(R.id.rounded_corners);
        CheckBox showBoardButton = (CheckBox)findViewById(R.id.show_board);
        CheckBox showRefreshButton = (CheckBox)findViewById(R.id.show_refresh);
        CheckBox showConfigureButton = (CheckBox)findViewById(R.id.show_configure);
        roundedCorners.setChecked(widgetConf.roundedCorners);
        showBoardButton.setChecked(widgetConf.showBoardTitle);
        showRefreshButton.setChecked(widgetConf.showRefreshButton);
        showConfigureButton.setChecked(widgetConf.showConfigureButton);
        roundedCorners.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                widgetConf.roundedCorners = isChecked;
                updateContainerBackgroundState();
            }
        });
        showBoardButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                widgetConf.showBoardTitle = isChecked;
                updateBoardTitleState();
            }
        });
        showRefreshButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                widgetConf.showRefreshButton = isChecked;
                updateRefreshButtonState();
            }
        });
        showConfigureButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                widgetConf.showConfigureButton = isChecked;
                updateConfigButtonState();
            }
        });
    }
    
    protected void addColorClickHandler() {
        EditText backgroundColorButton = (EditText) findViewById(R.id.board_title_color);
        if (backgroundColorButton == null)
            return;
        backgroundColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ColorPickerDialog d = new ColorPickerDialog(WidgetConfigureActivity.this,
                        widgetConf.boardTitleColor);
                d.setAlphaSliderVisible(true);
                d.setButton(DialogInterface.BUTTON_POSITIVE,
                        getString(R.string.done),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                widgetConf.boardTitleColor = d.getColor();
                                updateBoardTitleState();
                            }
                        });
                d.setButton(DialogInterface.BUTTON_NEGATIVE,
                        getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                d.show();
            }
        });
    }

    protected void updateWidgetConfWithSelectedBoard(String boardSpinnerLine) {
        if (boardSpinnerLine == null || boardSpinnerLine.isEmpty())
            boardSpinnerLine = "";
        String boardCode;
        if (getString(R.string.board_watch).equals(boardSpinnerLine)) {
            boardCode = ChanBoard.WATCH_BOARD_CODE;
        }
        else if (getString(R.string.board_popular).equals(boardSpinnerLine)) {
            boardCode = ChanBoard.POPULAR_BOARD_CODE;
        }
        else if (getString(R.string.board_latest).equals(boardSpinnerLine)) {
            boardCode = ChanBoard.LATEST_BOARD_CODE;
        }
        else if (getString(R.string.board_latest_images).equals(boardSpinnerLine)) {
            boardCode = ChanBoard.LATEST_IMAGES_BOARD_CODE;
        }
        else {
            Pattern p = Pattern.compile("/([^/]*)/.*");
            Matcher m = p.matcher(boardSpinnerLine);
            if (m.matches())
                boardCode = m.group(1);
            else
                boardCode = ChanBoard.DEFAULT_BOARD_CODE;
        }
        widgetConf.boardCode = boardCode;
        updateBoardTitleState();
        setBoardImages();
    }

    protected void addDoneClickHandler() {
        Button doneButton = (Button) findViewById(R.id.done);
        if (doneButton == null)
            return;
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) Log.i(TAG, "Configured widget=" + appWidgetId + " configuring for board=" + widgetConf.boardCode);
                BoardWidgetProvider.storeWidgetConf(WidgetConfigureActivity.this, widgetConf);
                Intent intent = new Intent();
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                WidgetConfigureActivity.this.setResult(Activity.RESULT_OK, intent);
                Intent updateWidget = new Intent(WidgetConfigureActivity.this, BoardWidgetProvider.class);
                updateWidget.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = { appWidgetId };
                updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                WidgetConfigureActivity.this.sendBroadcast(updateWidget);
                WidgetConfigureActivity.this.finish();
            }
        });
    }
    
    protected void initWidgetLayoutState() {
        updateContainerBackgroundState();
        updateBoardTitleState();
        updateRefreshButtonState();
        updateConfigButtonState();
        setBoardImages();
    }

    protected void updateContainerBackgroundState() {
        int containerBackground = widgetConf.roundedCorners ? R.drawable.widget_rounded_background : 0;
        RelativeLayout container = (RelativeLayout)findViewById(R.id.widget_preview);
        container.setBackgroundResource(containerBackground);
    }

    protected void updateBoardTitleState() {
        ChanBoard board = ChanBoard.getBoardByCode(this, widgetConf.boardCode);
        if (board == null)
            board = ChanBoard.getBoardByCode(this, ChanBoard.DEFAULT_BOARD_CODE);
        String boardTitle = ChanBoard.isVirtualBoard(widgetConf.boardCode)
                ? board.name
                : board.name + " /" + board.link + "/";
        int boardTitleColor = widgetConf.boardTitleColor;
        int boardTitleVisibility = widgetConf.showBoardTitle ? View.VISIBLE : View.GONE;
        TextView tv = (TextView)findViewById(R.id.board_title);
        tv.setText(boardTitle);
        tv.setTextColor(boardTitleColor);
        tv.setVisibility(boardTitleVisibility);
    }

    protected void updateRefreshButtonState() {
        int refreshBackground = widgetConf.showRefreshButton ? R.drawable.widget_refresh_gradient_bg : 0;
        int refreshDrawable = widgetConf.showRefreshButton ? R.drawable.widget_refresh_button_selector : 0;
        ImageView refresh = (ImageView)findViewById(R.id.refresh);
        refresh.setBackgroundResource(refreshBackground);
        refresh.setImageResource(refreshDrawable);
    }

    protected void updateConfigButtonState() {
        int configureBackground = widgetConf.showConfigureButton ? R.drawable.widget_configure_gradient_bg : 0;
        int configureDrawable = widgetConf.showConfigureButton ? R.drawable.widget_configure_button_selector : 0;
        ImageView configure = (ImageView)findViewById(R.id.configure);
        configure.setBackgroundResource(configureBackground);
        configure.setImageResource(configureDrawable);
    }

    protected void setBoardImages() {
        final Context context = getApplicationContext();
        final String boardCode = widgetConf.boardCode;
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int[] imageIds = { R.id.image_left, R.id.image_center, R.id.image_right };
                final String[] urls = boardThreadUrls(context, boardCode, imageIds.length);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < imageIds.length; i++) {
                            final int imageResourceId = imageIds[i];
                            final ImageView iv = (ImageView)findViewById(imageResourceId);
                            iv.setImageBitmap(null);
                            if (DEBUG) Log.i(TAG, "Calling displayImage i=" + i + " url=" + urls[i]);
                            ChanImageLoader.getInstance(context).displayImage(urls[i], iv);
                        }
                    }
                });
            }
        }).start();
    }

    protected String[] boardThreadUrls(Context context, String boardCode, int numThreads) {
        String[] urls = new String[numThreads];
        ChanPost[] threads = UpdateWidgetService.loadBestWidgetThreads(this, boardCode, numThreads);
        for (int i = 0; i < numThreads; i++) {
            ChanPost thread = threads[i];
            String url = ChanBoard.getBestWidgetImageUrl(thread, boardCode, i);
            urls[i] = url;
        }
        return urls;
    }

}
