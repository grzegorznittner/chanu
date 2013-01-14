package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/13/13
 * Time: 10:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class UpdateWidgetService extends Service {

    public static final String TAG = UpdateWidgetService.class.getSimpleName();

    private static final boolean DEBUG = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG)
            Log.i(TAG, "starting UpdateWidgetService");
        if (intent.hasExtra(ChanHelper.FIRST_TIME_INIT))
            (new WidgetUpdateTask()).defaultWidgetInit();
        else
            (new WidgetUpdateTask()).execute();
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static String getConfiguredBoardWidget(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(SettingsActivity.PREF_WIDGET_BOARD, ChanBoard.DEFAULT_BOARD_CODE);
    }

    public class WidgetUpdateTask extends AsyncTask<Void, Void, Boolean> {

        private static final int NUM_TOP_THREADS = 3;
        private static final int BITMAP_BUFFER_SIZE = 8192;

        private Context context;
        private String boardCode;
        private ChanPost[] threads = new ChanPost[NUM_TOP_THREADS];
        private List<Bitmap> bitmaps = new ArrayList<Bitmap>(NUM_TOP_THREADS);

        public WidgetUpdateTask() {
            context = getApplicationContext();
            boardCode = getConfiguredBoardWidget(context);
            if (DEBUG)
                Log.i(TAG, "Found boardCode=" + boardCode + " for widget update");
        }

        @Override
        public Boolean doInBackground(Void... params) {
            if (DEBUG)
                Log.i(TAG, "Starting background thread for widget update");
            if (!isConnected()) {
                if (DEBUG)
                    Log.i(TAG, "Network not active, skipping widget update");
                return false;
            }
            if (!loadBoard()) {
                Log.e(TAG, "Couldn't load board for widget, skipping update");
                return false;
            }
            boolean success = downloadBitmaps();
            return success;
        }

        @Override
        public void onPostExecute(Boolean result) {
            if (result) {
                if (DEBUG)
                    Log.i(TAG, "Successful data load, now setting widget views");
                updateWidgetViews(); // run on UI thread since setting views
            }
            else {
                if (DEBUG)
                    Log.i(TAG, "Bypassing widget update, unable to load fresh data, defaulting");
                defaultWidgetInit();
            }
        }

        public void defaultWidgetInit() { // fast inits to default with no loading or downloading
            if (DEBUG)
                Log.i(TAG, "Default init for widget");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetProvider = new ComponentName(context, BoardWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider);
            final int N = appWidgetIds.length;
            // Perform this loop procedure for each App Widget that belongs to this provider
            for (int i=0; i<N; i++) {
                int appWidgetId = appWidgetIds[i];

                int[] imageIds = { R.id.image_left, R.id.image_center, R.id.image_right };
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.board_widget);
                for (int j = 0; j < imageIds.length; j++) {
                    setDefaultImageView(views, imageIds[j]);
                    bindDefaultImageView(views, imageIds[j]);
                }
                appWidgetManager.updateAppWidget(appWidgetId, views);

                if (DEBUG)
                    Log.i(TAG, "Finished default init for widgetId=" + appWidgetId);
            }
        }

        private boolean isConnected() {
            boolean success = false;
            ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected())
                success = true;
            return success;
        }

        private boolean loadBoard() {
            boolean success = false;
            int threadWidgetCount = 0;
            try {
                ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
                if (board.threads != null) {
                    threadWidgetCount = board.threads.length > NUM_TOP_THREADS
                            ? NUM_TOP_THREADS : board.threads.length;
                    for (int j = 0; j < threadWidgetCount; j++) {
                        threads[j] = board.threads[j];
                    }
                }
                Log.i(TAG, "Found board=" + boardCode + " with thread count=" + threadWidgetCount);
                success = true;
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't load board=" + boardCode + " so defaulting to board intent");
            }
            if (threads == null || threads.length == 0)
                success = false;
            return success;
        }

        private boolean downloadBitmaps() {
            boolean success = false;
            bitmaps.clear();
            for (int i = 0; i < NUM_TOP_THREADS; i++)
                bitmaps.add(null);
            int i = 0;
            for (ChanPost thread : threads) {
                if (thread != null) {
                    String thumbnailUrl = thread.getThumbnailUrl();
                    try {
                        if (!isConnected()) {
                            if (DEBUG)
                                Log.i(TAG, "Network went down, skipping bitmap download and widget update");
                            return false;
                        }
                        URLConnection conn = new URL(thumbnailUrl).openConnection();
                        conn.connect();
                        InputStream is = conn.getInputStream();
                        BufferedInputStream bis = new BufferedInputStream(is, BITMAP_BUFFER_SIZE);
                        Bitmap b = BitmapFactory.decodeStream(bis);
                        if (b != null) {
                            bitmaps.set(i, b);
                            success = true;
                        }
                        if (DEBUG)
                            Log.i(TAG, "Set bitmap for thread=" + thread.no + " i=" + i + " b=" + b);

                    }
                    catch (Exception e) {
                        Log.e(TAG, "Couldn't download image for thread=" + thread.no + " imageUrl=" + thumbnailUrl, e);
                    }
                }
                i++;
            }
            return success; // at least one bitmap worked, so update
        }

        private void updateWidgetViews() {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetProvider = new ComponentName(context, BoardWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider);
            final int N = appWidgetIds.length;
            // Perform this loop procedure for each App Widget that belongs to this provider
            for (int i=0; i<N; i++) {
                int appWidgetId = appWidgetIds[i];

                int[] imageIds = { R.id.image_left, R.id.image_center, R.id.image_right };
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.board_widget);
                for (int j = 0; j < imageIds.length; j++) {
                    setImageView(views, imageIds[j], bitmaps.get(j), threads[j]);
                }
                appWidgetManager.updateAppWidget(appWidgetId, views);

                if (DEBUG)
                    Log.i(TAG, "Updated widgetId=" + appWidgetId);
            }
        }

        private Intent createIntentForThread(ChanPost thread) {
            return ThreadActivity.createIntentForActivity(
                    context,
                    thread.board,
                    thread.no,
                    thread.getThreadText(),
                    thread.getThumbnailUrl(),
                    thread.tn_w,
                    thread.tn_h,
                    thread.tim,
                    false,
                    0
            );
        }

        private void setDefaultImageView(RemoteViews views, int imageId) {
            ChanBoard board = ChanBoard.getBoardByCode(context, boardCode);
            int imageResourceId = board.iconId;
            views.setImageViewResource(imageId, imageResourceId);
        }

        /*
        private void bindThreadImageView(RemoteViews views, int imageId, ChanPost thread) {
            Intent intent = createIntentForThread(thread);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(imageId, pendingIntent);
        }
        */

        private void bindDefaultImageView(RemoteViews views, int imageId) {
            Intent intent = BoardActivity.createIntentForActivity(context, boardCode);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // doesn't really work well
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(imageId, pendingIntent);
        }

        private void setImageView(RemoteViews views, int imageId, Bitmap b, ChanPost thread) {
            if (thread != null) {
                try {
                    if (b != null)
                        views.setImageViewBitmap(imageId, b);
                    else
                        setDefaultImageView(views, imageId);
                    if (DEBUG)
                        if (b == null)
                            Log.i(TAG, "Set image id=" + imageId + " to default bitmap click threadNo=" + thread.no);
                        else
                            Log.i(TAG, "Set image id=" + imageId + " to bitmap=" + b + " click threadNo=" + thread.no);

                    /* FIXME
                    can't get anything except the first thread click to work
                    bindThreadImageView(views, imageId, thread);
                    */
                    bindDefaultImageView(views, imageId);
                }
                catch (Exception e) {
                    Log.e(TAG, "Error setting image id=" + imageId + " for thread=" + thread.no + " setting to default click board=" + boardCode, e);
                    setDefaultImageView(views, imageId);
                    bindDefaultImageView(views, imageId);
                }
            }
            else {
                Log.e(TAG, "Null thread, setting default image for imageId=" + imageId + " click board=" + boardCode);
                setDefaultImageView(views, imageId);
                bindDefaultImageView(views, imageId);
            }
        }

    }
}
