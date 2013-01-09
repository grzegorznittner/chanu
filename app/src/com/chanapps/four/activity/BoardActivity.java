package com.chanapps.four.activity;

import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.chanapps.four.adapter.BoardCursorAdapter;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.GoToBoardDialogFragment;
import com.chanapps.four.handler.LoaderHandler;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class BoardActivity extends FragmentActivity implements ClickableLoaderActivity, ChanIdentifiedActivity {
	public static final String TAG = BoardActivity.class.getSimpleName();
	public static final boolean DEBUG = false;

    private static final String DEFAULT_BOARD_CODE = "a";

    public static final int LOADER_RESTART_INTERVAL_SUPER_MS = 10000;
    public static final int LOADER_RESTART_INTERVAL_LONG_MS = 5000;
    public static final int LOADER_RESTART_INTERVAL_MED_MS = 2000;
    public static final int LOADER_RESTART_INTERVAL_SHORT_MS = 1000;
    public static final int LOADER_RESTART_INTERVAL_MICRO_MS = 100;

//    private static final String AD_UNIT_ID = "1234";
//    private static final String ARLEY_TEST_SAMSUNG_S3 = "42f758079ab89f7f";

    protected static final int IMAGE_URL_HASHCODE_KEY = R.id.grid_item_image;

    protected BoardCursorAdapter adapter;
    protected GridView gridView;
    protected Handler handler;
    protected BoardCursorLoader cursorLoader;
    protected int prevTotalItemCount = 0;
    protected int scrollOnNextLoaderFinished = 0;
    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;
    protected SharedPreferences prefs;

    protected View popupView;
    protected ImageView countryFlag;
    protected TextView popupHeader;
    protected TextView popupText;
    protected PopupWindow popupWindow;
    protected TextView deadThreadTextView;
    protected Button replyButton;
    protected Button quoteButton;
    protected Button dismissButton;
    protected Button fullWidthDismissButton;
    protected Button highlightButton;

    protected long tim;
    protected String boardCode;

//    protected AdView adView;

    public static void startActivity(Activity from, String boardCode) {
        Intent intent = createIntentForActivity(from, boardCode);
        from.startActivity(intent);
    }

    public static Intent createIntentForActivity(Context context, String boardCode) {
        String intentBoardCode = boardCode == null || boardCode.isEmpty() ? ChanBoard.DEFAULT_BOARD_CODE : boardCode;
        Intent intent = new Intent(context, BoardActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, intentBoardCode);
        intent.putExtra(ChanHelper.PAGE, 0);
        intent.putExtra(ChanHelper.LAST_BOARD_POSITION, 0);
        intent.putExtra(ChanHelper.FROM_PARENT, true);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(ChanHelper.LAST_BOARD_POSITION, 0); // reset it
        editor.commit();
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "************ onCreate");
        super.onCreate(savedInstanceState);
        loadFromIntentOrPrefs();
//        adView = new AdView(this, AdSize.BANNER, AD_UNIT_ID);
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));
        displayImageOptions = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.stub_image)
                .cacheOnDisc()
                .imageScaleType(ImageScaleType.EXACT)
                .build();
        createGridView();
        ensureHandler();
        LoaderManager.enableDebugLogging(true);
        if (DEBUG) Log.v(TAG, "onCreate init loader");
        getLoaderManager().initLoader(0, null, this);
    }

    protected void sizeGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(gridView, display, ChanGridSizer.ServiceType.BOARD);
        cg.sizeGridToDisplay();
    }

    protected void initGridAdapter() {
        adapter = new BoardCursorAdapter(this,
                R.layout.board_grid_item,
                this,
                new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_SHORT_TEXT, ChanHelper.POST_COUNTRY_URL},
                new int[] {R.id.grid_item_image, R.id.grid_item_text, R.id.grid_item_country_flag});
        gridView.setAdapter(adapter);
    }

    protected void createGridView() {
        setContentView(R.layout.board_grid_layout);
        gridView = (GridView)findViewById(R.id.board_grid_view);
        sizeGridToDisplay();
        initGridAdapter();
        gridView.setClickable(true);
        gridView.setOnItemClickListener(this);
        gridView.setLongClickable(true);
        gridView.setOnItemLongClickListener(this);
/*
        RelativeLayout boardGridLayout = (RelativeLayout)findViewById(R.layout.board_grid_layout);
        adView.setGravity(Gravity.BOTTOM|Gravity.LEFT);
        boardGridLayout.addView(adView);
        AdRequest adRequest = new AdRequest();
        adRequest.addTestDevice(ARLEY_TEST_SAMSUNG_S3);
*/
        //adView.loadAd(new AdRequest());
    }

    protected Handler ensureHandler() {
        if (handler == null) {
            handler = new LoaderHandler(this);
        }
        return handler;
    }

    @Override
    protected void onStart() {
        super.onStart();
		if (DEBUG) Log.v(TAG, "onStart");
    }

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG) Log.v(TAG, "onResume");
        restoreInstanceState();
	}

//    public PullToRefreshGridView getGridView() {
    public GridView getGridView() {
        return gridView;
    }

    protected String getLastPositionName() {
        return ChanHelper.LAST_BOARD_POSITION;
    }

    protected void scrollToLastPosition() {
        String intentExtra = getLastPositionName();
        int lastPosition = getIntent().getIntExtra(intentExtra, 0);
        if (lastPosition == 0) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            lastPosition = prefs.getInt(intentExtra, 0);
        }
        if (lastPosition != 0)
            scrollOnNextLoaderFinished = lastPosition;
        if (DEBUG) Log.v(TAG, "Scrolling to:" + lastPosition);
    }

    @Override
	public void onWindowFocusChanged (boolean hasFocus) {
		if (DEBUG) Log.v(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        super.onPause();
        if (DEBUG) Log.v(TAG, "onPause");
        saveInstanceState();
    }

    protected void ensurePrefs() {
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    protected void loadFromIntentOrPrefs() {
        ensurePrefs();
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            List<String> params = data.getPathSegments();
            String uriBoardCode = params.get(0);
            if (ChanBoard.getBoardByCode(this, uriBoardCode) != null) {
                boardCode = uriBoardCode;
                if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from url intent");
            }
            else {
                if (DEBUG) Log.e(TAG, "Received invalid boardCode=" + uriBoardCode + " from url intent, ignoring");
            }
        }
        else if (intent.hasExtra(ChanHelper.BOARD_CODE)) {
            boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
            if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from board code intent");
        }
        else {
            boardCode = prefs.getString(ChanHelper.BOARD_CODE, DEFAULT_BOARD_CODE);
            if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from prefs or default");
        }
    }

    protected void restoreInstanceState() {
        if (DEBUG) Log.i(TAG, "Restoring instance state...");
        loadFromIntentOrPrefs();
        startLoadService();
        setActionBarTitle();
        scrollToLastPosition();
        if (getLoaderManager().getLoader(0) == null || !getLoaderManager().getLoader(0).isStarted()) {
            ensureHandler().sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_MICRO_MS); // shorter than usual
        }
    }

    protected void startLoadService() {
        startLoadService(false);
    }

    protected void startLoadService(boolean force) {
    	if (force) {
    		FetchChanDataService.startService(this, boardCode);
    	} else {
    		FetchChanDataService.startServiceWithPriority(this, boardCode);
    	}
    }

    protected void saveInstanceState() {
        if (DEBUG) Log.i(TAG, "Saving instance state...");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(ChanHelper.BOARD_CODE, boardCode);
        editor.putLong(ChanHelper.THREAD_NO, 0);
        editor.putInt(ChanHelper.LAST_BOARD_POSITION, gridView.getFirstVisiblePosition());
        editor.commit();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    protected void onStop () {
    	super.onStop();
    	if (DEBUG) Log.v(TAG, "onStop");
    	getLoaderManager().destroyLoader(0);
    	handler = null;
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
		if (DEBUG) Log.v(TAG, "onDestroy");
		getLoaderManager().destroyLoader(0);
		handler = null;
//        if (adView != null)
//            adView.destroy();
	}

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (view instanceof TextView) {
            final int loadPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOAD_PAGE));
            final int lastPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LAST_PAGE));
            TextView tv = (TextView) view;
            if (loadPage > 0 || lastPage > 0) {
                // nothing to set
            }
            else {
                String shortText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT));
                if (shortText == null || shortText.isEmpty()) {
                    tv.setVisibility(View.INVISIBLE);
                }
                else {
                    tv.setText(shortText);
                }
            }
            return true;
        } else if (view instanceof ImageView && view.getId() == R.id.grid_item_image) {
            String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
            int loadPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOAD_PAGE));
            ImageView iv = (ImageView) view;
            if (imageUrl != null && !imageUrl.isEmpty() && loadPage == 0) {
                smartSetImageView(iv, imageUrl);
            }
            else if (loadPage > 0) {
                setImageViewToLoading(iv);
            }
            else {
                iv.setImageBitmap(null); // blank
            }
            return true;
        } else if (view instanceof ImageView && view.getId() == R.id.grid_item_country_flag) {
            ImageView iv = (ImageView) view;
            String countryFlagImageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_COUNTRY_URL));
            if (DEBUG) Log.v(TAG, "Country flag url=" + countryFlagImageUrl);
            if (countryFlagImageUrl != null && !countryFlagImageUrl.isEmpty()) {
                smartSetImageView(iv, countryFlagImageUrl);
            }
            else {
                iv.setImageBitmap(null); // blank
            }
            return true;
        } else {
            return false;
        }
    }

    protected void smartSetImageView(ImageView iv, String imageUrl) {
        smartSetImageView(iv, imageUrl, imageLoader, displayImageOptions);
    }

    public static void smartSetImageView(ImageView iv, String imageUrl,
                                         ImageLoader imageLoader, DisplayImageOptions displayImageOptions) {
        try {
            Integer viewHashCodeInt = (Integer)iv.getTag(IMAGE_URL_HASHCODE_KEY);
            int viewHashCode = viewHashCodeInt != null ? viewHashCodeInt : 0;
            int urlHashCode = imageUrl.hashCode();
            if (DEBUG) Log.i(TAG, "iv urlhash=" + urlHashCode + " viewhash=" + viewHashCode);
            if (iv.getDrawable() == null || viewHashCode != urlHashCode) {
                if (DEBUG) Log.i(TAG, "calling imageloader for " + imageUrl);
                iv.setImageBitmap(null);
                iv.setTag(IMAGE_URL_HASHCODE_KEY, urlHashCode);
                imageLoader.displayImage(imageUrl, iv, displayImageOptions);
            }
        } catch (NumberFormatException nfe) {
            try {
                iv.setImageURI(Uri.parse(imageUrl));
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't set image view after number format exception with url=" + imageUrl, e);
                iv.setImageBitmap(null);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Exception setting image view with url=" + imageUrl, e);
            iv.setImageBitmap(null);
        }
    }

    protected void setImageViewToLoading(ImageView iv) {
        iv.setImageResource(R.drawable.navigation_refresh_light);
        Animation rotation = AnimationUtils.loadAnimation(this, R.animator.clockwise_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        cursorLoader = new BoardCursorLoader(this, boardCode);
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoadFinished");
		adapter.swapCursor(data);

        int size = data == null ? 0 : data.getCount();
        ensureHandler().sendEmptyMessageDelayed(0, getOptimalRefreshTime(size));

        if (gridView != null) {
            if (scrollOnNextLoaderFinished > 0) {
                gridView.setSelection(scrollOnNextLoaderFinished);
                scrollOnNextLoaderFinished = 0;
            }
        }
    }

    private int getOptimalRefreshTime(int size) {
        int restartInterval;
        if (size > 300)
            restartInterval = LOADER_RESTART_INTERVAL_SUPER_MS;
        else if (size > 200)
            restartInterval = LOADER_RESTART_INTERVAL_LONG_MS;
        else if (size > 100)
            restartInterval = LOADER_RESTART_INTERVAL_MED_MS;
        else
            restartInterval = LOADER_RESTART_INTERVAL_SHORT_MS;

        return restartInterval;
    }

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoaderReset");
		adapter.swapCursor(null);
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final int loadPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOAD_PAGE));
        final int lastPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LAST_PAGE));
        if (loadPage == 0 && lastPage == 0)
            ThreadActivity.startActivity(this, adapterView, view, position, id, true);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        return showPopupText(adapterView, view, position, id);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, BoardSelectorActivity.class);
                intent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
                NavUtils.navigateUpTo(this, intent);
                return true;
            case R.id.refresh_board_menu:
                Toast.makeText(this, R.string.board_activity_refresh, Toast.LENGTH_LONG).show();
                startLoadService(true);
                return true;
            case R.id.new_thread_menu:
                Intent replyIntent = new Intent(this, PostReplyActivity.class);
                replyIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                replyIntent.putExtra(ChanHelper.THREAD_NO, 0);
                replyIntent.putExtra(ChanHelper.POST_NO, 0);
                replyIntent.putExtra(ChanHelper.TIM, 0);
                replyIntent.putExtra(ChanHelper.TEXT, "");
                replyIntent.putExtra(ChanHelper.QUOTE_TEXT, "");
                startActivity(replyIntent);
                return true;
            case R.id.settings_menu:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.go_to_board_menu:
                new GoToBoardDialogFragment().show(getSupportFragmentManager(), GoToBoardDialogFragment.TAG);
                return true;
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
            case R.id.about_menu:
                RawResourceDialog aboutDialog = new RawResourceDialog(this, R.layout.about_dialog, R.raw.about_header, R.raw.about_detail);
                aboutDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void displayBoardRules() {
        int boardRulesId = R.raw.global_rules_detail;
        try {
            boardRulesId = R.raw.class.getField("board_" + boardCode + "_rules").getInt(null);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't find rules for board:" + boardCode);
        }
        RawResourceDialog rawResourceDialog
                = new RawResourceDialog(this, R.layout.board_rules_dialog, R.raw.board_rules_header, boardRulesId);
        rawResourceDialog.show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (DEBUG) Log.v(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_menu, menu);
        return true;
    }

    /*
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (DEBUG) Log.d(TAG, "onScroll firstVisibleItem=" + firstVisibleItem + " visibleItemCount=" + visibleItemCount + " totalItemCount=" + totalItemCount + " prevTotalItemCount=" + prevTotalItemCount);
        if (adapter != null
                && !adapter.isEmpty()
                && adapter.getCount() > 2
                && (firstVisibleItem + visibleItemCount) >= totalItemCount
                && totalItemCount != prevTotalItemCount)
        {
            if (DEBUG) Log.v(TAG, "onListEnd, extending list");
            prevTotalItemCount = totalItemCount;
            Toast.makeText(this, "Fetching next page...", Toast.LENGTH_SHORT).show();
            startLoadService();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int s) {
    }
    */

    protected void setActionBarTitle() {
        ActionBar a = getActionBar();
        if (a == null) {
            return;
        }
        String title = "/" + boardCode; // + " " + getString(R.string.board_activity);
        a.setTitle(title);
        a.setDisplayHomeAsUpEnabled(true);
    }

    protected void ensurePopupWindow() {
        if (popupView == null) {
            popupView = getLayoutInflater().inflate(R.layout.popup_full_text_layout, null);
            countryFlag = (ImageView)popupView.findViewById(R.id.popup_country_flag);
            popupHeader = (TextView)popupView.findViewById(R.id.popup_header);
            popupText = (TextView)popupView.findViewById(R.id.popup_full_text);
            popupWindow = new PopupWindow (popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            deadThreadTextView = (TextView)popupView.findViewById(R.id.popup_dead_thread_text_view);
            replyButton = (Button)popupView.findViewById(R.id.popup_reply_button);
            quoteButton = (Button)popupView.findViewById(R.id.popup_quote_button);
            highlightButton = (Button)popupView.findViewById(R.id.popup_highlight_button);
            dismissButton = (Button)popupView.findViewById(R.id.popup_dismiss_button);
            dismissButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popupWindow.dismiss();
                }
            });
            fullWidthDismissButton = (Button)popupView.findViewById(R.id.popup_full_width_dismiss_button);
            fullWidthDismissButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popupWindow.dismiss();
                }
            });
        }
    }

    public boolean showPopupText(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String countryFlagUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_COUNTRY_URL));
        final String headerText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_HEADER_TEXT));
        final String rawText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        final String text = rawText == null ? "" : rawText;
        final int isDeadInt = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_IS_DEAD));
        final boolean isDead = isDeadInt == 0 ? false : true;
        final long resto = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_RESTO));
        final String clickedBoardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        final long tim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
        final long clickedThreadNo = resto == 0 ? postId : resto;
        final long clickedPostNo = resto == 0 || postId == resto ? 0 : postId;
        Log.i(TAG, "Calling popup with id=" + id + " isDead=" + isDead + " postNo=" + postId + " resto=" + resto + " text=" + text);
        ensurePopupWindow();
        popupHeader.setText(headerText);
        popupText.setText(text);

        if (DEBUG) Log.v(TAG, "Country flag url=" + countryFlagUrl);
        if (countryFlagUrl != null && !countryFlagUrl.isEmpty()) {
            try {
                if (DEBUG) Log.v(TAG, "calling imageloader for country flag" + countryFlagUrl);
                countryFlag.setVisibility(View.VISIBLE);
                this.imageLoader.displayImage(countryFlagUrl, countryFlag, displayImageOptions);
//                    countryFlag.setImageURI(Uri.parse(countryFlagUrl));
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't set country flag image with url=" + countryFlagUrl, e);
                countryFlag.setVisibility(View.GONE);
                countryFlag.setImageBitmap(null);
            }
        }
        else {
            countryFlag.setVisibility(View.GONE);
            countryFlag.setImageBitmap(null);
        }

        if (isDead) {
            replyButton.setVisibility(View.GONE);
            quoteButton.setVisibility(View.GONE);
            deadThreadTextView.setVisibility(View.VISIBLE);
        }
        else {
            deadThreadTextView.setVisibility(View.GONE);
            replyButton.setVisibility(View.VISIBLE);
            replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent replyIntent = new Intent(getApplicationContext(), PostReplyActivity.class);
                    replyIntent.putExtra(ChanHelper.BOARD_CODE, clickedBoardCode);
                    replyIntent.putExtra(ChanHelper.THREAD_NO, clickedThreadNo);
                    replyIntent.putExtra(ChanHelper.POST_NO, clickedPostNo);
                    replyIntent.putExtra(ChanHelper.TIM, tim);
                    replyIntent.putExtra(ChanHelper.TEXT, "");
                    replyIntent.putExtra(ChanHelper.QUOTE_TEXT, "");
                    startActivity(replyIntent);
                    popupWindow.dismiss();
                }
            });
            quoteButton.setVisibility(View.VISIBLE);
            quoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent replyIntent = new Intent(getApplicationContext(), PostReplyActivity.class);
                    replyIntent.putExtra(ChanHelper.BOARD_CODE, clickedBoardCode);
                    replyIntent.putExtra(ChanHelper.THREAD_NO, clickedThreadNo);
                    replyIntent.putExtra(ChanHelper.POST_NO, clickedPostNo);
                    replyIntent.putExtra(ChanHelper.TIM, tim);
                    replyIntent.putExtra(ChanHelper.TEXT, "");
                    replyIntent.putExtra(ChanHelper.QUOTE_TEXT, text);
                    startActivity(replyIntent);
                    popupWindow.dismiss();
                }
            });
        }

        displayHighlightButton(clickedPostNo);

        popupWindow.showAtLocation(adapterView, Gravity.CENTER, 0, 0);
        return true;
    }

    protected void displayHighlightButton(long postNo) { // board-level doesn't highlight, only thread-level does
        highlightButton.setVisibility(View.GONE);
        dismissButton.setVisibility(View.GONE);
        fullWidthDismissButton.setVisibility(View.VISIBLE);
    }

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.BOARD_ACTIVITY, boardCode);
	}

	@Override
	public AsyncTaskLoader<Cursor> getChanCursorLoader() {
		return cursorLoader;
	}
	
	@Override
	public Handler getChanHandler() {
		return handler;
	}

}
