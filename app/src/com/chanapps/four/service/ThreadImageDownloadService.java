/**
 * 
 */
package com.chanapps.four.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 */
public class ThreadImageDownloadService extends BaseChanService implements ChanIdentifiedService {
	private static final String TAG = ThreadImageDownloadService.class.getSimpleName();
    private static final boolean DEBUG = true;
    
    private static final String GALLERY_PREFIX = "4Channer_";
    
    private static final String TARGET_TYPE = "ThreadImageDownloadService.targetType";
    private static final String START_POST_NO = "ThreadImageDownloadService.startPostNo";
    private static final String RESTART_COUNTER = "ThreadImageDownloadService.restartCounter";
    private static final String TARGET_FOLDER = "ThreadImageDownloadService.folder";
    
    private enum TargetType {TO_BOARD, TO_GALLERY};
    
	private static final int MAX_RESTARTS = 3;

    public static void startDownloadToBoardFolder(Context context, String board, long threadNo) {
        startDownload(context, board, threadNo, TargetType.TO_BOARD, 0, 0, null);
    }
    
    public static void startDownloadToGalleryFolder(Context context, String board, long threadNo, String galleryFolder) {
        startDownload(context, board, threadNo, TargetType.TO_GALLERY, 0, 0, galleryFolder);
    }

    private static void startDownload(Context context, String board, long threadNo, TargetType targetType,
    		long startPostNo, int restartCounter, String folder) {
        if (DEBUG) Log.i(TAG, (restartCounter > 0 ? "Restart " : "Start") 
        		+ " all image download service for thread " + board + "/" + threadNo
        		+ (startPostNo == 0 ? "" : " from post " + startPostNo));
        Intent intent = new Intent(context, ThreadImageDownloadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, board);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(TARGET_TYPE, targetType.toString());
        intent.putExtra(START_POST_NO, startPostNo);
        intent.putExtra(RESTART_COUNTER, restartCounter);
        intent.putExtra(TARGET_FOLDER, folder);
        context.startService(intent);
    }
    /*
    public static void cancelService(Context context, String url) {
        if (DEBUG) Log.i(TAG, "Cancelling image download service for " + url);
        Intent intent = new Intent(context, ThreadImageDownloadService.class);
        intent.putExtra(ChanHelper.CLEAR_FETCH_QUEUE, 1);
        intent.putExtra(ChanHelper.IMAGE_URL, url);
        context.startService(intent);
    }
    */

    public ThreadImageDownloadService() {
   		super("threadimagesdownload");
   	}

    protected ThreadImageDownloadService(String name) {
   		super(name);
   	}
    
    private String board = null;
    private long threadNo = 0;
    private long startPostNo = 0;
    private int restartCounter = 0;
    private TargetType targetType = null;
    private String targetFolder = null;
	
	@Override
	protected void onHandleIntent(Intent intent) {
		board = intent.getStringExtra(ChanHelper.BOARD_CODE);
		threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
		startPostNo = intent.getLongExtra(START_POST_NO, 0);
		restartCounter = intent.getIntExtra(RESTART_COUNTER, 0);
		targetType = TargetType.valueOf(intent.getStringExtra(TARGET_TYPE));
		targetFolder = intent.getStringExtra(TARGET_FOLDER);
		
		
		ChanThread thread = ChanFileStorage.loadThreadData(getBaseContext(), board, threadNo);
		if (targetType == TargetType.TO_GALLERY && targetFolder == null) {
			Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			String now = formatter.format(Calendar.getInstance().getTime());
			targetFolder = GALLERY_PREFIX + thread.board + "_" + now;				
		}
		
		try {			
			if (DEBUG) Log.i(TAG, (restartCounter > 0 ? "Restart " : "Start") 
	        		+ " handling all image download service for thread " + board + "/" + threadNo
	        		+ (startPostNo == 0 ? "" : " from post " + startPostNo)
	        		+ (restartCounter > 0 ? ", restarted " + restartCounter + " time(s)." : ""));

			boolean startPointFound = startPostNo == 0;
			for (ChanPost post : thread.posts) {
				if (startPointFound || post.no == startPostNo) {
					startPointFound = true;
					if (post.tim != 0) {
						downloadImage(post);
						if (targetType == TargetType.TO_GALLERY) {
							storeImageInGallery(post);
						}
					}
					startPostNo = post.no;  // setting last fetched image no
				}
			}
			notifyDownloadFinished(thread);
		} catch (Exception e) {
            Log.e(TAG, "Error in image download service", e);
            if (restartCounter > MAX_RESTARTS) {
            	NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
            	notifyDownloadError(thread);
            } else {
            	startDownload(getBaseContext(), board, threadNo, targetType, startPostNo, restartCounter + 1, targetFolder);
            }
		}
	}

	private int downloadImage(ChanPost post) throws IOException, MalformedURLException, FileNotFoundException, InterruptedException {
		long startTime = Calendar.getInstance().getTimeInMillis();
		
		String imageFile = ChanFileStorage.getLocalImageUrl(getBaseContext(), post);
		File targetFile = new File(URI.create(imageFile));
		if (targetFile.exists()) {
			return (int)targetFile.length();
		}
		
		InputStream in = null;
		OutputStream out = null;
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)new URL(post.getImageUrl()).openConnection();
			FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
			conn.setReadTimeout(fetchParams.readTimeout);
			//conn.setConnectTimeout(fetchParams.connectTimeout);
			
			in = conn.getInputStream();
			out = new FileOutputStream(targetFile);
			int fileLength = IOUtils.copy(in, out);
			long endTime = Calendar.getInstance().getTimeInMillis();
			NetworkProfileManager.instance().finishedImageDownload(this, (int)(endTime - startTime), fileLength);
			if (DEBUG) Log.i(TAG, "Stored image " + post.getImageUrl() + " to file "
					+ targetFile.getAbsolutePath() + " in " + (endTime - startTime) + "ms.");
			return fileLength;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			closeConnection(conn);
		}
	}
	
	private void storeImageInGallery(ChanPost post) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			String imageFile = ChanFileStorage.getLocalImageUrl(getBaseContext(), post);
			in = new FileInputStream(new File(imageFile));
			File galleryFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), 
			    targetFolder);
			if (!galleryFolder.exists() || !galleryFolder.isDirectory()) {
				galleryFolder.mkdirs();
			}
			File galleryFile = new File(galleryFolder, post.getImageName());
			out = new FileOutputStream(galleryFile);
			IOUtils.copy(in, out);
			IOUtils.closeQuietly(out);
			
			addImageToGallery(galleryFile);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}

	private void addImageToGallery(File image) {
	    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	    Uri contentUri = Uri.fromFile(image);
	    mediaScanIntent.setData(contentUri);
	    this.sendBroadcast(mediaScanIntent);
	}
	
	private void notifyDownloadFinished(ChanThread thread) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        boolean hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, true);
        boolean useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        if (thread != null) {
            thread.hideAllText = hideAllText;
            thread.hidePostNumbers = hidePostNumbers;
            thread.useFriendlyIds = useFriendlyIds;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Builder notifBuilder = new Notification.Builder(getApplicationContext());
		notifBuilder.setWhen(Calendar.getInstance().getTimeInMillis());
		notifBuilder.setAutoCancel(true);
		notifBuilder.setContentTitle("Images downloaded for thread /" + board + " / " + threadNo);
		notifBuilder.setContentText(thread.getBoardThreadText());
		notifBuilder.setSmallIcon(R.drawable.four_leaf_clover_1);
		
		Intent threadActivityIntent = ThreadActivity.createIntentForActivity(getApplicationContext(),
				board, threadNo,
                thread.getThreadText(),
                thread.getThumbnailUrl(),
                thread.tn_w, thread.tn_h, thread.tim,
                false, 0);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				threadActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		notifBuilder.setContentIntent(pendingIntent);
		
		notificationManager.notify((int)thread.no, notifBuilder.getNotification());
	}
	
	private void notifyDownloadError(ChanThread thread) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        boolean hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, true);
        boolean useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        if (thread != null) {
            thread.hideAllText = hideAllText;
            thread.hidePostNumbers = hidePostNumbers;
            thread.useFriendlyIds = useFriendlyIds;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Builder notifBuilder = new Notification.Builder(getApplicationContext());
		notifBuilder.setWhen(Calendar.getInstance().getTimeInMillis());
		notifBuilder.setAutoCancel(true);
		notifBuilder.setContentTitle("Error downloading images for thread /" + board + " / " + threadNo);
		notifBuilder.setContentText(thread.getBoardThreadText());
		notifBuilder.setSmallIcon(R.drawable.four_leaf_clover_1);
		
		Intent threadActivityIntent = ThreadActivity.createIntentForActivity(getApplicationContext(),
				board, threadNo,
                thread.getThreadText(),
                thread.getThumbnailUrl(),
                thread.tn_w, thread.tn_h, thread.tim,
                false, 0);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				threadActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		notifBuilder.setContentIntent(pendingIntent);
		
		notificationManager.notify((int)thread.no, notifBuilder.getNotification());	}
	
	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(null, board, threadNo);
	}
}
