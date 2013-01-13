package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.*;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * User: mpop
 * Date: 11/22/12
 * Time: 11:30 PM
 */
public class TopBoard4ChannerWidget extends AppWidgetProvider {
    public static final String TAG = TopBoard4ChannerWidget.class.getSimpleName();

    public String boardCode = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            Log.i(TAG, "WidgetId:" + appWidgetId);

            boardCode = ChanBoard.DEFAULT_BOARD_CODE; // FIXME: load from prefs which can be configured

            ChanPost[] threads = new ChanPost[3];
            try {
                ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
                if (board.threads != null) {
                    int threadWidgetCount = board.threads.length > 3 ? 3 : board.threads.length;
                    for (int j = 0; j < threadWidgetCount; j++) {
                        threads[j] = board.threads[j];
                    }
                }
                Log.i(TAG, "Found board=" + boardCode);
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't load board=" + boardCode + " so defaulting to board intent");
            }

            int[] imageIds = { R.id.image_left, R.id.image_center, R.id.image_right };
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.channer_widget);
            for (int j = 0; j < threads.length; j++) {
                setImageView(context, views, imageIds[j], threads[j]);
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private Intent createIntentForThread(Context context, ChanPost thread) {
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

    private void setDefaultImageView(Context context, RemoteViews views, int imageId) {
        Intent intent = BoardActivity.createIntentForActivity(context, boardCode);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(imageId, pendingIntent);
        views.setImageViewResource(imageId, R.drawable.board_3);
        Log.i(TAG, "Set image id=" + imageId + " to default=" + R.drawable.board_3);
    }

    private void setImageView(Context context, RemoteViews views, int imageId, ChanPost thread) {
        if (thread != null) {
            String thumbnailUrl = thread.getThumbnailUrl();
            try {
//                views.setImageViewUri(imageId, Uri.parse(thumbnailUrl));
                URLConnection conn = new URL(thumbnailUrl).openConnection();
                conn.connect();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is, 8192);
                Bitmap b = BitmapFactory.decodeStream(bis);
                views.setImageViewBitmap(imageId, b);

                Intent intent = createIntentForThread(context, thread);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                views.setOnClickPendingIntent(imageId, pendingIntent);
                Log.i(TAG, "Set image id=" + imageId + " to url=" + thumbnailUrl + " with bitmap=" + b);
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't parse imageUrl=" + thumbnailUrl, e);
                setDefaultImageView(context, views, imageId);
            }
        }
        else {
            setDefaultImageView(context, views, imageId);
        }
    }

}