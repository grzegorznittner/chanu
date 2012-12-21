package com.chanapps.four.loader;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.*;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class ThreadCursorLoader extends BoardCursorLoader {

    private static final String TAG = ThreadCursorLoader.class.getSimpleName();
    protected SharedPreferences prefs;

    protected long threadNo;

    protected ThreadCursorLoader(Context context) {
        super(context);
    }

    public ThreadCursorLoader(Context context, String boardName, long threadNo) {
        this(context);
        this.context = context;
        this.boardName = boardName;
        this.threadNo = threadNo;
        if (threadNo == 0) {
            throw new ExceptionInInitializerError("Can't have zero threadNo in a thread cursor loader");
        }
        ChanPost.initClickForMore(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	Log.i(TAG, "loadInBackground");
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        ChanThread thread = ChanFileStorage.loadThreadData(getContext(), boardName, threadNo);
        if (thread != null) {
            MatrixCursor matrixCursor = new MatrixCursor(ChanHelper.POST_COLUMNS);
            for (ChanPost post : thread.posts) {
                if (post.tn_w <= 0 || post.tim == null) {
                    if (!hideAllText) {
                        String postText = (String) post.getPostText();
                        if (postText != null && !postText.isEmpty())
                            matrixCursor.addRow(new Object[] {
                                    post.no, boardName, threadNo, "",
                                    postText, post.getFullText(),
                                    post.tn_w, post.tn_h, post.w, post.h, post.tim, 0, 0});
                    }
                } else {
                    matrixCursor.addRow(new Object[] {
                            post.no, boardName, threadNo, post.getThumbnailUrl(),
                            post.getPostText(hideAllText), post.getFullText(), post.tn_w, post.tn_h, post.w, post.h, post.tim, 0, 0});
                }
            }
            if (thread.posts.length > 0) {
                registerContentObserver(matrixCursor, mObserver);
                return matrixCursor;
            }
            else {
                return null;
            }
        }
        return null;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("boardName="); writer.println(boardName);
        writer.print(prefix); writer.print("threadNo="); writer.println(threadNo);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
