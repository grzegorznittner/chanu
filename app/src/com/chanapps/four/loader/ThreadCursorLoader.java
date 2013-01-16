package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;

public class ThreadCursorLoader extends BoardCursorLoader {

    private static final String TAG = ThreadCursorLoader.class.getSimpleName();
    private static final boolean DEBUG = true;
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
    	try {
    	if (DEBUG) Log.i(TAG, "loadInBackground");
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        ChanThread thread;
        try {
            thread = ChanFileStorage.loadThreadData(getContext(), boardName, threadNo);
            Log.i(TAG, "Got thread " + thread);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't load thread from storage " + boardName + "/" + threadNo, e);
            thread = null;
        }
        int isDead = thread != null && thread.isDead ? 1 : 0;
        if (DEBUG) Log.i(TAG, "Thread dead status for " + boardName + "/" + threadNo + " is " + isDead);
        MatrixCursor matrixCursor = new MatrixCursor(ChanHelper.POST_COLUMNS);
        for (ChanPost post : thread.posts) {
            post.isDead = thread.isDead; // inherit from parent
            if (post.tn_w <= 0 || post.tim == 0) {
                if (!hideAllText || post.resto == 0) {
                    String postText = post.resto == 0 ? post.getThreadText() : post.getPostText();
                    if (postText != null && !postText.isEmpty()) {
                        matrixCursor.addRow(new Object[] {
                                post.no, boardName, threadNo,
                                "", post.getCountryFlagUrl(),
                                postText, post.getHeaderText(), post.getFullText(),
                                post.tn_w, post.tn_h, post.w, post.h, post.tim, isDead, 0, 0});
                        if (DEBUG) Log.v(TAG, "added cursor row text-only no=" + post.no + " text=" + postText);
                    }
                }
            } else {
                String postText = post.resto == 0 ? post.getThreadText(hideAllText) : post.getPostText(hideAllText);
                matrixCursor.addRow(new Object[] {
                        post.no, boardName, threadNo,
                        post.getThumbnailUrl(), post.getCountryFlagUrl(),
                        postText, post.getHeaderText(), post.getFullText(),
                        post.tn_w, post.tn_h, post.w, post.h, post.tim, isDead, 0, 0});
                if (DEBUG) Log.v(TAG, "added cursor row image+text no=" + post.no + " text=" + postText);
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
