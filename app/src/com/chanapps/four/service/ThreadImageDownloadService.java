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
import java.util.*;

import com.chanapps.four.activity.*;
import org.apache.commons.io.IOUtils;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 */
public class ThreadImageDownloadService extends BaseChanService implements ChanIdentifiedService {
	private static final String TAG = ThreadImageDownloadService.class.getSimpleName();
    private static final boolean DEBUG = true;
    
    private static final String TARGET_TYPE = "ThreadImageDownloadService.targetType";
    private static final String START_POST_NO = "ThreadImageDownloadService.startPostNo";
    private static final String RESTART_COUNTER = "ThreadImageDownloadService.restartCounter";
    private static final String TARGET_FOLDER = "ThreadImageDownloadService.folder";
    private static final String SCHEDULE_TIME = "ThreadImageDownloadService.startTime";
    private static final String POST_NOS = "ThreadImageDownloadService.postNos";

    private enum TargetType {TO_BOARD, TO_GALLERY, TO_ZIP};
    
	private static final int MAX_RESTARTS = 3;

    public static void startDownloadToBoardFolder(Context context, String board, long threadNo) {
        startDownload(context, board, threadNo, TargetType.TO_BOARD, 0, 0, null, new long[] {});
    }
    
    public static void startDownloadToGalleryFolder(Context context, String board, long threadNo, String galleryFolder) {
        startDownload(context, board, threadNo, TargetType.TO_GALLERY, 0, 0, galleryFolder, new long[] {});
    }

    public static void startDownloadToGalleryFolder(Context context, String board, long threadNo, String galleryFolder, long[] postNos) {
        startDownload(context, board, threadNo, TargetType.TO_GALLERY, 0, 0, galleryFolder, postNos);
    }
    private static void startDownload(Context context, String board, long threadNo, TargetType targetType,
    		long startPostNo, int restartCounter, String folder, long[] postNos) {
        if (DEBUG) Log.i(TAG, (restartCounter > 0 ? "Restart " : "Start") 
        		+ " all image download service for thread " + board + "/" + threadNo
        		+ (startPostNo == 0 ? "" : " from post " + startPostNo) + " " + targetType);
        Intent intent = new Intent(context, ThreadImageDownloadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, board);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(TARGET_TYPE, targetType.toString());
        intent.putExtra(START_POST_NO, startPostNo);
        intent.putExtra(RESTART_COUNTER, restartCounter);
        intent.putExtra(TARGET_FOLDER, folder);
        intent.putExtra(SCHEDULE_TIME, Calendar.getInstance().getTimeInMillis());
        intent.putExtra(POST_NOS, postNos);
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
    private long scheduleTime = 0;
    private long[] postNos = {};
	
	@Override
	protected void onHandleIntent(Intent intent) {
		board = intent.getStringExtra(ChanHelper.BOARD_CODE);
		threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
		startPostNo = intent.getLongExtra(START_POST_NO, 0);
		restartCounter = intent.getIntExtra(RESTART_COUNTER, 0);
		targetType = TargetType.valueOf(intent.getStringExtra(TARGET_TYPE));
		targetFolder = intent.getStringExtra(TARGET_FOLDER);
		scheduleTime = intent.getLongExtra(SCHEDULE_TIME, 0);
		postNos = intent.getLongArrayExtra(POST_NOS);

		if (NetworkProfile.Type.NO_CONNECTION == NetworkProfileManager.instance().getCurrentProfile().getConnectionType()) {
			startDownload(getBaseContext(), board, threadNo, targetType, startPostNo, restartCounter, targetFolder, postNos);
		}
		
		ChanThread thread = ChanFileStorage.loadThreadData(getBaseContext(), board, threadNo);
		if (targetType == TargetType.TO_GALLERY && targetFolder == null) {
			Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			String now = formatter.format(scheduleTime);
            String galleryPrefix = getString(R.string.app_name);
			targetFolder = galleryPrefix + "_" + thread.board + "_" + now;
		}
		
		try {			
			if (DEBUG) Log.i(TAG, (restartCounter > 0 ? "Restart " : "Start") 
	        		+ " handling all image download service for thread " + board + "/" + threadNo
	        		+ (postNos.length == 0 ? "" : " for posts " + Arrays.toString(postNos))
	        		+ (startPostNo == 0 ? "" : " from post " + startPostNo)
	        		+ (restartCounter > 0 ? ", restarted " + restartCounter + " time(s)." : ""));

            // be efficient
            Set<Long> postNoSet = new HashSet<Long>(postNos.length);
            for (int i = 0; i < postNos.length; i++)
                postNoSet.add(postNos[i]);

			boolean startPointFound = startPostNo == 0;
			int fileLength = 0;
			for (ChanPost post : thread.posts) {
                if (postNos.length != 0 && !postNoSet.contains(post.no)) // only download selected posts
                    continue;
				if (startPointFound || post.no == startPostNo) {
					startPointFound = true;
					if (post.tim != 0) {
						fileLength = downloadImage(post);
						if (fileLength > 0 && targetType == TargetType.TO_GALLERY) {
							storeImageInGallery(post);
						}
					}
					startPostNo = post.no;  // setting last fetched image no
				}
			}
			if (startPostNo != 0) {
				if (targetType == TargetType.TO_GALLERY) {
					new MultipleFileMediaScanner(getApplicationContext(), targetType, thread, board, threadNo, targetFolder);
				} else {
					notifyDownloadFinished(getApplicationContext(), targetType, thread, board, threadNo, targetFolder);
				}
			} else {
                if (DEBUG) Log.w(TAG, "No images to download for thread " + board + "/" + threadNo);
			}
		} catch (Exception e) {
			if (NetworkProfile.Type.NO_CONNECTION == NetworkProfileManager.instance().getCurrentProfile().getConnectionType()) {
				startDownload(getBaseContext(), board, threadNo, targetType, startPostNo, restartCounter, targetFolder, postNos);
			} else {
	            Log.e(TAG, "Error in image download service", e);
	            if (restartCounter > MAX_RESTARTS) {
	            	NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
	            	notifyDownloadError(thread);
	            } else {
	            	startDownload(getBaseContext(), board, threadNo, targetType, startPostNo, restartCounter + 1, targetFolder, postNos);
	            }
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
			// we need to double read timeout as file might be large
			conn.setReadTimeout(fetchParams.readTimeout * 2);
			conn.setConnectTimeout(fetchParams.connectTimeout);
			
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
			String imageFilePath = ChanFileStorage.getLocalImageUrl(getBaseContext(), post);
			File imageFile = new File(URI.create(imageFilePath));
			
			in = new FileInputStream(imageFile);
			File galleryFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), 
			    targetFolder);
			if (!galleryFolder.exists() || !galleryFolder.isDirectory()) {
				galleryFolder.mkdirs();
			}
			File galleryFile = new File(galleryFolder, post.getImageName());
			out = new FileOutputStream(galleryFile);
			IOUtils.copy(in, out);
			IOUtils.closeQuietly(out);
			
			//addImageToGallery(galleryFile);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}

	private void addImageToGallery(File image) {
	    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	    Uri contentUri = Uri.fromFile(image);
        if (DEBUG) Log.i(TAG, "Adding to gallery: " + contentUri);
	    mediaScanIntent.setData(contentUri);
	    this.sendBroadcast(mediaScanIntent);
	}
	
	private static void notifyDownloadFinished(Context context, TargetType targetType,
			ChanThread thread, String board, long threadNo, String targetFile) {
        if (DEBUG) Log.i(TAG, "notifyDownloadFinished " + targetType + " " + board + "/" + threadNo
				+ " " + thread.posts.length + " posts, file " + targetFile);
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, false);
        boolean useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        if (thread != null) {
            thread.hidePostNumbers = hidePostNumbers;
            thread.useFriendlyIds = useFriendlyIds;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		Builder notifBuilder = new Notification.Builder(context);
		notifBuilder.setWhen(Calendar.getInstance().getTimeInMillis());
		notifBuilder.setAutoCancel(true);
		notifBuilder.setContentTitle(context.getString(R.string.download_all_images_complete));
		notifBuilder.setContentText
                (String.format(context.getString(R.string.download_all_images_complete_detail), board, threadNo));
		notifBuilder.setSmallIcon(R.drawable.app_icon);
		
		Intent threadActivityIntent = null;
		switch(targetType) {
		case TO_BOARD:
		case TO_ZIP:
			threadActivityIntent = ThreadActivity.createIntentForActivity(context, board, threadNo);
			break;
		case TO_GALLERY:
            threadActivityIntent = GalleryViewActivity.getAlbumViewIntent(context, board, threadNo);
			/*
            Uri firstImageUri = Uri.fromFile(new File(targetFile));
			if (DEBUG) Log.i(TAG, "Trying to open file in gallery: " + firstImageUri);
			if (firstImageUri != null) {
				threadActivityIntent = new Intent(Intent.ACTION_VIEW);
				threadActivityIntent.setDataAndType(firstImageUri, "image/*");
				// android.content.ContentResolver.CURSOR_DIR_BASE_TYPE + "/image");
				threadActivityIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
				//threadActivityIntent.putExtra("slideshow", true);
			} else {
				threadActivityIntent = ThreadActivity.createIntentForActivity(context,
						board, threadNo,
		                thread.getThreadNotificationText(),
		                thread.getThumbnailUrl(),
		                thread.tn_w, thread.tn_h, thread.tim,
		                false, 0);
			}
			*/
			break;
		}
        
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				threadActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_UPDATE_CURRENT);
		notifBuilder.setContentIntent(pendingIntent);
		
		notificationManager.notify((int)thread.no, notifBuilder.getNotification());
	}
	
	private Uri getGalleryURIForFirstImage(ChanThread thread) {
		File galleryFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), 
			    targetFolder);
		for (ChanPost post : thread.posts) {
			if (post.tim != 0) {
				File image = new File(galleryFolder, post.getImageName());
				if (image.exists()) {
					return Uri.fromFile(image);
				}
			}
		}
		return null;
	}
	
	private void notifyDownloadError(ChanThread thread) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, false);
        boolean useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        if (thread != null) {
            thread.hidePostNumbers = hidePostNumbers;
            thread.useFriendlyIds = useFriendlyIds;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Builder notifBuilder = new Notification.Builder(getApplicationContext());
		notifBuilder.setWhen(Calendar.getInstance().getTimeInMillis());
		notifBuilder.setAutoCancel(true);
		notifBuilder.setContentTitle("Error downloading images for thread /" + board + " / " + threadNo);
		notifBuilder.setContentText(thread.getFullText());
		notifBuilder.setSmallIcon(R.drawable.app_icon);
		
		Intent threadActivityIntent = ThreadActivity.createIntentForActivity(getApplicationContext(), board, threadNo);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				threadActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		notifBuilder.setContentIntent(pendingIntent);
		
		notificationManager.notify((int)thread.no, notifBuilder.getNotification());	}
	
	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(null, board, threadNo);
	}
	
	public static class MultipleFileMediaScanner implements MediaScannerConnectionClient {

		private Context context;
		private MediaScannerConnection scannerConn;
		private ChanThread thread;
		private String board = null;
	    private long threadNo = 0;
	    private TargetType targetType = null;
	    private String targetFolder = null;
	    private String firstImage = null;
	    
	    private int scansScheduled = 0;

		public MultipleFileMediaScanner(Context context, TargetType targetType, ChanThread thread,
				String board, long threadNo, String targetFolder) {
			this.context = context;
		    this.thread = thread;
		    this.board = board;
		    this.threadNo = threadNo;
		    this.targetFolder = targetFolder;
		    this.targetType = targetType;
		    scannerConn = new MediaScannerConnection(context, this);
		    scannerConn.connect();
		}

		@Override
		public void onMediaScannerConnected() {
			File galleryFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), 
				    targetFolder);
			for (ChanPost post : thread.posts) {
				if (post.tim != 0) {
					File image = new File(galleryFolder, post.getImageName());
					if (image.exists()) {
						scansScheduled++;
                        if (DEBUG) Log.i(TAG, "Schedulling scan: " + image.getAbsolutePath() + " counter=" + scansScheduled);
						scannerConn.scanFile(image.getAbsolutePath(), null);
						if (firstImage == null) {
							firstImage = image.getAbsolutePath();
						}
					}
				}
			}
		}

		@Override
		public void onScanCompleted(String path, Uri uri) {
			scansScheduled--;
            if (DEBUG) Log.i(TAG, "Finished scan: " + path + " counter=" + scansScheduled);
			if (scansScheduled <= 0) {
				File galleryFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), 
					    targetFolder);
				notifyDownloadFinished(context, targetType, thread, board, threadNo, firstImage);
				scannerConn.disconnect();
			}
		}

	}
}
