package com.chanapps.four.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
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
public class CardStackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = CardStackRemoteViewsFactory.class.getSimpleName();
    private static final boolean DEBUG = false;

    private Context context;
    private int appWidgetId;
    private WidgetConf widgetConf;
    private List<ChanPost> threads = new ArrayList<ChanPost>();

    public CardStackRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        initWidget();
    }

    private void initWidget() {
        widgetConf = WidgetProviderUtils.loadWidgetConf(context, appWidgetId);
        if (widgetConf == null)
            widgetConf = new WidgetConf(appWidgetId); // new widget or no config;
        threads = WidgetProviderUtils.viableThreads(context, widgetConf.boardCode, BoardCoverFlowCardWidgetProvider.MAX_THREADS);
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
    public RemoteViews getViewAt(int i) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_coverflowcard_item);
        ChanPost thread = i >= 0 && i < threads.size() ? threads.get(i) : null;
        if (thread != null) {
            if (DEBUG) Log.i(TAG, "getViewAt() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ pos=" + i
                + " set thread no=" + thread.no);
            setThreadView(views, thread, i);
        }
        else {
            if (DEBUG) Log.i(TAG, "getViewAt() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ pos=" + i
                    + " no thread found at position");
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

    private void setThreadView(RemoteViews views, ChanPost thread, int position) {
        setImage(views, thread, position);
        setSubCom(views, thread);
        setInfo(views, thread);
        setCountryFlag(views, thread);
        setClickTarget(views, thread);
    }

    private void setImage(RemoteViews views, ChanPost thread, int position) {
        String url = thread.thumbnailUrl(context);
        WidgetProviderUtils.safeSetRemoteViewThumbnail(context, widgetConf, views, R.id.widget_coverflowcard_image, url, position);
    }

    private void setSubCom(RemoteViews views, ChanPost thread) {
        String subject = thread.combinedSubCom();
        if (subject != null && !subject.isEmpty())
            views.setTextViewText(R.id.widget_coverflowcard_subject, Html.fromHtml(subject));
    }

    private void setInfo(RemoteViews views, ChanPost thread) {
        String headline = thread.threadInfoLine(context, true, true, true);
        if (headline != null && !headline.isEmpty())
            views.setTextViewText(R.id.widget_coverflowcard_info, Html.fromHtml(headline));

    }

    private void setCountryFlag(RemoteViews views, ChanPost thread) {
        String url = thread.countryFlagUrl(context);
        if (url == null)
            return;
        boolean isCached = WidgetProviderUtils.safeSetRemoteViewThumbnail(context, widgetConf, views, R.id.widget_coverflowcard_flag, url, -1);
        if (!isCached) {
            WidgetProviderUtils.asyncDownloadAndCacheUrl(context, url, urlDownloadCallback);
            if (DEBUG) Log.i(TAG, "getViewAt() url=" + url + " no file, downloading country flag");
        }
    }

    private Runnable urlDownloadCallback = new Runnable() {
        @Override
        public void run() {
            AppWidgetManager
                    .getInstance(context)
                    .notifyAppWidgetViewDataChanged(widgetConf.appWidgetId, R.id.widget_board_coverflow_container);
        }
    };

    private void setClickTarget(RemoteViews views, ChanPost thread) {
        Bundle extras = new Bundle();
        extras.putLong(ThreadActivity.THREAD_NO, thread.no);
        Intent intent = new Intent();
        intent.putExtras(extras);
        views.setOnClickFillInIntent(R.id.widget_coverflowcard_frame, intent);
    }

}
