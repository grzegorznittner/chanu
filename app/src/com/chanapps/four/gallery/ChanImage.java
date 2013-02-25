package com.chanapps.four.gallery;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.GalleryViewActivity;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.NetworkProfileManager;
import com.nostra13.universalimageloader.core.ImageDecoder;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.download.URLConnectionImageDownloader;
import com.nostra13.universalimageloader.utils.FileUtils;

public class ChanImage extends MediaItem {
    private static final String TAG = "ChanImage";
    public static final boolean DEBUG  = false;
    
    private static final int MIN_DOWNLOAD_PROGRESS_UPDATE = 300;
	private static final int IMAGE_BUFFER_SIZE = 20480;

    private final String url;
    private final String thumbUrl;
    private final String localImagePath;
    private final String contentType;
    private final int tn_h, tn_w;
    private final int fsize;
    private final String ext;

    private int width;
    private int height;

    private GalleryApp mApplication;

    public ChanImage(GalleryApp application, Path path, ChanPost post) {
        super(path, nextVersionNumber());
        mApplication = application;
        url = post.getImageUrl();
        thumbUrl = post.getThumbnailUrl();
        tn_h = post.tn_h;
        tn_w = post.tn_w;
        width = post.w;
        height = post.h;
        fsize = post.fsize;
        ext = post.ext;
        localImagePath = ChanFileStorage.getBoardCacheDirectory(mApplication.getAndroidContext(), post.board) + "/" + post.getImageName();
        mApplication = Utils.checkNotNull(application);
        String extNoDot = post.ext != null && post.ext.startsWith(".") ? post.ext.substring(1) : post.ext;
        contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extNoDot);
    }

    @Override
    public Job<Bitmap> requestImage(int type) {
    	if (DEBUG) Log.i(TAG, "requestImage " + thumbUrl + " " + (type == TYPE_THUMBNAIL ? "TYPE_THUMBNAIL" : "TYPE_MICROTHUMBNAIL"));
        return new BitmapJob(type);
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage() {
    	if (DEBUG) Log.i(TAG, "requestLargeImage " + this.url);
        return new RegionDecoderJob();
    }

    private class RegionDecoderJob implements Job<BitmapRegionDecoder> {
        public BitmapRegionDecoder run(JobContext jc) {
        	if (!new File(localImagePath).exists()) {
        		downloadFullImage();
        	}

        	if (new File(localImagePath).exists()) {
	        	if (DEBUG) Log.i(TAG, "Large image exists " + localImagePath);
	    		try {
					return BitmapRegionDecoder.newInstance(localImagePath, true);
				} catch (IOException e) {
					Log.e(TAG, "BitmapRegionDecoder error for " + localImagePath, e);
				}
        	}
            return null;
        }
    }
    
	protected void downloadFullImage() {
        long startTime = Calendar.getInstance().getTimeInMillis();
        InputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
		try {
			if (DEBUG) Log.i(TAG, "Handling image download service for " + url);
			
			File targetFile = new File(localImagePath);
			
			conn = (HttpURLConnection)new URL(url).openConnection();
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
			}
			
			long endTime = Calendar.getInstance().getTimeInMillis();
			//NetworkProfileManager.instance().finishedImageDownload(this, (int)(endTime - startTime), fileLength);
            if (DEBUG) Log.i(TAG, "Stored image " + url + " to file "
            		+ targetFile.getAbsolutePath() + " in " + (endTime - startTime) + "ms.");
            
		    //notifyDownloadFinished(fileLength);
		} catch (Exception e) {
            Log.e(TAG, "Error in image download service", e);
            //NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
            //notifyDownloadError();
		} finally {
			notifyDownloadProgress(fsize);
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			closeConnection(conn);
		}
	}
	
	private void notifyDownloadProgress(int fileLength) {
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null && activity.getChanActivityId().activity == LastActivity.FULL_SCREEN_IMAGE_ACTIVITY) {
			Handler handler = activity.getChanHandler();
			if (handler != null) {
				Message msg = handler.obtainMessage(GalleryViewActivity.PROGRESS_REFRESH_MSG, fileLength, 0);
				handler.sendMessage(msg);
			}
		}
	}

	protected void closeConnection(HttpURLConnection tc) {
		if (tc != null) {
			try {
		        tc.disconnect();
			} catch (Exception e) {
				Log.e(TAG, "Error closing connection", e);
			}
		}
	}

    private class BitmapJob implements Job<Bitmap> {
        private int type;

        protected BitmapJob(int type) {
            this.type = type;
        }

        public Bitmap run(JobContext jc) {
        	Bitmap bitmap = null;
            if (type == TYPE_THUMBNAIL) {
            	downloadFullImage();
            	if (new File(localImagePath).exists()) {
    	        	if (DEBUG) Log.i(TAG, "Large image exists but used as thumbnail " + localImagePath);
//    	        	if (".gif".equals(ext)) {
//    	        		bitmap = decodeGif(localImagePath);
//    	        	} else {
	    	    		try {
	    					bitmap = BitmapFactory.decodeFile(localImagePath);
	    				} catch (Throwable e) {
	    					Log.e(TAG, "Bitmap docode error for " + localImagePath, e);
	    					bitmap = downloadThumbnail();
	    				}
//    	        	}
            	}
            } else {
            	bitmap = downloadThumbnail();
            }
            return ensureGLCompatibleBitmap(bitmap);
        }

		private Bitmap decodeGif(String filePath) {
			GifDecoder gifDecoder = new GifDecoder();
			int result;
			try {
				result = gifDecoder.read(new FileInputStream(filePath));
				if (result == 0) {
					return gifDecoder.getBitmap();
				} else {
					if (DEBUG) Log.w(TAG, "GifDecoder returned " + result);
				}
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Error loading GIF", e);
			}
			return null;
		}

		private Bitmap downloadThumbnail() {
			File thumbFile = ImageLoader.getInstance().getDiscCache().get(thumbUrl);
            Bitmap bitmap = null;
            try {
	            if (!thumbFile.exists()) {
	            	saveImageOnDisc(thumbFile);
	            }
//	            if (".gif".equals(ext)) {
//	        		bitmap = decodeGif(thumbFile.getAbsolutePath());
//	        	} else {
	        		bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), null);
//	        	}
            } catch (Exception e) {
        		Log.e(TAG, "Error loading/transforming thumbnail", e);
        		thumbFile.delete();
        	}
			return bitmap;
		}
		
		public Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
	        if (bitmap == null || bitmap.getConfig() != null)
	        	return bitmap;
	        Bitmap newBitmap = bitmap.copy(Config.ARGB_8888, false);
	        bitmap.recycle();
	        return newBitmap;
	    }
        
    	private void saveImageOnDisc(File targetFile) throws URISyntaxException, IOException {
			// Download, decode, compress and save image
			ImageSize targetImageSize = new ImageSize(tn_w, tn_h);
			FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
			URLConnectionImageDownloader downloader = new URLConnectionImageDownloader(fetchParams.connectTimeout, fetchParams.readTimeout);
			ImageDecoder decoder = new ImageDecoder(new URI(thumbUrl), downloader);
			Bitmap bmp = decoder.decode(targetImageSize, ImageScaleType.EXACT);

			OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile));
			boolean compressedSuccessfully = bmp.compress(Bitmap.CompressFormat.JPEG, 80, os);
			if (compressedSuccessfully) {
				bmp.recycle();
				return;
			}

    		// If previous compression wasn't needed or failed
    		// Download and save original image
    		InputStream is = downloader.getStream(new URI(thumbUrl));
    		try {
    			OutputStream os2 = new BufferedOutputStream(new FileOutputStream(targetFile));
    			try {
    				FileUtils.copyStream(is, os2);
    			} finally {
    				os2.close();
    			}
    		} finally {
    			is.close();
    		}
    	}
    }

    @Override
    public int getSupportedOperations() {
        int supported = SUPPORT_SETAS;
        if (isSharable()) supported |= SUPPORT_SHARE;
        if (".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext)) {
            supported |= SUPPORT_FULL_IMAGE;
        }
//        if (".gif".equals(ext)) {
//        	supported |= SUPPORT_PLAY;
//        }
        return supported;
    }

    private boolean isSharable() {
    	return true;
    }

    @Override
    public int getMediaType() {
    	if (".gif".equals(ext)) {
    		return MEDIA_TYPE_VIDEO;
    	} else {
    		return MEDIA_TYPE_IMAGE;
    	}
    }
    
    @Override
	public Uri getPlayUri() {
    	File localFile = new File (localImagePath);
    	if (localFile.exists()) {
    		return Uri.fromFile(localFile);
    	} else {
    		return Uri.parse(url);
    	}
	}

	@Override
    public Uri getContentUri() {
        return getPlayUri();
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        if (width != 0 && height != 0) {
            details.addDetail(MediaDetails.INDEX_WIDTH, width);
            details.addDetail(MediaDetails.INDEX_HEIGHT, height);
        }
        details.addDetail(MediaDetails.INDEX_MIMETYPE, contentType);
        return details;
    }

    @Override
    public String getMimeType() {
        return contentType;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
        	mApplication = null;
        } finally {
            super.finalize();
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
