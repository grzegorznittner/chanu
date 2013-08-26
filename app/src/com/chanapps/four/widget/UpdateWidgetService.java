package com.chanapps.four.widget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViewsService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.display.FakeBitmapDisplayer;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/13/13
 * Time: 10:54 PM
 */
public class UpdateWidgetService extends RemoteViewsService {

    public static final String TAG = UpdateWidgetService.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.d(TAG, "onGetViewFactory");
        return new StackRemoteViewsFactory(getApplicationContext(), intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "onStartCommand() invalid app widget id passed, service terminating");
        } else {
            WidgetConf widgetConf = WidgetProviderUtils.loadWidgetConf(this, appWidgetId);
            if (widgetConf == null)
                widgetConf = new WidgetConf(appWidgetId); // new widget or no config;
            if (DEBUG) Log.i(TAG, "onStartCommand() id=" + appWidgetId + " /" + widgetConf.boardCode + "/");
            (new WidgetUpdateTask(getApplicationContext(), widgetConf)).execute();
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
