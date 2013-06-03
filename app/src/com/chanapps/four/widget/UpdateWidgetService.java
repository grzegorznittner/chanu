package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.*;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.FetchPopularThreadsService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FakeBitmapDisplayer;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/13/13
 * Time: 10:54 PM
 */
public class UpdateWidgetService extends RemoteViewsService {

    public static final String TAG = UpdateWidgetService.class.getSimpleName();

    public static final int NUM_TOP_THREADS = 6;

    private static final boolean DEBUG = true;

    private static DisplayImageOptions optionsWithFakeDisplayer;

    static {
        optionsWithFakeDisplayer = new DisplayImageOptions.Builder().displayer(new FakeBitmapDisplayer()).build();
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.d(TAG, "onGetViewFactory");
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid app widget id passed, service terminating");
        } else {
            if (DEBUG) Log.i(TAG, "starting update widget service for widget=" + appWidgetId);
            WidgetConf widgetConf = WidgetProviderUtils.loadWidgetConf(this, appWidgetId);
            if (widgetConf == null)
                widgetConf = new WidgetConf(appWidgetId); // new widget or no config;
            (new WidgetUpdateTask(getApplicationContext(), widgetConf)).execute();
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class WidgetUpdateTask extends AsyncTask<Void, Void, Void> {

        private Context context;
        private WidgetConf widgetConf;
        private ChanPost[] threads = new ChanPost[NUM_TOP_THREADS];

        public WidgetUpdateTask(Context context, WidgetConf widgetConf) {
            this.context = context;
            this.widgetConf = widgetConf;
            if (widgetConf.boardCode == null) {
                Log.e(TAG, "Null board code found for widget=" + widgetConf.appWidgetId + " defaulting to /a");
                widgetConf.boardCode = "a";
            }
            if (DEBUG)
                Log.i(TAG, "Found widgetConf.boardCode=" + widgetConf.boardCode + " for widget=" + widgetConf.appWidgetId + " now updating");
        }

        @Override
        public Void doInBackground(Void... params) {
            if (DEBUG) Log.i(TAG, "Starting background thread for widget update");
            loadBoard();
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            if (WidgetConstants.WIDGET_TYPE_BOARD.equalsIgnoreCase(widgetConf.widgetType)) {
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_layout);
                int[] imageIds = {R.id.image_left, R.id.image_center, R.id.image_right, R.id.image_left1, R.id.image_center1, R.id.image_right1};
                updateWidgetViews(R.layout.widget_board_layout, R.id.widget_board_container, remoteViews);
                updateImageWidgetsView(imageIds, remoteViews);

            }
            if (WidgetConstants.WIDGET_TYPE_ONE_IMAGE.equalsIgnoreCase(widgetConf.widgetType)) {
                int[] imageIds = {R.id.image_left1};
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_oneimage_layout);
                updateWidgetViews(R.layout.widget_board_oneimage_layout, R.id.widget_board_oneimage_container, remoteViews );
                updateImageWidgetsView(imageIds, remoteViews);
            }

            if (WidgetConstants.WIDGET_TYPE_COVER_FLOW.equalsIgnoreCase(widgetConf.widgetType)) {
                final Intent intent = new Intent(context, UpdateWidgetService.class);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
                intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_coverflow_layout);
                remoteViews.setRemoteAdapter(widgetConf.appWidgetId, R.id.stack_view_coverflow, intent);
                remoteViews.setEmptyView(R.id.stack_view_coverflow, R.id.stack_view_empty);

                updateWidgetViews(R.layout.widget_board_coverflow_layout, R.id.widget_board_coverflow_container, remoteViews );

                AppWidgetManager.getInstance(context).updateAppWidget(widgetConf.appWidgetId, remoteViews);

            }
        }

        private void loadBoard() {
            try {
                threads = WidgetProviderUtils.loadBestWidgetThreads(context, widgetConf.boardCode, NUM_TOP_THREADS);

                if (DEBUG) Log.i(TAG, "Loaded board=" + widgetConf.boardCode + " with widget threads");
            } catch (Exception e) {
                Log.e(TAG, "Couldn't load board=" + widgetConf.boardCode + ", defaulting to cached values");
            }
        }

        private void updateWidgetViews(int widgetLayout, int widgetContainer, RemoteViews views) {

            int containerBackground = widgetConf.roundedCorners ? R.drawable.widget_rounded_background : 0;
            views.setInt(widgetContainer, "setBackgroundResource", containerBackground);

            ChanBoard board = ChanBoard.getBoardByCode(context, widgetConf.boardCode);
            if (board == null)
                board = ChanBoard.getBoardByCode(context, ChanBoard.DEFAULT_BOARD_CODE);
            if (board == null) {
                Log.e(TAG, "Something very bad happened null board code=" + widgetConf.boardCode);
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

            views.setInt(R.id.home_button, "setImageResource", R.drawable.app_icon);
            views.setOnClickPendingIntent(R.id.home_button, makeHomeIntent());

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



            if (DEBUG) Log.i(TAG, "Updated widgetId=" + widgetConf.appWidgetId + " for board=" + widgetConf.boardCode);
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
                    ? BoardActivity.createIntentForActivity(context, widgetConf.boardCode, "")
                    : ThreadActivity.createIntentForActivity(context, thread.board, thread.no, "");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            int uniqueId = (100 * widgetConf.appWidgetId) + 5 + i;
            return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private PendingIntent makeHomeIntent() {
            Intent intent = BoardSelectorActivity.createIntentForActivity(context, BoardSelectorTab.BOARDLIST);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            int uniqueId = (100 * widgetConf.appWidgetId) + 1;
            return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private PendingIntent makeBoardIntent() {
            Intent intent;
            if (ChanBoard.isVirtualBoard(widgetConf.boardCode)) {
                BoardSelectorTab tab = ChanBoard.WATCHLIST_BOARD_CODE.equals(widgetConf.boardCode)
                        ? BoardSelectorTab.WATCHLIST
                        : BoardSelectorTab.RECENT;
                intent = BoardSelectorActivity.createIntentForActivity(context, tab);
            } else {
                intent = BoardActivity.createIntentForActivity(context, widgetConf.boardCode, "");
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            int uniqueId = (100 * widgetConf.appWidgetId) + 2;
            return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private PendingIntent makeRefreshIntent() {
            Intent intent;

            if (WidgetConstants.WIDGET_TYPE_COVER_FLOW.equalsIgnoreCase(widgetConf.widgetType)) return null;
            if (ChanBoard.WATCHLIST_BOARD_CODE.equals(widgetConf.boardCode)) {
                return null;
            } else if (ChanBoard.isVirtualBoard(widgetConf.boardCode)) {
                intent = new Intent(context, FetchPopularThreadsService.class);
            } else {
                intent = new Intent(context, FetchChanDataService.class);
                intent.putExtra(ChanHelper.BOARD_CODE, widgetConf.boardCode);
                intent.putExtra(ChanHelper.BOARD_CATALOG, 1);
                intent.putExtra(ChanHelper.PAGE, -1);

            }
            intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
            intent.putExtra(ChanHelper.BACKGROUND_LOAD, true);
            int uniqueId = (100 * widgetConf.appWidgetId) + 3;
            return PendingIntent.getService(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private PendingIntent makeConfigureIntent() {
            Intent intent = null;
            if (WidgetConstants.WIDGET_TYPE_ONE_IMAGE.equalsIgnoreCase(widgetConf.widgetType)) {
                intent = new Intent(context, WidgetConfigureOneImageActivity.class);
            }
            if (WidgetConstants.WIDGET_TYPE_BOARD.equalsIgnoreCase(widgetConf.widgetType)) {
                intent = new Intent(context, WidgetConfigureActivity.class);
            }
            if (WidgetConstants.WIDGET_TYPE_COVER_FLOW.equalsIgnoreCase(widgetConf.widgetType)) {
                intent = new Intent(context, WidgetConfigureCoverFlowActivity.class);
            }
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
            int uniqueId = (100 * widgetConf.appWidgetId) + 4;
            return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

    }

    public static class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        public static final String TAG = StackRemoteViewsFactory.class.getSimpleName();
        private static final boolean DEBUG = true;

        private Context mContext;
        private Cursor mCursor;
        private int mAppWidgetId;

        public StackRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        public void onCreate() {
            Log.d(TAG, "OnCreate method");
            // Since we reload the cursor in onDataSetChanged() which gets called immediately after
            // onCreate(), we do nothing here.
        }

        public void onDestroy() {
            Log.d(TAG, "onDestroy method");
            if (mCursor != null) {
                mCursor.close();
            }
        }

        public int getCount() {
            Log.d(TAG, "getCount method");
            return 2;
        }

        public RemoteViews getViewAt(int position) {
            Log.d(TAG, "getViewAt method" + position);
            // Get the data for this position from the content provider
            int imageId = position == 0 ? R.drawable.a_2 : R.drawable.a_3;


            // Return a proper item with the proper day and temperature
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_coverflow_item);
            rv.setImageViewResource(R.id.image_coverflow_item, imageId);
            return rv;
        }
        public RemoteViews getLoadingView() {
            // We aren't going to return a default loading view in this sample
            return null;
        }

        public int getViewTypeCount() {
            // Technically, we have two types of views (the dark and light background views)
            return 1;
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean hasStableIds() {
            return true;
        }

        public void onDataSetChanged() {
            // Refresh the cursor
            if (mCursor != null) {
                mCursor.close();
            }
            Log.d(TAG, "onDataSetChanged method");
            //todo - initialize cursor
        }
    }

}
