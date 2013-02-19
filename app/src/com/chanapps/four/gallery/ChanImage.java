package com.chanapps.four.gallery;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.ThreadPool.CancelListener;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.chanapps.four.data.ChanFileStorage;
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

    private static final int STATE_INIT = 0;
    private static final int STATE_DOWNLOADING = 1;
    private static final int STATE_DOWNLOADED = 2;
    private static final int STATE_ERROR = -1;

    private final String url;
    private final String thumbUrl;
    private final String localImagePath;
    private final String mContentType;
    private final int tn_h, tn_w;

    private DownloadCache.Entry mCacheEntry;
    private ParcelFileDescriptor mFileDescriptor;
    private int mState = STATE_INIT;
    private int mWidth;
    private int mHeight;

    private GalleryApp mApplication;

    public ChanImage(GalleryApp application, Path path, ChanPost post) {
        super(path, nextVersionNumber());
        mApplication = application;
        url = post.getImageUrl();
        thumbUrl = post.getThumbnailUrl();
        tn_h = post.tn_h;
        tn_w = post.tn_w;
        mWidth = post.w;
        mHeight = post.h;
        localImagePath = ChanFileStorage.getBoardCacheDirectory(mApplication.getAndroidContext(), post.board) + "/" + post.getImageName();
        mApplication = Utils.checkNotNull(application);
        mContentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(post.ext);
    }

    @Override
    public Job<Bitmap> requestImage(int type) {
    	Log.i(TAG, "requestImage " + thumbUrl + " " + (type == TYPE_THUMBNAIL ? "TYPE_THUMBNAIL" : "TYPE_MICROTHUMBNAIL"));
        return new BitmapJob(type);
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage() {
    	Log.i(TAG, "requestLargeImage " + this.url);
        return new RegionDecoderJob();
    }

    private void openFileOrDownloadTempFile(JobContext jc) {
        int state = openOrDownloadInner(jc);
        synchronized (this) {
            mState = state;
            if (mState != STATE_DOWNLOADED) {
                if (mFileDescriptor != null) {
                    Utils.closeSilently(mFileDescriptor);
                    mFileDescriptor = null;
                }
            }
            notifyAll();
        }
    }

    private int openOrDownloadInner(JobContext jc) {
        try {
            URL url = new URL(this.url);
            mCacheEntry = mApplication.getDownloadCache().download(jc, url);
            if (jc.isCancelled()) return STATE_INIT;
            if (mCacheEntry == null) {
                Log.w(TAG, "download failed " + url);
                return STATE_ERROR;
            }
            mFileDescriptor = ParcelFileDescriptor.open(
                    mCacheEntry.cacheFile, ParcelFileDescriptor.MODE_READ_ONLY);
            return STATE_DOWNLOADED;
        } catch (Throwable t) {
            Log.w(TAG, "download error", t);
            return STATE_ERROR;
        }
    }

    private boolean prepareInputFile(JobContext jc) {
        jc.setCancelListener(new CancelListener() {
            public void onCancel() {
                synchronized (this) {
                    notifyAll();
                }
            }
        });

        while (true) {
            synchronized (this) {
                if (jc.isCancelled()) return false;
                if (mState == STATE_INIT) {
                    mState = STATE_DOWNLOADING;
                    // Then leave the synchronized block and continue.
                } else if (mState == STATE_ERROR) {
                    return false;
                } else if (mState == STATE_DOWNLOADED) {
                    return true;
                } else /* if (mState == STATE_DOWNLOADING) */ {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignored.
                    }
                    continue;
                }
            }
            // This is only reached for STATE_INIT->STATE_DOWNLOADING
            openFileOrDownloadTempFile(jc);
        }
    }

    private class RegionDecoderJob implements Job<BitmapRegionDecoder> {
        public BitmapRegionDecoder run(JobContext jc) {
        	/*
            if (!prepareInputFile(jc)) return null;
            BitmapRegionDecoder decoder = DecodeUtils.requestCreateBitmapRegionDecoder(
                    jc, mFileDescriptor.getFileDescriptor(), false);
            mWidth = decoder.getWidth();
            mHeight = decoder.getHeight();
            */
        	if (new File(localImagePath).exists()) {
        		Log.i(TAG, "Large image exists " + localImagePath);
        		try {
					return BitmapRegionDecoder.newInstance(localImagePath, true);
				} catch (IOException e) {
					Log.e(TAG, "BitmapRegionDecoder error for " + localImagePath, e);
				}
        	}
            return null;
        }
    }

    private class BitmapJob implements Job<Bitmap> {
        private int mType;

        protected BitmapJob(int type) {
            mType = type;
        }

        public Bitmap run(JobContext jc) {
            int targetSize = LocalImage.getTargetSize(mType);
            File thumbFile = ImageLoader.getInstance().getDiscCache().get(thumbUrl);
            Bitmap bitmap = null;
            try {
	            if (!thumbFile.exists()) {
	            	saveImageOnDisc(thumbFile);
	            }
	            bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), null);
            } catch (Exception e) {
        		Log.e(TAG, "Error loading/transforming thumbnail", e);
        		thumbFile.delete();
        	}

            Log.i(TAG, "Bitmap for " + thumbUrl + " " + (bitmap != null ? (bitmap.getWidth() + "x" + bitmap.getHeight()) : "null"));
            if (bitmap != null) {
	            if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
	                bitmap = BitmapUtils.resizeDownAndCropCenter(bitmap,
	                        targetSize, true);
	            } else {
	                bitmap = BitmapUtils.resizeDownBySideLength(bitmap,
	                        targetSize, true);
	            }
            }
            return bitmap;
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
        int supported = SUPPORT_EDIT | SUPPORT_SETAS | SUPPORT_FULL_IMAGE;
        if (isSharable()) supported |= SUPPORT_SHARE;
        if (BitmapUtils.isSupportedByRegionDecoder(mContentType)) {
            supported |= SUPPORT_FULL_IMAGE;
        }
        return supported;
    }

    private boolean isSharable() {
    	return true;
    }

    @Override
    public int getMediaType() {
        return MEDIA_TYPE_IMAGE;
    }

    @Override
    public Uri getContentUri() {
        return Uri.parse(this.url);
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        if (mWidth != 0 && mHeight != 0) {
            details.addDetail(MediaDetails.INDEX_WIDTH, mWidth);
            details.addDetail(MediaDetails.INDEX_HEIGHT, mHeight);
        }
        details.addDetail(MediaDetails.INDEX_MIMETYPE, mContentType);
        return details;
    }

    @Override
    public String getMimeType() {
        return mContentType;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mFileDescriptor != null) {
                Utils.closeSilently(mFileDescriptor);
            }
        } finally {
            super.finalize();
        }
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }
}
