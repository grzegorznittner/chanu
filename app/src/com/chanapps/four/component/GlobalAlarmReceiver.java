package com.chanapps.four.component;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanWatchlist;
import com.chanapps.four.widget.BoardWidgetProvider;

import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/13/13
 * Time: 10:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlobalAlarmReceiver extends BroadcastReceiver {

    public static final String TAG = GlobalAlarmReceiver.class.getSimpleName();

    public static final String GLOBAL_ALARM_RECEIVER_UPDATE_ACTION = "com.chanapps.four.component.GlobalAlarmReceiver.update";

    private static final long WIDGET_UPDATE_INTERVAL_MS = AlarmManager.INTERVAL_FIFTEEN_MINUTES; // FIXME should be configurable
    //private static final long WIDGET_UPDATE_INTERVAL_MS = 60000; // 60 sec, just for testing
    private static final boolean DEBUG = false;

    @Override
    public void onReceive(final Context context, Intent intent) { // when first boot up, default and then schedule for refresh
        String action = intent.getAction();

        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            /* use this when you screw up widgets and need to reset
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.remove(ChanHelper.PREF_WIDGET_BOARDS);
            editor.commit();
            */
            new Thread(new Runnable() {
                @Override
                public void run() {
                    updateAndScheduleRepeating(context);
                    fetchAll(context);
                }
            }).start();
        }
        else if (action.equals(GLOBAL_ALARM_RECEIVER_UPDATE_ACTION)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    fetchAll(context);
                }
            }).start();
        }
        else {
            Log.e(TAG, "Received unknown action: " + action);
        }
    }

    public static void updateAndScheduleRepeating(Context context) {
        if (DEBUG) Log.i(TAG, "updateAndScheduleRepeating");
        update(context);
        scheduleGlobalAlarm(context);
    }

    private static void update(Context context) {
        if (DEBUG) Log.i(TAG, "update");
        BoardWidgetProvider.updateAll(context);
    }

    private static void fetchAll(Context context) {
        if (DEBUG) Log.i(TAG, "fetchAll");
        BoardWidgetProvider.fetchAllWidgets(context);
        ChanWatchlist.fetchWatchlistThreads(context);
        ChanBoard.preloadUncachedBoards(context);
    }

    private static void scheduleGlobalAlarm(Context context) {
        if (DEBUG) Log.i(TAG, "scheduleGlobalAlarm interval ms=" + WIDGET_UPDATE_INTERVAL_MS);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        long scheduleAt = SystemClock.elapsedRealtime();
        PendingIntent pendingIntent = getPendingIntentForGlobalAlarm(context);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, scheduleAt, WIDGET_UPDATE_INTERVAL_MS, pendingIntent);
        if (DEBUG) Log.i(TAG, "Scheduled UpdateWidgetService at t=" + scheduleAt + " repeating every delta=" + WIDGET_UPDATE_INTERVAL_MS + "ms");
    }

    private static PendingIntent getPendingIntentForGlobalAlarm(Context context) {
        Intent intent = new Intent(context, GlobalAlarmReceiver.class);
        intent.setAction(GLOBAL_ALARM_RECEIVER_UPDATE_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public static void cancelGlobalAlarm(Context context) {
        if (DEBUG) Log.i(TAG, "cancelGlobalAlarm");
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getPendingIntentForGlobalAlarm(context);
        alarmManager.cancel(pendingIntent);
        if (DEBUG)
            Log.i(TAG, "Canceled alarms for UpdateWidgetService");
    }

}
