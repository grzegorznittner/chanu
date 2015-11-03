package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
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
import com.chanapps.four.service.BaseChanService;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.FetchPopularThreadsService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;

import java.util.List;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 8/26/13
* Time: 11:16 AM
* To change this template use File | Settings | File Templates.
*/
public class WidgetUpdateTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = WidgetUpdateTask.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int NUM_TOP_THREADS = 6;

    private Context context;
    private WidgetConf widgetConf;
    private ChanPost[] threads = new ChanPost[NUM_TOP_THREADS];

    public WidgetUpdateTask(Context context, WidgetConf widgetConf) {
        if (DEBUG) Log.i(TAG, "WidgetUpdateTask() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ type=" + widgetConf.widgetType);
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
        NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
        if (ChanBoard.boardNeedsRefresh(context, widgetConf.boardCode, false)
                &&  health != NetworkProfile.Health.NO_CONNECTION
                && health != NetworkProfile.Health.BAD) {
            if (DEBUG) Log.i(TAG, "doInBackground() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ scheduling board fetch");
            FetchChanDataService.scheduleBoardFetch(context, widgetConf.boardCode, false, true);
        }
        if (WidgetConstants.WIDGET_TYPE_BOARD.equalsIgnoreCase(widgetConf.widgetType))
            updateWideWidget();
        else if (WidgetConstants.WIDGET_TYPE_ONE_IMAGE.equalsIgnoreCase(widgetConf.widgetType))
            updateOneImageWidget();
        else if (WidgetConstants.WIDGET_TYPE_COVER_FLOW.equalsIgnoreCase(widgetConf.widgetType))
            updateCoverFlowWidget();
        else if (WidgetConstants.WIDGET_TYPE_COVER_FLOW_CARD.equalsIgnoreCase(widgetConf.widgetType))
            updateCoverFlowCardWidget();
        return null;
    }

    @Override
    public void onPostExecute(Void result) {
        if (DEBUG) Log.i(TAG, "onPostExecute() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ type=" + widgetConf.widgetType);
    }

    private void updateWideWidget() {
        if (DEBUG) Log.i(TAG, "updateWideWidget() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ type=" + widgetConf.widgetType);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_layout);
        bindClickTargets(R.id.widget_board_container, remoteViews, false);
        AppWidgetManager.getInstance(context).updateAppWidget(widgetConf.appWidgetId, remoteViews);
        int[] imageIds = {R.id.image_left, R.id.image_center, R.id.image_right};
        updateImages(BoardWidgetProvider.MAX_THREADS, 0, R.layout.widget_board_layout, imageIds);
    }

    private void updateOneImageWidget() {
        if (DEBUG) Log.i(TAG, "updateOneImageWidget() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ type=" + widgetConf.widgetType);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_oneimage_layout);
        bindClickTargets(R.id.widget_board_oneimage_container, remoteViews, true);
        AppWidgetManager.getInstance(context).updateAppWidget(widgetConf.appWidgetId, remoteViews);
        int[] imageIds = {R.id.image_left1};
        updateImages(BoardOneImageWidgetProvider.MAX_THREADS, 0, R.layout.widget_board_oneimage_layout, imageIds);
    }

    private void updateCoverFlowWidget() {
        if (DEBUG) Log.i(TAG, "updateCoverFlowWidget() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ type=" + widgetConf.widgetType);
        final Intent intent = new Intent(context, StackWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_coverflow_layout);
        remoteViews.setRemoteAdapter(R.id.stack_view_coverflow, intent);
        remoteViews.setEmptyView(R.id.stack_view_coverflow, R.id.stack_view_empty);
        bindClickTargets(R.id.widget_board_coverflow_container, remoteViews, false);

        final Intent viewIntent = new Intent(context, ThreadActivity.class);
        viewIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
        viewIntent.putExtra(ThreadActivity.BOARD_CODE, widgetConf.boardCode);
        viewIntent.setData(Uri.parse(viewIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);
        remoteViews.setPendingIntentTemplate(R.id.stack_view_coverflow, viewPendingIntent);

        try {
            if (DEBUG) Log.i(TAG, "updateCoverFlowWidget() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode
                    + "/ type=" + widgetConf.widgetType + " updating app widget");
            AppWidgetManager.getInstance(context).updateAppWidget(widgetConf.appWidgetId, remoteViews);
            if (DEBUG) Log.i(TAG, "updateCoverFlowWidget() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode
                    + "/ type=" + widgetConf.widgetType + " notifying app widget data");
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(widgetConf.appWidgetId, R.id.stack_view_coverflow);
            int maxThreads = NetworkProfileManager.instance().getCurrentProfile().getFetchParams().maxThumbnailPrefetches;
            updateImages(maxThreads, R.id.stack_view_coverflow, 0, null);
        }
        catch (Exception e) {
            Log.e(TAG, "Exception updating widget id=" + widgetConf.appWidgetId);
        }
    }

    private void updateCoverFlowCardWidget() {
        if (DEBUG) Log.i(TAG, "updateCoverFlowCardWidget() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ type=" + widgetConf.widgetType);
        final Intent intent = new Intent(context, CardStackWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_coverflowcard_layout);
        remoteViews.setRemoteAdapter(R.id.stack_view_coverflow, intent);
        remoteViews.setEmptyView(R.id.stack_view_coverflow, R.id.stack_view_empty);
        bindClickTargets(R.id.widget_board_coverflow_container, remoteViews, false);

        final Intent viewIntent = new Intent(context, ThreadActivity.class);
        viewIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
        viewIntent.putExtra(ThreadActivity.BOARD_CODE, widgetConf.boardCode);
        viewIntent.setData(Uri.parse(viewIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);
        remoteViews.setPendingIntentTemplate(R.id.stack_view_coverflow, viewPendingIntent);

        if (DEBUG) Log.i(TAG, "updateCoverFlowWidget() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode
                + "/ type=" + widgetConf.widgetType + " updating app widget");
        AppWidgetManager.getInstance(context).updateAppWidget(widgetConf.appWidgetId, remoteViews);
        if (DEBUG) Log.i(TAG, "updateCoverFlowWidget() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode
                + "/ type=" + widgetConf.widgetType + " notifying app widget data");
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(widgetConf.appWidgetId, R.id.stack_view_coverflow);
        int maxThreads = NetworkProfileManager.instance().getCurrentProfile().getFetchParams().maxThumbnailPrefetches;
        updateImages(maxThreads, R.id.stack_view_coverflow, 0, null);
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

    private void bindClickTargets(int widgetContainer, RemoteViews views, boolean boardCodeOnly) {
        if (DEBUG) Log.i(TAG, "bindClickTargets() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/");

        int containerBackground = widgetConf.roundedCorners ? R.drawable.widget_rounded_background : 0;
        views.setInt(widgetContainer, "setBackgroundResource", containerBackground);

        ChanBoard board = ChanBoard.getBoardByCode(context, widgetConf.boardCode);
        if (board == null)
            board = ChanBoard.getBoardByCode(context, ChanBoard.DEFAULT_BOARD_CODE);
        if (board == null) {
            Log.e(TAG, "bindClickTargets() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/"
                    + " null board, exiting");
            return;
        }

        String boardTitle;
        if (boardCodeOnly)
            boardTitle = "/" + board.link + "/";
        else if (ChanBoard.isVirtualBoard(board.link))
            boardTitle = board.getName(context);
        else
            boardTitle = board.getName(context) + " /" + board.link + "/";
        if (DEBUG) Log.i(TAG, "bindClickTargets() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/"
                + " boardTitle=" + boardTitle);
        int boardTitleColor = widgetConf.boardTitleColor;
        int boardTitleVisibility = widgetConf.showBoardTitle ? View.VISIBLE : View.GONE;
        views.setTextViewText(R.id.board_title, boardTitle);
        views.setTextColor(R.id.board_title, boardTitleColor);
        views.setViewVisibility(R.id.board_title, boardTitleVisibility);
        if (widgetConf.showBoardTitle)
            views.setOnClickPendingIntent(R.id.board_title, makeBoardIntent());

        int refreshDrawable = widgetConf.showRefreshButton ? R.drawable.widget_refresh_button_selector : 0;
        views.setInt(R.id.refresh_board, "setImageResource", refreshDrawable);
        views.setOnClickPendingIntent(R.id.refresh_board, makeRefreshIntent());

        int configureDrawable = widgetConf.showConfigureButton ? R.drawable.widget_configure_button_selector : 0;
        views.setInt(R.id.configure, "setImageResource", configureDrawable);
        views.setOnClickPendingIntent(R.id.configure, makeConfigureIntent());

        if (DEBUG) Log.i(TAG, "bindClickTargets() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/ finished");
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

    private PendingIntent makeBoardIntent() {
        Intent intent = BoardActivity.createIntent(context, widgetConf.boardCode, "");
        intent.putExtra(ActivityDispatcher.IGNORE_DISPATCH, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int uniqueId = (100 * widgetConf.appWidgetId) + 2;
        return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent makeRefreshIntent() {
        Intent intent;
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
        else if (WidgetConstants.WIDGET_TYPE_COVER_FLOW_CARD.equalsIgnoreCase(widgetConf.widgetType))
            intent = new Intent(context, WidgetConfigureCoverFlowCardActivity.class);
        else
            return null;
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
        intent.putExtra(ActivityDispatcher.IGNORE_DISPATCH, true);
        int uniqueId = (100 * widgetConf.appWidgetId) + 4;
        return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void updateImages(final int maxThreads, final int containerId, final int layoutId, final int[] imageIds) {
        final List<String> preloadURLs = WidgetProviderUtils.preloadThumbnailURLs(context, widgetConf.boardCode, maxThreads);
        if (preloadURLs == null) {
            if (DEBUG) Log.i(TAG, "updateImages() no images available, fetching board /" + widgetConf.boardCode + "/");
            FetchChanDataService.scheduleBoardFetch(context, widgetConf.boardCode, true, true);
            return;
        }
        final int numPreloads = preloadURLs.size();
        if (numPreloads <= 0) {
            if (DEBUG) Log.i(TAG, "updateImages() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/"
                    + " no preloads needed, directly loading images");
            updateWidget(containerId, layoutId, imageIds);
            return;
        }
        if (DEBUG) Log.i(TAG, "updateImages() id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/"
                + " preloading " + preloadURLs.size() + " images");
        for (final String url : preloadURLs) {
            WidgetProviderUtils.downloadAndCacheUrl(context, url, null);
            if (DEBUG) Log.i(TAG, "updateImages preloaded url=" + url);
        }
        updateWidget(containerId, layoutId, imageIds);
    }

    private void updateWidget(int containerId, int layoutId, int[] imageIds) {
        if (containerId > 0) {
            if (DEBUG) Log.i(TAG, "updateWidget() notifying widget data changed");
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(widgetConf.appWidgetId, containerId);
        }
        else if (imageIds != null) {
            if (DEBUG) Log.i(TAG, "updateWidget() updating app images directly");
            updateAppWidgetImages(layoutId, imageIds);
        }
    }

    private void updateAppWidgetImages(final int layoutId, final int[] imageIds) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        List<ChanPost> threads = WidgetProviderUtils.viableThreads(context, widgetConf.boardCode, imageIds.length);
        if (DEBUG) Log.i(TAG, "updateAppWidgetImages() found " + threads.size() + " viable threads for "
                + " id=" + widgetConf.appWidgetId + " /" + widgetConf.boardCode + "/");
        int j = 0;
        for (int i = 0; i < imageIds.length; i++) {
            int imageId = imageIds[i];
            ChanPost thread = j >= threads.size() ? null : threads.get(j++);
            String url = thread == null ? null : thread.thumbnailUrl(context);
            WidgetProviderUtils.safeSetRemoteViewThumbnail(context, widgetConf, views, imageId, url, i);
            if (thread != null) {
                PendingIntent pendingIntent = makeThreadIntent(thread, i);
                views.setOnClickPendingIntent(imageId, pendingIntent);
            }
        }
        AppWidgetManager.getInstance(context).updateAppWidget(widgetConf.appWidgetId, views);
    }

}
