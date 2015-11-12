/**
 * 
 */
package com.chanapps.four.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
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
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.gallery3d.data.Path;
import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.NotificationComponent;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.DownloadImageTargetType;
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

    private static final String TARGET_TYPE = "ThreadImageDownloadService.downloadImageTargetType";
    private static final String START_POST_NO = "ThreadImageDownloadService.startPostNo";
    private static final String RESTART_COUNTER = "ThreadImageDownloadService.restartCounter";
    private static final String SCHEDULE_TIME = "ThreadImageDownloadService.startTime";
    private static final String POST_NOS = "ThreadImageDownloadService.postNos";
    private static final String FILE_NAMES = "ThreadImageDownloadService.fileNames";
    private static final String NOTIFICATION_ID = "ThreadImageDownloadService.notificationId";
    
    private static final String NOMEDIA_FILENAME = ".nomedia";

	private static final int MAX_RESTARTS = 3;
	
	private static ArrayList<Integer> stoppedDownloads = new ArrayList<Integer>();

    /*
    public static void startDownloadToBoardFolder(Context context, String board, long threadNo) {
        //NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.PRELOAD_ALL_IMAGES);
        startDownload(context, board, threadNo, DownloadImageTargetType.TO_BOARD, 0, 0, new long[] {}, null);
    }

    public static void startDownloadViaThreadMenu(Context context, String board, long threadNo) {
        //NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.DOWNLOAD_ALL_IMAGES_TO_GALLERY);
        startDownload(context, board, threadNo, DownloadImageTargetType.TO_GALLERY, 0, 0, new long[] {}, null);
    }
    */

    public static void startDownloadViaThreadMenu(Context context, String board, long threadNo, long[] postNos) {
        startDownload(context, board, threadNo, DownloadImageTargetType.TO_GALLERY, 0, 0, postNos, null, 0);
    }
    
    public static void startDownloadViaGalleryView(Context context, Path mMediaSetPath, ArrayList<Path> ids) {
		String[] mMediaSet = mMediaSetPath.split();
		if (mMediaSet[0].equals(ChanOffLineSource.SOURCE_PREFIX)) {
            Log.e(TAG, "Download from offline source not supported");
            /*
            String board = mMediaSet[1];
			if (ids == null || ids.size() == 0) {
				if (DEBUG) Log.w(TAG, "Download images from cache " + mMediaSetPath + ", all images");
                startDownload(context, board, 0, DownloadImageTargetType.TO_GALLERY, 0, 0, null, null);
            } else {
				String[] fileNames = new String[ids.size()];
				int i = 0;
				for (Path path : ids) {
					fileNames[i++] = path.split()[2];
					if (DEBUG) Log.w(TAG, "   filename: " + fileNames[i-1] + ", path: " + ids);
				}
				if (DEBUG) Log.w(TAG, "Download images from cache " + mMediaSetPath + ", num images: " + fileNames.length);
                startDownload(context, board, 0, DownloadImageTargetType.TO_GALLERY, 0, 0, null, fileNames);
			}
			*/
		} else {
			long threadId = NumberUtils.toLong(mMediaSet[2], 0);
			if (ids == null || ids.size() == 0) {
				if (DEBUG) Log.w(TAG, "Download images from gallery " + mMediaSetPath + ", all images");
				ThreadImageDownloadService.startDownloadViaThreadMenu(context, mMediaSet[1], threadId, new long[]{});
			} else {
				long[] fileNames = new long[ids.size()];
				int i = 0;
				for (Path path : ids) {
					fileNames[i++] = NumberUtils.toLong(path.split()[3], 0);
				}
				if (DEBUG) Log.w(TAG, "Download images from gallery " + mMediaSetPath + ", num images: " + fileNames.length);
				ThreadImageDownloadService.startDownloadViaThreadMenu(context, mMediaSet[1], threadId, fileNames);
			}
		}
	}

    private static void startDownload(Context context, String board, long threadNo, DownloadImageTargetType downloadImageTargetType,
    		long startPostNo, int restartCounter, long[] postNos, String[] fileNames, int notificationId) {
        if (DEBUG) Log.i(TAG, (restartCounter > 0 ? "Restart " : "Start") 
        		+ " all image download service for thread " + board + "/" + threadNo
        		+ (startPostNo == 0 ? "" : " from post " + startPostNo) + " " + downloadImageTargetType);

        if (notificationId != 0) {
        	// notification id provided so task was restarted
        } else if (threadNo == 0) {
        	// copy cache to gallery
        	notificationId = board.hashCode() + (int)new Date().getTime();
        } else {
        	// regular image download
        	notificationId = board.hashCode() + (int)threadNo + (int)new Date().getTime();
        }
        NotificationComponent.notifyDownloadScheduled(context, notificationId, board, threadNo);
        
        Intent intent = new Intent(context, ThreadImageDownloadService.class);
        intent.putExtra(NOTIFICATION_ID, notificationId);
        intent.putExtra(ChanBoard.BOARD_CODE, board);
        intent.putExtra(ChanThread.THREAD_NO, threadNo);
        intent.putExtra(TARGET_TYPE, downloadImageTargetType.toString());
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
    private DownloadImageTargetType downloadImageTargetType = null;
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
		downloadImageTargetType = DownloadImageTargetType.valueOf(intent.getStringExtra(TARGET_TYPE));
		scheduleTime = intent.getLongExtra(SCHEDULE_TIME, 0);
		postNos = intent.getLongArrayExtra(POST_NOS);
		fileNames = intent.getStringArrayExtra(FILE_NAMES);

        if (threadNo <= 0) {
            Log.e(TAG, "non-thread image download not supported, exiting");
            return;
        }
        else if (checkIfStopped(notificationId)) {
			if (DEBUG) Log.i(TAG, "download manually stopped by user, exiting");
            return;
		}

		if (NetworkProfile.Type.NO_CONNECTION == NetworkProfileManager.instance().getCurrentProfile().getConnectionType()) {
			if (DEBUG) Log.i(TAG, "no connection, re-scheduling download and exiting");
			delayedStartDownload(false);
            return;
		}
		
		ChanThread thread = ChanFileStorage.loadThreadData(getBaseContext(), board, threadNo);
        if (thread == null) {
            Log.e(TAG, "thread could not be loaded for download, exiting");
            return;
        }

        if (restartCounter > MAX_RESTARTS) {
            Log.e(TAG, "image download restarts exceeded max, exiting");
            NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
            NotificationComponent.notifyDownloadError(this, notificationId, thread);
            return;
        }

        prepareTargetFolder();
        prepareNomedia();

        if (DEBUG) Log.i(TAG, (restartCounter > 0 ? "Restart " : "Start")
                + " handling all image download service type=" + downloadImageTargetType
                + " for nid=(" + notificationId + ") thread=/" + board + "/" + threadNo
                + ((postNos != null && postNos.length == 0) ? "" : " for posts " + Arrays.toString(postNos))
                + ((fileNames != null && fileNames.length == 0) ? "" : " for filenames " + Arrays.toString(fileNames))
                + (startPostNo == 0 ? "" : " from post " + startPostNo)
                + " destination folder=" + targetFolder
                + (restartCounter > 0 ? ", restarted " + restartCounter + " time(s)." : ""));

        try {
	        downloadImages(thread);

            if (checkIfStopped(notificationId)) {
				if (DEBUG) Log.i(TAG, "download stopped by user, exiting");
                return;
			}

			if (startPostNo != 0 || threadNo == 0) {
                boolean downloadNomedia = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.PREF_DOWNLOAD_NOMEDIA, false);
				if (downloadImageTargetType == DownloadImageTargetType.TO_GALLERY && !downloadNomedia) {
                    if (DEBUG) Log.i(TAG, "onHandleIntent starting media scanner to display images in gallery");
                    new MultipleFileMediaScanner(getApplicationContext(), notificationId, downloadImageTargetType, thread, board, threadNo, fileNames, targetFolder);
                } else if (downloadImageTargetType == DownloadImageTargetType.TO_GALLERY && !downloadNomedia) {
                    if (DEBUG) Log.i(TAG, "onHandleIntent nomedia configured, skipping media file scanner");
                } else {
					NotificationComponent.notifyDownloadFinished(getApplicationContext(), notificationId, downloadImageTargetType, thread, board, threadNo, targetFolder);
				}
			}
		} catch (Exception e) {
            Log.e(TAG, "Error in image download service", e);
            delayedStartDownload(true);
		} finally {
			stoppedDownloads.remove(Integer.valueOf(notificationId));
		}
	}

	private void delayedStartDownload(final boolean increaseCounter) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
		    @Override
		    public void run() {
		    	Log.i(TAG, "Delayed scheduling image download service");
		        startDownload(getBaseContext(), board, threadNo, downloadImageTargetType, startPostNo,
		        		restartCounter + (increaseCounter ? 1 : 0), postNos, fileNames, notificationId);
		    }
		}, 10000);
	}

    private void prepareNomedia() {
        boolean downloadNomedia = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.PREF_DOWNLOAD_NOMEDIA, false);
        if (DEBUG) Log.i(TAG, "createNomediaIfConfigured downloadNomedia=" + downloadNomedia + " targetFolder=" + targetFolder);
        File d = new File(targetFolder);
        if (DEBUG) Log.i(TAG, "createNomediaIfConfigured downloadFolder=" + d);
        if (d == null || !d.exists() || !d.isDirectory())
            return;
        File f = new File(d, NOMEDIA_FILENAME);
        try {
            if (downloadNomedia) {
                if (f.createNewFile()) {
                    if (DEBUG) Log.i(TAG, "created new nomedia file path=" + f.getAbsolutePath());
                }
                else {
                    if (DEBUG) Log.i(TAG, "nomedia file already exists path=" + f.getAbsolutePath());
                }
            }
            else if (!downloadNomedia) {
                if (!f.exists()) {
                    if (DEBUG) Log.i(TAG, "nomedia file correctly not present path=" + f.getAbsolutePath());
                }
                else if (f.delete()) {
                    if (DEBUG) Log.i(TAG, "nomedia file deleted path=" + f.getAbsolutePath());
                }
                else {
                    Log.e(TAG, "unabled to remove nomedia file path=" + f.getAbsolutePath());
                }
            }
        }
        catch (IOException e) {
            Log.e(TAG, "exception creating nomedia file in path=" + f.getAbsolutePath(), e);
        }
    }

	public static synchronized boolean checkIfStopped(int notificationId) {
		if (stoppedDownloads.contains(notificationId)) {
			if (DEBUG) Log.w(TAG, "Download stopped: (" + notificationId + ") ");
			return true;
		}
		return false;
	}

	private void downloadImages(ChanThread thread) throws IOException, MalformedURLException, FileNotFoundException, InterruptedException {
        if (thread == null)
            return;
        Set<Long> postNoSet = new HashSet<Long>(postNos.length);
        for (int i = 0; i < postNos.length; i++) {
        	if (postNos[i] != 0) {
        		postNoSet.add(postNos[i]);
        	}
        }
        int totalNumPosts = postNos.length != 0 ? postNoSet.size() : thread.posts.length;
        int totalNumImages = thread.posts != null && thread.posts.length > 0 && thread.posts[0] != null && thread.posts[0].images > 0
                ? thread.posts[0].images
                : totalNumPosts;
        if (DEBUG) Log.i(TAG, "downloadImages() numPosts:" + totalNumPosts + " numImages:" + totalNumImages);
		lastUpdateTime = NotificationComponent.notifyDownloadUpdated(getApplicationContext(), notificationId, board, threadNo,
                totalNumImages, 0, lastUpdateTime);
        boolean downloadNomedia = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.PREF_DOWNLOAD_NOMEDIA, false);

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
					/*
                    if (!downloadNomedia && fileLength > 0 && downloadImageTargetType == DownloadImageTargetType.TO_GALLERY) {
						Uri uri = ChanFileStorage.getDownloadImagePath(getBaseContext(), post);
						File imageFile = new File(URI.create(uri.toString()));
                        storeImageInGallery(imageFile, post.imageName());
					}
					*/
					lastUpdateTime = NotificationComponent.notifyDownloadUpdated(getApplicationContext(), notificationId, board, threadNo,
                            totalNumImages, index++, lastUpdateTime);
				}
				startPostNo = post.no;  // setting last fetched image no
			}
			if (checkIfStopped(notificationId)) {
				return;
			}
		}
	}

    /*
	private void downloadImagesFromCache(String board) throws IOException, MalformedURLException, FileNotFoundException, InterruptedException {
		File boardCacheFolder = ChanFileStorage.getBoardCacheDirectory(getBaseContext(), board);
		if (fileNames == null || fileNames.length == 0) {
			// store all images from cache into gallery
			fileNames = boardCacheFolder.list();
		}
		lastUpdateTime = NotificationComponent.notifyDownloadUpdated(getBaseContext(), notificationId, board, threadNo,
                fileNames.length, 0, lastUpdateTime);
		int index = 0;
		for (String fileName : fileNames) {
			if (fileName.endsWith(".gif") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
				File imageFile = new File(boardCacheFolder, fileName);
				if (imageFile.exists() && downloadImageTargetType == DownloadImageTargetType.TO_GALLERY) {
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
			lastUpdateTime = NotificationComponent.notifyDownloadUpdated(getApplicationContext(), notificationId, board, threadNo,
                    fileNames.length, ++index, lastUpdateTime);
		}
	}
    */

	private int downloadImage(ChanPost post) throws IOException, MalformedURLException, FileNotFoundException, InterruptedException {
		long startTime = Calendar.getInstance().getTimeInMillis();

        Uri uri = Uri.parse("file://" + targetFolder + "/" + post.imageName());
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
			
			in = new BufferedInputStream(conn.getInputStream());
			out = new BufferedOutputStream(new FileOutputStream(targetFile));
			int fileLength = IOUtils.copy(in, out);
			long endTime = Calendar.getInstance().getTimeInMillis();
			NetworkProfileManager.instance().finishedImageDownload(this, (int)(endTime - startTime), fileLength);
			if (DEBUG) Log.i(TAG, "Stored image " + post.imageUrl(getApplicationContext()) + " to file "
					+ targetFile.getAbsolutePath() + " in " + (endTime - startTime) + "ms.");
			return fileLength;
		} catch(Exception e) {
		    Log.e(TAG, "Failed to download image: " + post.tim);
		    return -1;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			closeConnection(conn);
		}
	}

    private void prepareTargetFolder() {
        boolean isSingleImage = (postNos != null && postNos.length > 0) || (fileNames != null && fileNames.length > 0);
        File galleryFolder = ChanFileStorage.getDownloadFolder(this, board, threadNo, isSingleImage);
        if (!galleryFolder.exists() || !galleryFolder.isDirectory()) {
            galleryFolder.mkdirs();
        }
        targetFolder = galleryFolder.getAbsolutePath();
    }

    /*
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
    */

	private void addImageToGallery(File image) {
	    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	    Uri contentUri = Uri.fromFile(image);
        if (DEBUG) Log.i(TAG, "Adding to gallery: " + contentUri);
	    mediaScanIntent.setData(contentUri);
	    this.sendBroadcast(mediaScanIntent);
	}

    @Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(null, board, threadNo);
	}
	
	private static class MultipleFileMediaScanner implements MediaScannerConnectionClient {
		private int notificationId;
		private Context context;
		private MediaScannerConnection scannerConn;
		private ChanThread thread;
		private String board = null;
	    private long threadNo = 0;
	    private String[] filenames;
	    private DownloadImageTargetType downloadImageTargetType = null;
	    private String targetFolder = null;
	    private String firstImage = null;
	    
	    private int scansScheduled = 0;

		public MultipleFileMediaScanner(Context context, int notificationId, DownloadImageTargetType downloadImageTargetType, ChanThread thread,
				String board, long threadNo, String filenames[], String targetFolder) {
			this.notificationId = notificationId;
			this.context = context;
		    this.thread = thread;
		    this.board = board;
		    this.threadNo = threadNo;
		    this.filenames = filenames;
		    this.targetFolder = targetFolder;
		    this.downloadImageTargetType = downloadImageTargetType;
		    scannerConn = new MediaScannerConnection(context, this);
		    scannerConn.connect();
		}
		
		@Override
		public void onMediaScannerConnected() {
			if (threadNo > 0) {
				for (ChanPost post : thread.posts) {
					if (post.tim != 0) {
						File image = new File(targetFolder, post.imageName());
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
					File image = new File(targetFolder, filename);
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
				if (threadNo > 0) {
					NotificationComponent.notifyDownloadFinished(context, notificationId, downloadImageTargetType, thread, board, threadNo, firstImage);
				}
				scannerConn.disconnect();
			}
		}

	}
}
