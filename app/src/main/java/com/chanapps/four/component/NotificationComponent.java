package com.chanapps.four.component;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.chanapps.four.activity.*;
import com.chanapps.four.data.*;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 11/18/13
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class NotificationComponent {

    private static final String TAG = NotificationComponent.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int CLEAR_CACHE_NOTIFY_ID = 0x870932; // a unique notify idea is needed for each notify to "clump" together
    private static final long NOTIFICATION_UPDATE_TIME = 3000;  // 1s

    public static void notifyNewReplies(Context context, ChanPost watchedThread, ChanThread loadedThread) {
        if (DEBUG) Log.i(TAG, "notifyNewReplies() watched=" + watchedThread + " loaded=" + loadedThread);
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;
        if (watchedThread == null || loadedThread == null)
            return;
        if (loadedThread.posts == null || loadedThread.posts.length == 0 || loadedThread.posts[0] == null)
            return;

        String board = loadedThread.board;
        long threadNo = loadedThread.no;
        ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        ChanActivityId aid = NetworkProfileManager.instance().getActivityId();
        // limit notification when on watchlist or active thread
        if (activity != null && activity.getChanHandler() != null && aid != null) {
            if (board.equals(aid.boardCode) && threadNo == aid.threadNo) {
                if (DEBUG) Log.i(TAG, "notifyNewReplies /" + board + "/" + threadNo + " user on thread, skipping notification");
                return;
            }
            else if (ChanBoard.WATCHLIST_BOARD_CODE.equals(aid.boardCode)) {
                if (DEBUG) Log.i(TAG, "notifyNewReplies /" + board + "/" + threadNo + " user on watchlist, skipping notification");
                return;
            }
        }

        int numNewReplies = loadedThread.posts[0].replies - (watchedThread.replies >= 0 ? watchedThread.replies : 0);
        if (DEBUG) Log.i(TAG, "notifyNewReplies() /" + board + "/" + threadNo + " newReplies=" + numNewReplies);
        if (numNewReplies <= 0 && !loadedThread.isDead)
            return;
        int notificationId = board.hashCode() + (int)threadNo;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String title = context.getString(R.string.app_name);
        String postText;
        if (loadedThread.isDead) {
            postText = context.getString(R.string.mobile_profile_fetch_dead_thread);
        }
        else {
            String postPlurals = context.getResources().getQuantityString(R.plurals.thread_activity_updated, numNewReplies);
            //String imagePlurals = context.getResources().getQuantityString(R.plurals.thread_num_images, numNewImages);
            postText = String.format(postPlurals, 0).replace("0", "");
        }
        String threadId = "/" + board + "/" + threadNo;
        //String imageText = String.format(imagePlurals, numNewImages);
        //String text = String.format("%s and %s for %s/%d", postText, imageText, board, threadNo);
        String text = (postText + " " + threadId).trim();

        Intent threadActivityIntent = ThreadActivity.createIntent(context, board, threadNo, "");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)System.currentTimeMillis(),
                threadActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
        Notification.Builder notifBuilder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.app_icon_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        Bitmap largeIcon = loadLargeIcon(context, loadedThread.posts[0]);
        if (largeIcon != null)
            notifBuilder.setLargeIcon(largeIcon);
        if (numNewReplies > 0)
            notifBuilder.setNumber(numNewReplies);
        Notification noti = buildNotification(notifBuilder);
        if (DEBUG) Log.i(TAG, "notifyNewReplies() sending notification for " + numNewReplies + " new replies for /" + board + "/" + threadNo);
        notificationManager.notify(notificationId, noti);
    }

    protected static Notification buildNotification(Notification.Builder notifBuilder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return deprecatedBuildNotification(notifBuilder);
        else
            return notifBuilder.build();
    }

    @SuppressWarnings("deprecation")
    protected static Notification deprecatedBuildNotification(Notification.Builder notifBuilder) {
        return notifBuilder.getNotification();
    }

    public static void notifyClearCacheCancelled(Context context) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(CLEAR_CACHE_NOTIFY_ID, makeNotification(context, context.getString(R.string.pref_clear_cache_error)));
    }

    public static void notifyClearCacheResult(Context context, String result) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(CLEAR_CACHE_NOTIFY_ID, makeNotification(context, result));
    }

    private static Notification makeNotification(Context context, String contentText) {
        Intent intent = BoardActivity.createIntent(context, ChanBoard.defaultBoardCode(context), "");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.app_icon_notification)
                .setContentTitle(context.getString(R.string.pref_clear_cache_notification_title))
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .build();
        return notification;
    }

    public static void notifyDownloadScheduled(Context context, int notificationId, String board, long threadNo) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;

        String titleText = context.getString(R.string.download_all_images_to_gallery_menu);
        String threadText = "/" + board + "/" + threadNo;
        String text = titleText + " " + threadText;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.drawable.app_icon_notification);

        notificationManager.notify(notificationId, notifBuilder.build());
    }

    public static void notifyDownloadFinished(Context context, int notificationId, DownloadImageTargetType downloadImageTargetType,
                                              ChanThread thread, String board, long threadNo, String targetFile) {
        if (ThreadImageDownloadService.checkIfStopped(notificationId)) {
            return;
        }
        if (DEBUG) Log.i(TAG, "notifyDownloadFinished " + downloadImageTargetType + " " + board + "/" + threadNo
                + " " + thread.posts.length + " posts, file " + targetFile);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;

        //boolean useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        boolean useFriendlyIds = false;
        if (thread != null)
            thread.useFriendlyIds = useFriendlyIds;

        Intent threadActivityIntent = null;
        switch(downloadImageTargetType) {
            case TO_BOARD:
            case TO_ZIP:
                threadActivityIntent = ThreadActivity.createIntent(context, board, threadNo, "");
                break;
            case TO_GALLERY:
                threadActivityIntent = GalleryViewActivity.getAlbumViewIntent(context, board, threadNo);
                break;
        }

        if (downloadImageTargetType != DownloadImageTargetType.TO_BOARD) { // notify except on board auto-download
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification.Builder notifBuilder = new Notification.Builder(context);
            notifBuilder.setSmallIcon(R.drawable.app_icon_notification);
            notifBuilder.setWhen(Calendar.getInstance().getTimeInMillis());
            notifBuilder.setAutoCancel(true);
            notifBuilder.setContentTitle(context.getString(R.string.download_all_images_complete));
            notifBuilder.setContentText
                    (String.format(context.getString(R.string.download_all_images_complete_detail), board, threadNo));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    threadActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_UPDATE_CURRENT);
            notifBuilder.setContentIntent(pendingIntent);
            Notification noti = buildNotification(notifBuilder);
            notificationManager.notify(notificationId, noti);
        }
    }

    private static Bitmap loadLargeIcon(Context context, ChanPost loadedThread) {
        Bitmap largeIcon = null;
        try {
            try {
                largeIcon = loadIconBitmap(context, loadedThread);
                if (DEBUG) Log.i(TAG, "loadLargeIcon() loaded notification large icon=" + largeIcon);
            }
            catch (Exception e) {
                Log.e(TAG, "loadLargeIcon() exception loading thumbnail for notification for thread=" + loadedThread.no, e);
                if (DEBUG) Log.i(TAG, "using default notification large icon");
            }
            catch (Error e) {
                Log.e(TAG, "loadLargeIcon() error loading thumbnail for notification for thread=" + loadedThread.no, e);
                if (DEBUG) Log.i(TAG, "using default notification large icon");
            }
            if (largeIcon == null) {
                if (DEBUG) Log.i(TAG, "loadLargeIcon() null bitmap, loading board default");
                int drawableId = ChanBoard.getRandomImageResourceId(loadedThread.board, loadedThread.no);
                largeIcon = BitmapFactory.decodeResource(context.getResources(), drawableId);
            }
            if (largeIcon == null) {
                if (DEBUG) Log.i(TAG, "loadLargeIcon() null bitmap, loading app-wide resource");
                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon_notification_large);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "loadLargeIcon() exception loading default thumbnail for notification for thread=" + loadedThread.no, e);
        }
        catch (Error e) {
            Log.e(TAG, "loadLargeIcon() error loading default thumbnail for notification for thread=" + loadedThread.no, e);
        }
        return largeIcon;
    }

    private static Bitmap loadIconBitmap(Context context, ChanPost loadedThread) throws Exception {
        if (loadedThread == null)
            return null;
        String imageUrl = loadedThread.thumbnailUrl(context);
        if (DEBUG) Log.i(TAG, "loadLargeIcon() imageUrl=" + imageUrl);
        if (imageUrl == null)
            return null;
        File bitmapFile = ChanImageLoader.getInstance(context).getDiscCache().get(imageUrl);
        if (DEBUG) Log.i(TAG, "loadLargeIcon() bitmapFile=" + bitmapFile);
        Bitmap tmp = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath());
        int widthPx = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
        int heightPx = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
        int offsetX = (tmp.getWidth() - widthPx) / 2;
        int offsetY = (tmp.getHeight() - heightPx) / 2;
        return Bitmap.createBitmap(tmp, offsetX, offsetY, widthPx, heightPx);
    }

    public static long notifyDownloadUpdated(Context context, int notificationId, String board, long threadNo,
                                             int totalNumImages, int downloadedImages, long lastUpdateTime) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return lastUpdateTime;
        if (ThreadImageDownloadService.checkIfStopped(notificationId)) {
			return lastUpdateTime;
		}
		long now = new Date().getTime();
		if (now - lastUpdateTime < NOTIFICATION_UPDATE_TIME) {
			return lastUpdateTime;
		}
		lastUpdateTime = now;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String titleText = totalNumImages > 1
                ? context.getString(R.string.download_all_images_to_gallery_menu)
                : context.getString(R.string.download_images_to_gallery_menu);
        String threadText = "/" + board + "/" + threadNo;
        String downloadText = downloadedImages + "/" + totalNumImages;
        String text = titleText + " " + threadText + " " + downloadText;

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setProgress(totalNumImages, downloadedImages, false)
                .setSmallIcon(R.drawable.app_icon_notification);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                CancelDownloadActivity.createIntent(context, notificationId, board, threadNo),
        		Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_UPDATE_CURRENT);
        notifBuilder.setContentIntent(pendingIntent);

		notificationManager.notify(notificationId, notifBuilder.build());
        return lastUpdateTime;
	}

    public static void notifyDownloadError(Context context, int notificationId, ChanThread thread) {
        if (thread == null)
            return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;

        //boolean useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        boolean useFriendlyIds = false;
        if (thread != null)
            thread.useFriendlyIds = useFriendlyIds;

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder notifBuilder = new Notification.Builder(context);
        notifBuilder.setWhen(Calendar.getInstance().getTimeInMillis());
        notifBuilder.setAutoCancel(true);
        notifBuilder.setContentTitle(context.getString(R.string.thread_image_download_error));
        notifBuilder.setContentText(thread.board + "/" + thread.no);
        notifBuilder.setSmallIcon(R.drawable.app_icon_notification);

        Intent threadActivityIntent = ThreadActivity.createIntent(context, thread.board, thread.no, "");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                threadActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
        notifBuilder.setContentIntent(pendingIntent);

        Notification noti = buildNotification(notifBuilder);
        notificationManager.notify(notificationId, noti);
    }

    private static final int FAVORITES_NOTIFICATION_TOKEN = 0x13;

    public static void notifyNewThreads(final Context context, final String boardCode, final int numNewThreads,
                                        final ChanThread newThread) {
        if (DEBUG) Log.i(TAG, "notifyNewThreads() /" + boardCode + "/ numThreads=" + numNewThreads);
        /*
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;
        if (boardCode == null || boardCode.isEmpty())
            return;
        if (numNewThreads <= 0)
            return;

        ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        ChanActivityId aid = NetworkProfileManager.instance().getActivityId();
        // limit notification when on watchlist or active thread
        if (activity != null && activity.getChanHandler() != null && aid != null) {
            if (boardCode.equals(aid.boardCode)) {
                if (DEBUG) Log.i(TAG, "notifyNewThreads() /" + boardCode + "/ user on board, skipping notification");
                return;
            }
        }

        int notificationId = boardCode.hashCode() + FAVORITES_NOTIFICATION_TOKEN;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String title = context.getString(R.string.app_name);
        String postPlurals = context.getResources().getQuantityString(R.plurals.board_activity_updated, numNewThreads);
        String text = String.format(postPlurals, boardCode);

        ChanPost iconThread;
        if (newThread == null)
            iconThread = null;
        else if (newThread.posts == null || newThread.posts.length == 0 || newThread.posts[0] == null)
            iconThread = newThread;
        else
            iconThread = newThread.posts[0];
        Bitmap largeIcon = loadLargeIcon(context, iconThread);

        Intent boardActivityIntent = BoardActivity.createIntent(context, boardCode, "");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)System.currentTimeMillis(),
                boardActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
        Notification.Builder notifBuilder = new Notification.Builder(context)
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.app_icon_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setNumber(numNewThreads)
                ;
        Notification noti = notifBuilder.getNotification();
        if (DEBUG) Log.i(TAG, "notifyNewThreads() sending notification for " + numNewThreads
                + " new threads for /" + boardCode + "/");
        notificationManager.notify(notificationId, noti);
        */
    }

}
