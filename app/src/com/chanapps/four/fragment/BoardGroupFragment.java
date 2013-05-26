package com.chanapps.four.fragment;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.chanapps.four.activity.*;
import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.adapter.BoardSelectorGridCursorAdapter;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.TutorialOverlay;
import com.chanapps.four.data.*;
import com.chanapps.four.loader.*;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.viewer.BoardGridViewer;
import com.chanapps.four.viewer.BoardSelectorBoardsViewer;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

import java.lang.ref.WeakReference;

/**
* User: arley
*/
public class BoardGroupFragment
    extends Fragment
    implements
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        AbstractBoardCursorAdapter.ViewBinder
{

    private static final String TAG = BoardGroupFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private BoardSelectorTab boardSelectorTab;
    private ResourceCursorAdapter adapter;
    private View layout;
    private AbsListView absListView;
    private TextView emptyText;
    private int columnWidth = 0;
    private int columnHeight = 0;

    protected Handler handler;
    protected Loader<Cursor> cursorLoader;

    protected static boolean scheduledWatchlistRefresh = false;

    public static void scheduleWatchlistRefresh() {
        scheduledWatchlistRefresh = true;
    }

    public void refresh() {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    getLoaderManager().restartLoader(0, null, BoardGroupFragment.this);
                }
            });
    }

    public Context getBaseContext() {
        return getActivity().getBaseContext();
    }
    /*
    @Override
    public FragmentManager getSupportFragmentManager() {
        return getActivity().getSupportFragmentManager();
    }
    */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String selectorString = getArguments() != null
                ? getArguments().getString(BoardSelectorActivity.BOARD_SELECTOR_TAB)
                : null;
        boardSelectorTab = (selectorString != null && !selectorString.isEmpty())
                ? BoardSelectorTab.valueOf(selectorString)
                : BoardSelectorActivity.DEFAULT_BOARD_SELECTOR_TAB;
        setHasOptionsMenu(true);
        if (DEBUG) Log.v(TAG, "BoardGroupFragment " + boardSelectorTab + " onCreate");
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
        ImageLoader imageLoader = ChanImageLoader.getInstance(getBaseContext());
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
    }

    protected void assignCursorAdapter() {
        switch (boardSelectorTab) {
            default:
            case BOARDLIST:
                adapter = new BoardSelectorGridCursorAdapter(getActivity().getApplicationContext(), this, columnWidth, columnHeight);
                break;
            case WATCHLIST:
            case RECENT:
                adapter = new BoardGridCursorAdapter(getActivity().getApplicationContext(), this, columnWidth, columnHeight);
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (DEBUG) Log.d(TAG, "BoardGroupFragment " + boardSelectorTab + " onCreateView");
        layout = inflater.inflate(R.layout.board_selector_grid_layout, container, false);
        emptyText = (TextView)layout.findViewById(R.id.board_empty_text);
        createAbsListView(layout);
        return layout;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (DEBUG) Log.i(TAG, "onPrepareOptionsMenu tab=" + boardSelectorTab);
        MenuItem rules = menu.findItem(R.id.global_rules_menu);
        MenuItem refresh = menu.findItem(R.id.refresh_menu);
        MenuItem clear = menu.findItem(R.id.clear_watchlist_menu);
        switch (boardSelectorTab) {
            default:
            case BOARDLIST:
                rules.setVisible(true);
                refresh.setVisible(false);
                clear.setVisible(false);
                break;
            case RECENT:
                rules.setVisible(false);
                refresh.setVisible(true);
                clear.setVisible(false);
                break;
            case WATCHLIST:
                rules.setVisible(false);
                refresh.setVisible(false);
                clear.setVisible(true);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume boardSelectorTab=" + boardSelectorTab);
        if (handler == null)
            handler = createHandler();
        // to overcome bug in viewPager when tabs are rapidly switched
        // if watchlist/nsfwboards need to be updated, loader should be called manually via handler
        /*
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null)
                    getActivity().invalidateOptionsMenu();
            }
        }, 250);
        */
        //if (boardSelectorTab == BoardSelectorTab.BOARDLIST) { // doesn't change except for NSFW switch
        //    //if (absListView != null && absListView.getCount() <= 0)
        //    //    getLoaderManager().restartLoader(0, null, this);
        //}
        //else {
        //    getLoaderManager().restartLoader(0, null, this);
        //}
        if (absListView.getCount() <= 0) {
            if (boardSelectorTab == BoardSelectorTab.WATCHLIST)
                scheduledWatchlistRefresh = false;
            if (DEBUG) Log.i(TAG, "No data displayed, starting loader");
            getLoaderManager().restartLoader(0, null, BoardGroupFragment.this);
        }
        else if (scheduledWatchlistRefresh) {
            if (boardSelectorTab == BoardSelectorTab.WATCHLIST)
                scheduledWatchlistRefresh = false;
            if (DEBUG) Log.i(TAG, "Refresh scheduled, starting loader");
            getLoaderManager().restartLoader(0, null, BoardGroupFragment.this);
        }
        if (boardSelectorTab == BoardSelectorTab.BOARDLIST)
            new TutorialOverlay(layout, TutorialOverlay.Page.BOARDLIST);
    }

    protected Handler createHandler() {
        if (DEBUG) Log.i(TAG, "creating handler");
        return new Handler();
    }

    public Handler getChanHandler() {
        return handler;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart");
        if (handler == null)
            handler = createHandler();
    }

    @Override
    public void onStop () {
    	super.onStop();
        handler = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.i(TAG, "onDestroy tab=" + boardSelectorTab);
        if (boardSelectorTab == BoardSelectorTab.WATCHLIST)
            scheduledWatchlistRefresh = false;
    }


    protected Loader<Cursor> createCursorLoader() {
        if (DEBUG) Log.v(TAG, "createCursorLoader boardSelectorType=" + boardSelectorTab);
        switch (boardSelectorTab) {
            case BOARDLIST:
                return new BoardSelectorCursorLoader(getBaseContext());
            case WATCHLIST:
                return new BoardCursorLoader(getBaseContext(), boardSelectorTab.boardCode(), "");
            case RECENT:
            default:
                return new BoardTypeRecentCursorLoader(getBaseContext());
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader boardSelectorType=" + boardSelectorTab);
        cursorLoader = createCursorLoader();
        if (getActivity() != null && boardSelectorTab != BoardSelectorTab.BOARDLIST)
            getActivity().setProgressBarIndeterminateVisibility(true);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoadFinished boardSelectorType=" + boardSelectorTab);
        adapter.swapCursor(data);
        if (data.getCount() > 0) {
            emptyText.setVisibility(View.GONE);
        }
        else {
            emptyText.setText(boardSelectorTab.emptyStringId());
            emptyText.setVisibility(View.VISIBLE);
        }
        if (getActivity() != null && boardSelectorTab != BoardSelectorTab.BOARDLIST)
            getActivity().setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoaderReset boardSelectorType=" + boardSelectorTab);
        adapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (DEBUG) Log.i(TAG, "clicked item on boardSelectorTab=" + boardSelectorTab);

        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        int threadFlags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((threadFlags & ChanThread.THREAD_FLAG_AD) > 0) {
            return;
        }
        final FragmentActivity activity = getActivity();

        final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TITLE));
        final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        if ((threadFlags & ChanThread.THREAD_FLAG_TITLE) > 0
                && title != null && !title.isEmpty()
                && desc != null && !desc.isEmpty()) {
            (new GenericDialogFragment(title.replaceAll("<[^>]*>", " "), desc))
                    .show(activity.getSupportFragmentManager(), BoardGroupFragment.TAG);
            return;
        }

        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        switch (boardSelectorTab) {
            case BOARDLIST:
                if (DEBUG) Log.i(TAG, "clicked board " + boardCode);
                BoardActivity.startActivity(activity, boardCode);
                break;
            case WATCHLIST:
            case RECENT:
            default:
                final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                if (DEBUG) Log.i(TAG, "clicked thread " + boardCode + "/" + threadNo);
                ThreadActivity.startActivity(getActivity(), boardCode, threadNo);
                break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        switch (boardSelectorTab) {
            case WATCHLIST:
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                if (DEBUG) Log.i(TAG, "Long click " + boardSelectorTab + " /" + boardCode + "/" + threadNo);
                ChanThread thread = ChanFileStorage.loadThreadData(getActivity(), boardCode, threadNo);
                if (thread != null && thread.posts != null && thread.posts[0] != null && thread.posts[0].tim > 0) {
                    WatchlistDeleteDialogFragment d = new WatchlistDeleteDialogFragment(handler, thread);
                    d.show(getFragmentManager(), WatchlistDeleteDialogFragment.TAG);
                    return true;
                }
                else {
                    return false;
                }
            case RECENT:
            case BOARDLIST:
            default:
                return false;
        }
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        switch (boardSelectorTab) {
            case BOARDLIST:
                return BoardSelectorBoardsViewer.setViewValue(view, cursor);
            case WATCHLIST:
            case RECENT:
            default:
                return BoardGridViewer.setViewValue(view, cursor, boardSelectorTab.boardCode());
        }
    }

    public BoardSelectorTab getBoardSelectorTab() {
        return boardSelectorTab;
    }
}
