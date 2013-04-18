package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.*;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.MalformedURLException;
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

    public static final int NUM_TOP_THREADS = 3;

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
            WidgetConf widgetConf = BoardWidgetProvider.loadWidgetConf(this, appWidgetId);
            if (widgetConf == null)
                widgetConf = new WidgetConf(appWidgetId); // new widget or no config;
            (new WidgetUpdateTask(getApplicationContext(), widgetConf, firstTimeInit)).execute();
        }
        return Service.START_NOT_STICKY;
    }

    static public void firstTimeInit(Context context, WidgetConf widgetConf) {
        (new WidgetUpdateTask(context, widgetConf, true)).execute();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class WidgetUpdateTask extends AsyncTask<Void, Void, Void> {

        private static final int BITMAP_BUFFER_SIZE = 8192;

        private Context context;
        private WidgetConf widgetConf;
        private boolean firstTimeInit;
        private ChanPost[] threads = new ChanPost[NUM_TOP_THREADS];
        private List<Bitmap> bitmaps = new ArrayList<Bitmap>(NUM_TOP_THREADS);

        public WidgetUpdateTask(Context context, WidgetConf widgetConf, boolean firstTimeInit) {
            this.context = context;
            this.widgetConf = widgetConf;
            this.firstTimeInit = firstTimeInit;
            if (widgetConf.boardCode == null) {
                Log.e(TAG, "Null board code found for widget=" + widgetConf.appWidgetId + " defaulting to /a");
                widgetConf.boardCode = "a";
            }
            if (DEBUG) Log.i(TAG, "Found widgetConf.boardCode=" + widgetConf.boardCode + " for widget=" + widgetConf.appWidgetId + " now updating");
        }

        @Override
        public Void doInBackground(Void... params) {
            if (DEBUG) Log.i(TAG, "Starting background thread for widget update");
            if (!firstTimeInit) {
                loadBoard();
                if (widgetConf.boardCode == null) {
                	return null;
                }
                loadBitmaps();
            }
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            updateWidgetViews();
            if (firstTimeInit)
                (new WidgetUpdateTask(context, widgetConf, false)).execute();
        }

        private void loadBoard() {
            try {
                ChanBoard board = ChanFileStorage.loadBoardData(context, widgetConf.boardCode);
                if (board == null) {
                    Log.e(TAG, "Couldn't load widget null board for widgetConf.boardCode=" + widgetConf.boardCode);
                    return;
                }
                ChanPost[] boardThreads = board.loadedThreads != null && board.loadedThreads.length > 0
                        ? board.loadedThreads
                        : board.threads;
                if (boardThreads == null || boardThreads.length == 0) {
                    Log.e(TAG, "Couldn't load widget no threads for widgetConf.boardCode=" + widgetConf.boardCode);
                    return;
                }
                threads = boardThreads;

                int threadIndex = 0;
                for (int i = 0; i < NUM_TOP_THREADS; i++) {
                    ChanPost thread = null;
                    while (threadIndex < threads.length
                            && ((thread = threads[threadIndex]) == null || thread.sticky != 0))
                        threadIndex++;
                    threads[i] = thread;
                    if (thread != null)
                        if (DEBUG) Log.i(TAG, "Loaded board=" + widgetConf.boardCode + "/" + thread.no + " threadIndex=" + threadIndex + " i=" + i + " with widget threads");
                    else
                        if (DEBUG) Log.i(TAG, "Loaded board=" + widgetConf.boardCode + " i=" + i + " with null thread");
                    threadIndex++;
                }
                if (DEBUG) Log.i(TAG, "Loaded board=" + widgetConf.boardCode + " with widget threads");
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't load board=" + widgetConf.boardCode + ", defaulting to cached values");
            }
        }

        private void loadBitmaps() {
            bitmaps.clear();
            for (int i = 0; i < NUM_TOP_THREADS; i++) {
                bitmaps.add(getWidgetBitmap(i));
            }
        }

        private Bitmap getWidgetBitmap(int i) {
            Bitmap b;
            if ((b = ChanFileStorage.getBoardWidgetBitmap(context, widgetConf.boardCode, i)) != null) {
                if (DEBUG) Log.i(TAG, "Loaded cached bitmap since download failed for i=" + i);
            }
            else if ((b = getWidgetBitmapFromBoardStorage(i)) != null)
            {
                if (DEBUG) Log.i(TAG, "Loaded bitmap from board storage for i=" + i);
            }
            else if ((b = downloadWidgetBitmap(i)) != null)
            {
                if (DEBUG) Log.i(TAG, "Downloaded bitmap since empty storage for i=" + i);
            }
            else {
                if (DEBUG) Log.i(TAG, "Returned default bitmap since empty cache for i=" + i);
                b = loadDefaultBoardBitmap(i);
            }
            return b;
        }

        private String getThumbnailUrl(int i) {
            if (threads == null || threads.length < (i+1) || threads[i] == null || threads[i].no < 1)
                return null;
            return threads[i].thumbnailUrl();
        }

        private Bitmap getWidgetBitmapFromBoardStorage(int i) {
            String thumbnailUrl = getThumbnailUrl(i);
            Bitmap b = null;
            File thumbFile = null;
            if (thumbnailUrl != null
                    && !thumbnailUrl.isEmpty()
                    && ImageLoader.getInstance() != null
                    && ImageLoader.getInstance().getDiscCache() != null)
            {
                thumbFile = ImageLoader.getInstance().getDiscCache().get(thumbnailUrl);
            }
            if (thumbFile != null && thumbFile.exists()) { // try to load from board
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    fis = new FileInputStream(thumbFile);
                    bis = new BufferedInputStream(fis);
                    b = BitmapFactory.decodeStream(bis);
                    ChanFileStorage.storeBoardWidgetBitmap(context, widgetConf.boardCode, i, b);
                }
                catch (FileNotFoundException e) {
                    if (DEBUG) Log.i(TAG, "FileNotFound on file load for board=" + widgetConf.boardCode + " i=" + i + " imageUrl=" + thumbnailUrl, e);
                }
                catch (IOException e) {
                    if (DEBUG) Log.i(TAG, "IOException on file load for board=" + widgetConf.boardCode + " i=" + i + " imageUrl=" + thumbnailUrl, e);
                }
                finally {
                    IOUtils.closeQuietly(bis);
                    IOUtils.closeQuietly(fis);
                }
            }
            return b;
        }

        private Bitmap downloadWidgetBitmap(int i) {
            String thumbnailUrl = getThumbnailUrl(i);
            Bitmap b = null;

            NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
            if (!NetworkProfileManager.isConnected()
                    || health == NetworkProfile.Health.NO_CONNECTION
                    || health == NetworkProfile.Health.BAD)
            {
                if (DEBUG) Log.i(TAG, "network down, can't load bitmap for i=" + i + " skipping download");
                return null;
            }

            InputStream is = null;
            BufferedInputStream bis = null;
            try {
                URLConnection conn = new URL(thumbnailUrl).openConnection();
                conn.connect();
                is = conn.getInputStream();
                bis = new BufferedInputStream(is, BITMAP_BUFFER_SIZE);
                b = BitmapFactory.decodeStream(bis);
                ChanFileStorage.storeBoardWidgetBitmap(context, widgetConf.boardCode, i, b);
                if (b != null) {
                    if (DEBUG) Log.i(TAG, "Successfully downloaded and cached bitmap from url=" + thumbnailUrl);
                }
                else {
                    if (DEBUG) Log.i(TAG, "Null bitmap for url=" + thumbnailUrl);
                }
            }
            catch (MalformedURLException e) {
                if (DEBUG) Log.i(TAG, "MalformedURL for board=" + widgetConf.boardCode + " i=" + i + " imageUrl=" + thumbnailUrl, e);
            }
            catch (FileNotFoundException e) {
                if (DEBUG) Log.i(TAG, "FileNotFound on download for board=" + widgetConf.boardCode + " i=" + i + " imageUrl=" + thumbnailUrl, e);
            }
            catch (IOException e) {
                if (DEBUG) Log.i(TAG, "IOException on download for board=" + widgetConf.boardCode + " i=" + i + " imageUrl=" + thumbnailUrl + " rechecking network", e);
                NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(context);
            }
            catch (OutOfMemoryError e) {
                if (DEBUG) Log.i(TAG, "Out of memory on download for board=" + widgetConf.boardCode + " i=" + i + " imageUrl=" + thumbnailUrl, e);
            }
            finally {
                IOUtils.closeQuietly(bis);
                IOUtils.closeQuietly(is);
            }
            return b;
        }

        private Bitmap loadDefaultBoardBitmap(int i) {
            int imageResourceId = ChanBoard.getIndexedImageResourceId(widgetConf.boardCode, i);
            return BitmapFactory.decodeResource(context.getResources(), imageResourceId);
        }

        private void updateWidgetViews() {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_board_layout);

            int containerBackground = widgetConf.roundedCorners ? R.drawable.widget_rounded_background : 0;
            views.setInt(R.id.widget_board_container, "setBackgroundResource", containerBackground);

            ChanBoard board = ChanBoard.getBoardByCode(context, widgetConf.boardCode);
            if (board == null)
                board = ChanBoard.getBoardByCode(context, ChanBoard.DEFAULT_BOARD_CODE);
            String boardTitle = board.name + " /" + board.link + "/";
            int boardTitleColor = widgetConf.boardTitleColor;
            int boardTitleVisibility = widgetConf.showBoardTitle ? View.VISIBLE : View.GONE;
            views.setTextViewText(R.id.board_title, boardTitle);
            views.setTextColor(R.id.board_title, boardTitleColor);
            views.setViewVisibility(R.id.board_title, boardTitleVisibility);

            int refreshBackground = widgetConf.showRefreshButton ? R.color.PaletteBlackHalfOpacity : 0;
            int refreshDrawable = widgetConf.showRefreshButton ? R.drawable.widget_refresh_button_selector : 0;
            views.setInt(R.id.refresh, "setBackgroundResource", refreshBackground);
            views.setInt(R.id.refresh, "setImageResource", refreshDrawable);

            int configureBackground = widgetConf.showConfigureButton ? R.color.PaletteBlackHalfOpacity : 0;
            int configureDrawable = widgetConf.showConfigureButton ? R.drawable.widget_configure_button_selector : 0;
            views.setInt(R.id.configure, "setBackgroundResource", configureBackground);
            views.setInt(R.id.configure, "setImageResource", configureDrawable);

            int[] imageIds = { R.id.image_left, R.id.image_center, R.id.image_right };
            for (int i = 0; i < imageIds.length; i++) {
                int imageId = imageIds[i];
                Bitmap b = bitmaps != null && bitmaps.size() >= i+1 && bitmaps.get(i) != null
                        ? bitmaps.get(i)
                        : loadDefaultBoardBitmap(i);
                views.setImageViewBitmap(imageId, b);
                ChanPost thread = threads != null && threads.length >= i+1
                        ? threads[i]
                        : null;
                PendingIntent pendingIntent = makePendingIntent(thread, i);
                views.setOnClickPendingIntent(imageId, pendingIntent);
            }
            views.setOnClickPendingIntent(R.id.refresh, makeRefreshIntent());
            views.setOnClickPendingIntent(R.id.configure, makeConfigureIntent());

            AppWidgetManager.getInstance(context).updateAppWidget(widgetConf.appWidgetId, views);
            if (DEBUG) Log.i(TAG, "Updated widgetId=" + widgetConf.appWidgetId + " for board=" + widgetConf.boardCode);
        }

        private PendingIntent makePendingIntent(ChanPost thread, int i) {
            Intent intent = (thread == null || thread.no < 1)
                ? BoardActivity.createIntentForActivity(context, new String(widgetConf.boardCode))
                : ThreadActivity.createIntentForActivity(context, new String(thread.board), thread.no);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            int uniqueId = 100 * widgetConf.appWidgetId + i;
            return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private PendingIntent makeRefreshIntent() {
            Intent intent = new Intent(context, FetchChanDataService.class);
            intent.putExtra(ChanHelper.BOARD_CODE, widgetConf.boardCode);
            intent.putExtra(ChanHelper.BOARD_CATALOG, 1);
            intent.putExtra(ChanHelper.PAGE, -1);
            intent.putExtra(ChanHelper.PRIORITY_MESSAGE, 1);
            intent.putExtra(ChanHelper.FORCE_REFRESH, true);
            intent.putExtra(ChanHelper.BACKGROUND_LOAD, true);
            int uniqueId = 10 * widgetConf.appWidgetId + 1;
            return PendingIntent.getService(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }


        private PendingIntent makeConfigureIntent() {
            Intent intent = new Intent(context, WidgetConfigureActivity.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
            int uniqueId = 10 * widgetConf.appWidgetId + 2;
            return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

    }
}
