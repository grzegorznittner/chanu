package com.chanapps.four.gallery;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.URLConnectionImageDownloader;

public class ChanImage extends MediaItem implements ChanIdentifiedService {
    private static final String TAG = "ChanImage";
    public static final boolean DEBUG  = true;
    
    private static final int MIN_DOWNLOAD_PROGRESS_UPDATE = 300;
	private static final int IMAGE_BUFFER_SIZE = 20480;
	
	private static final int THUMBNAIL_TARGET_SIZE = 640;
    private static final int MICROTHUMBNAIL_TARGET_SIZE = 200;

	private final ChanActivityId activityId;
	private final String name;
    private final String url;
    private final String thumbUrl;
    private final String localImagePath;
    private final String contentType;
    private final int w, h;
    private final int tn_h, tn_w;
    private final int fsize;
    private final String ext;
    private final boolean isDead;

    private int width;
    private int height;

    private GalleryApp mApplication;

    public ChanImage(GalleryApp application, Path path, ChanPost post) {
        super(path, nextVersionNumber());
        mApplication = application;
        activityId = new ChanActivityId(post.board, post.resto != 0 ? post.resto : post.no, false);
        url = post.imageUrl();
        thumbUrl = post.thumbnailUrl();
        tn_h = post.tn_h;
        tn_w = post.tn_w;
        w = post.w;
        h = post.h;
        fsize = post.fsize;
        ext = post.ext;
        isDead = post.isDead;
        name = "/" + post.board + "/" + (post.resto != 0 ? post.resto : post.no);
        localImagePath = ChanFileStorage.getBoardCacheDirectory(mApplication.getAndroidContext(), post.board) + "/" + post.imageName();
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
        	File localImageFile = new File(localImagePath);
        	if (!localImageFile.exists() && !isDead) {
        		downloadFullImage();
        	}

        	if (localImageFile.exists()) {
	        	if (DEBUG) {
	        		Log.i(TAG, "Large image exists, local: " + localImagePath + " url: " + url);
	        		FileInputStream fis = null;
	        		try {
	        			fis = new FileInputStream(localImageFile);
	        			byte buffer[] = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	        			int actualSize = fis.read(buffer, 0, 10);
	        			Log.i(TAG, "Magic code for url: " + url + " hex: " + toHexString(buffer));
	        		} catch (Exception e) {
	        			Log.e(TAG, "Error while getting magic number", e);
	        		} finally {
	        			IOUtils.closeQuietly(fis);
	        		}
	        	}
	        	
	    		try {
					return BitmapRegionDecoder.newInstance(localImagePath, false);
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
            if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.e(TAG, "downloadFullImage() no longer exists url=" + url);
                return;
            }
            FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
            // we need to double read timeout as file might be large
			conn.setReadTimeout(fetchParams.readTimeout * 2);
			conn.setConnectTimeout(fetchParams.connectTimeout);
            //conn.setRequestProperty("connection", "close"); // prevent keep-alive on big images

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
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            closeConnection(conn);

            NetworkProfileManager.instance().finishedImageDownload(this, (int)(endTime - startTime), fileLength);
            if (DEBUG) Log.i(TAG, "Stored image " + url + " to file "
            		+ targetFile.getAbsolutePath() + " in " + (endTime - startTime) + "ms.");
            
		    //notifyDownloadFinished(fileLength);
		} catch (Exception e) {
            Log.e(TAG, "Error in image download service url=" + url, e);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            closeConnection(conn);
            NetworkProfileManager.instance().failedFetchingData(this, NetworkProfile.Failure.NETWORK);
            //notifyDownloadError();
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			closeConnection(conn);
            notifyDownloadProgress(fsize);
        }
	}
	
	private void notifyDownloadProgress(int fileLength) {
		/*
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null && activity.getChanActivityId().activity == LastActivity.GALLERY_ACTIVITY) {
			Handler handler = activity.getChanHandler();
			if (handler != null) {
				Message msg = handler.obtainMessage(GalleryViewActivity.PROGRESS_REFRESH_MSG, fileLength, 0);
				handler.sendMessage(msg);
			}
		}
		*/
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
        	try {
        		Bitmap bmp = getBitmap();
        		if (bmp != null && type == TYPE_MICROTHUMBNAIL) {
        			bmp = centerCrop(bmp);
        		}
        		return bmp;
        	} catch (Throwable e) {
				Log.e(TAG, "Bitmap docode error for " + localImagePath, e);
				return null;
			}
        }

		private Bitmap getBitmap() {
			Bitmap bitmap = null;
			if (type == TYPE_THUMBNAIL && !isAnimatedGif()) {
				bitmap = downloadFullImageAsThumb();
				if (bitmap != null) {
					return bitmap;
				}
			}
			return downloadThumbnail();
		}

        private Bitmap centerCrop(Bitmap srcBmp) {
			Bitmap dstBmp = null;
			if (srcBmp.getWidth() >= srcBmp.getHeight()) {
				dstBmp = Bitmap.createBitmap(srcBmp, srcBmp.getWidth() / 2 - srcBmp.getHeight() / 2, 0,
						srcBmp.getHeight(), srcBmp.getHeight());
			} else {
				dstBmp = Bitmap.createBitmap(srcBmp, 0, srcBmp.getHeight() / 2 - srcBmp.getWidth() / 2,
						srcBmp.getWidth(), srcBmp.getWidth());
			}
			return dstBmp;
        }
        
        private Bitmap downloadThumbnail() {
			File thumbFile = ImageLoader.getInstance().getDiscCache().get(thumbUrl);
            Bitmap bitmap = null;
            try {
	            if (!thumbFile.exists()) {
	            	saveImageOnDisc(thumbFile);
	            }
	            
	            Options options = getBitmapOptions(thumbFile);
        		bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
            } catch (Exception e) {
        		Log.e(TAG, "Error loading/transforming thumbnail", e);
        		thumbFile.delete();
        	}
			return bitmap;
		}

        private Bitmap downloadFullImageAsThumb() {
            Bitmap bitmap = null;
            try {
            	File localImageFile = new File(localImagePath);
            	if (!localImageFile.exists() && !isDead) {
            		downloadFullImage();
            	}
            	Options options = getBitmapOptions(localImageFile);

            	if (".gif".equals(ext)) {            		
            		GifDecoder decoder = new GifDecoder();
            		int status = decoder.read(new FileInputStream(localImageFile));
            		Log.w(TAG, "Status " + (status == 0 ? "OK" : status == 1 ? "FORMAT_ERROR" : "OPEN_ERROR") + " for file " + localImageFile.getName());
            		if (status == 0) {
            			bitmap = decoder.getBitmap();
            		} else if (status == 1) {
            			bitmap = BitmapFactory.decodeStream(new FileInputStream(localImageFile), null, options);
            			Log.w(TAG, localImageFile.getName() + (bitmap == null ? " not" : "") + " loaded via BitmapFactor");
            		}
            	} else if (localImageFile.exists()) {
            		bitmap = BitmapFactory.decodeFile(localImageFile.getAbsolutePath(), options);
            	}
            } catch (Throwable e) {
        		Log.e(TAG, "Error loading/transforming full image", e);
        	}
			return bitmap;
		}
		
		private Options getBitmapOptions(File thumbFile) throws IOException {
			Options options = new Options();
			options.inPreferredConfig = Config.ARGB_8888;
			switch (type) {
			case TYPE_THUMBNAIL:
				options.inSampleSize = computeImageScale(thumbFile, THUMBNAIL_TARGET_SIZE, THUMBNAIL_TARGET_SIZE);
				break;
			case TYPE_MICROTHUMBNAIL:
			default:
				options.inSampleSize = computeImageScale(thumbFile, MICROTHUMBNAIL_TARGET_SIZE, MICROTHUMBNAIL_TARGET_SIZE);
			}
			return options;
		}
        
    	private void saveImageOnDisc(File targetFile) throws URISyntaxException, IOException {
			FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
			URLConnectionImageDownloader downloader = new URLConnectionImageDownloader(mApplication.getAndroidContext(), fetchParams.connectTimeout, fetchParams.readTimeout);
    		InputStream is = null;
    		OutputStream os = null;
    		try {
    			is = downloader.getStreamFromNetwork(thumbUrl, null);
    			os = new BufferedOutputStream(new FileOutputStream(targetFile));
				IOUtils.copy(is, os);
    		} finally {
				IOUtils.closeQuietly(os);
    			IOUtils.closeQuietly(is);
    		}
    	}
    }

    @Override
    public int getSupportedOperations() {
        int supported = SUPPORT_SETAS | SUPPORT_INFO;
        if (isSharable()) supported |= SUPPORT_SHARE;
        if (".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext)) {
            supported |= SUPPORT_FULL_IMAGE;
        }
        if (isAnimatedGif()) {
        	supported |= SUPPORT_PLAY;
        }
        return supported;
    }

    private boolean isSharable() {
    	return true;
    }

    @Override
    public int getMediaType() {
    	if (isAnimatedGif()) {
    		return MEDIA_TYPE_VIDEO;
    	} else {
    		return MEDIA_TYPE_IMAGE;
    	}
    }
    
    private boolean isAnimatedGif() {
    	if (".gif".equals(ext)) {
    		if (fsize > 0) {
    			return fsize > w * h * 8 / 10;
    		}
    	}
    	return false;
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
            details.addDetail(MediaDetails.INDEX_WIDTH, w);
            details.addDetail(MediaDetails.INDEX_HEIGHT, h);
        }
        details.addDetail(MediaDetails.INDEX_PATH, this.url);
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

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ChanActivityId getChanActivityId() {
		return activityId;
	}

	@Override
	public Context getApplicationContext() {
		return mApplication != null ? mApplication.getAndroidContext() : null;
	}
	
	public int computeImageScale(File imageFile, int targetWidth, int targetHeight) throws IOException {
		// decode image size
		Options options = new Options();
		options.inJustDecodeBounds = true;
		InputStream imageStream = new FileInputStream(imageFile);
		try {
			BitmapFactory.decodeStream(imageStream, null, options);
			int scale = 1;
			int imageWidth = width = options.outWidth;
			int imageHeight = height = options.outHeight;

			while (imageWidth / 2 >= targetWidth && imageHeight / 2 >= targetHeight) { // &&
				imageWidth /= 2;
				imageHeight /= 2;
				scale *= 2;
			}

			if (scale < 1) {
				scale = 1;
			}

			return scale;
		} finally {
			IOUtils.closeQuietly(imageStream);
		}
	}
	
	public void updateImageBounds(File imageFile) {
		// decode image size
		Options options = new Options();
		options.inJustDecodeBounds = true;
		InputStream imageStream = null;
		try {
			imageStream = new FileInputStream(imageFile);
			BitmapFactory.decodeStream(imageStream, null, options);
			width = options.outWidth;
			height = options.outHeight;
		} catch (Exception e) {
			Log.e(TAG, "Error while decoding image bounds", e);
		} finally {
			IOUtils.closeQuietly(imageStream);
		}
	}

	public static String toHexString(byte[] magicNumber) {
        StringBuffer buf = new StringBuffer();
        for (byte b : magicNumber) {
        	buf.append("0x").append(Integer.toHexString((0xF0 & b) >>> 4)).append(Integer.toHexString(0x0F & b)).append(" ");
        }
        return buf.toString();
	}
}
