package com.chanapps.four.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.chanapps.four.activity.R;
import com.chanapps.four.component.ToastRunnable;
import com.chanapps.four.service.FetchChanDataService;

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
    
    public static final String DEFAULT_WATCHTEXT = "new thread";

    public static final long MAX_DEAD_THREAD_RETENTION_MS = 0; // clear immediately

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
            if (DEBUG) Log.v(TAG, "Thread " + boardCode + "/" + threadNo + " already in watchlist");
            return R.string.thread_already_in_watchlist;
        }
        else {
            String threadPath = getThreadPath(tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
            Set<String> savedWatchlist = getWatchlistFromPrefs(ctx);
            savedWatchlist.add(threadPath);
            saveWatchlist(ctx, savedWatchlist);
            if (DEBUG) Log.v(TAG, "Thread " + threadPath + " added to watchlist");
            return R.string.thread_added_to_watchlist;
        }
    }

    private static void saveWatchlist(Context ctx, Set<String> savedWatchlist) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putStringSet(ChanHelper.THREAD_WATCHLIST, savedWatchlist);
        editor.commit();
        if (DEBUG) Log.v(TAG, "Put watchlist to prefs: " + Arrays.toString(savedWatchlist.toArray()));
    }

    public static void updateThreadInfo(Context ctx, String boardCode, long threadNo, long tim,
                                        String text, String imageUrl, int imageWidth, int imageHeight, boolean isDead) {
        Set<String> savedWatchlist = getWatchlistFromPrefs(ctx);
        synchronized (savedWatchlist) {
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
                if (DEBUG) Log.v(TAG, "Thread " + newThreadPath + " added to watchlist");
            }
        }
    }


    public static boolean isThreadWatched(Context ctx, String boardCode, long threadNo) {
        boolean isWatched = false;
        Set<String> savedWatchlist = getWatchlistFromPrefs(ctx);
        synchronized (savedWatchlist) {
            for (String threadPath : savedWatchlist) {
                String[] components = getThreadPathComponents(threadPath);
                String watchedBoardCode = components[1];
                long watchedThreadNo = Long.valueOf(components[2]);
                if (watchedBoardCode.equals(boardCode) && watchedThreadNo == threadNo) {
                    isWatched = true;
                    break;
                }
            }
        }
        return isWatched;
    }

    public static List<ChanThread> getWatchedThreads(Context ctx) {
    	List<ChanThread> threadList = new ArrayList<ChanThread>();
        Set<String> savedWatchlist = getWatchlistFromPrefs(ctx);
        for (String threadPath : savedWatchlist) {
            String[] components = getThreadPathComponents(threadPath);
            ChanThread thread = new ChanThread();
            thread.tim = Long.valueOf(components[0]);
            thread.board = components[1];
            thread.no = Long.valueOf(components[2]);
            thread.sub = components[3];
            //thread.imageurl = components[4];
            thread.w = Integer.valueOf(components[5]);
            thread.h = Integer.valueOf(components[6]);
        }
        
        return threadList;
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
        if (DEBUG) Log.v(TAG, "Clearing watchlist...");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.remove(ChanHelper.THREAD_WATCHLIST);
        editor.commit();
        if (DEBUG) Log.v(TAG, "Watchlist cleared");
    }

    private static void cleanWatchlistSynchronous(Context ctx, boolean cleanAllDeadThreads) {
        if (DEBUG) Log.v(TAG, "Cleaning watchlist...");
        List<Long> deadTims = getDeadTims(ctx, cleanAllDeadThreads);
        deleteThreadsFromWatchlist(ctx, deadTims);
        if (DEBUG) Log.i(TAG, "Watchlist cleaned");
    }

    public static void deleteThreadFromWatchlist(Context ctx, long tim) {
        List<Long> deleteTims = new ArrayList<Long>();
        deleteTims.add(tim);
        deleteThreadsFromWatchlist(ctx, deleteTims);

        if (ctx instanceof Activity) // probably better way to do this
            ((Activity) ctx).runOnUiThread(new ToastRunnable(ctx, ctx.getString(R.string.dialog_deleted_from_watchlist)));
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
        if (DEBUG) Log.v(TAG, "Put watchlist to prefs: " + Arrays.toString(savedWatchlist.toArray()));
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
        if (DEBUG) Log.v(TAG, "Getting watchlist from prefs...");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Set<String> savedWatchlist = prefs.getStringSet(ChanHelper.THREAD_WATCHLIST, new HashSet<String>());
        if (DEBUG) Log.v(TAG, "Loaded watchlist from prefs:" + Arrays.toString(savedWatchlist.toArray()));
        return savedWatchlist;
    }

    public static void fetchWatchlistThreads(Context context) {
        if (DEBUG) Log.i(TAG, "fetchWatchlistThreads");
        Set<String> threadPaths = new HashSet<String>(); // copy so we don't get concurrent exception
        Set<String> origThreadPaths = ChanWatchlist.getWatchlistFromPrefs(context);
        if (DEBUG) Log.i(TAG, "origThreadPaths: " + Arrays.toString(origThreadPaths.toArray()));
        synchronized (origThreadPaths) {
            for (String path : origThreadPaths) {
                threadPaths.add(new String(path));
            }
        }
        if (DEBUG) Log.i(TAG, "XXXXthreadPaths: " + Arrays.toString(threadPaths.toArray()));
        for (String threadPath : threadPaths) {
            if (DEBUG) Log.i(TAG, "getting thread info for path: " + threadPath);
            try {
                String boardCode = ChanWatchlist.getBoardCodeFromThreadPath(threadPath);
                long threadNo = ChanWatchlist.getThreadNoFromThreadPath(threadPath);
                // FIXME should say if !threadIsDead we should store this somewhere
                if (DEBUG) Log.i(TAG, "Starting load service for watching thread " + boardCode + "/" + threadNo);
                FetchChanDataService.scheduleThreadFetch(context, boardCode, threadNo);
            }
            catch (Exception e) {
                Log.e(TAG, "Exception parsing watchlist", e);
            }
        }
    }

}
