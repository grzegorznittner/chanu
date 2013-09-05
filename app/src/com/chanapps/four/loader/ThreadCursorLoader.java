package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    protected SharedPreferences prefs;
    protected long threadNo;
    protected boolean showRelatedBoards;
    private boolean useFriendlyIds;

    protected ThreadCursorLoader(Context context) {
        super(context);
    }

    public ThreadCursorLoader(Context context, String boardName, long threadNo, String query, boolean showRelatedBoards) {
        this(context);
        this.context = context;
        this.boardName = boardName;
        this.threadNo = threadNo;
        this.query = query == null ? "" : query.toLowerCase().trim();
        this.showRelatedBoards = showRelatedBoards;
        initRandomGenerator();
        if (threadNo <= 0)
            throw new ExceptionInInitializerError("Can't have zero threadNo in a thread cursor loader");
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
        if (DEBUG) Log.i(TAG, "Thread toplevel thumb=" + thread.tn_w + "x" + thread.tn_h + " full=" + thread.w + "x" + thread.h);
        if (DEBUG) Log.i(TAG, "Thread postlevel thumb=" + thread.posts[0].tn_w + "x" + thread.posts[0].tn_h + " full=" + thread.posts[0].w + "x" + thread.posts[0].h);

        // first get the maps for thread references
        Map<Long, HashSet<Long>> backlinksMap = thread.backlinksMap();
        Map<Long, HashSet<Long>> repliesMap = thread.repliesMap(backlinksMap);
        Map<String, HashSet<Long>> sameIdsMap = thread.sameIdsMap();

        //int adSpace = MINIMUM_AD_SPACING;


        int i = 0;
        int numQueryMatches = 0;
        //int numPosts = thread.posts.length;
        for (ChanPost post : thread.posts) {
            if (ChanBlocklist.isBlocked(context, post))
                continue;
            if (!post.matchesQuery(query))
                continue;
            if (!query.isEmpty())
                numQueryMatches++;
            post.isDead = thread.isDead; // inherit from parent
            post.closed = thread.closed; // inherit
            post.hidePostNumbers = false; // always show
            post.useFriendlyIds = useFriendlyIds;
            byte[] backlinksBlob = ChanPost.blobify(backlinksMap.get(post.no));
            byte[] repliesBlob = ChanPost.blobify(repliesMap.get(post.no));
            HashSet<Long> sameIds = sameIdsMap.get(post.id);
            byte[] sameIdsBlob = (sameIds != null && sameIds.size() > 1) ? ChanPost.blobify(sameIds) : null;
            matrixCursor.addRow(post.makeRow(context, query, i, backlinksBlob, repliesBlob, sameIdsBlob));
            // randomly distribute ads
            /*
            if (generator.nextDouble() < AD_PROBABILITY
                    && !(adSpace > 0)
                    && i < (numPosts - MINIMUM_AD_SPACING)) {
                matrixCursor.addRow(board.makePostAdRow(getContext(), i));
                adSpace = MINIMUM_AD_SPACING;
            }
            else {
                adSpace--;
            }
            */
            i++;
        }

        /*
        if (!thread.isDead)
            matrixCursor.addRow(ChanPost.makeButtonRow(boardName, context.getString(R.string.reply_short).toUpperCase()));
        else
            matrixCursor.addRow(ChanPost.makeTitleRow(boardName, context.getString(R.string.dead_thread_reply).toUpperCase()));
        */

        if (thread.defData)
            return;

        if (!thread.isDead && (thread.posts != null && thread.replies > thread.posts.length))
            return;

        // put related threads at the bottom
        /*
        List<Object[]> rows = board.makePostRelatedThreadsRows(threadNo);
        if (rows.size() > 0) {
            matrixCursor.addRow(board.makePostRelatedThreadsHeaderRow(context));
            for (Object[] row : rows)
                matrixCursor.addRow(row);
        }
        */
        // put related boards at the bottom
        boolean showRelated;
        if (query != null && !query.isEmpty())
            showRelated = false;
        else if (thread.posts == null || thread.posts.length <= 0 || thread.posts[0] == null
                || (thread.posts[0].replies > 0 && thread.posts.length == 1))
            showRelated = false;
        else
            showRelated = showRelatedBoards;
        if (showRelated) {
            List<ChanBoard> boardRows = board.relatedBoards(context, threadNo);
            if (boardRows.size() > 0) {
                matrixCursor.addRow(board.makePostRelatedBoardsHeaderRow(context));
                for (ChanBoard relatedBoard : boardRows)
                    matrixCursor.addRow(relatedBoard.makePostBoardLinkRow(context, threadNo));
            }
        }

        // always put an ad at the bottom after done loading
        //if (i > 1 || thread.replies == 0)
        //    matrixCursor.addRow(board.makePostAdRow(getContext(), i));

    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("boardName="); writer.println(boardName);
        writer.print(prefix); writer.print("threadNo="); writer.println(threadNo);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }

}
