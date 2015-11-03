package com.chanapps.four.gallery;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;
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

public class ChanOffLineImage extends MediaItem implements ChanIdentifiedService {
    private static final String TAG = "ChanOffLineImage";
    public static final boolean DEBUG  = false;
    
	private ChanActivityId activityId;
	private String name;
	private String dir;
    private File imageFile;
    private String contentType;
    private String ext;

    private int width = 0;
    private int height = 0;
    private long size = 0;

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
        size = imageFile.length();
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
				return BitmapRegionDecoder.newInstance(imageFile.getAbsolutePath(), false);
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
            //Bitmap srcBmp = null;
    		try {
    			Bitmap bmp = getBitmap();
        		if (bmp != null && type == TYPE_MICROTHUMBNAIL) {
        			bmp = centerCrop(bmp);
        		}
        		return bmp;
			} catch (Throwable e) {
				Log.e(TAG, "Bitmap docode error for " + imageFile.getName(), e);
				return null;
			}
        }

		private Bitmap getBitmap() throws IOException {
			Bitmap dstBmp = null;
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

			InputStream imageStream;
			try {
			    imageStream = new BufferedInputStream(new FileInputStream(imageFile));
			}
			catch (Exception e) {
			    Log.e(TAG, "Couldn't load image file " + imageFile, e);
			    return null;
			}

			try {
				if ("gif".equals(ext)) {
					GifDecoder decoder = new GifDecoder();
					int status = decoder.read(imageStream);
					Log.w(TAG, "Status " + (status == 0 ? "OK" : status == 1 ? "FORMAT_ERROR" : "OPEN_ERROR") + " for file " + imageFile.getName());
					if (status == 0) {
						dstBmp = decoder.getBitmap();
						width = dstBmp.getWidth();
						height = dstBmp.getHeight();
					} else if (status == 1) {
						IOUtils.closeQuietly(imageStream);
						imageStream = new BufferedInputStream(new FileInputStream(imageFile));
						dstBmp = BitmapFactory.decodeStream(imageStream, null, options);
						Log.w(TAG, imageFile.getName() + (dstBmp == null ? " not" : "") + " loaded via BitmapFactor");
					}
				} else {
					// center crop
					dstBmp = BitmapFactory.decodeStream(imageStream, null, options);
				}
			}
			catch (Exception e) {
			    Log.e(TAG, "Couldn't decode bitmap file " + imageFile, e);
			    return null;
			}
			finally {
				IOUtils.closeQuietly(imageStream);
			}
			//return ensureGLCompatibleBitmap(bitmap);
			return dstBmp;
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

		private int computeImageScale(int targetWidth, int targetHeight) throws IOException {
			// decode image size
			Options options = new Options();
			options.inJustDecodeBounds = true;

            InputStream imageStream;
            try {
                imageStream = new BufferedInputStream(new FileInputStream(imageFile));
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't open image file " + imageFile, e);
                return 1;
            }
            if (imageStream == null)
                return 1;

			try {
				BitmapFactory.decodeStream(imageStream, null, options);
			} catch(Exception e) {
                Log.e(TAG, "Couldn't decode image file " + imageFile, e);
                return 1;
            }
            finally {
            	IOUtils.closeQuietly(imageStream);
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
        int supported = SUPPORT_SETAS | SUPPORT_INFO;
        if (isSharable()) supported |= SUPPORT_SHARE;
        if ("jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext)) {
            supported |= SUPPORT_FULL_IMAGE;
        }
        if (isAnimatedGif()) {
        	supported |= SUPPORT_ANIMATED_GIF;
        }
        if (DEBUG) {
        	StringBuffer buf = new StringBuffer();
        	buf.append((supported & SUPPORT_SETAS) > 0? "SUPPORT_SETAS " : "");
        	buf.append((supported & SUPPORT_SHARE) > 0? "SUPPORT_SHARE " : "");
        	buf.append((supported & SUPPORT_FULL_IMAGE) > 0? "SUPPORT_FULL_IMAGE " : "");
        	buf.append((supported & SUPPORT_ANIMATED_GIF) > 0? "SUPPORT_ANIMATED_GIF " : "");
        	Log.i(TAG, "Supported operations for " + this.name + " " + buf.toString());
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
    
    private boolean isAnimatedGif() {
    	if ("gif".equals(ext)) {
    		if (width > 0 && height > 0) {
    			return size > width * height * 8 / 10;
    		} else {
    			return size > 128000;
    		}
    	}
    	return false;
    }
    
    @Override
	public Uri getPlayUri() {
    	return Uri.fromFile(imageFile);
	}

	@Override
    public Uri getContentUri() {
        return getPlayUri();
    }
	
	public void updateImageBounds(File imageFile) {
		// decode image size
		Options options = new Options();
		options.inJustDecodeBounds = true;
		InputStream imageStream = null;
		try {
			imageStream = new BufferedInputStream(new FileInputStream(imageFile));
			BitmapFactory.decodeStream(imageStream, null, options);
			width = options.outWidth;
			height = options.outHeight;
		} catch (Exception e) {
			Log.e(TAG, "Error while decoding image bounds", e);
		} finally {
			IOUtils.closeQuietly(imageStream);
		}
	}

    @Override
    public MediaDetails getDetails() {
    	updateImageBounds(imageFile);
        MediaDetails details = super.getDetails();
        if (width != 0 && height != 0) {
            details.addDetail(MediaDetails.INDEX_WIDTH, width);
            details.addDetail(MediaDetails.INDEX_HEIGHT, height);
        }
        details.addDetail(MediaDetails.INDEX_PATH, this.imageFile.getAbsoluteFile());
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
