package com.chanapps.four.gallery;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView.ScaleType;

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
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;

public class ChanOffLineImage extends MediaItem implements ChanIdentifiedService {
    private static final String TAG = "ChanOffLineImage";
    public static final boolean DEBUG  = true;
    
	private ChanActivityId activityId;
	private String name;
	private String dir;
    private File imageFile;
    private String contentType;
    private String ext;

    private int width;
    private int height;

    private GalleryApp mApplication;

    public ChanOffLineImage(GalleryApp application, Path path, String dir, String image) {
        super(path, nextVersionNumber());
        Context context = application.getAndroidContext();
        File cacheFolder = ChanFileStorage.getCacheDirectory(context);
        init(application, dir, new File(cacheFolder, dir + "/" + image));
    }
    
    public ChanOffLineImage(GalleryApp application, Path path, String dir, File imageFile) {
        super(path, nextVersionNumber());
        init(application, dir, imageFile);
    }

	private void init(GalleryApp application, String dir, File imageFile) {
		mApplication = application;
        activityId = new ChanActivityId(dir, 0, false);
        this.dir = dir;
        this.imageFile = imageFile;
        if (!this.imageFile.exists()) {
        	Log.e(TAG, "Initialized with not existing image! " + imageFile.getAbsolutePath(), new Exception());
        }
        String tmpExt = FilenameUtils.getExtension(imageFile.getName());
        ext = tmpExt == null || tmpExt.isEmpty() ? "jpg" : tmpExt;
        name = "Cached /" + dir + "/" + imageFile.getName();
        mApplication = Utils.checkNotNull(application);
        contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
	}

    @Override
    public Job<Bitmap> requestImage(int type) {
    	if (DEBUG) Log.i(TAG, "requestImage " + imageFile.getName() + " " + (type == TYPE_THUMBNAIL ? "TYPE_THUMBNAIL" : "TYPE_MICROTHUMBNAIL"));
        return new BitmapJob(type);
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage() {
    	if (DEBUG) Log.i(TAG, "requestLargeImage " + imageFile.getName());
        return new RegionDecoderJob();
    }

    private class RegionDecoderJob implements Job<BitmapRegionDecoder> {
        public BitmapRegionDecoder run(JobContext jc) {
        	if (DEBUG) Log.i(TAG, "Large image exists " + imageFile.getAbsolutePath());
    		try {
				return BitmapRegionDecoder.newInstance(imageFile.getAbsolutePath(), true);
			} catch (IOException e) {
				Log.e(TAG, "BitmapRegionDecoder error for " + imageFile.getAbsolutePath(), e);
			}
            return null;
        }
    }
    
    private class BitmapJob implements Job<Bitmap> {
    	int type;
    	
        protected BitmapJob(int type) {
        	this.type = type;
        }

        public Bitmap run(JobContext jc) {
        	Bitmap bitmap = null;
    		try {
    			Options options = new Options();
    			options.inPreferredConfig = Config.ARGB_8888;
    			switch (type) {
                case TYPE_THUMBNAIL:
        			options.inSampleSize = computeImageScale(200, 200);
        			break;
                case TYPE_MICROTHUMBNAIL:
                default:
        			options.inSampleSize = computeImageScale(100, 100);
    			}
    			    			
    			InputStream imageStream = new FileInputStream(imageFile);
    			try {
    				bitmap = BitmapFactory.decodeStream(imageStream, null, options);
    			} finally {
    				imageStream.close();
    			}
	            //return ensureGLCompatibleBitmap(bitmap);
    			return bitmap;
			} catch (Throwable e) {
				Log.e(TAG, "Bitmap docode error for " + imageFile.getName(), e);
				return null;
			}
        }

		public Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
	        if (bitmap == null || bitmap.getConfig() != null)
	        	return bitmap;
	        Bitmap newBitmap = bitmap.copy(Config.ARGB_8888, false);
	        bitmap.recycle();
	        return newBitmap;
	    }
		
		private int computeImageScale(int targetWidth, int targetHeight) throws IOException {
			// decode image size
			Options options = new Options();
			options.inJustDecodeBounds = true;
			InputStream imageStream = new FileInputStream(imageFile);
			try {
				BitmapFactory.decodeStream(imageStream, null, options);
			} finally {
				imageStream.close();
			}

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
		}
    }

    @Override
    public int getSupportedOperations() {
        int supported = SUPPORT_SETAS;
        if (isSharable()) supported |= SUPPORT_SHARE;
        if ("jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext)) {
            supported |= SUPPORT_FULL_IMAGE;
        }
        if ("gif".equals(ext)) {
        	supported |= SUPPORT_PLAY;
        }
        if (DEBUG) {
        	StringBuffer buf = new StringBuffer();
        	buf.append((supported & SUPPORT_SETAS) > 0? "SUPPORT_SETAS " : "");
        	buf.append((supported & SUPPORT_SHARE) > 0? "SUPPORT_SHARE " : "");
        	buf.append((supported & SUPPORT_FULL_IMAGE) > 0? "SUPPORT_FULL_IMAGE " : "");
        	buf.append((supported & SUPPORT_PLAY) > 0? "SUPPORT_PLAY " : "");
        	Log.i(TAG, "Supported operations for " + this.name + " " + buf.toString());
        }
        return supported;
    }

    private boolean isSharable() {
    	return true;
    }

    @Override
    public int getMediaType() {
    	if ("gif".equals(ext)) {
    		return MEDIA_TYPE_VIDEO;
    	} else {
    		return MEDIA_TYPE_IMAGE;
    	}
    }
    
    @Override
	public Uri getPlayUri() {
    	return Uri.fromFile(imageFile);
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
}
