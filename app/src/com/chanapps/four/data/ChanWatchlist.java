package com.chanapps.four.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ToastRunnable;

import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
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

    private static String getThreadPath(long tim, String boardCode, long threadNo,
                                        String text, String imageUrl, int imageWidth, int imageHeight)
    {
        return tim + FIELD_SEPARATOR
                + boardCode + FIELD_SEPARATOR
                + threadNo + FIELD_SEPARATOR
                + (text == null ? "" : text.replaceAll(FIELD_SEPARATOR_REGEX,"")) + FIELD_SEPARATOR
                + (imageUrl == null ? "" : imageUrl.replaceAll(FIELD_SEPARATOR_REGEX,"")) + FIELD_SEPARATOR
                + imageWidth + FIELD_SEPARATOR
                + imageHeight;
    }

    public static String[] getThreadPathComponents(String threadPath) {
        return threadPath.split(FIELD_SEPARATOR_REGEX);
    }

    public static String getBoardCodeFromThreadPath(String threadPath) {
        return threadPath.split(FIELD_SEPARATOR_REGEX)[1];
    }

    public static long getThreadNoFromThreadPath(String threadPath) {
        return Long.valueOf(threadPath.split(FIELD_SEPARATOR_REGEX)[2]);
    }

    public static void watchThread(
            Context ctx,
            long tim,
            String boardCode,
            long threadNo,
            String text,
            String imageUrl,
            int imageWidth,
            int imageHeight) {
        Set<String> savedWatchlist = getWatchlistFromPrefs(ctx);
        String threadPath = getThreadPath(tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
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
            Log.d(TAG, "Put watchlist to prefs: " + Arrays.toString(savedWatchlist.toArray()));
            Toast.makeText(ctx, R.string.thread_added_to_watchlist, Toast.LENGTH_SHORT);
        }
    }

    public static class CleanWatchlistTask extends AsyncTask<Void, Void, Void> {
        private Context ctx;
        private BaseAdapter adapter;
        private boolean userInteraction = false;
        public CleanWatchlistTask(Context ctx, BaseAdapter adapter, boolean userInteraction) {
            this.ctx = ctx;
            this.adapter = adapter;
            this.userInteraction = userInteraction;
        }
        public Void doInBackground(Void... params) {
            if (userInteraction)
                Toast.makeText(ctx, R.string.dialog_cleaning_watchlist, Toast.LENGTH_SHORT).show();
            cleanWatchlistSynchronous(ctx);
            return null;
        }
        protected void onPostExecute(Void result) {
            if (adapter != null)
                adapter.notifyDataSetChanged();
            if (userInteraction)
                Toast.makeText(ctx, R.string.dialog_cleaned_watchlist, Toast.LENGTH_SHORT).show();
        }
    }

    public static void clearWatchlist(Context ctx) {
        Log.i(TAG, "Clearing watchlist...");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.remove(ChanHelper.THREAD_WATCHLIST);
        editor.commit();
        Log.i(TAG, "Watchlist cleared");
    }

    private static void cleanWatchlistSynchronous(Context ctx) {
        Log.i(TAG, "Cleaning watchlist...");
        List<Long> deadTims = getDeadTims(ctx);
        deleteThreadsFromWatchlist(ctx, deadTims);
        Log.i(TAG, "Watchlist cleaned");
    }

    public static void deleteThreadFromWatchlist(Context ctx, long tim) {
        List<Long> deleteTims = new ArrayList<Long>();
        deleteTims.add(tim);
        deleteThreadsFromWatchlist(ctx, deleteTims);
    }

    public static void deleteThreadsFromWatchlist(Context ctx, List<Long> tims) {
        Set<String> savedWatchlist = getWatchlistFromPrefs(ctx);
        Set<String> toDelete = new HashSet<String>();
        for (String s : savedWatchlist) {
            for (long tim : tims) { // order n^2, my CS prof would kill me
                if (s.startsWith(tim + FIELD_SEPARATOR)) {
                    toDelete.add(s);
                    Log.i(TAG, "Thread " + s + " deleted from watchlist");
                    break;
                }
            }
        }
        savedWatchlist.removeAll(toDelete);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putStringSet(ChanHelper.THREAD_WATCHLIST, savedWatchlist);
        editor.commit();
        Log.d(TAG, "Put watchlist to prefs: " + Arrays.toString(savedWatchlist.toArray()));
    }

    public static List<String> getSortedWatchlistFromPrefs(Context ctx) {
        Set<String> threadPaths = getWatchlistFromPrefs(ctx);
        List<String> threadPathList = new ArrayList<String>();
        threadPathList.addAll(threadPaths);
        Collections.sort(threadPathList);
        Collections.reverse(threadPathList);
        return threadPathList;
    }

    public static List<Long> getDeadTims(Context ctx) {
        Set<String> threadPaths = getWatchlistFromPrefs(ctx);
        List<Long> deadTims = new ArrayList<Long>();
        for (String threadPath : threadPaths) {
            String[] threadComponents = getThreadPathComponents(threadPath);
            long tim = Long.valueOf(threadComponents[0]);
            String boardCode = threadComponents[1];
            long threadNo = Long.valueOf(threadComponents[2]);
            try {
                ChanThread thread = ChanFileStorage.loadThreadData(ctx, boardCode, threadNo);
                if (thread.isDead)
                    deadTims.add(tim);
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't load thread to determine dead status " + boardCode + "/" + threadNo, e);
            }
        }
        return deadTims;
    }

    public static Set<String> getWatchlistFromPrefs(Context ctx) {
        Log.i(TAG, "Getting watchlist from prefs...");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Set<String> savedWatchlist = prefs.getStringSet(ChanHelper.THREAD_WATCHLIST, new HashSet<String>());
        Log.d(TAG, "Loaded watchlist from prefs:" + Arrays.toString(savedWatchlist.toArray()));
        return savedWatchlist;
    }

}
