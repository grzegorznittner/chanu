package com.chanapps.four.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.adapter.BoardCursorAdapter;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.handler.LoaderHandler;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.service.BoardLoadService;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class BoardActivity extends Activity implements ClickableLoaderActivity, AbsListView.OnScrollListener {
	public static final String TAG = BoardActivity.class.getSimpleName();
    protected static final int LOADER_RESTART_INTERVAL_MS = 5000;
    protected static final int LOADER_REFRESH_DELAY_MS = 1000;

    protected BoardCursorAdapter adapter;
    protected PullToRefreshGridView gridView;
    protected Handler handler;
    protected BoardCursorLoader cursorLoader;
    protected int prevTotalItemCount = 0;
    protected int scrollOnNextLoaderFinished = 0;
    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;
    protected SharedPreferences prefs;

    protected View popupView;
    protected TextView popupText;
    protected PopupWindow popupWindow;
    protected Button replyButton;
    protected Button quoteButton;
    protected Button dismissButton;

    protected long tim;
    protected String boardCode;
    private int pageNo = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "************ onCreate");
        super.onCreate(savedInstanceState);
        loadFromIntentOrPrefs();
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
        Log.v(TAG, "onCreate init loader");
        getLoaderManager().initLoader(0, null, this);
    }

    protected void sizeGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(gridView.getRefreshableView(), display, ChanGridSizer.ServiceType.BOARD);
        cg.sizeGridToDisplay();
    }

    protected void initGridAdapter() {
        adapter = new BoardCursorAdapter(this,
                R.layout.board_grid_item,
                this,
                new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_SHORT_TEXT},
                new int[] {R.id.board_activity_grid_item_image, R.id.board_activity_grid_item_text});
        gridView.getRefreshableView().setAdapter(adapter);
    }

    protected void createGridView() {
        setContentView(R.layout.board_grid_layout);
        gridView = (PullToRefreshGridView)findViewById(R.id.board_activity_grid_view);
        sizeGridToDisplay();
        initGridAdapter();
        gridView.setClickable(true);
        gridView.getRefreshableView().setOnItemClickListener(this);
        gridView.setLongClickable(true);
        gridView.getRefreshableView().setOnItemLongClickListener(this);
        gridView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<GridView>() {
            @Override
            public void onRefresh(PullToRefreshBase<GridView> refreshView) {
                reloadBoard();
            }
        });
        gridView.setOnScrollListener(this);
    }

    protected void ensureHandler() {
        if (handler == null) {
            handler = new LoaderHandler(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
		Log.v(TAG, "onStart");
    }

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
        restoreInstanceState();
	}

    protected void reloadBoard() {
        pageNo = 0;
        startLoadService();
    }

    public PullToRefreshGridView getGridView() {
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
        Log.v(TAG, "Scrolling to:" + lastPosition);
    }

    @Override
	public void onWindowFocusChanged (boolean hasFocus) {
		Log.v(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
        saveInstanceState();
    }

    protected void ensurePrefs() {
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    protected void loadFromIntentOrPrefs() {
        ensurePrefs();
        Intent intent = getIntent();
        boardCode = intent.hasExtra(ChanHelper.BOARD_CODE)
                ? intent.getStringExtra(ChanHelper.BOARD_CODE)
                : prefs.getString(ChanHelper.BOARD_CODE, "a");
        pageNo = 0;
    }

    protected void restoreInstanceState() {
        loadFromIntentOrPrefs();
        startLoadService();
        setActionBarTitle();
        scrollToLastPosition();
    }

    protected void startLoadService() {
        Log.i(TAG, "Start board load service for " + boardCode + " page " + pageNo );
        Intent intent = new Intent(this, BoardLoadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.PAGE, pageNo);
        startService(intent);
    }

    protected void saveInstanceState() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(ChanHelper.BOARD_CODE, boardCode);
        editor.putLong(ChanHelper.THREAD_NO, 0);
        editor.putInt(ChanHelper.LAST_BOARD_POSITION, gridView.getRefreshableView().getFirstVisiblePosition());
        editor.commit();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    protected void onStop () {
    	super.onStop();
    	Log.v(TAG, "onStop");
    	getLoaderManager().destroyLoader(0);
    	handler = null;
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
		Log.v(TAG, "onDestroy");
		getLoaderManager().destroyLoader(0);
		handler = null;
	}

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        final int loadPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOAD_PAGE));
        final int lastPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LAST_PAGE));
        String shortText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT));
        String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        if (view instanceof TextView) {
            //todo - @john - if the text is hidden then the image should take the full available space.
            TextView tv = (TextView) view;
            if (loadPage > 0 || lastPage > 0) {
                // nothing to set
            }
            else if (shortText == null || shortText.isEmpty()) {
                tv.setVisibility(View.INVISIBLE);
            }
            else {
                tv.setText(shortText);
            }
            return true;
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            if (loadPage > 0) {
                Animation rotation = AnimationUtils.loadAnimation(this, R.animator.clockwise_refresh);
                rotation.setRepeatCount(Animation.INFINITE);
                iv.startAnimation(rotation);
            }
            else if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    this.imageLoader.displayImage(imageUrl, iv, displayImageOptions);
                } catch (NumberFormatException nfe) {
                    iv.setImageURI(Uri.parse(imageUrl));
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        cursorLoader = new BoardCursorLoader(this, boardCode, pageNo);
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.v(TAG, ">>>>>>>>>>> onLoadFinished");
		adapter.swapCursor(data);
        ensureHandler();
        Message m = Message.obtain(handler, LoaderHandler.RESTART_LOADER_MSG);
        handler.sendMessageDelayed(m, LOADER_RESTART_INTERVAL_MS);
        Message m2 = Message.obtain(handler, LoaderHandler.REFRESH_COMPLETE_MSG);
        handler.sendMessageDelayed(m2, LOADER_REFRESH_DELAY_MS);
        if (gridView != null) {
//            gridView.onRefreshComplete();
            if (scrollOnNextLoaderFinished > 0) {
                gridView.getRefreshableView().setSelection(scrollOnNextLoaderFinished);
                scrollOnNextLoaderFinished = 0;
            }
        }
	}

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.v(TAG, ">>>>>>>>>>> onLoaderReset");
		adapter.swapCursor(null);
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final int loadPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOAD_PAGE));
        final int lastPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LAST_PAGE));
        if (loadPage == 0 && lastPage == 0)
            ThreadActivity.startActivity(this, adapterView, view, position, id);
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
            case R.id.new_thread_menu:
                Intent replyIntent = new Intent(this, PostReplyActivity.class);
                replyIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                startActivity(replyIntent);
                return true;
            case R.id.prefetch_board_menu:
                Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.settings_menu:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.raw.help_header, R.raw.help_board_grid);
                rawResourceDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_menu, menu);
        return true;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Log.d(TAG, "onScroll firstVisibleItem=" + firstVisibleItem + " visibleItemCount=" + visibleItemCount + " totalItemCount=" + totalItemCount + " prevTotalItemCount=" + prevTotalItemCount);
        if (adapter != null
                && !adapter.isEmpty()
                && adapter.getCount() > 2
                && (firstVisibleItem + visibleItemCount) >= totalItemCount
                && totalItemCount != prevTotalItemCount)
        {
            Log.v(TAG, "onListEnd, extending list");
            prevTotalItemCount = totalItemCount;
            Toast.makeText(this, "Fetching next page...", Toast.LENGTH_SHORT).show();
            pageNo++;
            startLoadService();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int s) {
    }

    protected void setActionBarTitle() {
        ActionBar a = getActionBar();
        if (a == null) {
            return;
        }
        String title = "/" + boardCode + " " + getString(R.string.board_activity);
        a.setTitle(title);
        a.setDisplayHomeAsUpEnabled(true);
    }

    protected void ensurePopupWindow() {
        if (popupView == null) {
            popupView = getLayoutInflater().inflate(R.layout.popup_full_text_layout, null);
            popupText = (TextView)popupView.findViewById(R.id.popup_full_text);
            popupWindow = new PopupWindow (popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            replyButton = (Button)popupView.findViewById(R.id.popup_reply_button);
            quoteButton = (Button)popupView.findViewById(R.id.popup_quote_button);
            dismissButton = (Button)popupView.findViewById(R.id.popup_dismiss_button);
            dismissButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popupWindow.dismiss();
                }
            });
        }
    }

    public boolean showPopupText(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        Log.i(TAG, "Calling popup with id=" + id);
        if (text != null && !text.trim().isEmpty()) {
            final String clickedBoardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
            final long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
            final long resto = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_RESTO));
            final long clickedThreadNo = resto == 0 ? postId : resto;
            ensurePopupWindow();
            popupText.setText(text);
            replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent replyIntent = new Intent(getApplicationContext(), PostReplyActivity.class);
                    replyIntent.putExtra(ChanHelper.BOARD_CODE, clickedBoardCode);
                    replyIntent.putExtra(ChanHelper.THREAD_NO, clickedThreadNo);
                    startActivity(replyIntent);
                    popupWindow.dismiss();
                }
            });
            quoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent replyIntent = new Intent(getApplicationContext(), PostReplyActivity.class);
                    replyIntent.putExtra(ChanHelper.BOARD_CODE, clickedBoardCode);
                    replyIntent.putExtra(ChanHelper.THREAD_NO, clickedThreadNo);
                    replyIntent.putExtra(ChanHelper.TEXT, text);
                    startActivity(replyIntent);
                    popupWindow.dismiss();
                }
            });
            popupWindow.showAtLocation(adapterView, Gravity.CENTER, 0, 0);
            return true;
        }
        else {
            return false;
        }
    }

}
