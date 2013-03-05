package com.nostra13.universalimageloader.core;

import java.io.IOException;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.assist.FailReason;

/**
 * Presents load'n'display image task. Used to load image from resource, decode it to {@link Bitmap}, and
 * display it in {@link ImageView} through {@link DisplayBitmapTask}.
 * 
 * @author Grzegorz Nittner (grzegorz.nittner[at]gmail[dot]com)
 * @see ImageLoaderConfiguration
 * @see ImageLoadingInfo
 */
class LoadAndDisplayResourceImageTask extends LoadAndDisplayImageTask {
	
	protected static final String LOG_LOAD_IMAGE_FROM_RESOURCE = "Load image from resource [%s]";

	public LoadAndDisplayResourceImageTask(ImageLoaderConfiguration configuration, ImageLoadingInfo imageLoadingInfo, Handler handler) {
		super(configuration, imageLoadingInfo, handler);
	}

	@Override
	protected Bitmap tryLoadBitmap() {
		Bitmap bitmap = null;
		try {
			Log.w(ImageLoader.TAG, "Loading from resource: " + imageLoadingInfo.resourceId);
			// Load image from Web
			if (configuration.loggingEnabled) Log.i(ImageLoader.TAG, String.format(LOG_LOAD_IMAGE_FROM_RESOURCE, imageLoadingInfo.memoryCacheKey));

			bitmap = decodeImage(imageLoadingInfo.resourceId);
			if (bitmap == null) {
				fireImageLoadingFailedEvent(FailReason.IO_ERROR);
			}
		} catch (IOException e) {
			Log.e(ImageLoader.TAG, e.getMessage(), e);
			fireImageLoadingFailedEvent(FailReason.IO_ERROR);
		} catch (OutOfMemoryError e) {
			Log.e(ImageLoader.TAG, e.getMessage(), e);
			fireImageLoadingFailedEvent(FailReason.OUT_OF_MEMORY);
		} catch (Throwable e) {
			Log.e(ImageLoader.TAG, e.getMessage(), e);
			fireImageLoadingFailedEvent(FailReason.UNKNOWN);
		}
		return bitmap;
	}

	private Bitmap decodeImage(int resourceId) throws IOException {
		Bitmap bmp = null;

		if (configuration.handleOutOfMemory) {
			bmp = decodeWithOOMHandling(resourceId);
		} else {
			ResourceImageDecoder decoder = new ResourceImageDecoder(imageLoadingInfo.imageView.getContext(), resourceId);
			bmp = decoder.decode(imageLoadingInfo.targetSize, imageLoadingInfo.options.getImageScaleType(), imageLoadingInfo.imageView.getScaleType());
		}
		return bmp;
	}

	private Bitmap decodeWithOOMHandling(int resourceId) throws IOException {
		Bitmap result = null;
		ResourceImageDecoder decoder = new ResourceImageDecoder(imageLoadingInfo.imageView.getContext(), resourceId);
		for (int attempt = 1; attempt <= ATTEMPT_COUNT_TO_DECODE_BITMAP; attempt++) {
			try {
				result = decoder.decode(imageLoadingInfo.targetSize, imageLoadingInfo.options.getImageScaleType(), imageLoadingInfo.imageView.getScaleType());
			} catch (OutOfMemoryError e) {
				Log.e(ImageLoader.TAG, e.getMessage(), e);

				switch (attempt) {
					case 1:
						System.gc();
						break;
					case 2:
						configuration.memoryCache.clear();
						System.gc();
						break;
					case 3:
						throw e;
				}
				// Wait some time while GC is working
				SystemClock.sleep(attempt * 1000);
				continue;
			}
			break;
		}
		return result;
	}
}
