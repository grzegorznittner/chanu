package com.chanapps.four.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.text.Html;
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
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FakeBitmapDisplayer;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import java.io.*;
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
    private static final boolean DEBUG = true;

    private static final int COUNTRY_FLAG_WIDTH_PX = 16;
    private static final int COUNTRY_FLAG_HEIGHT_PX = 11;

    private static DisplayImageOptions optionsWithFakeDisplayer;
    static {
        optionsWithFakeDisplayer = new DisplayImageOptions.Builder().displayer(new FakeBitmapDisplayer()).cacheOnDisc().build();
    }

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
        if (DEBUG) Log.i(TAG, "getViewAt() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ pos=" + i);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_coverflowcard_item);
        if (i >= 0 && i< threads.size() && threads.get(i) != null)
            setThreadView(views, threads.get(i), i);
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
        String url = thread.thumbnailUrl();
        File f = ChanImageLoader.getInstance(context).getDiscCache().get(url);
        if (f != null && f.canRead() && f.length() > 0) {
            views.setImageViewUri(R.id.widget_coverflowcard_image, Uri.parse(f.getAbsolutePath()));
            if (DEBUG) Log.i(TAG, "getViewAt() url=" + url + " set image to file=" + f.getAbsolutePath());
        }
        else {
            int defaultImageId = ChanBoard.getRandomImageResourceId(widgetConf.boardCode, position);
            views.setImageViewResource(R.id.widget_coverflowcard_image, defaultImageId);
            if (DEBUG) Log.i(TAG, "getViewAt() url=" + url + " no file, set image to default resource");
        }
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
        String url = thread.countryFlagUrl();
        if (url == null)
            return;
        File f = ChanImageLoader.getInstance(context).getDiscCache().get(url);
        if (f != null && f.canRead() && f.length() > 0) {
            views.setImageViewUri(R.id.widget_coverflowcard_flag, Uri.parse(f.getAbsolutePath()));
            views.setViewVisibility(R.id.widget_coverflowcard_flag, View.VISIBLE);
            if (DEBUG) Log.i(TAG, "getViewAt() url=" + url + " set country flag to file=" + f.getAbsolutePath());
        }
        else {
            asyncDownloadAndCacheUrl(url, urlDownloadCallback);
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

    private void asyncDownloadAndCacheUrl(final String url, final Runnable downloadCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap b = downloadBitmap(url);
                if (b == null || b.getByteCount() <= 0)
                    return;
                File f = ChanImageLoader.getInstance(context).getDiscCache().get(url);
                if (f == null)
                    return;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);
                    b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.flush();
                    if (DEBUG) Log.i(TAG, "asyncDownloadAndCacheUrl complete for url=" + url + " notifying callback");
                    downloadCallback.run();

                }
                catch (IOException e) {
                    Log.e(TAG, "Coludn't write file " + f.getAbsolutePath(), e);
                }
                finally {
                    IOUtils.closeQuietly(fos);
                }
            }
        }).start();
    }


    static Bitmap downloadBitmap(String url) {
        final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    return bitmap;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (Exception e) {
            getRequest.abort();
            if (DEBUG) Log.i(TAG, "Error while retrieving bitmap from " + url, e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return null;
    }

}
