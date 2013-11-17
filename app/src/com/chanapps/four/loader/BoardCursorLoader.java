package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.*;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.preference.PreferenceManager;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.AlphanumComparator;
import com.chanapps.four.data.*;

public class BoardCursorLoader extends AsyncTaskLoader<Cursor> {

    protected static final String TAG = BoardCursorLoader.class.getSimpleName();
    protected static final boolean DEBUG = true;

    //protected static final double AD_PROBABILITY = 0.20;
    //protected static final int MINIMUM_AD_SPACING = 4;

    protected final ForceLoadContentObserver mObserver;

    protected Cursor mCursor;
    protected Context context;

    protected String boardName;
    protected String query;
    protected boolean abbrev;
    protected boolean header;

    protected long generatorSeed;
    protected Random generator;

    protected BoardSortType boardSortType;

    protected BoardCursorLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    public BoardCursorLoader(Context context, String boardName, String query, boolean abbrev, boolean header,
                             BoardSortType boardSortType) {
        this(context);
        this.context = context;
        this.boardName = boardName;
        this.query = query == null ? "" : query.toLowerCase().trim();
        this.abbrev = abbrev;
        this.header = header;
        this.boardSortType = boardSortType;
        //initRandomGenerator();
        ChanBoard.initBoards(context);
    }

    /*
    protected void initRandomGenerator() { // to allow repeatable positions for ads
        if (boardName == null)
            return;
        generatorSeed = boardName.hashCode();
        generator = new Random(generatorSeed);
    }
    */

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	if (DEBUG) Log.i(TAG, "loadInBackground /" + boardName + "/");
        MatrixCursor matrixCursor = ChanThread.buildMatrixCursor();
        if (ChanBoard.META_BOARD_CODE.equals(boardName))
            loadMetaBoard(matrixCursor);
        else if (ChanBoard.isMetaBoard(boardName))
            loadMetaTypeBoard(matrixCursor);
        else if (ChanBoard.FAVORITES_BOARD_CODE.equals(boardName))
            loadFavoritesBoard(matrixCursor);
        else
            loadBoard(matrixCursor);
        registerContentObserver(matrixCursor, mObserver);
        return matrixCursor;
    }

    protected void loadMetaBoard(MatrixCursor matrixCursor) {
        if (DEBUG) Log.i(TAG, "loadMetaBoard");
        boolean showNSFWBoards = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);
        List<BoardType> sorted = new ArrayList<BoardType>();
        for (BoardType boardType : BoardType.values()) {
            //if (!boardType.isCategory())
            //    continue;
            if (!boardType.isSFW() && !showNSFWBoards)
                continue;
            if (boardName.equals(boardType.boardCode()))
                continue;
            sorted.add(boardType);
        }

        final AlphanumComparator comparator = new AlphanumComparator();
        Collections.sort(sorted, new Comparator<BoardType>() {
            @Override
            public int compare(BoardType lhs, BoardType rhs) {
                return comparator.compare(lhs.boardCode(), rhs.boardCode());
            }
        });
        for (BoardType type : sorted) {
            Object[] row = ChanThread.makeBoardTypeRow(context, type);
            matrixCursor.addRow(row);
            if (DEBUG) Log.v(TAG, "Added board row: " + Arrays.toString(row));
        }
        if (DEBUG) Log.i(TAG, "Loading meta board complete");
    }

    protected void loadMetaTypeBoard(MatrixCursor matrixCursor) {
        boolean showNSFWBoards = ChanBoard.showNSFW(context);
        if (DEBUG) Log.i(TAG, "loadMetaTypeBoard showNSFWBoards=" + showNSFWBoards);
        List<ChanBoard> sorted = new ArrayList<ChanBoard>();
        for (BoardType boardType : BoardType.values()) {
            if (BoardType.ALL_BOARDS == boardType)
                continue;
            if (!boardType.isCategory())
                continue;
            if (!boardType.isSFW() && !showNSFWBoards)
                continue;
            if (!ChanBoard.isMetaBoard(boardType.boardCode()))
                continue;
            if (!boardName.equals(boardType.boardCode()) && !boardName.equals(ChanBoard.ALL_BOARDS_BOARD_CODE))
                continue;
            List<ChanBoard> boards = ChanBoard.getBoardsByType(context, boardType);
            if (boards == null || boards.isEmpty())
                continue;
            if (DEBUG) Log.i(TAG, "Found " + boards.size() + " boards = " + Arrays.toString(boards.toArray()));
            for (ChanBoard board : boards) {
                if (board.isMetaBoard())
                    continue;
                if (ChanBoard.isRemoved(board.link)) {
                    if (DEBUG) Log.i(TAG, "Board /" + board.link + "/ has been removed from 4chan");
                    continue;
                }
                sorted.add(board);
            }
        }

        final AlphanumComparator comparator = new AlphanumComparator();
        Collections.sort(sorted, new Comparator<ChanBoard>() {
            @Override
            public int compare(ChanBoard lhs, ChanBoard rhs) {
                return comparator.compare(lhs.link, rhs.link);
            }
        });

        for (ChanBoard board : sorted) {
            Object[] row = board.makeRow(context);
            matrixCursor.addRow(row);
            if (DEBUG) Log.i(TAG, "Added board row: " + Arrays.toString(row));
        }
        if (DEBUG) Log.i(TAG, "Loading boards complete");
    }

    protected void loadFavoritesBoard(MatrixCursor matrixCursor) {
        ChanBoard board = ChanFileStorage.loadBoardData(getContext(), boardName);
        if (DEBUG)  {
            Log.i(TAG, "loadFavoritesBoard /" + boardName + "/");
            Log.i(TAG, "threadcount=" + (board.threads != null ? board.threads.length : 0
                    + " loadedthreadcount=" + (board.loadedThreads != null ? board.loadedThreads.length : 0)));
        }

        if (!board.hasData()) {
            Log.i(TAG, "Favorites board doesn't have data, exiting");
            return;
        }

        if (DEBUG) Log.i(TAG, "Loading " + board.threads.length + " favorite boards");
        List<ChanPost> sorted = new ArrayList<ChanPost>();
        for (ChanPost thread : board.threads) {
            if (DEBUG) Log.i(TAG, "Loading favorite board " + thread);
            if (!ChanBoard.FAVORITES_BOARD_CODE.equals(board.link) && thread.no <= 0) {
                if (DEBUG) Log.i(TAG, "Skipped zero thread " + thread);
                continue;
            }
            if (ChanBoard.isRemoved(thread.board)) {
                if (DEBUG) Log.i(TAG, "Board /" + thread.board + "/ has been removed from 4chan");
                continue;
            }
            if (thread.no <= 0)
                sorted.add(thread);
        }

        final AlphanumComparator comparator = new AlphanumComparator();
        Collections.sort(sorted, new Comparator<ChanPost>() {
            @Override
            public int compare(ChanPost lhs, ChanPost rhs) {
                return comparator.compare(lhs.board, rhs.board);
            }
        });

        int i = 0;
        for (ChanPost thread : sorted) {
            String boardCode = thread.board;
            String name = ChanBoard.getName(context, boardCode);
            int imageId = ChanBoard.getImageResourceId(boardCode, 0, 0);
            if (DEBUG) Log.i(TAG, "loadBoard adding board link row /" + boardCode
                    + "/ name=" + name
                    + " resourceId=" + imageId);
            Object[] row = ChanThread.makeBoardRow(context, boardCode, name, imageId, 0);
            matrixCursor.addRow(row);
            if (DEBUG) Log.v(TAG, "Added board row: " + Arrays.toString(row));
        }
        i++;
        if (DEBUG) Log.i(TAG, "Loaded " + i + " favorite boards");
    }

    protected void loadBoard(MatrixCursor matrixCursor) {
        ChanBoard board = ChanFileStorage.loadBoardData(getContext(), boardName);
        if (DEBUG)  {
            Log.i(TAG, "loadBoard /" + boardName + "/");
            Log.i(TAG, "boardSortType=" + boardSortType + " ");
            Log.i(TAG, "threadcount=" + (board.threads != null ? board.threads.length : 0
                    + " loadedthreadcount=" + (board.loadedThreads != null ? board.loadedThreads.length : 0)));
        }

        if (board.shouldSwapThreads())
        { // auto-update if we have no threads to show
            if (DEBUG) Log.i(TAG, "auto-swapping /" + boardName + "/");
            board.swapLoadedThreads();
        }

        if (!board.hasData()) {
            if (DEBUG) Log.i(TAG, "board /" + boardName + "/ has no data, exiting cursor load");
            return;
        }

        if (!board.isVirtualBoard() && header && query.isEmpty()) {
            Object[] headerRow = board.makeHeaderRow(context);
            matrixCursor.addRow(headerRow);
        }

        //if (!board.isVirtualBoard()) {
        //    matrixCursor.addRow(board.makeThreadAdRow(getContext(), 0));
        //}

        if (DEBUG) Log.i(TAG, "Loading " + board.threads.length + " threads");
        //int adSpace = MINIMUM_AD_SPACING;
        /*
        int numQueryMatches = 0;
        int i = 0;
        for (ChanThread thread : board.threads) {
            boolean match = loadThread(matrixCursor, board, thread, i);
            if (match)
                numQueryMatches++;
        }
        */
        for (ChanThread thread : board.threads)
            loadThread(matrixCursor, board, thread);
        if (DEBUG) Log.i(TAG, "Loaded " + board.threads.length + " threads");

        //addRelatedBoards(matrixCursor, board);
        // always put an ad at the bottom
        //if (!board.isVirtualBoard())
        //    matrixCursor.addRow(board.makeThreadAdRow(getContext(), i));
    }

    //protected boolean loadThread(MatrixCursor matrixCursor, ChanBoard board, ChanThread thread, int i) {
    protected void loadThread(MatrixCursor matrixCursor, ChanBoard board, ChanThread thread) {
        if (DEBUG) Log.i(TAG, "Loading thread " + thread);
        if (ChanBlocklist.isBlocked(context, thread)) {
            if (DEBUG) Log.i(TAG, "Skipped blocked thread " + thread);
            //return false;
            return;
        }
        if (!ChanBoard.FAVORITES_BOARD_CODE.equals(board.link) && thread.no <= 0) {
            if (DEBUG) Log.i(TAG, "Skipped zero thread " + thread);
            //return false;
            return;
        }
        if (!thread.matchesQuery(query)) {
            if (DEBUG) Log.i(TAG, "Skipped non-matching to query thread " + thread);
            return;
        }
        if (ChanBoard.isRemoved(thread.board)) {
            if (DEBUG) Log.i(TAG, "Board /" + thread.board + "/ has been removed from 4chan");
            //return false;
            return;
        }
        //boolean matchedQuery = !query.isEmpty();
        Object row[];
        if (thread.no <= 0) {
            String name = ChanBoard.getName(context, thread.board);
            int imageId = ChanBoard.getImageResourceId(thread.board, 0, 0);
            if (DEBUG) Log.i(TAG, "loadBoard adding board link row /" + thread.board
                    + "/ name=" + name
                    + " resourceId=" + imageId);
            row = ChanThread.makeBoardRow(context, thread.board, name, imageId, 0);
        }
        else {
            if (DEBUG) Log.i(TAG, "loadBoard adding thread row " + thread);
            //row = ChanThread.makeRow(context, thread, query, 0, !board.isVirtualBoard(), abbrev);
            row = ChanThread.makeRow(context, thread, query, 0, !board.isVirtualBoard(), abbrev);
        }
        matrixCursor.addRow(row);
        //i++;
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
        //return matchedQuery;
    }

    /*
    protected void addRelatedBoards(MatrixCursor matrixCursor, ChanBoard board) {
        long threadNo = (board.threads != null && board.threads.length >= 3 && board.threads[2] != null)
                ? board.threads[2].no // skip over stickies
                : 0; // used to cause stable but random related board images
        int stringId;
        if (ChanBoard.POPULAR_BOARD_CODE.equals(board.link))
            stringId = R.string.board_related_boards_title_popular_format;
        else if (ChanBoard.LATEST_BOARD_CODE.equals(board.link))
            stringId = R.string.board_related_boards_title_latest_format;
        else if (ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(board.link))
            stringId = R.string.board_related_boards_title_recent_format;
        else
            stringId = R.string.board_related_boards_title_format;
        if (ChanBoard.WATCHLIST_BOARD_CODE.equals(board.link)) {
            // skip until we figure out how to do it
        }
        else if (board.isVirtualBoard()) {
            /*
            Set<String> relatedCodes = new HashSet<String>();
            for (ChanPost thread : board.threads)
                if (thread != null && thread.board != null && !thread.board.isEmpty())
                    relatedCodes.add(thread.board);
            for (String relatedCode : relatedCodes) {
                ChanBoard relatedBoard = ChanBoard.getBoardByCode(context, relatedCode);
                if (relatedBoard != null) {
                    ChanBoard addBoard = relatedBoard.copy();
                    matrixCursor.addRow(addBoard.makeRow(context, threadNo));
                }
            }
        }
        else { // add related boards
            //matrixCursor.addRow(ChanThread.makeButtonRow(boardName, context.getString(R.string.new_thread_short).toUpperCase()));
            //String desc = String.format(context.getString(R.string.board_related_boards_desc), board.link);
            //matrixCursor.addRow(ChanThread.makeTitleRow(boardName,
            //        context.getString(R.string.board_related_boards_title), desc));
            /*
            for (ChanBoard relatedBoard : board.relatedBoards(context, threadNo)) {
                ChanBoard addBoard = relatedBoard.copy();
                matrixCursor.addRow(addBoard.makeRow(context, threadNo));
            }
        }
    }
    */

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
