package com.chanapps.four.loader;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;
import com.chanapps.four.data.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//import android.content.AsyncTaskLoader;

public class PopularCursorLoader extends BoardCursorLoader {

    protected static final String TAG = PopularCursorLoader.class.getSimpleName();
    protected static final boolean DEBUG = false;

    public PopularCursorLoader(Context context) {
        super(context);
        this.context = context;
        this.boardName = "";
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	if (DEBUG) Log.i(TAG, "loadInBackground");
        MatrixCursor matrixCursor = ChanThread.buildMatrixCursor(10);
        loadBoard(matrixCursor, ChanBoard.POPULAR_BOARD_CODE);
        loadBoard(matrixCursor, ChanBoard.LATEST_BOARD_CODE);
        loadBoard(matrixCursor, ChanBoard.LATEST_IMAGES_BOARD_CODE);
        addRecommendedBoardLink(matrixCursor);
        registerContentObserver(matrixCursor, mObserver);
        return matrixCursor;
    }

    protected void loadBoard(MatrixCursor matrixCursor, String boardCode) {
        ChanBoard board = ChanFileStorage.loadBoardData(getContext(), boardCode);
        if (DEBUG) Log.i(TAG,
                "board threadcount=" + (board.threads != null ? board.threads.length : 0)
                        + "board loadedthreadcount=" + (board.loadedThreads != null ? board.loadedThreads.length : 0));

        if (board == null || board.threads == null || board.threads.length == 0 || board.defData)
            return;

        if (DEBUG) Log.i(TAG, "Loading " + board.threads.length + " threads");
        int i = 0;
        for (ChanThread thread : board.threads) {
            if (DEBUG) Log.i(TAG, "Loading thread:" + thread.no);
            if (ChanBlocklist.contains(context, ChanBlocklist.BlockType.TRIPCODE, thread.trip)
                    || ChanBlocklist.contains(context, ChanBlocklist.BlockType.NAME, thread.name)
                    || ChanBlocklist.contains(context, ChanBlocklist.BlockType.EMAIL, thread.email)
                    || ChanBlocklist.contains(context, ChanBlocklist.BlockType.ID, thread.id))
            {
                if (DEBUG) Log.i(TAG, "Skipped thread: " + thread.no);
                continue;
            }
            Object[] row = ChanThread.makeRow(context, thread, "", threadFlag(boardCode), false, false);
            matrixCursor.addRow(row);
            i++;
            if (DEBUG) Log.v(TAG, "Added board row: " + Arrays.toString(row));
        }
        if (DEBUG) Log.i(TAG, "Loaded " + i + " threads");
    }

    protected int threadFlag(String boardCode) {
        if (ChanBoard.POPULAR_BOARD_CODE.equals(boardCode))
            return ChanThread.THREAD_FLAG_POPULAR_THREAD;
        else if (ChanBoard.LATEST_BOARD_CODE.equals(boardCode))
            return ChanThread.THREAD_FLAG_LATEST_POST;
        else if (ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(boardCode))
            return ChanThread.THREAD_FLAG_RECENT_IMAGE;
        else
            return 0;
    }

    protected void addRecommendedBoardLink(MatrixCursor matrixCursor) {
        String[] boardCodes = { "a", "v", "vg", "fit", "mu", "sp", "co", "g", "tv" }; // b s gif
        List<String> boardCodeList = new ArrayList<String>(Arrays.asList(boardCodes));
        Collections.shuffle(boardCodeList);
        for (String boardCode : boardCodeList) {
            ChanBoard board = ChanBoard.getBoardByCode(getContext(), boardCode);
            matrixCursor.addRow(board.makeRow(context, 0));
        }
    }

}