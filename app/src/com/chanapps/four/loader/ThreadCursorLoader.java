package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.content.Context;
import android.content.Intent;
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
import com.chanapps.four.service.ThreadLoadService;

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
        ChanThread thread;
        try {
            thread = ChanFileStorage.loadThreadData(getContext(), boardName, threadNo);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't load thread from storage " + boardName + "/" + threadNo, e);
            thread = null;
        }
        int isDead = thread != null && thread.isDead ? 1 : 0;
        Log.i(TAG, "Thread dead status for " + boardName + "/" + threadNo + " is " + isDead);
        if (thread == null) { // this shouldn't happen, so reload
            Log.i(TAG, "Reloading thread " + boardName + "/" + threadNo + " - starting ThreadLoadService");
            Intent threadIntent = new Intent(context, ThreadLoadService.class);
            threadIntent.putExtra(ChanHelper.BOARD_CODE, boardName);
            threadIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
            // threadIntent.putExtra(ChanHelper.RETRIES, ++retries);
            context.startService(threadIntent);
        }
        else {
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
                            Log.v(TAG, "added cursor row text-only no=" + post.no + " text=" + postText);
                        }
                    }
                } else {
                    String postText = post.resto == 0 ? post.getThreadText(hideAllText) : post.getPostText(hideAllText);
                    matrixCursor.addRow(new Object[] {
                            post.no, boardName, threadNo,
                            post.getThumbnailUrl(), post.getCountryFlagUrl(),
                            postText, post.getHeaderText(), post.getFullText(),
                            post.tn_w, post.tn_h, post.w, post.h, post.tim, isDead, 0, 0});
                    Log.v(TAG, "added cursor row image+text no=" + post.no + " text=" + postText);
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
