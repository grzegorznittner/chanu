package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.preference.PreferenceManager;
import android.util.Log;

import android.widget.AbsListView;
import android.widget.GridView;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.*;

public class ThreadCursorLoader extends BoardCursorLoader {

    private static final String TAG = ThreadCursorLoader.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int DEFAULT_NUM_GRID_COLUMNS_PORTRAIT = 2;
    private static final int DEFAULT_NUM_GRID_COLUMNS_LANDSCAPE = 3;

    protected SharedPreferences prefs;
    protected long threadNo;
    protected int numGridColumns;
    private boolean hidePostNumbers;
    private boolean useFriendlyIds;

    protected ThreadCursorLoader(Context context) {
        super(context);
    }

    public ThreadCursorLoader(Context context, String boardName, long threadNo, String query, AbsListView absListView) {
        this(context);
        this.context = context;
        this.boardName = boardName;
        this.threadNo = threadNo;
        this.query = query.toLowerCase().trim();
        initRandomGenerator();
        ChanHelper.Orientation orientation = ChanHelper.getOrientation(context);
        int defaultNumColumns = (orientation == ChanHelper.Orientation.PORTRAIT) ? DEFAULT_NUM_GRID_COLUMNS_PORTRAIT : DEFAULT_NUM_GRID_COLUMNS_LANDSCAPE;
        if (absListView instanceof GridView) {
            GridView gridView = (GridView)absListView;
            int currentNumGridColumns = gridView.getNumColumns();
            this.numGridColumns = (gridView == null || currentNumGridColumns <= 0) ? defaultNumColumns : currentNumGridColumns;
        }
        else {
            this.numGridColumns = 0;
        }
        if (threadNo == 0) {
            throw new ExceptionInInitializerError("Can't have zero threadNo in a thread cursor loader");
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected void initRandomGenerator() { // to allow repeatable positions for ads
        generatorSeed = threadNo;
        generator = new Random(generatorSeed);
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
        try {
            hidePostNumbers = boardName.equals("b") ? false : prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, false);
            useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
            ChanBoard board = ChanFileStorage.loadBoardData(getContext(), boardName);
            if (DEBUG) Log.i(TAG, "Loaded board from storage " + board);
            ChanThread thread;
            try {
                thread = ChanFileStorage.loadThreadData(getContext(), boardName, threadNo);
                if (DEBUG) Log.i(TAG, "Loaded thread from storage " + thread);
            } catch (Exception e) {
                Log.e(TAG, "Couldn't load thread from storage " + boardName + "/" + threadNo, e);
                thread = null;
            }
            int isDead = thread != null && thread.isDead ? 1 : 0;
            if (DEBUG) Log.i(TAG, "loadInBackground " + thread.board + "/" + thread.no + " num posts " + (thread.posts != null ? thread.posts.length : 0));
            if (DEBUG) Log.i(TAG, "Thread dead status for " + boardName + "/" + threadNo + " is " + isDead);
            if (DEBUG) Log.i(TAG, "Thread closed status for " + boardName + "/" + threadNo + " is closed=" + thread.closed);
            MatrixCursor matrixCursor = ChanPost.buildMatrixCursor();

            if (!query.isEmpty()) {
                String title = String.format(context.getString(R.string.thread_search_results), "<i>" + query + "</i>");
                matrixCursor.addRow(ChanPost.makeTitleRow(boardName, title));
            }
            if (board != null && thread != null && thread.posts != null && thread.posts.length > 0) { // show loading for no thread data
                loadMatrixCursor(matrixCursor, board, thread);
                if (DEBUG) Log.i(TAG, "Remaining to load:" + (thread.posts[0].replies - thread.posts.length));
            }
            registerContentObserver(matrixCursor, mObserver);
            return matrixCursor;
    	} catch (Exception e) {
    		Log.e(TAG, "loadInBackground", e);
    		return null;
    	}
    }

    private void loadMatrixCursor(MatrixCursor matrixCursor, ChanBoard board, ChanThread thread) {
        int adSpace = MINIMUM_AD_SPACING;
        int i = 0;
        int numQueryMatches = 0;
        int numPosts = thread.posts.length;
        for (ChanPost post : thread.posts) {
            if (ChanBlocklist.isBlocked(context, post))
                continue;
            if (!post.matchesQuery(query))
                continue;
            if (!query.isEmpty())
                numQueryMatches++;
            post.isDead = thread.isDead; // inherit from parent
            post.closed = thread.closed; // inherit
            post.hidePostNumbers = hidePostNumbers;
            post.useFriendlyIds = useFriendlyIds;
            matrixCursor.addRow(post.makeRow(query));
            // randomly distribute ads
            if (generator.nextDouble() < AD_PROBABILITY
                    && !(adSpace > 0)
                    && i < (numPosts - MINIMUM_AD_SPACING)) {
                matrixCursor.addRow(board.makePostAdRow(getContext(), i));
                adSpace = MINIMUM_AD_SPACING;
            }
            else {
                adSpace--;
            }
            i++;
        }

        // no search results marker
        if (!query.isEmpty() && numQueryMatches == 0)
            matrixCursor.addRow(ChanPost.makeTitleRow(boardName, context.getString(R.string.thread_search_no_results)));

        // always put an ad at the bottom
        if (i > 1 || thread.replies == 0)
            matrixCursor.addRow(board.makePostAdRow(getContext(), i));

        // put related threads at the bottom
        List<Object[]> rows = board.makePostRelatedThreadsRows(threadNo);
        if (rows.size() > 0) {
            matrixCursor.addRow(board.makePostRelatedThreadsHeaderRow(getContext()));
            for (Object[] row : rows)
                matrixCursor.addRow(row);
        }

        // put related boards at the bottom
        List<ChanBoard> boardRows = board.relatedBoards(context);
        if (boardRows.size() > 0) {
            matrixCursor.addRow(board.makePostRelatedBoardsHeaderRow(context));
            for (ChanBoard relatedBoard : boardRows)
                matrixCursor.addRow(relatedBoard.makePostBoardLinkRow());
        }
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("boardName="); writer.println(boardName);
        writer.print(prefix); writer.print("threadNo="); writer.println(threadNo);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }

}
