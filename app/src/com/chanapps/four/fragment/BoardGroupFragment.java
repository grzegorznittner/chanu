package com.chanapps.four.fragment;

import android.app.Activity;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.chanapps.four.activity.*;
import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.adapter.BoardSelectorGridCursorAdapter;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.data.*;
import com.chanapps.four.loader.BoardSelectorCursorLoader;
import com.chanapps.four.loader.BoardSelectorWatchlistCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;

/**
* User: arley
*/
public class BoardGroupFragment
    extends Fragment
    implements RefreshableActivity,
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        AbstractBoardCursorAdapter.ViewBinder
{

    private static final String TAG = BoardGroupFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    private ChanBoard.Type boardType;
    private ResourceCursorAdapter adapter;
    private AbsListView absListView;
    private ProgressBar progressBar;
    private TextView emptyWatchlistText;
    private int columnWidth = 0;
    private int columnHeight = 0;

    public boolean reloadNextTime = false;

    protected Handler handler;
    protected Loader<Cursor> cursorLoader;

    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;

    public BaseAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void refreshActivity() {
        // ignored
    }

    public void invalidate() {
        // ignored
    }

    @Override
    public Context getBaseContext() {
        return getActivity().getBaseContext();
    }

    @Override
    public FragmentManager getSupportFragmentManager() {
        return getActivity().getSupportFragmentManager();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boardType = getArguments() != null
                ? ChanBoard.Type.valueOf(getArguments().getString(ChanHelper.BOARD_TYPE))
                : ChanBoard.Type.JAPANESE_CULTURE;
        if (DEBUG) Log.v(TAG, "BoardGroupFragment " + boardType + " onCreate");
        if (boardType == ChanBoard.Type.WATCHLIST)
            ChanWatchlist.setWatchlistFragment(BoardGroupFragment.this);
    }

    private static final int SELECTOR_WIDTH_PX = 150;
    private static final int SELECTOR_HEIGHT_PX = 150;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ImageSize imageSize = new ImageSize(SELECTOR_WIDTH_PX, SELECTOR_HEIGHT_PX); // view pager needs micro images
        imageLoader = ChanImageLoader.getInstance(getActivity().getApplicationContext());
        displayImageOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.POWER_OF_2)
                .imageSize(imageSize)
                .cacheOnDisc()
                .resetViewBeforeLoading()
                .build();
        ensureHandler();
        LoaderManager.enableDebugLogging(true);
        getLoaderManager().initLoader(0, null, this);
        if (DEBUG) Log.v(TAG, "onCreate init loader");
    }

    protected void createAbsListView(View contentView) {
        absListView = (GridView)contentView.findViewById(R.id.board_grid_view);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(absListView, display, ChanGridSizer.ServiceType.SELECTOR);
        cg.sizeGridToDisplay();
        columnWidth = cg.getColumnWidth();
        columnHeight = cg.getColumnHeight();
        assignCursorAdapter();
        absListView.setAdapter(adapter);
        absListView.setClickable(true);
        absListView.setOnItemClickListener(this);
        absListView.setLongClickable(true);
        absListView.setOnItemLongClickListener(this);
    }

    protected void assignCursorAdapter() {
        if (boardType == ChanBoard.Type.WATCHLIST)
            adapter = new BoardGridCursorAdapter(getActivity(),
                    R.layout.board_grid_item,
                    this,
                    new String[] {
                            ChanThread.THREAD_THUMBNAIL_URL,
                            ChanThread.THREAD_SUBJECT,
                            ChanThread.THREAD_INFO,
                            ChanThread.THREAD_COUNTRY_FLAG_URL },
                    new int[] {
                            R.id.grid_item_thread_thumb,
                            R.id.grid_item_thread_subject,
                            R.id.grid_item_thread_info,
                            R.id.grid_item_country_flag},
                    columnWidth,
                    columnHeight);
        else
            adapter = new BoardSelectorGridCursorAdapter(getActivity(),
                    R.layout.board_selector_grid_item,
                    this,
                    new String[] {
                            ChanThread.THREAD_THUMBNAIL_URL,
                            ChanThread.THREAD_SUBJECT },
                    new int[] {
                            R.id.grid_item_thread_thumb,
                            R.id.grid_item_thread_subject},
                    columnWidth,
                    columnHeight);
    }

    protected synchronized Handler ensureHandler() {
        if (handler == null) {
            if (ChanHelper.onUIThread()) {
                handler = new LoaderHandler();
            }
            else {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handler = new LoaderHandler();
                    }
                });
            }
        }
        return handler;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (DEBUG) Log.d(TAG, "BoardGroupFragment " + boardType + " onCreateView");
        View layout = inflater.inflate(R.layout.board_selector_grid_layout, container, false);
        progressBar = (ProgressBar)layout.findViewById(R.id.board_progress_bar);
        if (boardType == ChanBoard.Type.WATCHLIST)
            emptyWatchlistText = (TextView)layout.findViewById(R.id.board_empty_watchlist);
        createAbsListView(layout);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "boardType=" + boardType + " reloadNextTime=" + reloadNextTime);
        if (boardType == ChanBoard.Type.WATCHLIST && reloadNextTime) {
            reloadNextTime = false;
            ensureHandler().sendEmptyMessageDelayed(0, 10);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop () {
    	super.onStop();
        handler = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        handler = null;
    }

    protected Loader<Cursor> createCursorLoader() {
        if (DEBUG) Log.v(TAG, "createCursorLoader type=" + boardType);
        if (boardType == ChanBoard.Type.WATCHLIST) {
            return new BoardSelectorWatchlistCursorLoader(getActivity());
        }
        else {
            return new BoardSelectorCursorLoader(getActivity(), boardType);
        }
    }

    private void setProgressOn(boolean progressOn) {
        if (getActivity() != null)
            getActivity().setProgressBarIndeterminateVisibility(progressOn);
        if (progressBar != null) {
            if (progressOn)
                progressBar.setVisibility(View.VISIBLE);
            else
                progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader type=" + boardType);
        cursorLoader = createCursorLoader();
        setProgressOn(true);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoadFinished type=" + boardType);
        adapter.swapCursor(data);
        if (boardType == ChanBoard.Type.WATCHLIST)
            if (data.getCount() <= 0)
                emptyWatchlistText.setVisibility(View.VISIBLE);
            else
                emptyWatchlistText.setVisibility(View.GONE);
        setProgressOn(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoaderReset type=" + boardType);
        adapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (DEBUG) Log.i(TAG, "clicked item boardType=" + boardType);
        final Activity activity = getActivity();
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        if (boardType == ChanBoard.Type.WATCHLIST) {
            final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
            if (DEBUG) Log.i(TAG, "clicked thread " + boardCode + "/" + threadNo);
            //ChanThread thread = ChanFileStorage.loadThreadData(getActivity(), boardCode, threadNo);
            //if (thread != null) {
            //    ThreadActivity.startActivity(getActivity(), thread, view, threadNo, true);
            //}
            //else {
                ThreadActivity.startActivity(getActivity(), boardCode, threadNo);
            //}
        }
        else {
            if (DEBUG) Log.i(TAG, "clicked board " + boardCode);
            BoardActivity.startActivity(activity, boardCode);
        }
        ChanHelper.fadeout(activity, view);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (boardType == ChanBoard.Type.WATCHLIST) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
            final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
            if (DEBUG) Log.i(TAG, "Long click " + boardType + " /" + boardCode + "/" + threadNo);
            ChanThread thread = ChanFileStorage.loadThreadData(getActivity(), boardCode, threadNo);
            if (thread != null && thread.posts != null && thread.posts[0] != null && thread.posts[0].tim > 0) {
                WatchlistDeleteDialogFragment d = new WatchlistDeleteDialogFragment(ensureHandler(), thread.posts[0].tim);
                d.show(getFragmentManager(), WatchlistDeleteDialogFragment.TAG);
                return true;
            }
        }
        return false;
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
                return setThreadCountryFlag((ImageView) view, cursor);
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

    protected boolean setThreadCountryFlag(ImageView iv, Cursor cursor) {
        imageLoader.displayImage(
                cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_COUNTRY_FLAG_URL)),
                iv,
                displayImageOptions);
        return true;
    }

    public class LoaderHandler extends Handler {

        private final String TAG = LoaderHandler.class.getSimpleName();
        private static final boolean DEBUG = false;

        public LoaderHandler() {}
        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                switch (msg.what) {
                    default:
                        if (DEBUG) Log.i(getClass().getSimpleName(), ">>>>>>>>>>> restart message received restarting loader");
                        getLoaderManager().restartLoader(0, null, BoardGroupFragment.this);
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't handle message " + msg, e);
            }
        }
    }
}
