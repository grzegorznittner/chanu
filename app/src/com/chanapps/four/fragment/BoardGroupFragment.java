package com.chanapps.four.fragment;

import android.app.Activity;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
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
    private static final boolean DEBUG = false;

    private BoardSelectorTab boardSelectorTab;
    private ResourceCursorAdapter adapter;
    private AbsListView absListView;
    //private ProgressBar progressBar;
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
    public void refresh() {
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
        boardSelectorTab = getArguments() != null
                ? BoardSelectorTab.valueOf(getArguments().getString(BoardSelectorActivity.BOARD_SELECTOR_TAB))
                : BoardSelectorActivity.DEFAULT_BOARD_SELECTOR_TAB;
        if (DEBUG) Log.v(TAG, "BoardGroupFragment " + boardSelectorTab + " onCreate");
        if (boardSelectorTab == BoardSelectorTab.WATCHLIST)
            ChanWatchlist.setWatchlistFragment(BoardGroupFragment.this);
    }

    @Override                                             
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        
        ImageSize imageSize = new ImageSize(columnWidth, columnHeight); // view pager needs micro images
        imageLoader = ChanImageLoader.getInstance(getActivity().getApplicationContext());
        displayImageOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.POWER_OF_2)
                .imageSize(imageSize)
                .cacheOnDisc()
                .cacheInMemory()
                .resetViewBeforeLoading()
                .build();
        
        assignCursorAdapter();
        absListView.setAdapter(adapter);
        absListView.setClickable(true);
        absListView.setOnItemClickListener(this);
        absListView.setLongClickable(true);
        absListView.setOnItemLongClickListener(this);
    }

    protected void assignCursorAdapter() {
        switch (boardSelectorTab) {
            case WATCHLIST:
                adapter = new BoardGridCursorAdapter(getActivity(),
                        R.layout.board_grid_item,
                        this,
                        new String[] {
                                ChanThread.THREAD_THUMBNAIL_URL,
                                ChanThread.THREAD_SUBJECT,
                                //ChanThread.THREAD_INFO,
                                ChanThread.THREAD_COUNTRY_FLAG_URL,
                                ChanThread.THREAD_NUM_REPLIES,
                                ChanThread.THREAD_NUM_IMAGES},
                        new int[] {
                                R.id.grid_item_thread_thumb,
                                R.id.grid_item_thread_subject,
                                //R.id.grid_item_thread_info,
                                R.id.grid_item_country_flag,
                                R.id.grid_item_num_replies,
                                R.id.grid_item_num_images},
                        columnWidth,
                        columnHeight);
                break;
            case BOARDLIST:
            default:
                adapter = new BoardSelectorGridCursorAdapter(getActivity(),
                        R.layout.board_selector_grid_item,
                        this,
                        new String[] {
                                ChanThread.THREAD_THUMBNAIL_URL,
                                ChanThread.THREAD_SUBJECT,
                                ChanThread.THREAD_INFO
                        },
                        new int[] {
                                R.id.grid_item_thread_thumb,
                                R.id.grid_item_thread_subject,
                                R.id.grid_item_board_type_text
                        },
                        columnWidth,
                        columnHeight);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (DEBUG) Log.d(TAG, "BoardGroupFragment " + boardSelectorTab + " onCreateView");
        View layout = inflater.inflate(R.layout.board_selector_grid_layout, container, false);
        if (boardSelectorTab == BoardSelectorTab.WATCHLIST)
            emptyWatchlistText = (TextView)layout.findViewById(R.id.board_empty_watchlist);
        createAbsListView(layout);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        handler = new Handler();
        if (DEBUG) Log.i(TAG, "boardSelectorTab=" + boardSelectorTab + " reloadNextTime=" + reloadNextTime);
        if (boardSelectorTab == BoardSelectorTab.WATCHLIST && reloadNextTime) {
            reloadNextTime = false;
            adapter.notifyDataSetChanged();
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
        if (DEBUG) Log.v(TAG, "createCursorLoader boardSelectorType=" + boardSelectorTab);
        switch (boardSelectorTab) {
            case WATCHLIST:
                return new BoardSelectorWatchlistCursorLoader(getActivity());
            case BOARDLIST:
            default:
                return new BoardSelectorCursorLoader(getActivity());
        }
    }

    private void setProgressOn(boolean progressOn) {
        if (getActivity() != null)
            getActivity().setProgressBarIndeterminateVisibility(progressOn);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader boardSelectorType=" + boardSelectorTab);
        cursorLoader = createCursorLoader();
        setProgressOn(true);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoadFinished boardSelectorType=" + boardSelectorTab);
        adapter.swapCursor(data);
        if (boardSelectorTab == BoardSelectorTab.WATCHLIST)
            if (data.getCount() <= 0)
                emptyWatchlistText.setVisibility(View.VISIBLE);
            else
                emptyWatchlistText.setVisibility(View.GONE);
        setProgressOn(false);
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
        String boardTypeText = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_INFO));
        if (boardTypeText.equals("|||BOARD_TYPE|||"))
            return;

        final Activity activity = getActivity();
        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        switch (boardSelectorTab) {
            case WATCHLIST:
                final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                if (DEBUG) Log.i(TAG, "clicked thread " + boardCode + "/" + threadNo);
                ThreadActivity.startActivity(getActivity(), boardCode, threadNo);
                break;
            case BOARDLIST:
            default:
                if (DEBUG) Log.i(TAG, "clicked board " + boardCode);
                BoardActivity.startActivity(activity, boardCode);
        }
        ChanHelper.simulateClickAnim(activity, view);
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
                    WatchlistDeleteDialogFragment d = new WatchlistDeleteDialogFragment(handler, thread.posts[0].tim);
                    d.show(getFragmentManager(), WatchlistDeleteDialogFragment.TAG);
                    return true;
                }
                else {
                    return false;
                }
            case BOARDLIST:
            default:
                return false;
        }
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        switch (boardSelectorTab) {
            case WATCHLIST:
                return BoardActivity.setViewValue(view, cursor, columnIndex, imageLoader, displayImageOptions);
            case BOARDLIST:
            default:
                return setBoardlistViewValue(view, cursor, columnIndex);
        }
    }

    protected boolean setBoardlistViewValue(View view, Cursor cursor, int columnIndex) {
        switch (view.getId()) {
            case R.id.grid_item_thread_subject:
                return setThreadSubject((TextView) view, cursor);
            case R.id.grid_item_board_type_text:
                return setBoardTypeText((TextView) view, cursor);
            case R.id.grid_item_thread_thumb:
                return setThreadThumb((ImageView) view, cursor);
            case R.id.grid_item_country_flag:
                return setThreadCountryFlag((ImageView) view, cursor);
        }
        return false;
    }

    protected boolean setThreadSubject(TextView tv, Cursor cursor) {
        String boardTypeText = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_INFO));
        if (boardTypeText.equals("|||BOARD_TYPE|||")) {
            tv.setVisibility(View.GONE);
            tv.setText("");
        }
        else {
            tv.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT))));
            tv.setVisibility(View.VISIBLE);
        }
        return true;
    }

    protected boolean setBoardTypeText(TextView tv, Cursor cursor) {
        String boardTypeText = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_INFO));
        if (boardTypeText.equals("|||BOARD_TYPE|||")) {
            tv.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT))));
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.GONE);
            tv.setText("");
        }
        return true;
    }

    protected boolean setThreadThumb(ImageView iv, Cursor cursor) {
        String boardTypeText = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_INFO));
        if (boardTypeText.equals("|||BOARD_TYPE|||")) {
            iv.setImageBitmap(null);
        }
        else {
            imageLoader.displayImage(
                    cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL)),
                    iv,
                    //displayImageOptions); // load async
                    displayImageOptions.modifyCenterCrop(true)); // load async
            }
        return true;
    }

    protected boolean setThreadCountryFlag(ImageView iv, Cursor cursor) {
        imageLoader.displayImage(
                cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_COUNTRY_FLAG_URL)),
                iv,
                displayImageOptions);
        return true;
    }

}
