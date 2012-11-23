package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import com.chanapps.four.activity.BoardGridActivity;
import com.chanapps.four.activity.R;

/**
 * User: mpop
 * Date: 11/22/12
 * Time: 11:30 PM
 */
public class TopBoard4ChannerWidget extends AppWidgetProvider {
    public static final String TAG = TopBoard4ChannerWidget.class.getSimpleName();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            Log.i(TAG, "WidgetId:" + appWidgetId);
            // Create an Intent to launch ExampleActivity
//            Intent intent = new Intent(context, BoardGridActivity.class);
//            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
//            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.channer_widget);
            //views.setOnClickPendingIntent(R.id.button, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
//            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}