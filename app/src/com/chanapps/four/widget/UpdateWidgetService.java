package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
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

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/13/13
 * Time: 10:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class UpdateWidgetService extends Service {

    public static final String TAG = UpdateWidgetService.class.getSimpleName();

    public static final int NUM_TOP_THREADS = 3;

    private static final boolean DEBUG = false;

    private static DisplayImageOptions optionsWithFakeDisplayer;
    static {
        optionsWithFakeDisplayer = new DisplayImageOptions.Builder().displayer(new FakeBitmapDisplayer()).build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid app widget id passed, service terminating");
        }
        else {
            if (DEBUG) Log.i(TAG, "starting update widget service for widget=" + appWidgetId);
            WidgetConf widgetConf = BoardWidgetProvider.loadWidgetConf(this, appWidgetId);
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
            if (DEBUG) Log.i(TAG, "Found widgetConf.boardCode=" + widgetConf.boardCode + " for widget=" + widgetConf.appWidgetId + " now updating");
        }

        @Override
        public Void doInBackground(Void... params) {
            if (DEBUG) Log.i(TAG, "Starting background thread for widget update");
            loadBoard();
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            updateWidgetViews();
        }

       private void loadBoard() {
            try {
                threads = loadBestWidgetThreads(context, widgetConf.boardCode, NUM_TOP_THREADS);

                if (DEBUG) Log.i(TAG, "Loaded board=" + widgetConf.boardCode + " with widget threads");
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't load board=" + widgetConf.boardCode + ", defaulting to cached values");
            }
        }

        private void updateWidgetViews() {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_board_layout);

            int containerBackground = widgetConf.roundedCorners ? R.drawable.widget_rounded_background : 0;
            views.setInt(R.id.widget_board_container, "setBackgroundResource", containerBackground);

            ChanBoard board = ChanBoard.getBoardByCode(context, widgetConf.boardCode);
            if (board == null)
                board = ChanBoard.getBoardByCode(context, ChanBoard.DEFAULT_BOARD_CODE);
            if (board == null) {
                Log.e(TAG, "EXCEPTION something very bad happened null board code=" + widgetConf.boardCode);
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

            int[] imageIds = { R.id.image_left, R.id.image_center, R.id.image_right };
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

            if (DEBUG) Log.i(TAG, "Updated widgetId=" + widgetConf.appWidgetId + " for board=" + widgetConf.boardCode);
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
                ? BoardActivity.createIntentForActivity(context, new String(widgetConf.boardCode))
                : ThreadActivity.createIntentForActivity(context, new String(thread.board), thread.no);
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
            }
            else {
                intent = BoardActivity.createIntentForActivity(context, new String(widgetConf.boardCode));
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            int uniqueId = (100 * widgetConf.appWidgetId) + 2;
            return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private PendingIntent makeRefreshIntent() {
            Intent intent;
            if (ChanBoard.WATCHLIST_BOARD_CODE.equals(widgetConf.boardCode)) {
                return null;
            }
            else if (ChanBoard.isVirtualBoard(widgetConf.boardCode)) {
                intent = new Intent(context, FetchPopularThreadsService.class);
            }
            else {
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
            Intent intent = new Intent(context, WidgetConfigureActivity.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
            int uniqueId = (100 * widgetConf.appWidgetId) + 4;
            return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

    }

    public static ChanPost[] loadBestWidgetThreads(Context context, String boardCode, int numThreads) {
        ChanPost[] widgetThreads = new ChanPost[numThreads];

        ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
        if (board == null) {
            Log.e(TAG, "Couldn't load widget null board for boardCode=" + boardCode);
            return widgetThreads;
        }

        ChanPost[] boardThreads = board.loadedThreads != null && board.loadedThreads.length > 0
                ? board.loadedThreads
                : board.threads;
        if (boardThreads == null || boardThreads.length == 0) {
            Log.e(TAG, "Couldn't load widget no threads for boardCode=" + boardCode);
            return widgetThreads;
        }

        // try to load what we can
        int threadIndex = 0;
        int filledCount = 0;
        Set<Integer> threadsUsed = new HashSet<Integer>(numThreads);
        for (int i = 0; i < numThreads; i++) {
            ChanPost thread = null;
            while (threadIndex < boardThreads.length) {
                ChanPost test = boardThreads[threadIndex];
                threadIndex++;
                if (test != null && test.sticky <= 0 && test.tim > 0 && test.no > 0) {
                    thread = test;
                    break;
                }
            }
            if (thread != null) {
                widgetThreads[i] = thread;
                threadsUsed.add(threadIndex - 1);
                filledCount = i + 1;
            }
        }

        // what if we are missing threads? for instance no images with latest threads
        threadIndex = 0;
        if (filledCount < numThreads) {
            for (int i = 0; i < numThreads; i++) {
                if (widgetThreads[i] != null)
                    continue;
                ChanPost thread = null;
                while (threadIndex < boardThreads.length) {
                    ChanPost test = boardThreads[threadIndex];
                    threadIndex++;
                    if (test != null && !threadsUsed.contains(threadIndex) && test.sticky <= 0 && test.no > 0) {
                        thread = test;
                        break;
                    }
                }
                if (thread != null) {
                    widgetThreads[i] = thread;
                    threadsUsed.add(threadIndex - 1);
                }
            }
        }

        return widgetThreads;
    }

}
