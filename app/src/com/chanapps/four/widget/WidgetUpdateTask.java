package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.BaseChanService;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.FetchPopularThreadsService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FakeBitmapDisplayer;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 8/26/13
* Time: 11:16 AM
* To change this template use File | Settings | File Templates.
*/
public class WidgetUpdateTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = WidgetUpdateTask.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final int NUM_TOP_THREADS = 6;

    private static DisplayImageOptions optionsWithFakeDisplayer;
    static {
        optionsWithFakeDisplayer = new DisplayImageOptions.Builder().displayer(new FakeBitmapDisplayer()).build();
    }

    private Context context;
    private WidgetConf widgetConf;
    private ChanPost[] threads = new ChanPost[NUM_TOP_THREADS];

    public WidgetUpdateTask(Context context, WidgetConf widgetConf) {
        if (DEBUG) Log.i(TAG, "WidgetUpdateTask() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/");
        this.context = context;
        this.widgetConf = widgetConf;
        if (widgetConf.boardCode == null) {
            Log.e(TAG, "WidgetUpdateTask() id=" + widgetConf.appWidgetId + " null board code, defaulting to /a/");
            widgetConf.boardCode = "a";
        }
    }

    @Override
    public Void doInBackground(Void... params) {
        if (DEBUG) Log.i(TAG, "doInBackground() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/");
        loadBoard();
        return null;
    }

    @Override
    public void onPostExecute(Void result) {
        if (DEBUG) Log.i(TAG, "onPostExecute() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/");
        if (WidgetConstants.WIDGET_TYPE_BOARD.equalsIgnoreCase(widgetConf.widgetType))
            updateWideWidget();
        else if (WidgetConstants.WIDGET_TYPE_ONE_IMAGE.equalsIgnoreCase(widgetConf.widgetType))
            updateOneImageWidget();
        else if (WidgetConstants.WIDGET_TYPE_COVER_FLOW.equalsIgnoreCase(widgetConf.widgetType))
            updateCoverFlowWidget();
    }

    private void updateWideWidget() {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_layout);
        int[] imageIds = {R.id.image_left, R.id.image_center, R.id.image_right, R.id.image_left1, R.id.image_center1, R.id.image_right1};
        updateWidgetViews(R.layout.widget_board_layout, R.id.widget_board_container, remoteViews);
        updateImageWidgetsView(imageIds, remoteViews);
    }

    private void updateOneImageWidget() {
        int[] imageIds = {R.id.image_left1};
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_oneimage_layout);
        updateWidgetViews(R.layout.widget_board_oneimage_layout, R.id.widget_board_oneimage_container, remoteViews );
        updateImageWidgetsView(imageIds, remoteViews);
    }

    private void updateCoverFlowWidget() {

        final Intent intent = new Intent(context, UpdateWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_coverflow_layout);
        remoteViews.setRemoteAdapter(R.id.stack_view_coverflow, intent);
        remoteViews.setEmptyView(R.id.stack_view_coverflow, R.id.stack_view_empty);
        updateWidgetViews(R.layout.widget_board_coverflow_layout, R.id.widget_board_coverflow_container, remoteViews );

        final Intent viewIntent = new Intent(context, ThreadActivity.class);
        viewIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
        viewIntent.putExtra(ThreadActivity.BOARD_CODE, widgetConf.boardCode);
        viewIntent.setData(Uri.parse(viewIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);
        remoteViews.setPendingIntentTemplate(R.id.stack_view_coverflow, viewPendingIntent);

        AppWidgetManager.getInstance(context).updateAppWidget(widgetConf.appWidgetId, remoteViews);
    }

    private void loadBoard() {
        try {
            threads = WidgetProviderUtils.loadBestWidgetThreads(context, widgetConf.boardCode, NUM_TOP_THREADS);
            if (DEBUG) Log.i(TAG, "loadBoard() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode
                    + "/ threadCount=" + threads.length);
        } catch (Exception e) {
            Log.e(TAG, "loadBoard() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode
                    + "/ couldn't load, defaulting to cached values");
        }
    }

    private void updateWidgetViews(int widgetLayout, int widgetContainer, RemoteViews views) {
        if (DEBUG) Log.i(TAG, "updateWidgetViews() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/");

        int containerBackground = widgetConf.roundedCorners ? R.drawable.widget_rounded_background : 0;
        views.setInt(widgetContainer, "setBackgroundResource", containerBackground);

        ChanBoard board = ChanBoard.getBoardByCode(context, widgetConf.boardCode);
        if (board == null)
            board = ChanBoard.getBoardByCode(context, ChanBoard.DEFAULT_BOARD_CODE);
        if (board == null) {
            Log.e(TAG, "updateWidgetViews() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/"
                    + " null board, exiting");
            return;
        }

        String boardTitle = ChanBoard.isVirtualBoard(board.link)
                ? board.name
                : board.name + " /" + board.link + "/";
        int boardTitleColor = widgetConf.boardTitleColor;
        int boardTitleVisibility = widgetConf.showBoardTitle ? View.VISIBLE : View.GONE;
        views.setTextViewText(R.id.board_title, boardTitle);
        views.setTextColor(R.id.board_title, boardTitleColor);
        views.setViewVisibility(R.id.board_title, boardTitleVisibility);
        if (widgetConf.showBoardTitle)
            views.setOnClickPendingIntent(R.id.board_title, makeBoardIntent());

        //views.setInt(R.id.home_button, "setImageResource", R.drawable.app_icon);
        //views.setOnClickPendingIntent(R.id.home_button, makeHomeIntent());

        int refreshBackground = widgetConf.showRefreshButton ? R.drawable.widget_refresh_gradient_bg : 0;
        int refreshDrawable = widgetConf.showRefreshButton ? R.drawable.widget_refresh_button_selector : 0;
        views.setInt(R.id.refresh, "setBackgroundResource", refreshBackground);
        views.setInt(R.id.refresh, "setImageResource", refreshDrawable);
        views.setOnClickPendingIntent(R.id.refresh, makeRefreshIntent());

        int configureBackground = widgetConf.showConfigureButton ? R.drawable.widget_configure_gradient_bg : 0;
        int configureDrawable = widgetConf.showConfigureButton ? R.drawable.widget_configure_button_selector : 0;
        views.setInt(R.id.configure, "setBackgroundResource", configureBackground);
        views.setInt(R.id.configure, "setImageResource", configureDrawable);
        views.setOnClickPendingIntent(R.id.configure, makeConfigureIntent());

        if (DEBUG) Log.i(TAG, "updateWidgetViews() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ finished");
    }

    private void updateImageWidgetsView(int [] imageIds, RemoteViews views) {
        for (int i = 0; i < imageIds.length; i++) {
            int imageId = imageIds[i];
            PendingIntent pendingIntent = makeThreadIntent(threads[i], i);
            views.setOnClickPendingIntent(imageId, pendingIntent);
        }

        AppWidgetManager.getInstance(context).updateAppWidget(widgetConf.appWidgetId, views);

        for (int i = 0; i < imageIds.length; i++) {
            int imageId = imageIds[i];
            ChanPost thread = threads[i];
            String url = ChanBoard.getBestWidgetImageUrl(thread, widgetConf.boardCode, i);
            asyncUpdateWidgetImageView(views, imageId, url);
        }
    }

    private void asyncUpdateWidgetImageView(final RemoteViews views, final int imageId, final String url) {
        ImageSize minImageSize = new ImageSize(125, 125);
        ChanImageLoader
                .getInstance(context)
                .loadImage(url, minImageSize, optionsWithFakeDisplayer,
                        new SimpleImageLoadingListener() {
                            @Override
                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                views.setImageViewBitmap(imageId, loadedImage);
                                AppWidgetManager
                                        .getInstance(context)
                                        .updateAppWidget(widgetConf.appWidgetId, views);
                            }
                        });
    }

    private PendingIntent makeThreadIntent(ChanPost thread, int i) {
        Intent intent = (thread == null || thread.no < 1)
                ? BoardActivity.createIntent(context, widgetConf.boardCode, "")
                : ThreadActivity.createIntent(context, thread.board, thread.no, "");
        intent.putExtra(ActivityDispatcher.IGNORE_DISPATCH, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int uniqueId = (100 * widgetConf.appWidgetId) + 5 + i;
        return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent makeHomeIntent() {
        Intent intent = BoardActivity.createIntent(context, ChanBoard.ALL_BOARDS_BOARD_CODE, "");
        intent.putExtra(ActivityDispatcher.IGNORE_DISPATCH, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int uniqueId = (100 * widgetConf.appWidgetId) + 1;
        return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent makeBoardIntent() {
        Intent intent = BoardActivity.createIntent(context, widgetConf.boardCode, "");
        intent.putExtra(ActivityDispatcher.IGNORE_DISPATCH, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int uniqueId = (100 * widgetConf.appWidgetId) + 2;
        return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent makeRefreshIntent() {
        Intent intent;

        //if (WidgetConstants.WIDGET_TYPE_COVER_FLOW.equalsIgnoreCase(widgetConf.widgetType)) return null;
        if (ChanBoard.WATCHLIST_BOARD_CODE.equals(widgetConf.boardCode)) {
            return null;
        } else if (ChanBoard.isVirtualBoard(widgetConf.boardCode)) {
            intent = new Intent(context, FetchPopularThreadsService.class);
        } else {
            intent = new Intent(context, FetchChanDataService.class);
            intent.putExtra(ChanBoard.BOARD_CODE, widgetConf.boardCode);
            intent.putExtra(ChanBoard.BOARD_CATALOG, 1);
            intent.putExtra(ChanBoard.PAGE, -1);
        }
        intent.putExtra(BaseChanService.PRIORITY_MESSAGE_FETCH, 1);
        intent.putExtra(BaseChanService.BACKGROUND_LOAD, true);
        intent.putExtra(ActivityDispatcher.IGNORE_DISPATCH, true);
        int uniqueId = (100 * widgetConf.appWidgetId) + 3;
        return PendingIntent.getService(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent makeConfigureIntent() {
        Intent intent;
        if (WidgetConstants.WIDGET_TYPE_ONE_IMAGE.equalsIgnoreCase(widgetConf.widgetType))
            intent = new Intent(context, WidgetConfigureOneImageActivity.class);
        else if (WidgetConstants.WIDGET_TYPE_BOARD.equalsIgnoreCase(widgetConf.widgetType))
            intent = new Intent(context, WidgetConfigureActivity.class);
        else if (WidgetConstants.WIDGET_TYPE_COVER_FLOW.equalsIgnoreCase(widgetConf.widgetType))
            intent = new Intent(context, WidgetConfigureCoverFlowActivity.class);
        else
            return null;
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
        intent.putExtra(ActivityDispatcher.IGNORE_DISPATCH, true);
        int uniqueId = (100 * widgetConf.appWidgetId) + 4;
        return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
