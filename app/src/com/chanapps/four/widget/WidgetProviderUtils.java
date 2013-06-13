package com.chanapps.four.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chanapps.four.component.GlobalAlarmReceiver;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.FetchPopularThreadsService;

import java.util.HashSet;
import java.util.Set;

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
        Set<String> widgetConf = pref.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());

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
                .putStringSet(ChanHelper.PREF_WIDGET_BOARDS, savedWidgetConf)
                .commit();
    }

    public static WidgetConf loadWidgetConf(Context context, int appWidgetId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
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
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
        Set<String> boardsToFetch = new HashSet<String>();
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split(WidgetConf.DELIM);
            String widgetBoardCode = components[1];
            boardsToFetch.add(widgetBoardCode);
        }
        for (String boardCode : boardsToFetch) {
            if (DEBUG) Log.i(WidgetProviderUtils.TAG, "fetchAllWidgets board=" + boardCode + " scheduling fetch");
            if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode))
                GlobalAlarmReceiver.fetchWatchlistThreads(context);
            else if (ChanBoard.isPopularBoard(boardCode))
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
            if (DEBUG) Log.i(WidgetProviderUtils.TAG, "calling update widget service for widget=" + appWidgetId);
            Intent updateIntent = new Intent(context, UpdateWidgetService.class);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            updateIntent.putExtra(WIDGET_PROVIDER_UTILS, widgetType);
            context.startService(updateIntent);
        } else {
            if (DEBUG)
                Log.i(WidgetProviderUtils.TAG, "widget conf not yet initialized, skipping update for widget=" + appWidgetId);
        }
    }

    public static void updateAll(Context context) {
        if (DEBUG) Log.i(WidgetProviderUtils.TAG, "Updating all widgets");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split("/");
            int appWidgetId = Integer.valueOf(components[0]);
            update(context, appWidgetId, components.length > 7 ? components[7] : WidgetConstants.WIDGET_TYPE_EMPTY);
        }

    }

    public static void updateAll(Context context, String boardCode) {
        if (DEBUG) Log.i(WidgetProviderUtils.TAG, "updateAll boardCode=" + boardCode);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
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
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
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
        editor.putStringSet(ChanHelper.PREF_WIDGET_BOARDS, newWidgetBoards);
        editor.commit();
        return true;
    }

    public static void asyncUpdateWidgetsAndWatchlist(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean hasWidgets = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>()).size() > 0;
                if (hasWidgets)
                    fetchAllWidgets(context);

                ChanBoard board = ChanFileStorage.loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
                boolean hasWatchlist = (board != null && board.threads != null && board.threads.length > 0);
                if (hasWatchlist)
                    GlobalAlarmReceiver.fetchWatchlistThreads(context);

                if (hasWidgets || hasWatchlist)
                    scheduleGlobalAlarm(context);
            }
        });
    }

    public static void scheduleGlobalAlarm(final Context context) { // will reschedule if not already scheduled
        Intent intent = new Intent(context, GlobalAlarmReceiver.class);
        intent.setAction(GlobalAlarmReceiver.GLOBAL_ALARM_RECEIVER_SCHEDULE_ACTION);
        context.startService(intent);
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
            Log.e(TAG, "Couldn't load widget no threads for boardCode=" + boardCode);
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
}
