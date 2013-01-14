package com.chanapps.four.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import com.chanapps.four.data.ChanHelper;

/**
 * User: mpop
 * Date: 11/22/12
 * Time: 11:30 PM
 */
public class BoardWidgetProvider extends AppWidgetProvider {
    public static final String TAG = BoardWidgetProvider.class.getSimpleName();

    public static int[] getAppWidgetIds(Context context) {
        ComponentName widgetProvider = new ComponentName(context, BoardWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider);
        return appWidgetIds;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WidgetAlarmReceiver.scheduleAlarms(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // ignore, we only cancel when everything is removed at "onDisabled"
    }

    @Override
    public void onEnabled(Context context) {
        Intent intent = new Intent(context, UpdateWidgetService.class);
        intent.putExtra(ChanHelper.FIRST_TIME_INIT, true);
        context.startService(intent);
        WidgetAlarmReceiver.scheduleAlarms(context);
    }

    @Override
    public void onDisabled(Context context) {
        WidgetAlarmReceiver.cancelAlarms(context);
    }
}