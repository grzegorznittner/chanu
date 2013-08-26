package com.chanapps.four.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.loader.ChanImageLoader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FakeBitmapDisplayer;

import java.io.File;
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
    private static final boolean DEBUG = true;

    private static final int MAX_THREADS = 30;

    private static DisplayImageOptions optionsWithFakeDisplayer;
    static {
        optionsWithFakeDisplayer = new DisplayImageOptions.Builder().displayer(new FakeBitmapDisplayer()).build();
    }

    private Context context;
    private int appWidgetId;
    private WidgetConf widgetConf;
    private List<ChanPost> threads = new ArrayList<ChanPost>();

    public StackRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        widgetConf = WidgetProviderUtils.loadWidgetConf(context, appWidgetId);
        if (widgetConf == null)
            widgetConf = new WidgetConf(appWidgetId); // new widget or no config;
        loadThreads();
    }

    private void loadThreads() {
        threads = WidgetProviderUtils.coverflowThreads(context, widgetConf.boardCode, MAX_THREADS);
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
        // Return a proper item with the proper day and temperature
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_coverflow_item);

        int i = position;
            /*
                PendingIntent pendingIntent = makeThreadIntent(threads[i], i);
                views.setOnClickPendingIntent(imageId, pendingIntent);
            */
        //AppWidgetManager.getInstance(context).updateAppWidget(widgetConf.appWidgetId, views);
        if (DEBUG) Log.i(TAG, "getViewAt() i=" + i + " threads.length=" + threads.size());
        if (i >= 0 && i< threads.size() && threads.get(i) != null) {
            ChanPost thread = threads.get(i);
            String url = thread.thumbnailUrl();
            File f = ChanImageLoader.getInstance(context).getDiscCache().get(url);
            if (f != null && f.canRead() && f.length() > 0) {
                views.setImageViewUri(R.id.image_coverflow_item, Uri.parse(f.getAbsolutePath()));
                if (DEBUG) Log.i(TAG, "getViewAt() url=" + url + " set image to file=" + f.getAbsolutePath());
            }
            else {
                // Get the data for this position from the content provider
                int defaultImageId = ChanBoard.getRandomImageResourceId(widgetConf.boardCode, position);
                views.setImageViewResource(R.id.image_coverflow_item, defaultImageId);
                if (DEBUG) Log.i(TAG, "getViewAt() url=" + url + " no file, set image to default resource");
                asyncUpdateWidgetImageView(views, R.id.image_coverflow_item, url);
            }
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
        loadThreads();
    }

    private void asyncUpdateWidgetImageView(final RemoteViews views, final int imageId, final String url) {
        if (DEBUG) Log.i(TAG, "asyncUpdateWidgetImageView() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/");
        final ImageSize minImageSize = new ImageSize(125, 125);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                ChanImageLoader
                        .getInstance(context)
                        .loadImage(url, minImageSize, optionsWithFakeDisplayer,
                                new SimpleImageLoadingListener() {
                                    @Override
                                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                        views.setImageViewBitmap(imageId, loadedImage);
                                        //AppWidgetManager
                                        //        .getInstance(context)
                                        //        .updateAppWidget(widgetConf.appWidgetId, views);
                                    }
                                });
                //To change body of implemented methods use File | Settings | File Templates.
            }
        }).start();
    }

}
