/**
 * 
 */
package com.chanapps.four.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;

import com.chanapps.four.data.*;
import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.activity.GalleryViewActivity;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 */
public class ImageDownloadService extends BaseChanService implements ChanIdentifiedService {
	private static final String TAG = ImageDownloadService.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int MIN_DOWNLOAD_PROGRESS_UPDATE = 300;
	private static final int IMAGE_BUFFER_SIZE = 20480;
    public static final String IMAGE_PATH = "imagePath";
    public static final String IMAGE_URL = "imageUrl";

    public static void startService(Context context, String board, long threadNo, long postNo, String url, String targetFile) {
        if (DEBUG) Log.i(TAG, "Start image download service for " + url);
        Intent intent = new Intent(context, ImageDownloadService.class);
        intent.putExtra(ChanBoard.BOARD_CODE, board);
        intent.putExtra(ChanThread.THREAD_NO, threadNo);
        intent.putExtra(ChanPost.POST_NO, postNo);
        intent.putExtra(IMAGE_URL, url);
        intent.putExtra(IMAGE_PATH, targetFile);
        context.startService(intent);
    }

    public static void cancelService(Context context, String url) {
        if (DEBUG) Log.i(TAG, "Cancelling image download service for " + url);
        Intent intent = new Intent(context, ImageDownloadService.class);
        intent.putExtra(CLEAR_FETCH_QUEUE, 1);
        intent.putExtra(IMAGE_URL, url);
        context.startService(intent);
    }

    public ImageDownloadService() {
   		super("imagedownload");
   	}

    protected ImageDownloadService(String name) {
   		super(name);
   	}
    
    private String imageUrl = null;
    private String targetImagePath = null;
    private String board = null;
    private long threadNo = 0;
    private long postNo = 0;
    private boolean stopDownload = false;
	
	@Override
	protected void onHandleIntent(Intent intent) {
        long startTime = Calendar.getInstance().getTimeInMillis();
        InputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
		try {
			stopDownload = false;
			board = intent.getStringExtra(ChanBoard.BOARD_CODE);
			threadNo = intent.getLongExtra(ChanThread.THREAD_NO, 0);
			postNo = intent.getLongExtra(ChanPost.POST_NO, 0);
			imageUrl = intent.getStringExtra(IMAGE_URL);
			targetImagePath = intent.getStringExtra(IMAGE_PATH);
			if (DEBUG) Log.i(TAG, "Handling image download service for " + imageUrl);
			
			File targetFile = new File(URI.create(targetImagePath));
			if (targetFile.exists()) {
				targetFile.delete();
			}
			
			conn = (HttpURLConnection)new URL(imageUrl).openConnection();
			FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
			// we need to double read timeout as file might be large
			conn.setReadTimeout(fetchParams.readTimeout * 2);
			conn.setConnectTimeout(fetchParams.connectTimeout);
			
			in = conn.getInputStream();
			out = new FileOutputStream(targetFile);
			byte[] buffer = new byte[IMAGE_BUFFER_SIZE];
			int len = -1;
			int fileLength = 0;
			long lastNotify = startTime;
			while ((len = in.read(buffer)) != -1) {
			    out.write(buffer, 0, len);
			    fileLength += len;
			    if (Calendar.getInstance().getTimeInMillis() - lastNotify > MIN_DOWNLOAD_PROGRESS_UPDATE) {
			    	notifyDownloadProgress(fileLength);
			    	lastNotify = Calendar.getInstance().getTimeInMillis();
			    }
			    if (stopDownload || Thread.interrupted()) {
			        throw new InterruptedException("Download interrupted");
			    }
			}
			
			long endTime = Calendar.getInstance().getTimeInMillis();
			NetworkProfileManager.instance().finishedImageDownload(this, (int)(endTime - startTime), fileLength);
            if (DEBUG) Log.i(TAG, "Stored image " + imageUrl + " to file "
            		+ targetFile.getAbsolutePath() + " in " + (endTime - startTime) + "ms.");
            
		    notifyDownloadFinished(fileLength);			    	
		} catch (Exception e) {
            Log.e(TAG, "Error in image download service", e);
            NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
            notifyDownloadError();
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			closeConnection(conn);
			imageUrl = null;
		}
	}

	private void notifyDownloadProgress(int fileLength) {
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null && activity.getChanActivityId().activity == LastActivity.GALLERY_ACTIVITY) {
			Handler handler = activity.getChanHandler();
			if (handler != null) {
				Message msg = handler.obtainMessage(GalleryViewActivity.PROGRESS_REFRESH_MSG, fileLength, 0);
				handler.sendMessage(msg);
			}
		}
	}
	
	private void notifyDownloadFinished(int fileLength) {
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null && activity.getChanActivityId().activity == LastActivity.GALLERY_ACTIVITY) {
			Handler handler = activity.getChanHandler();
			if (handler != null) {
				Message msg = handler.obtainMessage(GalleryViewActivity.FINISHED_DOWNLOAD_MSG, fileLength, 0);
				handler.sendMessage(msg);
			}
		}
	}
	
	private void notifyDownloadError() {
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null && activity.getChanActivityId().activity == LastActivity.GALLERY_ACTIVITY) {
			Handler handler = activity.getChanHandler();
			if (handler != null) {
				Message msg = handler.obtainMessage(GalleryViewActivity.DOWNLOAD_ERROR_MSG);
				handler.sendMessage(msg);
			}
		}
	}
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getIntExtra(CLEAR_FETCH_QUEUE, 0) == 1) {
            if (DEBUG) Log.i(TAG, "Clearing chan fetch service message queue");
        	mServiceHandler.removeMessages(PRIORITY_MESSAGE);
        	synchronized(this) {
        		priorityMessageCounter = 0;
        	}
        	if (imageUrl != null && imageUrl.equals(intent.getStringExtra(IMAGE_URL))) {
        		stopDownload = true;
        	}
        	return START_NOT_STICKY;
        }

    	mServiceHandler.removeMessages(PRIORITY_MESSAGE);
    	synchronized(this) {
    		priorityMessageCounter = 0;
    	}

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
    	msg.what = PRIORITY_MESSAGE;
    	mServiceHandler.sendMessageAtFrontOfQueue(msg);
    	synchronized(this) {
    		priorityMessageCounter++;
    	}
        
        return START_NOT_STICKY;
    }
	
	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(null, board, threadNo, postNo);
	}
}
