package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
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
import com.chanapps.four.data.*;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
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

    private static final boolean DEBUG = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid app widget id passed, service terminating");
        }
        else {
            boolean firstTimeInit = intent.getBooleanExtra(ChanHelper.FIRST_TIME_INIT, false);
            if (DEBUG) Log.i(TAG, "starting update widget service for widget=" + appWidgetId + " firstTime=" + firstTimeInit);
            (new WidgetUpdateTask(appWidgetId, firstTimeInit)).execute();
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class WidgetUpdateTask extends AsyncTask<Void, Void, Void> {

        private static final int NUM_TOP_THREADS = 3;
        private static final int BITMAP_BUFFER_SIZE = 8192;

        private int appWidgetId;
        private boolean firstTimeInit;
        private Context context;
        private String boardCode;
        private ChanPost[] threads = new ChanPost[NUM_TOP_THREADS];
        private List<Bitmap> bitmaps = new ArrayList<Bitmap>(NUM_TOP_THREADS);

        public WidgetUpdateTask(int appWidgetId, boolean firstTimeInit) {
            this.appWidgetId = appWidgetId;
            this.firstTimeInit = firstTimeInit;
            context = getApplicationContext();
            boardCode = BoardWidgetProvider.getBoardCodeForWidget(context, appWidgetId);
            if (boardCode == null) {
                Log.e(TAG, "Null board code found for widget=" + appWidgetId + " defaulting to /a");
                boardCode = "a";
            }
            if (DEBUG) Log.i(TAG, "Found boardCode=" + boardCode + " for widget=" + appWidgetId + " now updating");
        }

        @Override
        public Void doInBackground(Void... params) {
            if (DEBUG) Log.i(TAG, "Starting background thread for widget update");
            if (!firstTimeInit) {
                loadBoard();
                if (boardCode == null) {
                	return null;
                }
                loadBitmaps();
            }
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            if (firstTimeInit) {
                initWidgetViews();
                (new WidgetUpdateTask(appWidgetId, false)).execute();
            }
            else {
                updateWidgetViews(); // run on UI thread since setting views
            }
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
                    if (thread != null)
                        if (DEBUG) Log.i(TAG, "Loaded board=" + boardCode + "/" + thread.no + " threadIndex=" + threadIndex + " i=" + i + " with widget threads");
                    else
                        if (DEBUG) Log.i(TAG, "Loaded board=" + boardCode + " i=" + i + " with null thread");
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
                String thumbnailUrl = null;
                try {
                    ChanPost thread = threads == null ? null : threads[i];
                    if (thread == null) {
                        if (DEBUG) Log.i(TAG, "no thread found for i=" + i + " skipping download");
                    }
                    else if (!isConnected()) {
                        if (DEBUG) Log.i(TAG, "network down, can't load bitmap for i=" + i + " skipping download");
                    }
                    else if ((thumbnailUrl = thread.getThumbnailUrl()) == null) {
                        if (DEBUG) Log.i(TAG, "no thumbnail url found for i=" + i + " skipping download");
                    }
                    else if (thumbnailUrl.isEmpty()) {
                        if (DEBUG) Log.i(TAG, "empty thumbnail url found for i=" + i + " skipping download");
                    }
                    else {
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
                            if (DEBUG) Log.i(TAG, "Null bitmap for url=" + thumbnailUrl);
                        }
                    }
                }
                catch (FileNotFoundException e) {
                    if (DEBUG) Log.i(TAG, "FileNotFound for board=" + boardCode + " i=" + i + " imageUrl=" + thumbnailUrl, e);
                }
                catch (Exception e) {
                    Log.e(TAG, "Exception downloading image for board=" + boardCode + " i=" + i + " imageUrl=" + thumbnailUrl, e);
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
                    if (b == null) { // load as resource and kill the thread so it just goes to the board
                        b = loadDefaultBoardBitmap(i);
                        if (DEBUG) Log.i(TAG, "Null bitmap loaded for i=" + i + " thus loading from board default");
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

        private void initWidgetViews() { // do this first time to avoid blank widget on network timeouts
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.board_widget);
            int[] imageIds = { R.id.image_left, R.id.image_center, R.id.image_right };
            for (int i = 0; i < imageIds.length; i++) {
                int imageId = imageIds[i];
                Bitmap b = loadDefaultBoardBitmap(i);
                views.setImageViewBitmap(imageId, b);
                PendingIntent pendingIntent = makePendingIntent(null, i); // this will make a board intent
                views.setOnClickPendingIntent(imageId, pendingIntent);
            }
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
            if (DEBUG) Log.i(TAG, "Init completed for widgetId=" + appWidgetId + " for board=" + boardCode);
        }

        private void updateWidgetViews() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
            boolean hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, true);
            boolean useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.board_widget);
            int[] imageIds = { R.id.image_left, R.id.image_center, R.id.image_right };
            for (int i = 0; i < imageIds.length; i++) {
                int imageId = imageIds[i];

                Bitmap b = bitmaps.get(i);
                views.setImageViewBitmap(imageId, b);

                ChanPost thread = threads[i];
                if (thread != null) {
                    thread.hideAllText = hideAllText;
                    thread.hidePostNumbers = hidePostNumbers;
                    thread.useFriendlyIds = useFriendlyIds;
                }
                PendingIntent pendingIntent = makePendingIntent(thread, i);
                views.setOnClickPendingIntent(imageId, pendingIntent);
            }

            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
            if (DEBUG) Log.i(TAG, "Updated widgetId=" + appWidgetId + " for board=" + boardCode);
        }

        private PendingIntent makePendingIntent(ChanPost thread, int i) {
            Intent intent = thread == null
                ? BoardActivity.createIntentForActivity(context, new String(boardCode))
                : ThreadActivity.createIntentForThread(context, thread);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            int uniqueId = 100 * appWidgetId + i;
            return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

    }
}
