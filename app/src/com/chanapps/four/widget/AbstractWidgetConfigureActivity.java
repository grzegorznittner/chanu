package com.chanapps.four.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.mColorPicker.ColorPickerDialog;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.FetchPopularThreadsService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/15/13
 * Time: 11:07 PM
 */
public abstract class AbstractWidgetConfigureActivity extends FragmentActivity {

    public static final String TAG = AbstractWidgetConfigureActivity.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final long DELAY_BOARD_IMAGE_MS = 5 * 1000; // give board fetch time to finish

    protected int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    protected WidgetConf widgetConf;

    protected abstract int getContentViewLayout();

    protected abstract void setBoardImages();

    protected abstract String getWidgetType();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(getContentViewLayout());
        appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid app widget id received, exiting configuration");
            finish();
        } else {
            if (DEBUG) Log.i(TAG, "Configuring widget=" + appWidgetId);
        }
        widgetConf = WidgetProviderUtils.loadWidgetConf(this, appWidgetId);
        if (widgetConf == null)
            widgetConf = new WidgetConf(appWidgetId, getWidgetType()); // new widget or no config;
        setupSpinner();
        setupCheckboxes();
        addColorClickHandler();
        addDoneClickHandler();
        initWidgetLayoutState();
        AbstractWidgetConfigureActivity.this.setResult(Activity.RESULT_CANCELED);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    protected String[] spinnerArray() {
        List<ChanBoard> boards = ChanBoard.getNewThreadBoardsRespectingNSFW(this);
        String[] boardsArray = new String[boards.size() + 1];
        int i = 0;
        for (ChanBoard board : boards)
            boardsArray[i++] = "/" + board.link + "/ " + board.name;
        boardsArray[i++] = getString(R.string.board_watch);
        return boardsArray;
    }

    protected ArrayAdapter<String> createSpinnerAdapter() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, android.R.id.text1, spinnerArray());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return spinnerAdapter;
    }

    protected void setupSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.board_spinner);
        ArrayAdapter<String> spinnerAdapter = createSpinnerAdapter();
        spinner.setAdapter(spinnerAdapter);
        int position = 0;
        if (widgetConf.boardCode == null || widgetConf.boardCode.isEmpty()) {
            position = 0;
        } else {
            for (int i = 0; i < spinnerAdapter.getCount(); i++) {
                String boardText = (String) spinnerAdapter.getItem(i);
                if (ChanBoard.isVirtualBoard(widgetConf.boardCode)
                        && ChanBoard.WATCHLIST_BOARD_CODE.equals(widgetConf.boardCode)
                        && boardText.matches(getString(R.string.board_watch))) {
                    position = i;
                    break;
                }
                if (ChanBoard.isVirtualBoard(widgetConf.boardCode)
                        && ChanBoard.POPULAR_BOARD_CODE.equals(widgetConf.boardCode)
                        && boardText.matches(getString(R.string.board_popular))) {
                    position = i;
                    break;
                }
                if (ChanBoard.isVirtualBoard(widgetConf.boardCode)
                        && ChanBoard.LATEST_BOARD_CODE.equals(widgetConf.boardCode)
                        && boardText.matches(getString(R.string.board_latest))) {
                    position = i;
                    break;
                }
                if (ChanBoard.isVirtualBoard(widgetConf.boardCode)
                        && ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(widgetConf.boardCode)
                        && boardText.matches(getString(R.string.board_latest_images))) {
                    position = i;
                    break;
                } else if (!ChanBoard.isVirtualBoard(widgetConf.boardCode)
                        && boardText.matches("/" + widgetConf.boardCode + "/.*")) {
                    position = i;
                    break;
                }
            }
        }
        spinner.setSelection(position, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final Context context = AbstractWidgetConfigureActivity.this.getApplicationContext();
                updateWidgetConfWithSelectedBoard((String) parent.getItemAtPosition(position));
                boolean onDisk = ChanFileStorage.isBoardCachedOnDisk(context, widgetConf.boardCode);
                boolean freshFetch;
                if (onDisk) {
                    freshFetch = false;
                } else {
                    if (ChanBoard.WATCHLIST_BOARD_CODE.equals(widgetConf.boardCode)) {
                        freshFetch = false;
                    } else if (ChanBoard.isPopularBoard(widgetConf.boardCode)) {
                        if (DEBUG) Log.i(TAG, "scheduling popular fetch for board=" + widgetConf.boardCode);
                        freshFetch = FetchPopularThreadsService.schedulePopularFetchService(context, true, false);
                    } else if (ChanBoard.isVirtualBoard(widgetConf.boardCode)) {
                        if (DEBUG) Log.i(TAG, "skipping fetch for non-popular virtual board=" + widgetConf.boardCode);
                        freshFetch = false;
                    } else {
                        if (DEBUG) Log.i(TAG, "scheduling fetch for board=" + widgetConf.boardCode);
                        freshFetch = FetchChanDataService.scheduleBoardFetch(context, widgetConf.boardCode, true, false);
                    }
                }

                if (freshFetch) {
                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setBoardImages();
                        }
                    }, DELAY_BOARD_IMAGE_MS);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateWidgetConfWithSelectedBoard("");
            }
        });
    }

    protected void setupCheckboxes() {
        CheckBox roundedCorners = (CheckBox) findViewById(R.id.rounded_corners);
        CheckBox showBoardButton = (CheckBox) findViewById(R.id.show_board);
        CheckBox showRefreshButton = (CheckBox) findViewById(R.id.show_refresh);
        CheckBox showConfigureButton = (CheckBox) findViewById(R.id.show_configure);
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
                final ColorPickerDialog d = new ColorPickerDialog(AbstractWidgetConfigureActivity.this,
                        widgetConf.boardTitleColor);
                d.setAlphaSliderVisible(true);
                d.setButton(DialogInterface.BUTTON_POSITIVE,
                        getString(R.string.thread_context_select),
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
            boardCode = ChanBoard.WATCHLIST_BOARD_CODE;
        } else if (getString(R.string.board_popular).equals(boardSpinnerLine)) {
            boardCode = ChanBoard.POPULAR_BOARD_CODE;
        } else if (getString(R.string.board_latest).equals(boardSpinnerLine)) {
            boardCode = ChanBoard.LATEST_BOARD_CODE;
        } else if (getString(R.string.board_latest_images).equals(boardSpinnerLine)) {
            boardCode = ChanBoard.LATEST_IMAGES_BOARD_CODE;
        } else {
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

    protected abstract void addDoneClickHandler();

    protected void initWidgetLayoutState() {
        updateContainerBackgroundState();
        updateBoardTitleState();
        updateRefreshButtonState();
        updateConfigButtonState();
        setBoardImages();
    }

    protected void updateContainerBackgroundState() {
        int containerBackground = widgetConf.roundedCorners ? R.drawable.widget_rounded_background : 0;
        View container = findViewById(R.id.widget_preview);
        container.setBackgroundResource(containerBackground);
    }

    protected void updateBoardTitleState() {
        ChanBoard board = ChanBoard.getBoardByCode(this, widgetConf.boardCode);
        if (board == null)
            board = ChanBoard.getBoardByCode(this, ChanBoard.DEFAULT_BOARD_CODE);
        String boardTitle;
        if (WidgetConstants.WIDGET_TYPE_ONE_IMAGE.equals(widgetConf.widgetType))
            boardTitle = "/" + board.link + "/";
        else if (ChanBoard.isVirtualBoard(board.link))
            boardTitle = board.getName(this);
        else
            boardTitle = board.getName(this) + " /" + board.link + "/";
        int boardTitleColor = widgetConf.boardTitleColor;
        int boardTitleVisibility = widgetConf.showBoardTitle ? View.VISIBLE : View.GONE;
        TextView tv = (TextView) findViewById(R.id.board_title);
        tv.setText(boardTitle);
        tv.setTextColor(boardTitleColor);
        tv.setVisibility(boardTitleVisibility);
    }

    protected void updateRefreshButtonState() {
        int refreshDrawable = widgetConf.showRefreshButton ? R.drawable.widget_refresh_button_selector : 0;
        ImageView refresh = (ImageView) findViewById(R.id.refresh_board);
        refresh.setImageResource(refreshDrawable);
    }

    protected void updateConfigButtonState() {
        int configureDrawable = widgetConf.showConfigureButton ? R.drawable.widget_configure_button_selector : 0;
        ImageView configure = (ImageView) findViewById(R.id.configure);
        configure.setImageResource(configureDrawable);
    }

    protected String[] boardThreadUrls(Context context, String boardCode, int numThreads) {
        String[] urls = new String[numThreads];
        ChanPost[] threads = WidgetProviderUtils.loadBestWidgetThreads(this, boardCode, numThreads);
        for (int i = 0; i < numThreads; i++) {
            ChanPost thread = threads[i];
            String url = ChanBoard.getBestWidgetImageUrl(context, thread, boardCode, i);
            urls[i] = url;
        }
        return urls;
    }

}
