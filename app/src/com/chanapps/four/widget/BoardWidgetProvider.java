package com.chanapps.four.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanBoard;

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

    public static String getConfiguredBoardWidget(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(SettingsActivity.PREF_WIDGET_BOARD, ChanBoard.DEFAULT_BOARD_CODE);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WidgetAlarmReceiver.refreshWidget(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // ignore, we only cancel when everything is removed at "onDisabled"
    }

    @Override
    public void onEnabled(Context context) {
        WidgetAlarmReceiver.refreshWidget(context);
    }

    @Override
    public void onDisabled(Context context) {
        WidgetAlarmReceiver.cancelAlarms(context);
    }
}