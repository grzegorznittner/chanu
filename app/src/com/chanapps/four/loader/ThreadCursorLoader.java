package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.preference.PreferenceManager;
import android.util.Log;

import android.widget.GridView;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;

public class ThreadCursorLoader extends BoardCursorLoader {

    private static final String TAG = ThreadCursorLoader.class.getSimpleName();
    private static final boolean DEBUG = false;
    protected SharedPreferences prefs;

    protected long threadNo;
    protected int numGridColumns;

    protected ThreadCursorLoader(Context context) {
        super(context);
    }

    private static final int DEFAULT_NUM_GRID_COLUMNS_PORTRAIT = 2;
    private static final int DEFAULT_NUM_GRID_COLUMNS_LANDSCAPE = 3;

    public ThreadCursorLoader(Context context, String boardName, long threadNo, GridView gridView) {
        this(context);
        this.context = context;
        this.boardName = boardName;
        this.threadNo = threadNo;
        ChanHelper.Orientation orientation = ChanHelper.getOrientation(context);
        int defaultNumColumns = (orientation == ChanHelper.Orientation.PORTRAIT) ? DEFAULT_NUM_GRID_COLUMNS_PORTRAIT : DEFAULT_NUM_GRID_COLUMNS_LANDSCAPE;
        int currentNumGridColumns = gridView.getNumColumns();
        this.numGridColumns = (gridView == null || currentNumGridColumns <= 0) ? defaultNumColumns : currentNumGridColumns;
        if (threadNo == 0) {
            throw new ExceptionInInitializerError("Can't have zero threadNo in a thread cursor loader");
        }
        ChanPost.initClickForMore(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	try {
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        boolean hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, false);
        ChanThread thread = null;
        try {
            thread = ChanFileStorage.loadThreadData(getContext(), boardName, threadNo);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't load thread from storage " + boardName + "/" + threadNo, e);
            thread = null;
        }
    	if (DEBUG) Log.i(TAG, "loadInBackground " + thread.board + "/" + thread.no + " num posts " + (thread.posts != null ? thread.posts.length : 0));
        int isDead = thread != null && thread.isDead ? 1 : 0;
        if (DEBUG) Log.i(TAG, "Thread dead status for " + boardName + "/" + threadNo + " is " + isDead);
        MatrixCursor matrixCursor = new MatrixCursor(ChanHelper.POST_COLUMNS);
        Object[] currentRow = null;
        for (ChanPost post : thread.posts) {
            post.isDead = thread.isDead; // inherit from parent
            if (post.tn_w <= 0 || post.tim == 0) {
                if (!hideAllText || post.resto == 0) {
                    String postText = post.resto == 0
                            ? post.getThreadText(hideAllText, hidePostNumbers)
                            : post.getPostText(hideAllText, hidePostNumbers);
                    if (postText != null && !postText.isEmpty()) {
                        currentRow = new Object[] {
                                post.no, boardName, threadNo,
                                "", post.getCountryFlagUrl(),
                                postText, post.getHeaderText(), post.getFullText(hideAllText, hidePostNumbers),
                                post.tn_w, post.tn_h, post.w, post.h, post.tim, post.spoiler,
                                post.getSpoilerText(), post.getExifText(), isDead, 0, 0};
                        matrixCursor.addRow(currentRow);
                        if (DEBUG) Log.v(TAG, "added cursor row text-only no=" + post.no + " text=" + postText);
                    }
                }
            } else {
                String postText = post.resto == 0
                        ? post.getThreadText(hideAllText, hidePostNumbers)
                        : post.getPostText(hideAllText, hidePostNumbers);
                currentRow = new Object[] {
                        post.no, boardName, threadNo,
                        post.getThumbnailUrl(), post.getCountryFlagUrl(),
                        postText, post.getHeaderText(), post.getFullText(hideAllText, hidePostNumbers),
                        post.tn_w, post.tn_h, post.w, post.h, post.tim, post.spoiler,
                        post.getSpoilerText(), post.getExifText(), isDead, 0, 0};
                matrixCursor.addRow(currentRow);
                if (DEBUG) Log.v(TAG, "added cursor row image+text no=" + post.no + " spoiler=" + post.spoiler + " text=" + postText);
            }
            if (post.resto == 0) { // for initial thread, add extra null item to support full-width header
                if (DEBUG) Log.v(TAG, "added extra null rows for grid columns=1.." + numGridColumns);
                for (int i = 1; i < numGridColumns; i++) {
                    Object[] nullRow = currentRow.clone();
                    nullRow[0] = 0; // set postNo to zero to signal to rest of system that this is a null post
                    matrixCursor.addRow(nullRow);
                }
            }
        }
        if (thread.posts.length > 0) {
            registerContentObserver(matrixCursor, mObserver);
            return matrixCursor;
        }
        else {
            return null;
        }
    	} catch (Exception e) {
    		Log.e(TAG, "loadInBackground", e);
    		return null;
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
