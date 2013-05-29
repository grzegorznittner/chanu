package com.chanapps.four.fragment;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.BoardSelectorCursorLoader;
import com.chanapps.four.loader.BoardTypeRecentCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.viewer.BoardGridViewer;
import com.chanapps.four.viewer.BoardSelectorBoardsViewer;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

import java.lang.ref.WeakReference;

public class WatchlistFragment
    extends Fragment
    implements
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        AbstractBoardCursorAdapter.ViewBinder
{

    private static final String TAG = WatchlistFragment.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static WeakReference<WatchlistFragment> watchlistFragmentRef = null;
    private ResourceCursorAdapter adapter;
    private View layout;
    private AbsListView absListView;
    private TextView emptyText;
    private int columnWidth = 0;
    private int columnHeight = 0;

    protected Handler handler;
    protected Loader<Cursor> cursorLoader;

    public void refresh() {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    getLoaderManager().restartLoader(0, null, WatchlistFragment.this);
                }
            });
    }

    public void backgroundRefresh() {
        Handler handler = NetworkProfileManager.instance().getActivity().getChanHandler();
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    adapter.swapCursor(null);
                }
            });
    }

    public Context getBaseContext() {
        return getActivity().getBaseContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWatchlist(this);
    }

    protected static void setWatchlist(WatchlistFragment fragment) {
        synchronized (WatchlistFragment.class) {
            watchlistFragmentRef = new WeakReference<WatchlistFragment>(fragment);
        }
    }

    public static void refreshWatchlist() {
        synchronized (WatchlistFragment.class) {
            WatchlistFragment fragment;
            if (watchlistFragmentRef != null && (fragment = watchlistFragmentRef.get()) != null) {
                ChanActivityId activity = NetworkProfileManager.instance().getActivityId();
                if (activity != null && activity.activity == ChanHelper.LastActivity.BOARD_SELECTOR_ACTIVITY)
                    fragment.refresh();
                else
                    fragment.backgroundRefresh();
            }
        }
    }

    protected void createAbsListView(View contentView) {
        absListView = (GridView)contentView.findViewById(R.id.board_grid_view);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(absListView, display, ChanGridSizer.ServiceType.SELECTOR);
        cg.sizeGridToDisplay();
        columnWidth = cg.getColumnWidth();
        columnHeight = cg.getColumnHeight();
        adapter = new BoardGridCursorAdapter(getActivity().getApplicationContext(), this, columnWidth, columnHeight);
        absListView.setAdapter(adapter);
        absListView.setClickable(true);
        absListView.setOnItemClickListener(this);
        absListView.setLongClickable(true);
        absListView.setOnItemLongClickListener(this);
        ImageLoader imageLoader = ChanImageLoader.getInstance(getBaseContext());
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        layout = inflater.inflate(R.layout.board_selector_grid_layout, container, false);
        emptyText = (TextView)layout.findViewById(R.id.board_empty_text);
        createAbsListView(layout);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (handler == null)
            handler = new Handler();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart");
        if (handler == null)
            handler = new Handler();
        if (absListView.getCount() <= 0) {
            if (DEBUG) Log.i(TAG, "No data displayed, starting loader");
            getLoaderManager().restartLoader(0, null, this);
        }
        new TutorialOverlay(layout, TutorialOverlay.Page.WATCHLIST);
    }

    @Override
    public void onStop () {
    	super.onStop();
        handler = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        cursorLoader = new BoardCursorLoader(getBaseContext(), ChanBoard.WATCHLIST_BOARD_CODE, "");
        getActivity().setProgressBarIndeterminateVisibility(true);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
        if (data.getCount() > 0) {
            emptyText.setVisibility(View.GONE);
        }
        else {
            emptyText.setText(R.string.board_empty_watchlist);
            emptyText.setVisibility(View.VISIBLE);
        }
        getActivity().setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
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
                    .show(activity.getSupportFragmentManager(), WatchlistFragment.TAG);
            return;
        }

        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
        if (DEBUG) Log.i(TAG, "clicked thread " + boardCode + "/" + threadNo);
        ThreadActivity.startActivity(getActivity(), boardCode, threadNo, "");
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = (Cursor) parent.getItemAtPosition(position);
        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
        ChanThread thread = ChanFileStorage.loadThreadData(getActivity(), boardCode, threadNo);
        if (thread != null && thread.posts != null && thread.posts[0] != null && thread.posts[0].tim > 0) {
            WatchlistDeleteDialogFragment d = new WatchlistDeleteDialogFragment(handler, thread);
            d.show(getFragmentManager(), WatchlistDeleteDialogFragment.TAG);
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        return BoardGridViewer.setViewValue(view, cursor, ChanBoard.WATCHLIST_BOARD_CODE);
    }

}
