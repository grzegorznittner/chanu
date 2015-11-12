package com.chanapps.four.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.GlobalAlarmReceiver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User: mpop
 * Date: 11/22/12
 * Time: 11:30 PM
 */
public abstract class AbstractBoardWidgetProvider extends AppWidgetProvider {

    public static final String TAG = AbstractBoardWidgetProvider.class.getSimpleName();

    public static final String WIDGET_CACHE_DIR = "widgets";

    private static final boolean DEBUG = false;

    protected abstract String getWidgetType();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++)
            WidgetProviderUtils.update(context, appWidgetIds[i], getWidgetType());
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (DEBUG) Log.i(TAG, "deleting widgets: " + Arrays.toString(appWidgetIds));
        Set<Integer> widgetsToDelete = new HashSet<Integer>();
        for (int i = 0; i < appWidgetIds.length; i++)
            widgetsToDelete.add(appWidgetIds[i]);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(SettingsActivity.PREF_WIDGET_BOARDS, new HashSet<String>());
        Set<String> newWidgetBoards = new HashSet<String>();
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split(WidgetConf.DELIM);
            int widgetId = Integer.valueOf(components[0]);
            if (!widgetsToDelete.contains(widgetId))
                newWidgetBoards.add(widgetBoard);
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(SettingsActivity.PREF_WIDGET_BOARDS, newWidgetBoards);
        editor.commit();
    }

    @Override
    public void onEnabled(Context context) {
        // handled by config task
    }

    @Override
    public void onDisabled(Context context) {
        if (DEBUG) Log.i(TAG, "disabled all widgets");
        GlobalAlarmReceiver.scheduleGlobalAlarm(context); // will deschedule if appropriate
    }

}