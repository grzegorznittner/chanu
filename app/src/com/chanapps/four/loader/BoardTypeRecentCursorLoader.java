package com.chanapps.four.loader;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;
import com.chanapps.four.data.*;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

//import android.content.AsyncTaskLoader;

public class BoardTypeRecentCursorLoader extends BoardCursorLoader {

    protected static final String TAG = BoardTypeRecentCursorLoader.class.getSimpleName();
    protected static final boolean DEBUG = false;

    public BoardTypeRecentCursorLoader(Context context) {
        super(context);
        this.context = context;
        this.boardName = "";
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	if (DEBUG) Log.i(TAG, "loadInBackground");
        MatrixCursor matrixCursor = ChanThread.buildMatrixCursor();
        matrixCursor.addRow(ChanThread.makeBoardTypeRow(context, BoardType.POPULAR));
        loadBoard(matrixCursor, ChanBoard.POPULAR_BOARD_CODE);
        matrixCursor.addRow(ChanThread.makeBoardTypeRow(context, BoardType.LATEST));
        loadBoard(matrixCursor, ChanBoard.LATEST_BOARD_CODE);
        matrixCursor.addRow(ChanThread.makeBoardTypeRow(context, BoardType.LATEST_IMAGES));
        loadBoard(matrixCursor, ChanBoard.LATEST_IMAGES_BOARD_CODE);
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
        for (ChanPost thread : board.threads) {
            if (DEBUG) Log.i(TAG, "Loading thread:" + thread.no);
            if (ChanBlocklist.contains(context, ChanBlocklist.BlockType.TRIPCODE, thread.trip)
                    || ChanBlocklist.contains(context, ChanBlocklist.BlockType.NAME, thread.name)
                    || ChanBlocklist.contains(context, ChanBlocklist.BlockType.EMAIL, thread.email)
                    || ChanBlocklist.contains(context, ChanBlocklist.BlockType.ID, thread.id))
            {
                if (DEBUG) Log.i(TAG, "Skipped thread: " + thread.no);
                continue;
            }
            Object[] row = ChanThread.makeRow(context, thread, "");
            matrixCursor.addRow(row);
            i++;
            if (DEBUG) Log.v(TAG, "Added board row: " + Arrays.toString(row));
        }
        if (DEBUG) Log.i(TAG, "Loaded " + i + " threads");
    }

}