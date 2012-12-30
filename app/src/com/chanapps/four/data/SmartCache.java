package com.chanapps.four.data;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.Tag;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chanapps.four.service.BoardLoadService;
import com.chanapps.four.service.ThreadLoadService;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 10/30/12
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class SmartCache {

    private static final String TAG = SmartCache.class.getSimpleName();

    private static final long MAX_FAVORITES_CACHE_REFRESH_INTERVAL_MS = 360000; // 6 minutes
    private static final long MAX_WATCHLIST_CACHE_REFRESH_INTERVAL_MS = 360000; // 6 minutes
    private static final long MAX_NO_BOARD_CACHE_REFRESH_INTERVAL_MS = 3600000; // 1 hour

    private static boolean isWifiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Service.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi.isConnected();
    }

    public static void fillCache(Context context) {
        Log.i(TAG, "Starting cache fill operation");

        if (!isWifiConnected(context)) // don't blow the mobile data connection, this could be smarter
            return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (shouldFillCache(prefs, ChanHelper.LAST_FAVORITES_CACHE_TIME, MAX_FAVORITES_CACHE_REFRESH_INTERVAL_MS)) {
            Log.i(TAG, "Filling favorites cache");
            cacheFavoritesOnLaunch(context);
            saveCacheFillTime(prefs, ChanHelper.LAST_FAVORITES_CACHE_TIME);
        }
        else {
            Log.i(TAG, "Favorites cache interval has not expired, not filling cache");
        }

        if (shouldFillCache(prefs, ChanHelper.LAST_WATCHLIST_CACHE_TIME, MAX_WATCHLIST_CACHE_REFRESH_INTERVAL_MS)) {
            Log.i(TAG, "Filling watchlist cache");
            cacheWatchlistOnLaunch(context);
            saveCacheFillTime(prefs, ChanHelper.LAST_WATCHLIST_CACHE_TIME);
        }
        else {
            Log.i(TAG, "Watchlist cache interval has not expired, not filling cache");
        }

        if (shouldFillCache(prefs, ChanHelper.LAST_NO_BOARD_CACHE_TIME, MAX_NO_BOARD_CACHE_REFRESH_INTERVAL_MS)) {
            Log.i(TAG, "Filling missing board cache");
            cacheBoardsOnLaunch(context);
            saveCacheFillTime(prefs, ChanHelper.LAST_NO_BOARD_CACHE_TIME);
        }
        else {
            Log.i(TAG, "No board cache interval has not expired, not filling cache");
        }
    }

    private static boolean shouldFillCache(SharedPreferences prefs, String cachePref, long refreshIntervalMs) {
        long lastCacheTime = prefs.getLong(cachePref, 0);
        long now = (new Date()).getTime();
        return Math.abs(now - lastCacheTime) > refreshIntervalMs;
    }

    private static void saveCacheFillTime(SharedPreferences prefs, String cachePref) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(cachePref, (new Date()).getTime());
        editor.commit();
    }

    private static void cacheFavoritesOnLaunch(Context context) {
        List<ChanBoard> boards = ChanBoard.getBoardsByType(context, ChanBoard.Type.FAVORITES);
        for (ChanBoard board : boards) {
            Log.i(TAG, "Starting load service for favorite board " + board.link);
            BoardLoadService.startService(context, board.link);
        }
    }

    private static void cacheWatchlistOnLaunch(Context context) {
        Set<String> threadPaths = ChanWatchlist.getWatchlistFromPrefs(context);
        for (String threadPath : threadPaths) {
            String boardCode = ChanWatchlist.getBoardCodeFromThreadPath(threadPath);
            long threadNo = ChanWatchlist.getThreadNoFromThreadPath(threadPath);
            // FIXME should say if !threadIsDead we should store this somewhere
            Log.i(TAG, "Starting load service for watching thread " + boardCode + "/" + threadNo);
            ThreadLoadService.startService(context, boardCode, threadNo);
        }
    }

    private static void cacheBoardsOnLaunch(Context context) {
        List<ChanBoard> boards = ChanBoard.getBoards(context);
        for (ChanBoard board : boards) {
            if (!ChanFileStorage.isBoardCachedOnDisk(context, board.link)) { // if user never visited board before
                Log.i(TAG, "Starting load service for uncached board " + board.link);
                BoardLoadService.startService(context, board.link);
            }
        }
    }

}
