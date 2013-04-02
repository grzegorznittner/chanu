package com.chanapps.four.activity;

import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.handler.LoaderHandler;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;

public class BoardActivity
        extends FragmentActivity
        implements ClickableLoaderActivity, ChanIdentifiedActivity, RefreshableActivity
{
	public static final String TAG = BoardActivity.class.getSimpleName();
	public static final boolean DEBUG = false;

    private static final String DEFAULT_BOARD_CODE = "a";

    public static final int LOADER_RESTART_INTERVAL_MED_MS = 2000;
    public static final int LOADER_RESTART_INTERVAL_SHORT_MS = 1000;
    public static final int LOADER_RESTART_INTERVAL_MICRO_MS = 100;
    private static final int THUMB_WIDTH_PX = 150;
    private static final int THUMB_HEIGHT_PX = 150;

    protected AbstractBoardCursorAdapter adapter;
    protected AbsListView absListView;
    protected Class absListViewClass = GridView.class;
    protected Handler handler;
    protected BoardCursorLoader cursorLoader;
    protected int scrollOnNextLoaderFinished = 0;
    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;
    protected ProgressBar progressBar;
    protected Menu menu;
    protected MenuItem refreshMenuItem;
    protected SharedPreferences prefs;
    protected long tim;
    protected String boardCode;
    protected int columnWidth = 0;
    protected int columnHeight = 0;

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

    protected void initImageLoader() {
        ImageSize imageSize = new ImageSize(THUMB_WIDTH_PX, THUMB_HEIGHT_PX); // view pager needs micro images
        imageLoader = ChanImageLoader.getInstance(getApplicationContext());
        displayImageOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.POWER_OF_2)
                .imageSize(imageSize)
                .cacheOnDisc()
                .resetViewBeforeLoading()
                .build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "************ onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        loadFromIntentOrPrefs();
        initImageLoader();
        createAbsListView();
        ensureHandler();
        LoaderManager.enableDebugLogging(true);
        if (DEBUG) Log.v(TAG, "onCreate init loader");
        progressBar = (ProgressBar)findViewById(R.id.board_progress_bar);
        getLoaderManager().initLoader(0, null, this);
    }

    protected void setProgressOn(boolean progressOn) {
        if (refreshMenuItem != null)
            refreshMenuItem.setVisible(!progressOn);
        setProgressBarIndeterminateVisibility(progressOn);
        if (progressBar != null)
            progressBar.setVisibility(progressOn ? View.VISIBLE : View.GONE);
    }
    protected void sizeGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(absListView, display, ChanGridSizer.ServiceType.BOARD);
        cg.sizeGridToDisplay();
        columnWidth = cg.getColumnWidth();
        columnHeight = cg.getColumnHeight();
    }

    protected void initAdapter() {
        adapter = new BoardGridCursorAdapter(this,
                R.layout.board_grid_item,
                this,
                new String[] {
                        ChanThread.THREAD_THUMBNAIL_URL,
                        ChanThread.THREAD_SUBJECT,
                        ChanThread.THREAD_INFO,
                        ChanThread.THREAD_COUNTRY_FLAG_URL},
                new int[] {
                        R.id.grid_item_thread_thumb,
                        R.id.grid_item_thread_subject,
                        R.id.grid_item_thread_info,
                        R.id.grid_item_country_flag},
                columnWidth,
                columnHeight);
        absListView.setAdapter(adapter);
    }

    protected int getLayoutId() {
        if (GridView.class.equals(absListViewClass))
            return R.layout.board_grid_layout;
        else
            return R.layout.board_list_layout;
    }

    protected void setAbsListViewClass() { // override to change
        absListViewClass = GridView.class; // always for board view
    }

    protected void initAbsListView() {
        if (GridView.class.equals(absListViewClass)) {
            absListView = (GridView)findViewById(R.id.board_grid_view);
            sizeGridToDisplay();
            ImageSize imageSize = new ImageSize(columnWidth, columnHeight);
            displayImageOptions = new DisplayImageOptions.Builder()
                    .imageScaleType(ImageScaleType.POWER_OF_2)
                    .imageSize(imageSize)
                    .cacheOnDisc()
                    .resetViewBeforeLoading()
                    .build();
        }
        else {
            absListView = (ListView)findViewById(R.id.board_list_view);
        }
    }

    protected void createAbsListView() {
        setAbsListViewClass();
        setContentView(getLayoutId());
        initAbsListView();
        initAdapter();
        absListView.setClickable(true);
        absListView.setOnItemClickListener(this);
        absListView.setLongClickable(false);
    }

    protected synchronized Handler ensureHandler() {
        if (handler == null) {
            if (ChanHelper.onUIThread())
                handler = new LoaderHandler(this);
            else
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handler = new LoaderHandler(BoardActivity.this);
                    }
                });
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
		NetworkProfileManager.instance().activityChange(this);
		Loader loader = getLoaderManager().getLoader(0);
		if (loader == null) {
			getLoaderManager().initLoader(0, null, this);
		}
	}

    protected String getLastPositionName() {
        return ChanHelper.LAST_BOARD_POSITION;
    }

    protected void scrollToLastPosition() {
        String intentExtra = getLastPositionName();
        int lastPosition = getIntent().getIntExtra(intentExtra, 0);
        if (lastPosition == 0) {
            lastPosition = ensurePrefs().getInt(intentExtra, 0);
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

    protected SharedPreferences ensurePrefs() {
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs;
    }

    protected void loadFromIntentOrPrefs() {
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
            boardCode = ensurePrefs().getString(ChanHelper.BOARD_CODE, DEFAULT_BOARD_CODE);
            if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from prefs or default");
        }
    }

    protected void restoreInstanceState() {
        if (DEBUG) Log.i(TAG, "Restoring instance state...");
        loadFromIntentOrPrefs();
        setActionBarTitle();
        scrollToLastPosition();
    }

    protected void saveInstanceState() {
        if (DEBUG) Log.i(TAG, "Saving instance state...");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(ChanHelper.BOARD_CODE, boardCode);
        editor.putLong(ChanHelper.THREAD_NO, 0);
        editor.putInt(ChanHelper.LAST_BOARD_POSITION, absListView.getFirstVisiblePosition());
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
	}

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        switch (view.getId()) {
            case R.id.grid_item_thread_subject:
                return setThreadSubject((TextView) view, cursor);
            case R.id.grid_item_thread_info:
                return setThreadInfo((TextView) view, cursor);
            case R.id.grid_item_thread_thumb:
                return setThreadThumb((ImageView) view, cursor);
            case R.id.grid_item_country_flag:
                return setCountryFlag((ImageView) view, cursor);
        }
        return false;
    }

    protected boolean setThreadSubject(TextView tv, Cursor cursor) {
        tv.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT))));
        return true;
    }

    protected boolean setThreadInfo(TextView tv, Cursor cursor) {
        tv.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_INFO))));
        return true;
    }

    protected boolean setThreadThumb(ImageView iv, Cursor cursor) {
        /*
        ViewGroup.LayoutParams params = iv.getLayoutParams();
        if (params != null && columnWidth > 0 && columnHeight > 0) {
            params.width = columnWidth;
            params.height = columnHeight;
        }
        */
        imageLoader.displayImage(
                cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL)),
                iv,
                displayImageOptions); // load async
        return true;
    }

    protected boolean setCountryFlag(ImageView iv, Cursor cursor) {
        imageLoader.displayImage(
                cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_COUNTRY_FLAG_URL)),
                iv,
                displayImageOptions); // load async
        return true;
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        setProgressOn(true);
        cursorLoader = new BoardCursorLoader(this, boardCode);
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoadFinished");
		adapter.swapCursor(data);
        if (absListView != null) {
            if (scrollOnNextLoaderFinished > 0) {
                absListView.setSelection(scrollOnNextLoaderFinished);
                scrollOnNextLoaderFinished = 0;
            }
        }
        setProgressOn(false);
    }

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoaderReset");
        setProgressOn(true);
		adapter.swapCursor(null);
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String clickUrl = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_CLICK_URL));
        ChanHelper.fadeout(this, view);
        if (clickUrl == null || clickUrl.isEmpty()) {
            final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
            ThreadActivity.startActivity(this, boardCode, threadNo);
        }
        else {
            ChanHelper.launchUrlInBrowser(this, clickUrl);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, BoardSelectorActivity.class);
                intent.putExtra(ChanHelper.BOARD_TYPE, ChanBoard.getBoardByCode(this, boardCode).type.toString());
                intent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
                NavUtils.navigateUpTo(this, intent);
                return true;
            case R.id.refresh_menu:
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            case R.id.new_thread_menu:
                Intent replyIntent = new Intent(this, PostReplyActivity.class);
                replyIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                replyIntent.putExtra(ChanHelper.THREAD_NO, 0);
                replyIntent.putExtra(ChanHelper.POST_NO, 0);
                replyIntent.putExtra(ChanHelper.TIM, 0);
                replyIntent.putExtra(ChanHelper.TEXT, "");
                startActivity(replyIntent);
                return true;
            case R.id.offline_board_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, boardCode);
                return true;
            case R.id.offline_chan_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, null);
                return true;
            case R.id.settings_menu:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
            case R.id.exit_menu:
                ChanHelper.exitApplication(this);
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
        int menuId = ChanBoard.showNSFW(this) ? R.menu.board_menu_adult : R.menu.board_menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId, menu);
        ChanBoard.setupActionBarBoardSpinner(this, menu, boardCode);
        this.menu = menu;
        this.refreshMenuItem = menu.findItem(R.id.refresh_menu);
        return true;
    }

    protected void setActionBarTitle() {
        ActionBar a = getActionBar();
        if (a == null)
            return;
        a.setDisplayShowTitleEnabled(false);
        a.setDisplayHomeAsUpEnabled(true);
        invalidateOptionsMenu(); // because onPrepare isn't called when it should be
    }

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.BOARD_ACTIVITY, boardCode);
	}

	@Override
	public Handler getChanHandler() {
        return ensureHandler();
	}

    @Override
    public void refreshActivity() {
        invalidateOptionsMenu();
        createAbsListView();
        ensureHandler().sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
    }

}
