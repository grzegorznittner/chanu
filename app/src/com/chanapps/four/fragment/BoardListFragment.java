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
import com.chanapps.four.adapter.BoardSelectorGridCursorAdapter;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.TutorialOverlay;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.loader.BoardSelectorCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.viewer.BoardSelectorBoardsViewer;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

import java.lang.ref.WeakReference;

public class BoardListFragment
    extends Fragment
    implements
        AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        AbstractBoardCursorAdapter.ViewBinder
{

    private static final String TAG = BoardListFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    private ResourceCursorAdapter adapter;
    private View layout;
    private AbsListView absListView;
    private TextView emptyText;
    private int columnWidth = 0;
    //private int columnHeight = 0;

    protected Handler handler;
    protected Loader<Cursor> cursorLoader;

    public void refresh() {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    getLoaderManager().restartLoader(0, null, BoardListFragment.this);
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
    }

    protected void createAbsListView(View contentView) {
        absListView = (GridView)contentView.findViewById(R.id.board_grid_view);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(absListView, display, ChanGridSizer.ServiceType.SELECTOR);
        cg.sizeGridToDisplay();
        columnWidth = cg.getColumnWidth();
        //columnHeight = cg.getColumnHeight();
        adapter = new BoardSelectorGridCursorAdapter(getActivity().getApplicationContext(), this, columnWidth);//, columnHeight);
        absListView.setAdapter(adapter);
        absListView.setClickable(true);
        absListView.setOnItemClickListener(this);
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
            handler = new Handler();
        if (absListView.getCount() <= 0) {
            if (DEBUG) Log.i(TAG, "No data displayed, starting loader");
            getLoaderManager().restartLoader(0, null, this);
        }
        new TutorialOverlay(layout, TutorialOverlay.Page.BOARDLIST);
    }

    @Override
    public void onStop () {
    	super.onStop();
        handler = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        cursorLoader = new BoardSelectorCursorLoader(getBaseContext());
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
        if (data.getCount() > 0) {
            emptyText.setVisibility(View.GONE);
        }
        else {
            emptyText.setText(R.string.board_empty_default);
            emptyText.setVisibility(View.VISIBLE);
        }
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
        final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
        if ((threadFlags & ChanThread.THREAD_FLAG_TITLE) > 0
                && title != null && !title.isEmpty()
                && desc != null && !desc.isEmpty()) {
            (new GenericDialogFragment(title.replaceAll("<[^>]*>", " "), desc))
                    .show(activity.getSupportFragmentManager(), BoardListFragment.TAG);
            return;
        }
        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        if (DEBUG) Log.i(TAG, "clicked board " + boardCode);
        BoardActivity.startActivity(activity, boardCode, "");
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        return BoardSelectorBoardsViewer.setViewValue(view, cursor);
    }

}