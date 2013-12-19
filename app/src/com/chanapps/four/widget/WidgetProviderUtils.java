package com.chanapps.four.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.GlobalAlarmReceiver;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.FetchPopularThreadsService;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: mpop
 * Date: 6/2/13
 * Time: 1:37 AM
 */
public final class WidgetProviderUtils {

    public static final String TAG = WidgetProviderUtils.class.getSimpleName();
    private static final boolean DEBUG = false;
    public static final String WIDGET_PROVIDER_UTILS = "com.chanapps.four.widget.WidgetProviderUtils";

    public static Set<String> getActiveWidgetPref(Context context) {
        ComponentName widgetProvider = new ComponentName(context, AbstractBoardWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider);
        Set<Integer> activeWidgetIds = new HashSet<Integer>();
        for (int appWidgetId : appWidgetIds) {
            activeWidgetIds.add(appWidgetId);
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetConf = pref.getStringSet(SettingsActivity.PREF_WIDGET_BOARDS, new HashSet<String>());

        Set<String> savedWidgetConf = new HashSet<String>();
        for (String widgetBoard : widgetConf) {
            String[] components = widgetBoard.split(WidgetConf.DELIM);
            int widgetId = Integer.valueOf(components[0]);
            if (activeWidgetIds.contains(widgetId))
                savedWidgetConf.add(widgetBoard);
        }

        if (DEBUG) {
            if (DEBUG) Log.i(WidgetProviderUtils.TAG, "Dumping active widget conf:");
            for (String widgetBoard : savedWidgetConf) {
                if (DEBUG) Log.i(WidgetProviderUtils.TAG, widgetBoard);
            }
        }

        return savedWidgetConf;
    }

    public static void saveWidgetBoardPref(Context context, Set<String> savedWidgetConf) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(SettingsActivity.PREF_WIDGET_BOARDS, savedWidgetConf)
                .commit();
    }

    public static WidgetConf loadWidgetConf(Context context, int appWidgetId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(SettingsActivity.PREF_WIDGET_BOARDS, new HashSet<String>());
        for (String widgetBoard : widgetBoards) {
            WidgetConf widgetConf = new WidgetConf(widgetBoard);
            if (widgetConf.appWidgetId == appWidgetId)
                return widgetConf;
        }
        return null;
    }

    public static void fetchAllWidgets(Context context) {
        if (DEBUG) Log.i(WidgetProviderUtils.TAG, "fetchAllWidgets");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(SettingsActivity.PREF_WIDGET_BOARDS, new HashSet<String>());
        Set<String> boardsToFetch = new HashSet<String>();
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split(WidgetConf.DELIM);
            String widgetBoardCode = components[1];
            boardsToFetch.add(widgetBoardCode);
        }
        for (String boardCode : boardsToFetch) {
            if (DEBUG) Log.i(WidgetProviderUtils.TAG, "fetchAllWidgets board=" + boardCode + " scheduling fetch");
            //if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode))
            //    GlobalAlarmReceiver.fetchWatchlistThreads(context);
            //else
            if (ChanBoard.isPopularBoard(boardCode))
                FetchPopularThreadsService.schedulePopularFetchService(context, false, true);
            else if (ChanBoard.isVirtualBoard(boardCode))
                ;// skip
            else
                FetchChanDataService.scheduleBoardFetch(context, boardCode, false, true);
        }
    }

    public static void update(Context context, int appWidgetId, String widgetType) {
        WidgetConf widgetConf = loadWidgetConf(context, appWidgetId);
        if (widgetConf != null) {
            if (DEBUG) Log.i(WidgetProviderUtils.TAG, "update() calling update widget service for widget=" + appWidgetId + " /" + widgetConf.boardCode + "/");
            Intent updateIntent = new Intent(context, UpdateWidgetService.class);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            updateIntent.putExtra(WIDGET_PROVIDER_UTILS, widgetType);
            context.startService(updateIntent);
        } else {
            if (DEBUG)
                Log.i(WidgetProviderUtils.TAG, "update() widget conf not yet initialized, skipping update for widget=" + appWidgetId);
        }
    }

    public static void updateAll(Context context) {
        if (DEBUG) Log.i(WidgetProviderUtils.TAG, "Updating all widgets");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(SettingsActivity.PREF_WIDGET_BOARDS, new HashSet<String>());
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split("/");
            int appWidgetId = Integer.valueOf(components[0]);
            update(context, appWidgetId, components.length > 7 ? components[7] : WidgetConstants.WIDGET_TYPE_EMPTY);
        }

    }

    public static void updateAll(Context context, String boardCode) {
        if (DEBUG) Log.i(TAG, "updateAll boardCode=" + boardCode);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(SettingsActivity.PREF_WIDGET_BOARDS, new HashSet<String>());
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split(WidgetConf.DELIM);
            int widgetId = Integer.valueOf(components[0]);
            String widgetBoardCode = components[1];
            if (widgetBoardCode.equals(boardCode))
                update(context, widgetId, components.length > 7 ? components[7] : WidgetConstants.WIDGET_TYPE_EMPTY);
        }
    }

    public static boolean storeWidgetConf(final Context context, final WidgetConf widgetConf) {
        int appWidgetId = widgetConf.appWidgetId;
        String boardCode = widgetConf.boardCode;
        if (DEBUG) Log.i(WidgetProviderUtils.TAG, "Configuring widget=" + appWidgetId + " with board=" + boardCode);
        if (boardCode == null || ChanBoard.getBoardByCode(context, boardCode) == null) {
            Log.e(WidgetProviderUtils.TAG, "Couldn't find board=" + boardCode + " for widget=" + appWidgetId + " not adding widget");
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(SettingsActivity.PREF_WIDGET_BOARDS, new HashSet<String>());
        Set<String> newWidgetBoards = new HashSet<String>();
        String newWidgetBoard = widgetConf.serialize();
        boolean found = false;
        for (String widgetBoard : widgetBoards) {
            WidgetConf existingWidgetConf = new WidgetConf(widgetBoard);
            if (appWidgetId == existingWidgetConf.appWidgetId) {
                found = true;
                newWidgetBoards.add(newWidgetBoard);
            } else {
                newWidgetBoards.add(widgetBoard);
            }
        }
        if (!found) {
            newWidgetBoards.add(newWidgetBoard);
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(SettingsActivity.PREF_WIDGET_BOARDS, newWidgetBoards);
        editor.commit();
        return true;
    }

    public static boolean hasWidgets(final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getStringSet(SettingsActivity.PREF_WIDGET_BOARDS, new HashSet<String>()).size() > 0;
    }

    public static void asyncUpdateWidgetsAndWatchlist(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
               if (hasWidgets(context))
                    fetchAllWidgets(context);

                /*
                ChanBoard board = ChanFileStorage.loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
                boolean hasWatchlist = (board != null && board.threads != null && board.threads.length > 0);
                if (hasWatchlist)
                    GlobalAlarmReceiver.fetchWatchlistThreads(context);

                boolean hasFavorites = ChanBoard.hasFavorites(context);
                if (hasFavorites)
                    GlobalAlarmReceiver.fetchFavoriteBoards(context);
                */

                //if (hasWidgets) // || hasFavorites)
                scheduleGlobalAlarm(context); // always schedule in case widgets are added in the future
            }
        });
    }

    public static void scheduleGlobalAlarm(final Context context) { // will reschedule if not already scheduled
        GlobalAlarmReceiver.scheduleGlobalAlarm(context);
        /*
        Intent intent = new Intent(context, GlobalAlarmReceiver.class);
        intent.setAction(GlobalAlarmReceiver.GLOBAL_ALARM_RECEIVER_SCHEDULE_ACTION);
        context.startService(intent);
        */
        if (DEBUG) Log.i(TAG, "Scheduled global alarm");
    }

    public static ChanPost[] loadBestWidgetThreads(Context context, String boardCode, int numThreads) {
        ChanPost[] widgetThreads = new ChanPost[numThreads];

        ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
        if (board == null) {
            Log.e(TAG, "Couldn't load widget null board for boardCode=" + boardCode);
            return widgetThreads;
        }

        ChanPost[] boardThreads = board.loadedThreads != null && board.loadedThreads.length > 0
                ? board.loadedThreads
                : board.threads;
        if (boardThreads == null || boardThreads.length == 0) {
            Log.e(TAG, "Couldn't load widget no threads for boardCode=" + boardCode + " making pseudo threads");
            for (int i = 0; i < numThreads; i++) {
                ChanPost p = new ChanPost();
                p.board = boardCode;
                p.no = i;
                p.resto = 0;
                p.sub = board.getName(context);
                p.com = board.getDescription(context);
                widgetThreads[i] = p;
            }
            return widgetThreads;
        }

        // try to load what we can
        int threadIndex = 0;
        int filledCount = 0;
        Set<Integer> threadsUsed = new HashSet<Integer>(numThreads);
        for (int i = 0; i < numThreads; i++) {
            ChanPost thread = null;
            while (threadIndex < boardThreads.length) {
                ChanPost test = boardThreads[threadIndex];
                threadIndex++;
                if (test != null && test.sticky <= 0 && test.tim > 0 && test.no > 0) {
                    thread = test;
                    break;
                }
            }
            if (thread != null) {
                widgetThreads[i] = thread;
                threadsUsed.add(threadIndex - 1);
                filledCount = i + 1;
            }
        }

        // what if we are missing threads? for instance no images with latest threads
        threadIndex = 0;
        if (filledCount < numThreads) {
            for (int i = 0; i < numThreads; i++) {
                if (widgetThreads[i] != null)
                    continue;
                ChanPost thread = null;
                while (threadIndex < boardThreads.length) {
                    ChanPost test = boardThreads[threadIndex];
                    threadIndex++;
                    if (test != null && !threadsUsed.contains(threadIndex) && test.sticky <= 0 && test.no > 0) {
                        thread = test;
                        break;
                    }
                }
                if (thread != null) {
                    widgetThreads[i] = thread;
                    threadsUsed.add(threadIndex - 1);
                }
            }
        }

        return widgetThreads;
    }


    public static List<String> preloadThumbnailURLs(final Context context, final String boardCode, final int maxThreads) {
        ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
        if (board == null) {
            Log.e(TAG, "Couldn't load widget null board for boardCode=" + boardCode);
            return null;
        }

        ChanPost[] boardThreads = board.loadedThreads != null && board.loadedThreads.length > 0
                ? board.loadedThreads
                : board.threads;
        if (boardThreads == null || boardThreads.length == 0 || boardThreads[0] == null || boardThreads[0].defData) {
            Log.e(TAG, "Couldn't load widget no threads for boardCode=" + boardCode);
            return null;
        }

        // try to load what we can
        int validThreads = 0;
        List<String> preloadURLs = new ArrayList<String>();
        for (int i = 0; i < boardThreads.length; i++) {
            ChanPost thread = boardThreads[i];
            if (thread != null && thread.sticky <= 0 && thread.tim > 0 && thread.no > 0) {
                String url = thread.thumbnailUrl(context);
                File f = ChanImageLoader.getInstance(context).getDiscCache().get(url);
                if (f == null || !f.canRead() || f.length() <= 0)
                    preloadURLs.add(url);
                if (++validThreads >= maxThreads)
                    break;
            }
        }

        return preloadURLs;
    }

    public static List<ChanPost> viableThreads(final Context context, final String boardCode, final int maxThreads) {
        ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
        if (board == null) {
            Log.e(TAG, "viableThreads() couldn't load widget null board for boardCode=" + boardCode);
            return new ArrayList<ChanPost>();
        }

        ChanPost[] boardThreads = board.loadedThreads != null && board.loadedThreads.length > 0
                ? board.loadedThreads
                : board.threads;
        if (boardThreads == null || boardThreads.length == 0) {
            Log.e(TAG, "viableThreads() couldn't load widget no threads for boardCode=" + boardCode);
            return new ArrayList<ChanPost>();
        }

        // try to load what we can
        if (DEBUG) Log.i(TAG, "viableThreads checking " + boardThreads.length + " threads");
        List<ChanPost> viableThreads = new ArrayList<ChanPost>();
        for (int i = 0; i < boardThreads.length; i++) {
            ChanPost thread = boardThreads[i];
            boolean viable;
            if (thread == null)
                viable = false;
            else if (board.isPopularBoard()) // never have images or sticky, so always viable
                viable = true;
            else if (thread.sticky <= 0 && thread.tim > 0 && thread.no > 0)
                viable = true;
            else
                viable = false;
            if (!viable)
                continue;
            String url = thread.thumbnailUrl(context);
            File f = ChanImageLoader.getInstance(context).getDiscCache().get(url);
            if (f == null || !f.canRead() || f.length() <= 0)
                continue;
            if (viableThreads.size() < maxThreads) {
                if (DEBUG) Log.i(TAG, "viableThreads adding " + thread);
                viableThreads.add(thread);
                if (viableThreads.size() >= maxThreads)
                    break;
            }
        }

        return viableThreads;
    }

    static public void asyncDownloadAndCacheUrl(final Context context, final String url, final Runnable downloadCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadAndCacheUrl(context, url, downloadCallback);
            }
        }).start();
    }

    static public void downloadAndCacheUrl(final Context context, final String url, final Runnable downloadCallback) {
        Bitmap b = downloadBitmap(url);
        if (b == null || b.getByteCount() <= 0)
            return;
        File f = ChanImageLoader.getInstance(context).getDiscCache().get(url);
        if (f == null)
            return;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            if (DEBUG) Log.i(TAG, "downloadAndCacheUrl complete for url=" + url + " notifying callback");
            if (downloadCallback != null)
                downloadCallback.run();
        }
        catch (IOException e) {
            Log.e(TAG, "Coludn't write file " + f.getAbsolutePath(), e);
        }
        finally {
            IOUtils.closeQuietly(fos);
        }
    }

    static private Bitmap downloadBitmap(String url) {
        final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    return bitmap;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (Exception e) {
            getRequest.abort();
            if (DEBUG) Log.i(TAG, "Exception while retrieving bitmap from " + url, e);
        } catch (Error e) {
            getRequest.abort();
            if (DEBUG) Log.i(TAG, "Error while retrieving bitmap from " + url, e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return null;
    }

    static protected boolean safeSetRemoteViewThumbnail(Context context, WidgetConf widgetConf, RemoteViews views, int imageId, String url, int i) {
        File f = url == null || url.isEmpty() ? null : ChanImageLoader.getInstance(context).getDiscCache().get(url);
        boolean isCached = false;
        if (f != null && f.canRead() && f.length() > 0) {
            try {
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                views.setImageViewBitmap(imageId, b);
                isCached = true;
                if (DEBUG) Log.i(TAG, "safeSetRemoteViewThumbnail() i=" + i + " url=" + url + " set image to file=" + f.getAbsolutePath());
            }
            catch (Exception e) {
                Log.e(TAG, "safeSetRemoteViewThumbnail() i=" + i + " url=" + url + " exception setting image to file=" + f.getAbsolutePath(), e);
            }
        }
        if (!isCached && i > 0) {
            int defaultImageId = ChanBoard.getRandomImageResourceId(widgetConf.boardCode, i);
            views.setImageViewResource(imageId, defaultImageId);
            if (DEBUG) Log.i(TAG, "safeSetRemoteViewThumbnail() i=" + i + " url=" + url + " no file, set image to default resource");
        }
        return isCached;
    }
}
