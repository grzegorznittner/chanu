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
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.adapter.BoardListCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.component.TutorialOverlay.Page;
import com.chanapps.four.data.*;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.UserStatistics.ChanFeature;
import com.chanapps.four.fragment.GenericDialogFragment;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.BoardGridViewer;
import com.chanapps.four.viewer.BoardListViewer;
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

    protected AbstractBoardCursorAdapter adapter;
    protected View layout;
    protected AbsListView absListView;
    protected Class absListViewClass = GridView.class;
    protected Handler handler;
    protected BoardCursorLoader cursorLoader;
    protected Menu menu;
    protected long tim;
    protected String boardCode;
    protected String query = "";
    protected int columnWidth = 0;
    protected int columnHeight = 0;
    protected MenuItem searchMenuItem;
    protected ViewType viewType = ViewType.AS_GRID;

    public static void startActivity(Activity from, String boardCode, String query) {
        if (query != null && !query.isEmpty())
            NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.SEARCH_BOARD);
        from.startActivity(createIntentForActivity(from, boardCode, query));
    }

    public static Intent createIntentForActivity(Context context, String boardCode, String query) {
        String intentBoardCode = boardCode == null || boardCode.isEmpty() ? ChanBoard.DEFAULT_BOARD_CODE : boardCode;
        Intent intent = new Intent(context, BoardActivity.class);
        intent.putExtra(ChanBoard.BOARD_CODE, intentBoardCode);
        intent.putExtra(ChanHelper.PAGE, 0);
        intent.putExtra(SearchManager.QUERY, query);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    @Override
    public boolean isSelfBoard(String boardAsMenu) {
        return (boardAsMenu != null && boardAsMenu.matches("/" + boardCode + "/.*"));
    }

    @Override
    protected void createViews(Bundle bundle) {
        createAbsListView();
        if (bundle != null)
            onRestoreInstanceState(bundle);
        else
            setFromIntent(getIntent());
        if (DEBUG) Log.i(TAG, "onCreate /" + boardCode + "/ q=" + query);
        if (boardCode == null || boardCode.isEmpty())
            redirectToBoardSelector();
    }

    protected void redirectToBoardSelector() { // backup in case we are missing stuff
        Log.e(TAG, "Empty board code, redirecting to board selector");
        Intent selectorIntent = new Intent(this, BoardSelectorActivity.class);
        selectorIntent.putExtra(ChanHelper.BOARD_TYPE, BoardType.JAPANESE_CULTURE.toString());
        selectorIntent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
        selectorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(selectorIntent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(ChanBoard.BOARD_CODE, boardCode);
        savedInstanceState.putString(SearchManager.QUERY, query);
        if (DEBUG) Log.i(TAG, "onSaveInstanceState /" + boardCode + "/ q=" + query);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        boardCode = savedInstanceState.getString(ChanBoard.BOARD_CODE);
        query = savedInstanceState.getString(SearchManager.QUERY);
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
        }
        else {
            List<String> params = data.getPathSegments();
            String uriBoardCode = params.get(0);
            if (ChanBoard.getBoardByCode(this, uriBoardCode) != null) {
                boardCode = uriBoardCode;
                query = "";
                if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from url intent");
            }
            else {
                boardCode = ChanBoard.DEFAULT_BOARD_CODE;
                query = "";
                if (DEBUG) Log.e(TAG, "Received invalid boardCode=" + uriBoardCode + " from url intent, using default board");
            }
        }
        if (DEBUG) Log.i(TAG, "setFromIntent /" + boardCode + "/ q=" + query);
    }

    protected void sizeGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(absListView, display, ChanGridSizer.ServiceType.BOARD);
        cg.sizeGridToDisplay();
        columnWidth = cg.getColumnWidth();
        columnHeight = cg.getColumnHeight();
    }

    protected void initAdapter() {
        if (absListView instanceof GridView)
            adapter = new BoardGridCursorAdapter(this, this, columnWidth, columnHeight);
        else
            adapter = new BoardListCursorAdapter(this, this);
        absListView.setAdapter(adapter);
    }

    protected int getLayoutId() {
        if (GridView.class.equals(absListViewClass))
            return R.layout.board_grid_layout;
        else
            return R.layout.board_list_layout;
    }

    protected void setAbsListViewClass() { // override to change
        if (hasQuery()) viewType = ViewType.AS_LIST;
        absListViewClass = viewType == ViewType.AS_LIST ? ListView.class : GridView.class;
    }

    protected boolean hasQuery() {
        String searchQuery = getIntent() == null ? null : getIntent().getStringExtra(SearchManager.QUERY);
        return searchQuery != null && !searchQuery.isEmpty();
    }

    protected void initAbsListView() {
        if (GridView.class.equals(absListViewClass)) {
            absListView = (GridView)findViewById(R.id.board_grid_view);
            sizeGridToDisplay();
        }
        else {
            absListView = (ListView)findViewById(R.id.board_list_view);
        }
    }

    protected void createAbsListView() {
        setAbsListViewClass();
        // we don't use fragments, but create anything needed
        FrameLayout contentFrame = (FrameLayout)findViewById(R.id.content_frame);
        if (contentFrame.getChildCount() > 0)
            contentFrame.removeAllViews();
        layout = View.inflate(getApplicationContext(), getLayoutId(), null);
        contentFrame.addView(layout);
        initAbsListView();
        initAdapter();
        absListView.setClickable(true);
        absListView.setOnItemClickListener(boardItemListener);
        absListView.setLongClickable(false);
        ImageLoader imageLoader = ChanImageLoader.getInstance(getApplicationContext());
        absListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
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
		getSupportLoaderManager().restartLoader(0, null, this);
        new TutorialOverlay(layout, Page.BOARD);
    }

    @Override
	public void onWindowFocusChanged (boolean hasFocus) {
		if (DEBUG) Log.v(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
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
    	handler = null;
        /*
        adapter = null;
        layout = null;
        absListView = null;
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

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (viewType == ViewType.AS_GRID)
            return BoardGridViewer.setViewValue(view, cursor, boardCode);
        else
            return BoardListViewer.setViewValue(view, cursor, boardCode);
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
        if (absListView == null)
            createAbsListView();
		adapter.swapCursor(data);

        // retry load if maybe data wasn't there yet
        if ((data == null || data.getCount() < 1) && handler != null) {
            NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
            if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
                stopProgressBarIfLoadersDone();
                String msg = String.format(getString(R.string.mobile_profile_health_status),
                        health.toString().toLowerCase().replaceAll("_", " "));
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
            //else {
            //    handler.sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
            //}
        }
        else {
            handleUpdatedThreads(); // see if we need to update
            setActionBarTitle(); // to reflect updated time
            stopProgressBarIfLoadersDone();
        }
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

    AbsListView.OnItemClickListener boardItemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TITLE));
            final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
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
                setProgressBarIndeterminateVisibility(true);
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            case R.id.toggle_view_type_menu:
            	NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.BOARD_LIST_VIEW);
                viewType = viewType == ViewType.AS_GRID ? ViewType.AS_LIST : ViewType.AS_GRID;
                invalidateOptionsMenu();
                createAbsListView();
                getSupportLoaderManager().restartLoader(0, null, this);
                return true;
            case R.id.new_thread_menu:
                Intent replyIntent = new Intent(this, PostReplyActivity.class);
                replyIntent.putExtra(ChanBoard.BOARD_CODE, boardCode);
                replyIntent.putExtra(ChanHelper.THREAD_NO, 0);
                replyIntent.putExtra(ChanPost.POST_NO, 0);
                replyIntent.putExtra(ChanHelper.TIM, 0);
                replyIntent.putExtra(ChanHelper.TEXT, "");
                startActivity(replyIntent);
                return true;
            case R.id.offline_board_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, boardCode);
                return true;
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void displayBoardRules() {
        NetworkProfileManager.instance().getUserStatistics().featureUsed(ChanFeature.BOARD_RULES);
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.toggle_view_type_menu);
        if (item != null) {
            item.setIcon(viewType == ViewType.AS_GRID ? R.drawable.collections_view_as_list : R.drawable.collections_view_as_grid);
            item.setTitle(viewType == ViewType.AS_GRID ? R.string.view_as_list_menu : R.string.view_as_grid_menu);
            item.setVisible(!hasQuery()); // force to list view when has query
        }
        searchMenuItem = menu.findItem(R.id.search_menu);
        if (query == null || query.isEmpty()) {
            SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView)searchMenuItem.getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        else {
            searchMenuItem.setVisible(false);
            MenuItem refresh = menu.findItem(R.id.refresh_menu);
            refresh.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public void setActionBarTitle() {
        String title;
        if (query != null && !query.isEmpty()) {
            title = getString(R.string.search_results_title);
        }
        else {
            ChanBoard board = loadBoard();
            title = (board == null ? "Board" : board.name) + " /" + boardCode + "/";
        }
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
        //if (absListView == null || absListView.getCount() < 1)
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

}
