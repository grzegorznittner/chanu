package com.chanapps.four.fragment;

import android.app.Activity;
import android.os.Message;
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
import com.chanapps.four.loader.*;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.FetchPopularThreadsService;
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
    private TextView emptyText;
    private int columnWidth = 0;
    private int columnHeight = 0;

    protected Handler handler;
    protected Loader<Cursor> cursorLoader;

    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;

    public BaseAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void refresh() {
        //setActionBarTitle(); // for update time
        //invalidateOptionsMenu(); // in case spinner needs to be reset
        if (handler != null)
            handler.sendEmptyMessageDelayed(0, 200);
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
        String selectorString = getArguments() != null
                ? getArguments().getString(BoardSelectorActivity.BOARD_SELECTOR_TAB)
                : null;
        boardSelectorTab = (selectorString != null && !selectorString.isEmpty())
                ? BoardSelectorTab.valueOf(selectorString)
                : BoardSelectorActivity.DEFAULT_BOARD_SELECTOR_TAB;
        if (DEBUG) Log.v(TAG, "BoardGroupFragment " + boardSelectorTab + " onCreate");
    }

    @Override                                             
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        switch (boardSelectorTab) {
            case WATCHLIST:
                ChanWatchlist.setWatchlistFragment(this);
                break;
            case RECENT:
                FetchPopularThreadsService.schedulePopularFetchWithPriority(getActivity().getApplicationContext());
            default:
                break;
        }
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
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
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
            case BOARDLIST:
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
                break;
            case WATCHLIST:
            case RECENT:
            default:
                adapter = new BoardGridCursorAdapter(getActivity(),
                        R.layout.board_grid_item,
                        this,
                        new String[] {
                                ChanThread.THREAD_BOARD_CODE,
                                ChanThread.THREAD_THUMBNAIL_URL,
                                ChanThread.THREAD_SUBJECT,
                                //ChanThread.THREAD_INFO,
                                ChanThread.THREAD_COUNTRY_FLAG_URL,
                                ChanThread.THREAD_NUM_REPLIES,
                                ChanThread.THREAD_NUM_IMAGES,
                                ChanThread.THREAD_INFO
                        },
                        new int[] {
                                R.id.grid_item_board_abbrev,
                                R.id.grid_item_thread_thumb,
                                R.id.grid_item_thread_subject,
                                //R.id.grid_item_thread_info,
                                R.id.grid_item_country_flag,
                                R.id.grid_item_num_replies,
                                R.id.grid_item_num_images,
                                R.id.grid_item_board_type_text
                        },
                        columnWidth,
                        columnHeight);
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (DEBUG) Log.d(TAG, "BoardGroupFragment " + boardSelectorTab + " onCreateView");
        View layout = inflater.inflate(R.layout.board_selector_grid_layout, container, false);
        emptyText = (TextView)layout.findViewById(R.id.board_empty_text);
        createAbsListView(layout);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume boardSelectorTab=" + boardSelectorTab);
        if (handler == null)
            handler = createHandler();
    }

    protected Handler createHandler() {
        if (DEBUG) Log.i(TAG, "creating handler");
        return new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                switch (msg.what) {
                    default:
                        getLoaderManager().restartLoader(0, null, BoardGroupFragment.this);
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't handle message " + msg, e);
            }
        }
        };
    }

    public Handler getChanHandler() {
        if (DEBUG) Log.i(TAG, "for tab=" + boardSelectorTab + " returning handler=" + handler);
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
    public void onDetach() {
        super.onDetach();
        handler = null;
    }

    protected Loader<Cursor> createCursorLoader() {
        if (DEBUG) Log.v(TAG, "createCursorLoader boardSelectorType=" + boardSelectorTab);
        switch (boardSelectorTab) {
            case BOARDLIST:
                return new BoardSelectorCursorLoader(getActivity());
            case WATCHLIST:
                return new BoardSelectorWatchlistCursorLoader(getActivity());
            case RECENT:
            default:
                return new BoardTypeRecentCursorLoader(getActivity());
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
        if ((threadFlags & ChanThread.THREAD_FLAG_AD) > 0
                || (threadFlags & ChanThread.THREAD_FLAG_BOARD_TYPE) > 0)
            return;

        final Activity activity = getActivity();
        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        switch (boardSelectorTab) {
            case BOARDLIST:
                if (DEBUG) Log.i(TAG, "clicked board " + boardCode);
                FetchChanDataService.scheduleBoardFetchWithPriority(getActivity(), boardCode); // get board ready
                BoardActivity.startActivity(activity, boardCode);
                break;
            case WATCHLIST:
            case RECENT:
            default:
                final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                if (DEBUG) Log.i(TAG, "clicked thread " + boardCode + "/" + threadNo);
                FetchChanDataService.scheduleThreadFetchWithPriority(getActivity(), boardCode, threadNo);
                FetchChanDataService.scheduleBoardFetchWithPriority(getActivity(), boardCode); // get board ready
                ThreadActivity.startActivity(getActivity(), boardCode, threadNo);
                break;
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
                return setBoardlistViewValue(view, cursor, columnIndex);
            case WATCHLIST:
            case RECENT:
            default:
                return BoardActivity.setViewValue(view, cursor, columnIndex, imageLoader, displayImageOptions, boardSelectorTab.boardCode());
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
        int threadFlags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((threadFlags & ChanThread.THREAD_FLAG_BOARD_TYPE) > 0) {
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
        int threadFlags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((threadFlags & ChanThread.THREAD_FLAG_BOARD_TYPE) > 0) {
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
        int threadFlags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((threadFlags & ChanThread.THREAD_FLAG_BOARD_TYPE) > 0) {
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
