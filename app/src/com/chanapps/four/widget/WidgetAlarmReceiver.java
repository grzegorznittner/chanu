package com.chanapps.four.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/13/13
 * Time: 10:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class WidgetAlarmReceiver extends BroadcastReceiver {

    public static final String TAG = WidgetAlarmReceiver.class.getSimpleName();

    private static final long WIDGET_UPDATE_INTERVAL_MS = AlarmManager.INTERVAL_FIFTEEN_MINUTES; // FIXME should be configurable
    //private static final long WIDGET_UPDATE_INTERVAL_MS = 60000; // 60 sec, just for testing
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) { // when first boot up, default and then schedule for refresh
        refreshWidget(context);
    }

    public static void refreshWidget(Context context) {
        Intent updateIntent = new Intent(context, UpdateWidgetService.class);
        context.startService(updateIntent);
        scheduleAlarms(context);
    }

    private static void scheduleAlarms(Context context) {
        int[] appWidgetIds = BoardWidgetProvider.getAppWidgetIds(context);
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            if (DEBUG)
                Log.i(TAG, "No widgets configured, not scheduling UpdateWidgetService");
            return;
        }
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        long scheduleAt = SystemClock.elapsedRealtime();
        PendingIntent pendingIntent = getPendingIntentForWidgetAlarms(context);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, scheduleAt, WIDGET_UPDATE_INTERVAL_MS, pendingIntent);
        if (DEBUG)
            Log.i(TAG, "Scheduled UpdateWidgetService at t=" + scheduleAt + " repeating every delta=" + WIDGET_UPDATE_INTERVAL_MS + "ms");
    }

    public static PendingIntent getPendingIntentForWidgetAlarms(Context context) {
        Intent intent = new Intent(context, UpdateWidgetService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public static void cancelAlarms(Context context) {
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getPendingIntentForWidgetAlarms(context);
        alarmManager.cancel(pendingIntent);
        if (DEBUG)
            Log.i(TAG, "Canceled alarms for UpdateWidgetService");
    }

}
