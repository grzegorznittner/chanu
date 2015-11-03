package com.chanapps.four.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.ChanPost;
import java.util.ArrayList;
import java.util.List;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 8/26/13
* Time: 11:18 AM
* To change this template use File | Settings | File Templates.
*/
public class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = StackRemoteViewsFactory.class.getSimpleName();
    private static final boolean DEBUG = false;

    private Context context;
    private int appWidgetId;
    private WidgetConf widgetConf;
    private List<ChanPost> threads = new ArrayList<ChanPost>();

    public StackRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        initWidget();
    }

    private void initWidget() {
        widgetConf = WidgetProviderUtils.loadWidgetConf(context, appWidgetId);
        if (widgetConf == null)
            widgetConf = new WidgetConf(appWidgetId); // new widget or no config;
        threads = WidgetProviderUtils.viableThreads(context, widgetConf.boardCode, BoardCoverFlowWidgetProvider.MAX_THREADS);
        if (DEBUG) Log.i(TAG, "initWidget() threadCount=" + threads.size());
    }

    @Override
    public void onCreate() {
        if (DEBUG) Log.i(TAG, "onCreate() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/");
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/");
    }

    @Override
    public int getCount() {
        if (DEBUG) Log.i(TAG, "getCount() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ count=" + threads.size());
        return threads.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (DEBUG) Log.i(TAG, "getViewAt() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ pos=" + position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_coverflow_item);
        int i = position;
        if (i >= 0 && i< threads.size() && threads.get(i) != null) {
            ChanPost thread = threads.get(i);
            String url = thread.thumbnailUrl(context);
            WidgetProviderUtils.safeSetRemoteViewThumbnail(context, widgetConf, views, R.id.image_coverflow_item, url, position);
            Bundle extras = new Bundle();
            extras.putLong(ThreadActivity.THREAD_NO, thread.no);
            Intent intent = new Intent();
            intent.putExtras(extras);
            views.setOnClickFillInIntent(R.id.image_coverflow_item, intent);
        }
        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDataSetChanged() {
        if (DEBUG) Log.i(TAG, "onDataSetChanged() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/");
        initWidget();
    }

}
