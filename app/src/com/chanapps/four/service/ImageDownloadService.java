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

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.activity.FullScreenImageActivity;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 */
public class ImageDownloadService extends BaseChanService implements ChanIdentifiedService {
	private static final String TAG = ImageDownloadService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int MIN_DOWNLOAD_PROGRESS_UPDATE = 300;
	private static final int IMAGE_BUFFER_SIZE = 20480;

    public static void startService(Context context, String board, long threadNo, long postNo, String url, String targetFile) {
        if (DEBUG) Log.i(TAG, "Start image download service for " + url);
        Intent intent = new Intent(context, ImageDownloadService.class);
        intent.putExtra(ChanHelper.BOARD_CODE, board);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.POST_NO, postNo);
        intent.putExtra(ChanHelper.IMAGE_URL, url);
        intent.putExtra(ChanHelper.IMAGE_PATH, targetFile);
        context.startService(intent);
    }

    public static void cancelService(Context context, String url) {
        if (DEBUG) Log.i(TAG, "Cancelling image download service for " + url);
        Intent intent = new Intent(context, ImageDownloadService.class);
        intent.putExtra(ChanHelper.CLEAR_FETCH_QUEUE, 1);
        intent.putExtra(ChanHelper.IMAGE_URL, url);
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
			board = intent.getStringExtra(ChanHelper.BOARD_CODE);
			threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
			postNo = intent.getLongExtra(ChanHelper.POST_NO, 0);
			imageUrl = intent.getStringExtra(ChanHelper.IMAGE_URL);
			targetImagePath = intent.getStringExtra(ChanHelper.IMAGE_PATH);
			if (DEBUG) Log.i(TAG, "Handling image download service for " + imageUrl);
			
			File targetFile = new File(URI.create(targetImagePath));
			if (targetFile.exists()) {
				targetFile.delete();
			}
			
			conn = (HttpURLConnection)new URL(imageUrl).openConnection();
			FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
			conn.setReadTimeout(fetchParams.readTimeout);
			//conn.setConnectTimeout(fetchParams.connectTimeout);
			
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
		if (activity != null && activity.getChanActivityId().activity == LastActivity.FULL_SCREEN_IMAGE_ACTIVITY) {
			Handler handler = activity.getChanHandler();
			if (handler != null) {
				Message msg = handler.obtainMessage(FullScreenImageActivity.PROGRESS_REFRESH_MSG, fileLength, 0);
				handler.sendMessage(msg);
			}
		}
	}
	
	private void notifyDownloadFinished(int fileLength) {
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null && activity.getChanActivityId().activity == LastActivity.FULL_SCREEN_IMAGE_ACTIVITY) {
			Handler handler = activity.getChanHandler();
			if (handler != null) {
				Message msg = handler.obtainMessage(FullScreenImageActivity.FINISHED_DOWNLOAD_MSG, fileLength, 0);
				handler.sendMessage(msg);
			}
		}
	}
	
	private void notifyDownloadError() {
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null && activity.getChanActivityId().activity == LastActivity.FULL_SCREEN_IMAGE_ACTIVITY) {
			Handler handler = activity.getChanHandler();
			if (handler != null) {
				Message msg = handler.obtainMessage(FullScreenImageActivity.DOWNLOAD_ERROR_MSG);
				handler.sendMessage(msg);
			}
		}
	}
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getIntExtra(ChanHelper.CLEAR_FETCH_QUEUE, 0) == 1) {
        	Log.i(TAG, "Clearing chan fetch service message queue");
        	mServiceHandler.removeMessages(PRIORITY_MESSAGE);
        	synchronized(this) {
        		priorityMessageCounter = 0;
        	}
        	if (imageUrl != null && imageUrl.equals(intent.getStringExtra(ChanHelper.IMAGE_URL))) {
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
