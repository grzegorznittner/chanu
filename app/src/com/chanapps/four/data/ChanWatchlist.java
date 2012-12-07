package com.chanapps.four.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.chanapps.four.activity.R;

import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 12/3/12
 * Time: 6:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanWatchlist {
    
    public static final String TAG = ChanWatchlist.class.getSimpleName();
    private static final String FIELD_SEPARATOR = "\t";
    private static final String FIELD_SEPARATOR_REGEX = "\\t";

    public static String getThreadPath(String boardCode, long threadNo, String text, String imageUrl, int imageWidth, int imageHeight) {
        return boardCode + FIELD_SEPARATOR
                + threadNo + FIELD_SEPARATOR
                + text == null ? "" : text.replaceAll(FIELD_SEPARATOR_REGEX,"") + FIELD_SEPARATOR
                + imageUrl == null ? "" : imageUrl.replaceAll(FIELD_SEPARATOR_REGEX,"") + FIELD_SEPARATOR
                + imageWidth + FIELD_SEPARATOR
                + imageHeight;
    }

    public static String[] getThreadPathComponents(String threadPath) {
        return threadPath.split(FIELD_SEPARATOR_REGEX);
    }

    public static void watchThread(
            Context ctx,
            String boardCode,
            long threadNo,
            String text,
            String imageUrl,
            int imageWidth,
            int imageHeight) {
        Set<String> savedWatchlist = getWatchlistFromPrefs(ctx);
        String threadPath = getThreadPath(boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
        if (savedWatchlist.contains(threadPath)) {
            Log.i(TAG, "Thread " + threadPath + " already in watchlist");
            Toast.makeText(ctx, R.string.thread_already_in_watchlist, Toast.LENGTH_SHORT);
        }
        else {
            savedWatchlist.add(threadPath);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
            editor.putStringSet(ChanHelper.THREAD_WATCHLIST, savedWatchlist);
            editor.commit();
            Log.i(TAG, "Thread " + threadPath + " added to watchlist");
            Log.i(TAG, "Put watchlist to prefs: " + Arrays.toString(savedWatchlist.toArray()));
            Toast.makeText(ctx, R.string.thread_added_to_watchlist, Toast.LENGTH_SHORT);
        }
    }

    public static void clearWatchlist(Context ctx) {
        Log.i(TAG, "Clearing watchlist...");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.remove(ChanHelper.THREAD_WATCHLIST);
        editor.commit();
        Log.i(TAG, "Watchlist cleared");
    }

    public static List<String> getSortedWatchlistFromPrefs(Context ctx) {
        Set<String> threadPaths = getWatchlistFromPrefs(ctx);
        List<String> threadPathList = new ArrayList<String>();
        threadPathList.addAll(threadPaths);
        Collections.sort(threadPathList);
        return threadPathList;
    }

    public static Set<String> getWatchlistFromPrefs(Context ctx) {
        Log.i(TAG, "Getting watchlist from prefs...");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Set<String> savedWatchlist = prefs.getStringSet(ChanHelper.THREAD_WATCHLIST, new HashSet<String>());
        Log.i(TAG, "Loaded watchlist from prefs:" + Arrays.toString(savedWatchlist.toArray()));
        return savedWatchlist;
    }

}
