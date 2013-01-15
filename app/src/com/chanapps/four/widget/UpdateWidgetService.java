package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

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
        (new WidgetUpdateTask()).execute();
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class WidgetUpdateTask extends AsyncTask<Void, Void, Void> {

        private static final int NUM_TOP_THREADS = 3;
        private static final int BITMAP_BUFFER_SIZE = 8192;

        private Context context;
        private String boardCode;
        private ChanPost[] threads = new ChanPost[NUM_TOP_THREADS];
        private List<Bitmap> bitmaps = new ArrayList<Bitmap>(NUM_TOP_THREADS);

        public WidgetUpdateTask() {
            context = getApplicationContext();
            boardCode = BoardWidgetProvider.getConfiguredBoardWidget(context);
            if (DEBUG)
                Log.i(TAG, "Found boardCode=" + boardCode + " for widget update");
        }

        @Override
        public Void doInBackground(Void... params) {
            if (DEBUG)
                Log.i(TAG, "Starting background thread for widget update");
            loadBoard();
            loadBitmaps();
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            updateWidgetViews(); // run on UI thread since setting views
        }

        private boolean isConnected() {
            ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }

        private void loadBoard() {
            try {
                ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
                if (board == null || board.threads == null || board.threads.length == 0) {
                    if (DEBUG) Log.i(TAG, "Couldn't load board=" + boardCode + " with valid threads for widget");
                    return;
                }

                int threadIndex = 0;
                for (int i = 0; i < NUM_TOP_THREADS; i++) {
                    ChanPost thread = null;
                    while (threadIndex < board.threads.length
                            && ((thread = board.threads[threadIndex]) == null || thread.sticky != 0))
                        threadIndex++;
                    threads[i] = thread;
                    if (DEBUG)
                        if (thread != null)
                            Log.i(TAG, "Loaded board=" + boardCode + "/" + thread.no + " threadIndex=" + threadIndex + " i=" + i + " with widget threads");
                        else
                            Log.i(TAG, "Loaded board=" + boardCode + " i=" + i + " with null thread");
                    threadIndex++;
                }
                if (DEBUG) Log.i(TAG, "Loaded board=" + boardCode + " with widget threads");
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't load board=" + boardCode + ", defaulting to cached values");
            }
        }

        private void loadBitmaps() {
            bitmaps.clear();
            for (int i = 0; i < NUM_TOP_THREADS; i++) {
                InputStream is = null;
                Bitmap b = null;
                try {
                    ChanPost thread = threads == null ? null : threads[i];
                    if (thread == null) {
                        if (DEBUG) Log.i(TAG, "no thread found for i=" + i + " skipping download");
                    }
                    else if (!isConnected()) {
                        if (DEBUG) Log.i(TAG, "network down, can't load bitmap for i=" + i + " skipping download");
                    }
                    else {
                        String thumbnailUrl = thread.getThumbnailUrl();
                        URLConnection conn = new URL(thumbnailUrl).openConnection();
                        conn.connect();
                        is = conn.getInputStream();
                        BufferedInputStream bis = new BufferedInputStream(is, BITMAP_BUFFER_SIZE);
                        ChanFileStorage.storeBoardWidgetBitmapFile(context, boardCode, i, bis);
                        b = BitmapFactory.decodeStream(bis);
                        if (b != null) {
                            if (DEBUG) Log.i(TAG, "Successfully downloaded and cached bitmap from url=" + thumbnailUrl);
                        }
                        else {
                            Log.i(TAG, "Null bitmap for url=" + thumbnailUrl);
                        }
                    }
                }
                catch (Exception e) {
                    Log.e(TAG, "Exception downloading image for board=" + boardCode + " i=" + i + " imageUrl=", e);
                }
                finally {
                    try {
                        if (is != null)
                            is.close();
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Exception closing input stream for url download", e);
                    }
                    try {
                        if (b == null) {
                            b = ChanFileStorage.getBoardWidgetBitmap(context, boardCode, i);
                            if (DEBUG) Log.i(TAG, "Null bitmap for i=" + i + " thus loading from cache");
                        }
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Exception loading cached bitmap for i=" + i, e);
                    }
                    if (b == null) { // load as resource
                        b = loadDefaultBoardBitmap(i);
                        Log.i(TAG, "Null bitmap loaded for i=" + i + " thus loading from board default");
                    }
                    if (DEBUG)  Log.i(TAG, "Set bitmap for board=" + boardCode + " i=" + i + " b=" + b);
                    bitmaps.add(b);
                }
            }
        }

        private Bitmap loadDefaultBoardBitmap(int i) {
            ChanBoard board = ChanBoard.getBoardByCode(context, boardCode);
            int imageResourceId = board.iconId;
            return BitmapFactory.decodeResource(getResources(), imageResourceId);
        }

        private void updateWidgetViews() {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widgetProvider = new ComponentName(context, BoardWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider);

            // Perform this loop procedure for each App Widget that belongs to this provider
            for (int i=0; i < appWidgetIds.length; i++) {
                int appWidgetId = appWidgetIds[i];

                // git views
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.board_widget);

                Intent intent = BoardActivity.createIntentForLaunchFromWidget(context);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // doesn't really work well
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

                // fill images
                int[] imageIds = { R.id.image_left, R.id.image_center, R.id.image_right };
                for (int j = 0; j < imageIds.length; j++) {
                    int imageId = imageIds[j];
                    Bitmap b = bitmaps.get(j);
                    views.setImageViewBitmap(imageId, b);
                    views.setOnClickPendingIntent(imageId, pendingIntent);
                }

                appWidgetManager.updateAppWidget(appWidgetId, views);

                if (DEBUG)
                    Log.i(TAG, "Updated widgetId=" + appWidgetId);
            }
        }
        /*
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
        */

        /*
        private void bindThreadImageView(RemoteViews views, int imageId, ChanPost thread) {
            Intent intent = createIntentForThread(thread);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(imageId, pendingIntent);
        }
        */

        private void bindDefaultImageView(RemoteViews views, int imageId) {
        }

    }
}
