package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBlocklist;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;

public class BoardCursorLoader extends AsyncTaskLoader<Cursor> {

    protected static final String TAG = BoardCursorLoader.class.getSimpleName();
    protected static final boolean DEBUG = true;

    protected static final double AD_PROBABILITY = 0.20;
    protected static final int MINIMUM_AD_SPACING = 4;

    protected final ForceLoadContentObserver mObserver;

    protected Cursor mCursor;
    protected Context context;

    protected String boardName;
    protected String query;

    protected long generatorSeed;
    protected Random generator;

    protected BoardCursorLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    public BoardCursorLoader(Context context, String boardName, String query) {
        this(context);
        this.context = context;
        this.boardName = boardName;
        this.query = query.toLowerCase().trim();
        initRandomGenerator();
    }

    protected void initRandomGenerator() { // to allow repeatable positions for ads
        generatorSeed = boardName.hashCode();
        generator = new Random(generatorSeed);
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	if (DEBUG) Log.i(TAG, "loadInBackground");
        ChanBoard board = ChanFileStorage.loadBoardData(getContext(), boardName);
        if (DEBUG)  {
            Log.i(TAG, "board " + board.link + ", threadcount=" + (board.threads != null ? board.threads.length : 0));
            Log.i(TAG, "board loadedthreadcount=" + (board.loadedThreads != null ? board.loadedThreads.length : 0));
        }
        
        if (board.shouldSwapThreads())
        { // auto-update if we have no threads to show
            if (DEBUG) Log.i(TAG, "auto-swapping loaded threads since empty");
            board.swapLoadedThreads();
        }

        MatrixCursor matrixCursor = ChanThread.buildMatrixCursor();

        if (board.threads != null && !board.defData
                && board.threads.length > 0 && board.threads[0] != null && !board.threads[0].defData) { // show loading
            if (!board.isVirtualBoard()) {
                matrixCursor.addRow(board.makeThreadAdRow(getContext(), 0));
            }

            if (!query.isEmpty()) {
                String title = String.format(context.getString(R.string.board_search_results), "<i>" + query + "</i>");
                matrixCursor.addRow(ChanThread.makeTitleRow(boardName, title));
            }

            if (DEBUG) Log.i(TAG, "Loading " + board.threads.length + " threads");
            //int adSpace = MINIMUM_AD_SPACING;
            int numQueryMatches = 0;
            int i = 0;
            for (ChanPost thread : board.threads) {
                if (DEBUG) Log.i(TAG, "Loading thread: " + thread);
                if (ChanBlocklist.isBlocked(context, thread) || thread.no <= 0) {
                    if (DEBUG) Log.i(TAG, "Skipped thread: " + thread.no);
                    continue;
                }
                if (!thread.matchesQuery(query))
                    continue;
                if (!query.isEmpty())
                    numQueryMatches++;
                Object[] row = ChanThread.makeRow(context, thread, query);
                matrixCursor.addRow(row);
                i++;
                if (DEBUG) Log.v(TAG, "Added board row: " + Arrays.toString(row));
                /*
                if (generator.nextDouble() < AD_PROBABILITY && !(adSpace > 0)) {
                    matrixCursor.addRow(board.makeThreadAdRow(context, i));
                    adSpace = MINIMUM_AD_SPACING;
                }
                else {
                    adSpace--;
                }
                */
            }
            if (DEBUG) Log.i(TAG, "Loaded " + i + " threads");

            // no search results marker
            if (!query.isEmpty() && numQueryMatches == 0) {
                matrixCursor.addRow(ChanThread.makeTitleRow(boardName,
                        context.getString(R.string.thread_search_no_results)));
            }

            if (!board.isVirtualBoard() && i > 0) { // add related boards if any results
                matrixCursor.addRow(ChanThread.makeTitleRow(boardName,
                        context.getString(R.string.board_related_boards_title)));
                for (ChanBoard relatedBord : board.relatedBoards(context)) {
                    matrixCursor.addRow(relatedBord.makeRow(context));
                }
            }

            // always put an ad at the bottom
            if (!board.isVirtualBoard())
                matrixCursor.addRow(board.makeThreadAdRow(getContext(), i));
        }
        registerContentObserver(matrixCursor, mObserver);
        return matrixCursor;
    }

    /**
     * Registers an observer to get notifications from the content provider
     * when the cursor needs to be refreshed.
     */
    void registerContentObserver(Cursor cursor, ContentObserver observer) {
        cursor.registerContentObserver(mObserver);
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Cursor cursor) {
		if (DEBUG) Log.i(TAG, "deliverResult isReset(): " + isReset());
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
    	if (DEBUG) Log.i(TAG, "onStartLoading mCursor: " + mCursor);
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
    	if (DEBUG) Log.i(TAG, "onStopLoading");
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
    	if (DEBUG) Log.i(TAG, "onCanceled cursor: " + cursor);
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        if (DEBUG) Log.i(TAG, "onReset cursor: " + mCursor);
        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("boardName="); writer.println(boardName);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
