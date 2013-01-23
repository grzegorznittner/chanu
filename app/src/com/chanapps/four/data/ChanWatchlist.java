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
import com.chanapps.four.service.FetchChanDataService;

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
    private static final boolean DEBUG = false;
    
    public static final String DEFAULT_WATCHTEXT = "WatchingThread";

    public static final long MAX_DEAD_THREAD_RETENTION_MS = 604800000; // 1 week

    private static final String FIELD_SEPARATOR = "\t";
    private static final String FIELD_SEPARATOR_REGEX = "\\t";

    private static String getThreadPath(long tim, String boardCode, long threadNo,
                                        String text, String imageUrl, int imageWidth, int imageHeight)
    {
        String[] components = {
                Long.toString(tim),
                boardCode,
                Long.toString(threadNo),
                text == null ? "" : text.replaceAll(FIELD_SEPARATOR_REGEX,""),
                imageUrl == null ? "" : imageUrl.replaceAll(FIELD_SEPARATOR_REGEX,""),
                Integer.toString(imageWidth),
                Integer.toString(imageHeight)
        };
        return getThreadPath(components);
    }

    private static String getThreadPath(String[] components) {
        String threadPath = "";
        for (int i = 0; i < components.length; i++) {
            threadPath += components[i];
            if (i < components.length - 1)
                threadPath += FIELD_SEPARATOR;
        }
        return threadPath;
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

    public static int watchThread(
            Context ctx,
            long tim,
            String boardCode,
            long threadNo,
            String text,
            String imageUrl,
            int imageWidth,
            int imageHeight) {
        if (isThreadWatched(ctx, boardCode, threadNo)) {
            if (DEBUG) Log.i(TAG, "Thread " + boardCode + "/" + threadNo + " already in watchlist");
            return R.string.thread_already_in_watchlist;
        }
        else {
            String threadPath = getThreadPath(tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
            Set<String> savedWatchlist = getWatchlistFromPrefs(ctx);
            savedWatchlist.add(threadPath);
            saveWatchlist(ctx, savedWatchlist);
            if (DEBUG) Log.i(TAG, "Thread " + threadPath + " added to watchlist");
            return R.string.thread_added_to_watchlist;
        }
    }

    private static void saveWatchlist(Context ctx, Set<String> savedWatchlist) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putStringSet(ChanHelper.THREAD_WATCHLIST, savedWatchlist);
        editor.commit();
        if (DEBUG) Log.d(TAG, "Put watchlist to prefs: " + Arrays.toString(savedWatchlist.toArray()));
    }

    public static void updateThreadInfo(Context ctx, String boardCode, long threadNo, long tim,
                                        String text, String imageUrl, int imageWidth, int imageHeight, boolean isDead) {
        Set<String> savedWatchlist = getWatchlistFromPrefs(ctx);
        String[] matchingComponents = null;
        String matchingThreadPath = null;
        for (String threadPath : savedWatchlist) {
            String[] components = getThreadPathComponents(threadPath);
            String watchedBoardCode = components[1];
            long watchedThreadNo = Long.valueOf(components[2]);
            if (watchedBoardCode.equals(boardCode) && watchedThreadNo == threadNo) {
                matchingThreadPath = threadPath;
                matchingComponents = components;
                break;
            }
        }
        if (matchingThreadPath == null) {
            Log.e(TAG, "Couldn't find thread in watchlist to update: " + boardCode + "/" + threadNo);
        }
        else {
            savedWatchlist.remove(matchingThreadPath);
            matchingComponents[0] = Long.toString(tim);
            matchingComponents[3] = text;
            matchingComponents[4] = imageUrl;
            matchingComponents[5] = Integer.toString(imageWidth);
            matchingComponents[6] = Integer.toString(imageHeight);
            String newThreadPath = ChanWatchlist.getThreadPath(matchingComponents);
            savedWatchlist.add(newThreadPath);
            saveWatchlist(ctx, savedWatchlist);
            if (DEBUG) Log.i(TAG, "Thread " + newThreadPath + " added to watchlist");
        }
    }


    public static boolean isThreadWatched(Context ctx, String boardCode, long threadNo) {
        boolean isWatched = false;
        Set<String> savedWatchlist = getWatchlistFromPrefs(ctx);
        for (String threadPath : savedWatchlist) {
            String[] components = getThreadPathComponents(threadPath);
            String watchedBoardCode = components[1];
            long watchedThreadNo = Long.valueOf(components[2]);
            if (watchedBoardCode.equals(boardCode) && watchedThreadNo == threadNo) {
                isWatched = true;
                break;
            }
        }
        return isWatched;
    }

    public static class CleanWatchlistTask extends AsyncTask<Void, Void, String> {
        private Context ctx;
        private BaseAdapter adapter;
        private boolean userInteraction = false;
        public CleanWatchlistTask(Context ctx, BaseAdapter adapter, boolean userInteraction) {
            this.ctx = ctx;
            this.adapter = adapter;
            this.userInteraction = userInteraction;
            if (userInteraction)
                Toast.makeText(ctx, R.string.dialog_cleaning_watchlist, Toast.LENGTH_SHORT).show();
        }
        public String doInBackground(Void... params) {
            boolean cleanAllDeadThreads = userInteraction ? true : false;
            try {
                cleanWatchlistSynchronous(ctx, cleanAllDeadThreads);
                return null;
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't clean watchlist", e);
                return "Watchlist clean error: " + e.getLocalizedMessage();
            }
        }
        protected void onPostExecute(String result) {
            if (result == null) {
                if (userInteraction)
                    Toast.makeText(ctx, R.string.dialog_watchlist_cleaning_error, Toast.LENGTH_SHORT).show();
                return;
            }
            if (adapter != null)
                adapter.notifyDataSetChanged();
            if (userInteraction)
                Toast.makeText(ctx, R.string.dialog_cleaned_watchlist, Toast.LENGTH_SHORT).show();
        }
    }

    public static void clearWatchlist(Context ctx) {
        if (DEBUG) Log.i(TAG, "Clearing watchlist...");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.remove(ChanHelper.THREAD_WATCHLIST);
        editor.commit();
        if (DEBUG) Log.i(TAG, "Watchlist cleared");
    }

    private static void cleanWatchlistSynchronous(Context ctx, boolean cleanAllDeadThreads) {
        if (DEBUG) Log.i(TAG, "Cleaning watchlist...");
        List<Long> deadTims = getDeadTims(ctx, cleanAllDeadThreads);
        deleteThreadsFromWatchlist(ctx, deadTims);
        if (DEBUG) Log.i(TAG, "Watchlist cleaned");
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
                    if (DEBUG) Log.i(TAG, "Thread " + s + " deleted from watchlist");
                    break;
                }
            }
        }
        savedWatchlist.removeAll(toDelete);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putStringSet(ChanHelper.THREAD_WATCHLIST, savedWatchlist);
        editor.commit();
        if (DEBUG) Log.d(TAG, "Put watchlist to prefs: " + Arrays.toString(savedWatchlist.toArray()));
    }

    public static List<String> getSortedWatchlistFromPrefs(Context ctx) {
        Set<String> threadPaths = getWatchlistFromPrefs(ctx);
        List<String> threadPathList = new ArrayList<String>();
        threadPathList.addAll(threadPaths);
        Collections.sort(threadPathList);
        Collections.reverse(threadPathList);
        return threadPathList;
    }

    public static List<Long> getDeadTims(Context ctx, boolean cleanAllDeadThreads) {
        Set<String> threadPaths = new HashSet<String>();
        threadPaths.addAll(getWatchlistFromPrefs(ctx));
        List<Long> deadTims = new ArrayList<Long>();
        long now = (new Date()).getTime();
        for (String threadPath : threadPaths) {
            String[] threadComponents = getThreadPathComponents(threadPath);
            long tim = Long.valueOf(threadComponents[0]);
            String boardCode = threadComponents[1];
            long threadNo = Long.valueOf(threadComponents[2]);
            try {
                ChanThread thread = ChanFileStorage.loadThreadData(ctx, boardCode, threadNo);
                long interval = now - thread.lastFetched;
                boolean threadIsOld = interval > MAX_DEAD_THREAD_RETENTION_MS;
                if (thread.isDead && (cleanAllDeadThreads || threadIsOld))
                    deadTims.add(tim);
            }
            catch (Exception e) {
                if (DEBUG) Log.d(TAG, "Couldn't load thread to determine dead status " + boardCode + "/" + threadNo, e);
            }
        }
        return deadTims;
    }

    public static Set<String> getWatchlistFromPrefs(Context ctx) {
        if (DEBUG) Log.i(TAG, "Getting watchlist from prefs...");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Set<String> savedWatchlist = prefs.getStringSet(ChanHelper.THREAD_WATCHLIST, new HashSet<String>());
        if (DEBUG) Log.d(TAG, "Loaded watchlist from prefs:" + Arrays.toString(savedWatchlist.toArray()));
        return savedWatchlist;
    }

    public static void fetchWatchlistThreads(Context context) {
        Set<String> threadPaths = ChanWatchlist.getWatchlistFromPrefs(context);
        for (String threadPath : threadPaths) {
            String boardCode = ChanWatchlist.getBoardCodeFromThreadPath(threadPath);
            long threadNo = ChanWatchlist.getThreadNoFromThreadPath(threadPath);
            // FIXME should say if !threadIsDead we should store this somewhere
            Log.i(TAG, "Starting load service for watching thread " + boardCode + "/" + threadNo);
            FetchChanDataService.scheduleThreadFetch(context, boardCode, threadNo);
        }
    }

}
