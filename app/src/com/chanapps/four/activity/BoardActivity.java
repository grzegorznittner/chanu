package com.chanapps.four.activity;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.SearchManager;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.component.TutorialOverlay.Page;
import com.chanapps.four.data.*;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.GenericDialogFragment;
import com.chanapps.four.fragment.PickNewThreadBoardDialogFragment;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.BoardGridViewer;
import com.chanapps.four.viewer.ViewType;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

public class BoardActivity
        extends AbstractDrawerActivity
        implements ChanIdentifiedActivity,
        OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        AbstractBoardCursorAdapter.ViewBinder
{
	public static final String TAG = BoardActivity.class.getSimpleName();
	public static final boolean DEBUG = true;
    public static final int LOADER_RESTART_INTERVAL_SHORT_MS = 250;
    protected static final String FIRST_VISIBLE_POSITION = "firstVisiblePosition";
    protected static final String FIRST_VISIBLE_POSITION_OFFSET = "firstVisiblePositionOffset";

    protected AbstractBoardCursorAdapter adapter;
    protected View layout;
    protected GridView gridView;
    protected int columnWidth;
    protected int columnHeight;
    protected Handler handler;
    protected BoardCursorLoader cursorLoader;
    protected Menu menu;
    protected long tim;
    protected String boardCode;
    protected String query = "";
    protected MenuItem searchMenuItem;
    protected ViewType viewType = ViewType.AS_GRID;
    protected int firstVisiblePosition = -1;
    protected int firstVisiblePositionOffset = -1;

    public static void startActivity(Activity from, String boardCode, String query) {
        //if (query != null && !query.isEmpty())
        //    NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.SEARCH_BOARD);
        from.startActivity(createIntent(from, boardCode, query));
    }

    public static Intent createIntent(Context context, String boardCode, String query) {
        String intentBoardCode = boardCode == null || boardCode.isEmpty() ? ChanBoard.POPULAR_BOARD_CODE : boardCode;
        Intent intent = new Intent(context, BoardActivity.class);
        intent.putExtra(ChanBoard.BOARD_CODE, intentBoardCode);
        intent.putExtra(ChanHelper.PAGE, 0);
        intent.putExtra(SearchManager.QUERY, query);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    @Override
    public boolean isSelfBoard(String boardAsMenu) {
        return boardAsMenu != null && boardAsMenu.matches("/" + boardCode + "/.*") && (query == null || query.isEmpty());
    }

    @Override
    protected void createViews(Bundle bundle) {
        createAbsListView();
        if (bundle != null)
            onRestoreInstanceState(bundle);
        else
            setFromIntent(getIntent());
        if (boardCode == null || boardCode.isEmpty())
            boardCode = ChanBoard.META_BOARD_CODE;
        if (DEBUG) Log.i(TAG, "onCreate /" + boardCode + "/ q=" + query);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(ChanBoard.BOARD_CODE, boardCode);
        savedInstanceState.putString(SearchManager.QUERY, query);
        /*
        int pos = gridView == null ? -1 : gridView.getFirstVisiblePosition();
        View view = gridView == null ? null : gridView.getChildAt(0);
        int offset = view == null ? 0 : view.getTop();
        savedInstanceState.putInt(FIRST_VISIBLE_POSITION, pos);
        savedInstanceState.putInt(FIRST_VISIBLE_POSITION_OFFSET, offset);
        */
        if (DEBUG) Log.i(TAG, "onSaveInstanceState /" + boardCode + "/ q=" + query);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        boardCode = savedInstanceState.getString(ChanBoard.BOARD_CODE);
        query = savedInstanceState.getString(SearchManager.QUERY);
        //firstVisiblePosition = savedInstanceState.getInt(FIRST_VISIBLE_POSITION);
        //firstVisiblePositionOffset = savedInstanceState.getInt(FIRST_VISIBLE_POSITION_OFFSET);
        if (DEBUG) Log.i(TAG, "onRestoreInstanceState /" + boardCode + "/ q=" + query);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.i(TAG, "onNewIntent begin /" + intent.getStringExtra(ChanBoard.BOARD_CODE) + "/ q=" + query);
        setIntent(intent);
        setFromIntent(intent);
        if (DEBUG) Log.i(TAG, "onNewIntent end /" + boardCode + "/ q=" + query);
    }

    public void setFromIntent(Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
            query = intent.getStringExtra(SearchManager.QUERY);
            firstVisiblePosition = -1;
            firstVisiblePositionOffset = -1;
        }
        else {
            List<String> params = data.getPathSegments();
            String uriBoardCode = params.get(0);
            if (ChanBoard.getBoardByCode(this, uriBoardCode) != null) {
                boardCode = uriBoardCode;
                query = "";
                firstVisiblePosition = -1;
                firstVisiblePositionOffset = -1;
                if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from url intent");
            }
            else {
                boardCode = ChanBoard.POPULAR_BOARD_CODE;
                query = "";
                firstVisiblePosition = -1;
                firstVisiblePositionOffset = -1;
                if (DEBUG) Log.e(TAG, "Received invalid boardCode=" + uriBoardCode + " from url intent, using default board");
            }
        }
        if (DEBUG) Log.i(TAG, "setFromIntent /" + boardCode + "/ q=" + query);
    }

    protected int getLayoutId() {
        return R.layout.board_grid_layout;
    }

    protected void createAbsListView() {
        // we don't use fragments, but create anything needed
        FrameLayout contentFrame = (FrameLayout)findViewById(R.id.content_frame);
        if (contentFrame.getChildCount() > 0)
            contentFrame.removeAllViews();
        layout = View.inflate(getApplicationContext(), getLayoutId(), null);
        contentFrame.addView(layout);
        adapter = new BoardGridCursorAdapter(this, this, columnWidth, columnHeight);
        gridView = (GridView)findViewById(R.id.board_grid_view);
        columnWidth = ChanGridSizer.getCalculatedWidth(getResources().getDisplayMetrics(),
                getResources().getInteger(R.integer.BoardGridView_numColumns),
                getResources().getDimensionPixelSize(R.dimen.BoardGridView_spacing));
        columnHeight = 2 * columnWidth;
        gridView.setAdapter(adapter);
        gridView.setClickable(true);
        gridView.setOnItemClickListener(boardItemListener);
        //gridView.setLongClickable(false);
        gridView.setSelector(R.drawable.board_grid_selector_bg);

        ImageLoader imageLoader = ChanImageLoader.getInstance(getApplicationContext());
        gridView.setOnScrollListener(new PauseOnScrollListener(imageLoader, false, true));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (handler == null)
            handler = new LoaderHandler();
        if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/ q=" + query);
        setActionBarTitle();
    }

	@Override
	protected void onResume() {
		super.onResume();
        if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/ q=" + query);
        if (handler == null)
            handler = new LoaderHandler();
        handleUpdatedThreads();
        invalidateOptionsMenu(); // for correct spinner display
		NetworkProfileManager.instance().activityChange(this);
        if (adapter == null || adapter.getCount() == 0)
            getSupportLoaderManager().restartLoader(0, null, this);
        new TutorialOverlay(layout, Page.BOARD);
    }

    @Override
	public void onWindowFocusChanged (boolean hasFocus) {
		if (DEBUG) Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause /" + boardCode + "/ q=" + query);
        handler = null;
    }

    @Override
    protected void onStop () {
    	super.onStop();
        if (DEBUG) Log.i(TAG, "onStop /" + boardCode + "/ q=" + query);
        getLoaderManager().destroyLoader(0);
        closeSearch();
    	handler = null;
        /*
        adapter = null;
        layout = null;
        gridView = null;
        cursorLoader = null;
        menu = null;
        */
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
        if (DEBUG) Log.i(TAG, "onDestroy /" + boardCode + "/ q=" + query);
        if (cursorLoader != null)
            getLoaderManager().destroyLoader(0);
		handler = null;
	}

    protected View.OnClickListener viewActionListener = new View.OnClickListener(){
        public void onClick(View view) {
            String boardCode = (String)view.getTag(R.id.BOARD_CODE);
            Long threadNo = (Long)view.getTag(R.id.THREAD_NO);
            Long postNo = (Long)view.getTag(R.id.POST_NO);
            Boolean isImage = (Boolean)view.getTag(R.id.BOARD_GRID_IMAGE);
            if (threadNo == 0)
                BoardActivity.startActivity(BoardActivity.this, boardCode, "");
            else if (isImage)
                GalleryViewActivity.startAlbumViewActivity(BoardActivity.this, boardCode, threadNo);
            else
                ThreadActivity.startActivity(BoardActivity.this, boardCode, threadNo, postNo, "");
        }
    };


    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        return BoardGridViewer.setViewValue(view, cursor, boardCode, columnWidth, columnHeight);
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.i(TAG, "onCreateLoader /" + boardCode + "/ q=" + query + " id=" + id);
        setProgressBarIndeterminateVisibility(true);
        cursorLoader = new BoardCursorLoader(this, boardCode, query);
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (DEBUG) Log.i(TAG, "onLoadFinished /" + boardCode + "/ q=" + query + " id=" + loader.getId()
                + " count=" + (data == null ? 0 : data.getCount()));
        if (gridView == null)
            createAbsListView();
		adapter.swapCursor(data);

        // retry load if maybe data wasn't there yet
        if ((data == null || data.getCount() < 1) && handler != null) {
            NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
            if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
                String msg = String.format(getString(R.string.mobile_profile_health_status),
                        health.toString().toLowerCase().replaceAll("_", " "));
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
            //else {
            //    handler.sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
            //}
        }
        /*
        else {
            if (firstVisiblePosition >= 0) {
                //if (gridView instanceof ListView)
                //    ((ListView) gridView).setSelectionFromTop(firstVisiblePosition, firstVisiblePositionOffset);
                //else
                gridView.setSelection(firstVisiblePosition);
                firstVisiblePosition = -1;
                firstVisiblePositionOffset = -1;
            }
            handleUpdatedThreads(); // see if we need to update
            setActionBarTitle(); // to reflect updated time
        }
        */
        stopProgressBarIfLoadersDone();
    }

    protected void stopProgressBarIfLoadersDone() {
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
        if (DEBUG) Log.i(TAG, "onLoaderReset /" + boardCode + "/ q=" + query + " id=" + loader.getId());
        if (adapter != null)
            adapter.swapCursor(null);
	}

    AbsListView.OnItemClickListener boardItemListener = new AbsListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(position);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
            final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
            if ((flags & ChanThread.THREAD_FLAG_AD) > 0) {
                String[] clickUrls = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_CLICK_URL))
                        .split(ChanThread.AD_DELIMITER);
                String clickUrl = viewType == ViewType.AS_GRID ? clickUrls[0] : clickUrls[1];
                ChanHelper.launchUrlInBrowser(BoardActivity.this, clickUrl);
            }
            else if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0
                    && title != null && !title.isEmpty()
                    && desc != null && !desc.isEmpty()) {
                (new GenericDialogFragment(title.replaceAll("<[^>]*>", " "), desc))
                        .show(getSupportFragmentManager(), BoardActivity.TAG);
                return;
            }
            else if ((flags & ChanThread.THREAD_FLAG_BUTTON) > 0) {
                PostReplyActivity.startActivity(BoardActivity.this, boardCode, 0, 0, "");
                return;
            }
            else if ((flags & ChanThread.THREAD_FLAG_BOARD) > 0) {
                String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                startActivity(BoardActivity.this, boardLink, "");
            }
            else {
                String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                ThreadActivity.startActivity(BoardActivity.this, boardLink, threadNo, "");
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.refresh_menu:
                if (ChanBoard.isVirtualBoard(boardCode) && !ChanBoard.isPopularBoard(boardCode)) {
                    if (DEBUG) Log.i(TAG, "manual refresh skipped for non-popular virtual board /" + boardCode + "/");
                    return true;
                }
                setProgressBarIndeterminateVisibility(true);
                NetworkProfileManager.instance().manualRefresh(this);
                if (gridView != null)
                    gridView.setSelection(0);
                return true;
            case R.id.new_thread_menu:
                ChanBoard board = ChanBoard.getBoardByCode(this, boardCode);
                if (board == null || board.isVirtualBoard())
                    new PickNewThreadBoardDialogFragment(handler)
                            .show(getFragmentManager(), PickNewThreadBoardDialogFragment.TAG);
                else
                    PostReplyActivity.startActivity(this, boardCode, 0, 0, "");
                return true;
            case R.id.offline_board_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, boardCode);
                return true;
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
            case R.id.offline_chan_view_menu:
                GalleryViewActivity.startOfflineAlbumViewActivity(this, null);
                return true;
            case R.id.global_rules_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this,
                        R.layout.board_rules_dialog, R.raw.global_rules_header, R.raw.global_rules_detail);
                rawResourceDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void displayBoardRules() {
        //NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.BOARD_RULES);
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_menu, menu);
        searchMenuItem = menu.findItem(R.id.search_menu);
        SearchActivity.createSearchView(this, searchMenuItem);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ChanBoard board = ChanBoard.getBoardByCode(this, boardCode);
        if (board == null) {
            ; // ignore
        }
        else if (board.isPopularBoard()) {
            menu.findItem(R.id.refresh_menu).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.findItem(R.id.refresh_menu).setVisible(true);
            menu.findItem(R.id.search_menu).setVisible(false);
            menu.findItem(R.id.offline_board_view_menu).setVisible(false);
            menu.findItem(R.id.board_rules_menu).setVisible(false);
            menu.findItem(R.id.offline_chan_view_menu).setVisible(true);
            menu.findItem(R.id.global_rules_menu).setVisible(true);
        }
        else if (board.isVirtualBoard()) {
            menu.findItem(R.id.refresh_menu).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.findItem(R.id.refresh_menu).setVisible(false);
            menu.findItem(R.id.search_menu).setVisible(false);
            menu.findItem(R.id.offline_board_view_menu).setVisible(false);
            menu.findItem(R.id.board_rules_menu).setVisible(false);
            menu.findItem(R.id.offline_chan_view_menu).setVisible(true);
            menu.findItem(R.id.global_rules_menu).setVisible(true);
        }
        else {
            menu.findItem(R.id.refresh_menu).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.findItem(R.id.refresh_menu).setVisible(true);
            menu.findItem(R.id.search_menu).setVisible(true);
            menu.findItem(R.id.offline_board_view_menu).setVisible(true);
            menu.findItem(R.id.board_rules_menu).setVisible(true);
            menu.findItem(R.id.offline_chan_view_menu).setVisible(false);
            menu.findItem(R.id.global_rules_menu).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public void setActionBarTitle() {
        String title;
        ChanBoard board = loadBoard();
        title = (board == null ? "Board" : board.name);
        if (!board.isVirtualBoard())
            title += " /" + boardCode + "/";
        getActionBar().setTitle(title);
    }

    protected ChanBoard loadBoard() {
        ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (board == null) {
            board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
        }
        return board;
    }

    public void handleUpdatedThreads() {
        LinearLayout refreshLayout = (LinearLayout)this.findViewById(R.id.board_refresh_bar);
        if (refreshLayout == null)
            return;
        ChanBoard board = loadBoard();
        StringBuffer msg = new StringBuffer();
        if (board.shouldSwapThreads()) { // auto-update if we have no threads to show, don't display menu
            if (DEBUG) Log.i(TAG, "auto-updating threads since empty");
            board.swapLoadedThreads();
            refreshLayout.setVisibility(LinearLayout.GONE);
        } else if ((board.newThreads > 0 || board.updatedThreads > 0)
                && (query == null || query.isEmpty())) { // display update button
            if (board.newThreads > 0) {
                msg.append("" + board.newThreads + " new");
            }
            if (board.updatedThreads > 0) {
                if (board.newThreads > 0) {
                    msg.append(", ");
                }
                msg.append("" + board.updatedThreads + " updated");
            }
            msg.append(" thread");
            if (board.newThreads + board.updatedThreads > 1) {
                msg.append("s");
            }
            msg.append(" available");

            TextView refreshText = (TextView)refreshLayout.findViewById(R.id.board_refresh_text);
            refreshText.setText(msg.toString());
            ImageButton refreshButton = (ImageButton)refreshLayout.findViewById(R.id.board_refresh_button);
            refreshButton.setClickable(true);
            refreshButton.setOnClickListener(this);
            ImageButton ignoreButton = (ImageButton)refreshLayout.findViewById(R.id.board_ignore_button);
            ignoreButton.setClickable(true);
            ignoreButton.setOnClickListener(this);

            refreshLayout.setVisibility(LinearLayout.VISIBLE);
        } else { // don't display menu
            if (board.defData || board.lastFetched == 0) {
                msg.append("not yet fetched");
            } else if (Math.abs(board.lastFetched - new Date().getTime()) < 60000) {
                msg.append("fetched just now");
            } else {
                msg.append("fetched ").append(DateUtils.getRelativeTimeSpanString(
                        board.lastFetched, (new Date()).getTime(), 0, DateUtils.FORMAT_ABBREV_RELATIVE).toString());
            }
            TextView refreshText = (TextView)refreshLayout.findViewById(R.id.board_refresh_text);
            refreshText.setText("Board is up to date");

            refreshLayout.setVisibility(LinearLayout.GONE);
        }
    }

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.BOARD_ACTIVITY, boardCode);
	}

	@Override
	public Handler getChanHandler() {
        return handler;
	}

    @Override
    public void refresh() {
        handleUpdatedThreads();
        setActionBarTitle(); // for update time
        invalidateOptionsMenu(); // in case spinner needs to be reset
        //if (gridView == null || gridView.getCount() < 1)
        //    createAbsListView();
        ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (board == null) {
            board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
        }
        if (board.newThreads == 0 && board.updatedThreads == 0) {
	        if (handler != null) {
	        	handler.sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
	        }
        }
    }

    @Override
    public void closeSearch() {
        if (searchMenuItem != null)
            searchMenuItem.collapseActionView();
    }

	@Override
	public void onClick(View v) {
        if (v.getId() == R.id.board_refresh_button) {
            setProgressBarIndeterminateVisibility(true);
            LinearLayout refreshLayout = (LinearLayout)this.findViewById(R.id.board_refresh_bar);
            refreshLayout.setVisibility(LinearLayout.GONE);
            NetworkProfileManager.instance().manualRefresh(this);
        }
        else if (v.getId() == R.id.board_ignore_button) {
	        LinearLayout refreshLayout = (LinearLayout)this.findViewById(R.id.board_refresh_bar);
	        refreshLayout.setVisibility(LinearLayout.GONE);
		}
	}

    private class LoaderHandler extends Handler {
        public LoaderHandler() {}
        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                switch (msg.what) {
                    default:
                        if (DEBUG) Log.i(TAG, ">>>>>>>>>>> restart message received restarting loader");
                        getSupportLoaderManager().restartLoader(0, null, BoardActivity.this);
                }
            } catch (Exception e) {
                Log.e(TAG, "Couldn't handle message " + msg, e);
            }
        }
    }

    @Override
    public void startProgress() {
        if (handler != null)
            setProgressBarIndeterminateVisibility(true);
    }

    @Override
    public boolean onSearchRequested() {
        if (DEBUG) Log.i(TAG, "onSearchRequested /" + boardCode + "/ q=" + query);
        return super.onSearchRequested();
    }

}
