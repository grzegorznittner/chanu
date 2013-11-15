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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;

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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.gallery3d.data.Path;
import com.chanapps.four.activity.CancelDownloadActivity;
import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.activity.GalleryViewActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.activity.SettingsActivity.DownloadImages;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.gallery.ChanOffLineSource;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 */
public class ThreadImageDownloadService extends BaseChanService implements ChanIdentifiedService {
	private static final String TAG = ThreadImageDownloadService.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String TARGET_TYPE = "ThreadImageDownloadService.targetType";
    private static final String START_POST_NO = "ThreadImageDownloadService.startPostNo";
    private static final String RESTART_COUNTER = "ThreadImageDownloadService.restartCounter";
    private static final String SCHEDULE_TIME = "ThreadImageDownloadService.startTime";
    private static final String POST_NOS = "ThreadImageDownloadService.postNos";
    private static final String FILE_NAMES = "ThreadImageDownloadService.fileNames";
    private static final String NOTIFICATION_ID = "ThreadImageDownloadService.notificationId";
    
    private static final String CHANU_FOLDER = "chanu" + File.separator;
    
    private static final long NOTIFICATION_UPDATE_TIME = 3000;  // 1s

    private enum TargetType {TO_BOARD, TO_GALLERY, TO_ZIP};
    
	private static final int MAX_RESTARTS = 3;
	
	private static ArrayList<Integer> stoppedDownloads = new ArrayList<Integer>();

    public static void startDownloadToBoardFolder(Context context, String board, long threadNo) {
        //NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.PRELOAD_ALL_IMAGES);
        startDownload(context, board, threadNo, TargetType.TO_BOARD, 0, 0, new long[] {}, null);
    }
    
    public static void startDownloadToGalleryFolder(Context context, String board, long threadNo) {
        //NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.DOWNLOAD_ALL_IMAGES_TO_GALLERY);
        startDownload(context, board, threadNo, TargetType.TO_GALLERY, 0, 0, new long[] {}, null);
    }

    public static void startDownloadToGalleryFolder(Context context, String board, long threadNo, long[] postNos) {
        startDownload(context, board, threadNo, TargetType.TO_GALLERY, 0, 0, postNos, null);
    }
    
    public static void startCopyCacheToGalleryFolder(Context context, String board, String[] fileNames) {
        startDownload(context, board, 0, TargetType.TO_GALLERY, 0, 0, null, fileNames);
    }
    
    public static void startDownloadImagesFromGallery(Context context, Path mMediaSetPath, ArrayList<Path> ids) {
		String[] mMediaSet = mMediaSetPath.split();
		if (mMediaSet[0].equals(ChanOffLineSource.SOURCE_PREFIX)) {
			if (ids == null || ids.size() == 0) {
				if (DEBUG) Log.w(TAG, "Download images from gallery " + mMediaSetPath + ", all images");
				ThreadImageDownloadService.startCopyCacheToGalleryFolder(context, mMediaSet[1], null);
			} else {
				String[] fileNames = new String[ids.size()];
				int i = 0;
				for (Path path : ids) {
					fileNames[i++] = path.split()[2];
					if (DEBUG) Log.w(TAG, "   filename: " + fileNames[i-1] + ", path: " + ids);
				}
				if (DEBUG) Log.w(TAG, "Download images from gallery " + mMediaSetPath + ", num images: " + fileNames.length);
				ThreadImageDownloadService.startCopyCacheToGalleryFolder(context, mMediaSet[1], fileNames);
			}
		} else {
			long threadId = NumberUtils.toLong(mMediaSet[2], 0);
			if (ids == null || ids.size() == 0) {
				if (DEBUG) Log.w(TAG, "Download images from gallery " + mMediaSetPath + ", all images");
				ThreadImageDownloadService.startDownloadToGalleryFolder(context, mMediaSet[1], threadId);
			} else {
				long[] fileNames = new long[ids.size()];
				int i = 0;
				for (Path path : ids) {
					fileNames[i++] = NumberUtils.toLong(path.split()[3], 0);
				}
				if (DEBUG) Log.w(TAG, "Download images from gallery " + mMediaSetPath + ", num images: " + fileNames.length);
				ThreadImageDownloadService.startDownloadToGalleryFolder(context, mMediaSet[1], threadId, fileNames);
			}
		}
	}

    private static void startDownload(Context context, String board, long threadNo, TargetType targetType,
    		long startPostNo, int restartCounter, long[] postNos, String[] fileNames) {
        if (DEBUG) Log.i(TAG, (restartCounter > 0 ? "Restart " : "Start") 
        		+ " all image download service for thread " + board + "/" + threadNo
        		+ (startPostNo == 0 ? "" : " from post " + startPostNo) + " " + targetType);
        int notificationId = 0;
        if (threadNo == 0) {
        	// copy cache to gallery
        	notificationId = board.hashCode() + (int)new Date().getTime();
        } else {
        	// regular image download
        	notificationId = board.hashCode() + (int)threadNo + (int)new Date().getTime();
        }
        notifyDownloadScheduled(context, notificationId, board, threadNo);
        
        Intent intent = new Intent(context, ThreadImageDownloadService.class);
        intent.putExtra(NOTIFICATION_ID, notificationId);
        intent.putExtra(ChanBoard.BOARD_CODE, board);
        intent.putExtra(ChanThread.THREAD_NO, threadNo);
        intent.putExtra(TARGET_TYPE, targetType.toString());
        intent.putExtra(START_POST_NO, startPostNo);
        intent.putExtra(RESTART_COUNTER, restartCounter);
        intent.putExtra(SCHEDULE_TIME, Calendar.getInstance().getTimeInMillis());
        intent.putExtra(POST_NOS, postNos);
        intent.putExtra(FILE_NAMES, fileNames);
        context.startService(intent);
    }
    
    public static synchronized void cancelDownload(Context context, int notificationId) {
        if (DEBUG) Log.i(TAG, "Cancelling image download service for " + notificationId);
        stoppedDownloads.add(notificationId);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    public ThreadImageDownloadService() {
   		super("threadimagesdownload");
   	}

    protected ThreadImageDownloadService(String name) {
   		super(name);
   	}
    
    private int notificationId = 0;
    private String board = null;
    private long threadNo = 0;
    private long startPostNo = 0;
    private int restartCounter = 0;
    private TargetType targetType = null;
    private String targetFolder = null;
    private long scheduleTime = 0;
    private long[] postNos = {};
    private String[] fileNames = {};
    private long lastUpdateTime = 0;
	
	@Override
	protected void onHandleIntent(Intent intent) {
		notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);
		board = intent.getStringExtra(ChanBoard.BOARD_CODE);
		threadNo = intent.getLongExtra(ChanThread.THREAD_NO, 0);
		startPostNo = intent.getLongExtra(START_POST_NO, 0);
		restartCounter = intent.getIntExtra(RESTART_COUNTER, 0);
		targetType = TargetType.valueOf(intent.getStringExtra(TARGET_TYPE));
		scheduleTime = intent.getLongExtra(SCHEDULE_TIME, 0);
		postNos = intent.getLongArrayExtra(POST_NOS);
		fileNames = intent.getStringArrayExtra(FILE_NAMES);
		
		if (checkIfStopped(notificationId)) {
			return;
		}

		if (NetworkProfile.Type.NO_CONNECTION == NetworkProfileManager.instance().getCurrentProfile().getConnectionType()) {
			startDownload(getBaseContext(), board, threadNo, targetType, startPostNo, restartCounter, postNos, fileNames);
		}
		
		ChanThread thread = ChanFileStorage.loadThreadData(getBaseContext(), board, threadNo);
		targetFolder = determineDownloadFolder(thread);
		
		try {
			if (DEBUG) Log.i(TAG, (restartCounter > 0 ? "Restart " : "Start") 
	        		+ " handling all image download service for thread (" + notificationId + ") " + board + "/" + threadNo
	        		+ ((postNos != null && postNos.length == 0) ? "" : " for posts " + Arrays.toString(postNos))
	        		+ ((fileNames != null && fileNames.length == 0) ? "" : " for filenames " + Arrays.toString(fileNames))
	        		+ (startPostNo == 0 ? "" : " from post " + startPostNo)
	        		+ (restartCounter > 0 ? ", restarted " + restartCounter + " time(s)." : ""));

			if (threadNo != 0) {
				downloadImages(thread);
			} else {
				downloadImagesFromCache(board);
			}
			
			if (checkIfStopped(notificationId)) {
				return;
			}

			if (startPostNo != 0 || threadNo == 0) {
				if (targetType == TargetType.TO_GALLERY) {
					new MultipleFileMediaScanner(getApplicationContext(), notificationId, targetType, thread, board, threadNo, fileNames, targetFolder);
				} else {
					notifyDownloadFinished(getApplicationContext(), notificationId, targetType, thread, board, threadNo, targetFolder);
				}
			}
		} catch (Exception e) {
			if (NetworkProfile.Type.NO_CONNECTION == NetworkProfileManager.instance().getCurrentProfile().getConnectionType()) {
				startDownload(getBaseContext(), board, threadNo, targetType, startPostNo, restartCounter, postNos, fileNames);
			} else {
	            Log.e(TAG, "Error in image download service", e);
	            if (restartCounter > MAX_RESTARTS) {
	            	NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
	            	notifyDownloadError(thread);
	            } else {
	            	startDownload(getBaseContext(), board, threadNo, targetType, startPostNo, restartCounter + 1, postNos, fileNames);
	            }
			}
		} finally {
			stoppedDownloads.remove(Integer.valueOf(notificationId));
		}
	}

	private static synchronized boolean checkIfStopped(int notificationId) {
		if (stoppedDownloads.contains(notificationId)) {
			if (DEBUG) Log.w(TAG, "Download stopped: (" + notificationId + ") ");
			return true;
		}
		return false;
	}

	private void downloadImages(ChanThread thread) throws IOException, MalformedURLException, FileNotFoundException, InterruptedException {
        // be efficient
        Set<Long> postNoSet = new HashSet<Long>(postNos.length);
        for (int i = 0; i < postNos.length; i++) {
        	if (postNos[i] != 0) {
        		postNoSet.add(postNos[i]);
        	}
        }
        int totalNumImages = postNos.length != 0 ? postNoSet.size() : thread.posts.length;
		notifyDownloadUpdated(getApplicationContext(), notificationId, board, threadNo, totalNumImages, 0);

		boolean startPointFound = startPostNo == 0;
		int index = 0;
		int fileLength = 0;
		for (ChanPost post : thread.posts) {
		    if (postNos.length != 0 && !postNoSet.contains(post.no)) // only download selected posts
		        continue;
			if (startPointFound || post.no == startPostNo) {
				startPointFound = true;
				if (post.tim != 0) {
					fileLength = downloadImage(post);
					if (fileLength > 0 && targetType == TargetType.TO_GALLERY) {
						Uri uri = ChanFileStorage.getMediaVisibleLocalImageUri(getBaseContext(), post);
						File imageFile = new File(URI.create(uri.toString()));
						storeImageInGallery(imageFile, post.imageName());
					}
					notifyDownloadUpdated(getApplicationContext(), notificationId, board, threadNo, totalNumImages, index + 1);
				}
				startPostNo = post.no;  // setting last fetched image no
			}
			if (checkIfStopped(notificationId)) {
				return;
			}

			index++;
		}
	}

	private void downloadImagesFromCache(String board) throws IOException, MalformedURLException, FileNotFoundException, InterruptedException {
		File boardCacheFolder = ChanFileStorage.getBoardCacheDirectory(getBaseContext(), board);
		if (fileNames == null || fileNames.length == 0) {
			// store all images from cache into gallery
			fileNames = boardCacheFolder.list();
		}
		notifyDownloadUpdated(getApplicationContext(), notificationId, board, threadNo, fileNames.length, 0);
		int index = 0;
		for (String fileName : fileNames) {
			if (fileName.endsWith(".gif") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
				File imageFile = new File(boardCacheFolder, fileName);
				if (imageFile.exists() && targetType == TargetType.TO_GALLERY) {
					storeImageInGallery(imageFile, fileName);
					if (DEBUG) Log.w(TAG, "Image " + imageFile.getAbsolutePath() + " copied to gallery.");
				} else {
					if (DEBUG) Log.w(TAG, "Image " + imageFile.getAbsolutePath() + " will not be copied to gallery. It doesn't exist!");
				}
			} else {
				if (DEBUG) Log.w(TAG, "Image " + board + "/" + fileName + " will not be copied to gallery. It doesn't have image extension!");
			}
			if (checkIfStopped(notificationId)) {
				return;
			}
			notifyDownloadUpdated(getApplicationContext(), notificationId, board, threadNo, fileNames.length, ++index);
		}
	}

	private String determineDownloadFolder(ChanThread thread) {
		if (targetType == TargetType.TO_GALLERY) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			DownloadImages downloadType = DownloadImages.valueOf(prefs.getString(
					SettingsActivity.PREF_DOWNLOAD_IMAGES, DownloadImages.ALL_IN_ONE.toString()));

			switch(downloadType) {
			case ALL_IN_ONE:
				return "";
			case PER_BOARD:
				return "board_" + board;
			case PER_THREAD:
//				Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
//				String now = formatter.format(scheduleTime);
				if (thread != null) {
					return "board_" + thread.board + "_" + thread.no;
				} else {
					// offline mode doesn't provide thread info so download defaults to PER_BOARD
					return "board_" + board;
				}
			}
		}
		return "board_" + board;
	}

	private int downloadImage(ChanPost post) throws IOException, MalformedURLException, FileNotFoundException, InterruptedException {
		long startTime = Calendar.getInstance().getTimeInMillis();
		
		Uri uri = ChanFileStorage.getMediaVisibleLocalImageUri(getBaseContext(), post);
		File targetFile = new File(URI.create(uri.toString()));
		if (targetFile.exists()) {
			return (int)targetFile.length();
		}
		
		InputStream in = null;
		OutputStream out = null;
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)new URL(post.imageUrl(getApplicationContext())).openConnection();
			FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
			// we need to double read timeout as file might be large
			conn.setReadTimeout(fetchParams.readTimeout * 2);
			conn.setConnectTimeout(fetchParams.connectTimeout);
			
			in = conn.getInputStream();
			out = new FileOutputStream(targetFile);
			int fileLength = IOUtils.copy(in, out);
			long endTime = Calendar.getInstance().getTimeInMillis();
			NetworkProfileManager.instance().finishedImageDownload(this, (int)(endTime - startTime), fileLength);
			if (DEBUG) Log.i(TAG, "Stored image " + post.imageUrl(getApplicationContext()) + " to file "
					+ targetFile.getAbsolutePath() + " in " + (endTime - startTime) + "ms.");
			return fileLength;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			closeConnection(conn);
		}
	}
	
	private void storeImageInGallery(File imageFile, String galleryImageName) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			File galleryFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), 
					CHANU_FOLDER + targetFolder);
			if (!galleryFolder.exists() || !galleryFolder.isDirectory()) {
				galleryFolder.mkdirs();
			}
			File galleryFile = new File(galleryFolder, galleryImageName);
			if (imageFile.length() == galleryFile.length() && imageFile.lastModified() < galleryFile.lastModified()) {
				return;
			}
			in = new FileInputStream(imageFile);
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
	
	private static void notifyDownloadScheduled(Context context, int notificationId, String board, long threadNo) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;

        String titleText = context.getString(R.string.download_all_images_to_gallery_menu);
        String threadText = "/" + board + "/" + threadNo;
        String text = titleText + " " + threadText;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
		    .setContentTitle(context.getString(R.string.app_name_title))
		    .setContentText(text)
		    .setSmallIcon(R.drawable.app_icon_notification);
		
		notificationManager.notify(notificationId, notifBuilder.build());
	}

	private void notifyDownloadUpdated(Context context, int notificationId, String board, long threadNo,
			int totalNumImages, int downloadedImages) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;
        if (checkIfStopped(notificationId)) {
			return;
		}
		long now = new Date().getTime();
		if (now - lastUpdateTime < NOTIFICATION_UPDATE_TIME) {
			return;
		}
		lastUpdateTime = now;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        String titleText = totalNumImages > 1
                ? getString(R.string.download_all_images_to_gallery_menu)
                : getString(R.string.download_images_to_gallery_menu);
        String threadText = "/" + board + "/" + threadNo;
        String downloadText = downloadedImages + "/" + totalNumImages;
        String text = titleText + " " + threadText + " " + downloadText;

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.app_name_title))
                .setContentText(text)
                .setProgress(totalNumImages, downloadedImages, false)
                .setSmallIcon(R.drawable.app_icon_notification);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                CancelDownloadActivity.createIntent(context, notificationId, board, threadNo),
        		Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_UPDATE_CURRENT);
        notifBuilder.setContentIntent(pendingIntent);

		notificationManager.notify(notificationId, notifBuilder.build());
	}

	private static void notifyDownloadFinished(Context context, int notificationId, TargetType targetType,
			ChanThread thread, String board, long threadNo, String targetFile) {
		if (checkIfStopped(notificationId)) {
			return;
		}
        if (DEBUG) Log.i(TAG, "notifyDownloadFinished " + targetType + " " + board + "/" + threadNo
				+ " " + thread.posts.length + " posts, file " + targetFile);
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;

        boolean useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        if (thread != null)
            thread.useFriendlyIds = useFriendlyIds;

		Intent threadActivityIntent = null;
		switch(targetType) {
		case TO_BOARD:
		case TO_ZIP:
			threadActivityIntent = ThreadActivity.createIntent(context, board, threadNo, "");
			break;
		case TO_GALLERY:
            threadActivityIntent = GalleryViewActivity.getAlbumViewIntent(context, board, threadNo);
			break;
		}

        if (targetType != TargetType.TO_BOARD) { // notify except on board auto-download
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            Builder notifBuilder = new Notification.Builder(context);
            notifBuilder.setSmallIcon(R.drawable.app_icon_notification);
            notifBuilder.setWhen(Calendar.getInstance().getTimeInMillis());
            notifBuilder.setAutoCancel(true);
            notifBuilder.setContentTitle(context.getString(R.string.download_all_images_complete));
            notifBuilder.setContentText
                    (String.format(context.getString(R.string.download_all_images_complete_detail), board, threadNo));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    threadActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_UPDATE_CURRENT);
            notifBuilder.setContentIntent(pendingIntent);
            notificationManager.notify(notificationId, notifBuilder.getNotification());
        }
	}
	
	private void notifyDownloadError(ChanThread thread) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!prefs.getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true))
            return;

        boolean useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        if (thread != null)
            thread.useFriendlyIds = useFriendlyIds;

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Builder notifBuilder = new Notification.Builder(getApplicationContext());
		notifBuilder.setWhen(Calendar.getInstance().getTimeInMillis());
		notifBuilder.setAutoCancel(true);
		notifBuilder.setContentTitle(getString(R.string.thread_image_download_error));
		notifBuilder.setContentText(board + "/" + threadNo);
		notifBuilder.setSmallIcon(R.drawable.app_icon_notification);
		
		Intent threadActivityIntent = ThreadActivity.createIntent(getApplicationContext(), board, threadNo, "");
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
				threadActivityIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		notifBuilder.setContentIntent(pendingIntent);
		
		notificationManager.notify(notificationId, notifBuilder.getNotification());	}
	
	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(null, board, threadNo);
	}
	
	public static class MultipleFileMediaScanner implements MediaScannerConnectionClient {
		private int notificationId;
		private Context context;
		private MediaScannerConnection scannerConn;
		private ChanThread thread;
		private String board = null;
	    private long threadNo = 0;
	    private String[] filenames;
	    private TargetType targetType = null;
	    private String targetFolder = null;
	    private String firstImage = null;
	    
	    private int scansScheduled = 0;

		public MultipleFileMediaScanner(Context context, int notificationId, TargetType targetType, ChanThread thread,
				String board, long threadNo, String filenames[], String targetFolder) {
			this.notificationId = notificationId;
			this.context = context;
		    this.thread = thread;
		    this.board = board;
		    this.threadNo = threadNo;
		    this.filenames = filenames;
		    this.targetFolder = targetFolder;
		    this.targetType = targetType;
		    scannerConn = new MediaScannerConnection(context, this);
		    scannerConn.connect();
		}
		
		@Override
		public void onMediaScannerConnected() {
			File galleryFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), 
					CHANU_FOLDER + targetFolder);
			if (threadNo > 0) {
				for (ChanPost post : thread.posts) {
					if (post.tim != 0) {
						File image = new File(galleryFolder, post.imageName());
						if (image.exists()) {
							scansScheduled++;
	                        if (DEBUG) Log.w(TAG, "Schedulling scan: " + image.getAbsolutePath() + " counter=" + scansScheduled);
							scannerConn.scanFile(image.getAbsolutePath(), null);
							if (firstImage == null) {
								firstImage = image.getAbsolutePath();
							}
						}
					}
				}
			} else {
				for (String filename : filenames) {
					File image = new File(galleryFolder, filename);
					if (image.exists()) {
						scansScheduled++;
                        if (DEBUG) Log.w(TAG, "Schedulling scan: " + image.getAbsolutePath() + " counter=" + scansScheduled);
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
						CHANU_FOLDER + targetFolder);
				if (threadNo > 0) {
					notifyDownloadFinished(context, notificationId, targetType, thread, board, threadNo, firstImage);
				}
				scannerConn.disconnect();
			}
		}

	}
}
