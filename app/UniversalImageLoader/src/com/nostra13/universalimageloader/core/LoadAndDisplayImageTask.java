package com.nostra13.universalimageloader.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import android.content.Context;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;

import com.chanapps.four.service.NetworkProfileManager;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.utils.FileUtils;

/**
 * Presents load'n'display image task. Used to load image from Internet or file system, decode it to {@link Bitmap}, and
 * display it in {@link ImageView} through {@link DisplayBitmapTask}.
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageLoaderConfiguration
 * @see ImageLoadingInfo
 */
final class LoadAndDisplayImageTask implements Runnable {

    private static final String TAG = LoadAndDisplayImageTask.class.getSimpleName();
    private static final boolean DEBUG = false;

	private static final String LOG_START_DISPLAY_IMAGE_TASK = "Start display image task [%s]";
	private static final String LOG_LOAD_IMAGE_FROM_INTERNET = "Load image from Internet [%s]";
	private static final String LOG_LOAD_IMAGE_FROM_DISC_CACHE = "Load image from disc cache [%s]";
	private static final String LOG_CACHE_IMAGE_IN_MEMORY = "Cache image in memory [%s]";
	private static final String LOG_CACHE_IMAGE_ON_DISC = "Cache image on disc [%s]";
	private static final String LOG_DISPLAY_IMAGE_IN_IMAGEVIEW = "Display image in ImageView [%s]";

	private static final int ATTEMPT_COUNT_TO_DECODE_BITMAP = 3;

    private final Context context;
	private final ImageLoaderConfiguration configuration;
	private final ImageLoadingInfo imageLoadingInfo;
	private final Handler handler;

	public LoadAndDisplayImageTask(Context context, ImageLoaderConfiguration configuration, ImageLoadingInfo imageLoadingInfo, Handler handler) {
		this.context = context;
        this.configuration = configuration;
		this.imageLoadingInfo = imageLoadingInfo;
		this.handler = handler;
	}

	@Override
	public void run() {
		if (configuration.loggingEnabled) Log.i(ImageLoader.TAG, String.format(LOG_START_DISPLAY_IMAGE_TASK, imageLoadingInfo.memoryCacheKey));

		if (checkTaskIsNotActual()) return;
		Bitmap bmp;
        try {
            NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(context);
            if (imageLoadingInfo != null && imageLoadingInfo.targetSize != null)
                if (DEBUG) Log.i(TAG, "loadanddisplay run imageLoadingInfo target size " + imageLoadingInfo.targetSize.toString());
            else
                if (DEBUG) Log.i(TAG, "loadanddisplay null target size");
            bmp = tryLoadBitmap();
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't load bitmap, exception", e);
            bmp = null;
        }
        catch (OutOfMemoryError e) {
            Log.e(TAG, "Couldn't load bitmap, out of memory error", e);
            bmp = null;
        }
		if (bmp == null) return;

		if (checkTaskIsNotActual()) return;
		if (imageLoadingInfo.options.isCacheInMemory()) {
			if (configuration.loggingEnabled) Log.i(ImageLoader.TAG, String.format(LOG_CACHE_IMAGE_IN_MEMORY, imageLoadingInfo.memoryCacheKey));

			configuration.memoryCache.put(imageLoadingInfo.memoryCacheKey, bmp);
		}

		if (checkTaskIsNotActual()) return;
		if (configuration.loggingEnabled) Log.i(ImageLoader.TAG, String.format(LOG_DISPLAY_IMAGE_IN_IMAGEVIEW, imageLoadingInfo.memoryCacheKey));

		DisplayBitmapTask displayBitmapTask = new DisplayBitmapTask(bmp, imageLoadingInfo.imageView, imageLoadingInfo.listener);
		handler.post(displayBitmapTask);
	}

	/**
	 * Check whether the image URI of this task matches to image URI which is actual for current ImageView at this
	 * moment and fire {@link ImageLoadingListener#onLoadingCancelled()} event if it doesn't.
	 */
	boolean checkTaskIsNotActual() {
		String currentCacheKey = ImageLoader.getInstance().getLoadingUriForView(imageLoadingInfo.imageView);
		// Check whether memory cache key (image URI) for current ImageView is actual. 
		// If ImageView is reused for another task then current task should be cancelled.
		boolean imageViewWasReused = !imageLoadingInfo.memoryCacheKey.equals(currentCacheKey);
		if (imageViewWasReused) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					imageLoadingInfo.listener.onLoadingCancelled();
				}
			});
		}
		return imageViewWasReused;
	}

	private Bitmap tryLoadBitmap() {
		File imageFile = configuration.discCache.get(imageLoadingInfo.uri);

		Bitmap bitmap = null;
		try {
			// Try to load image from disc cache
			if (imageFile.exists()) {
				if (configuration.loggingEnabled) Log.i(ImageLoader.TAG, String.format(LOG_LOAD_IMAGE_FROM_DISC_CACHE, imageLoadingInfo.memoryCacheKey));

				Bitmap b = decodeImage(imageFile.toURI());
				if (b != null) {
					return b;
				}
			}

			// Load image from Web
			if (configuration.loggingEnabled) Log.i(ImageLoader.TAG, String.format(LOG_LOAD_IMAGE_FROM_INTERNET, imageLoadingInfo.memoryCacheKey));

			URI imageUriForDecoding;
			if (imageLoadingInfo.options.isCacheOnDisc()) {
				if (configuration.loggingEnabled) Log.i(ImageLoader.TAG, String.format(LOG_CACHE_IMAGE_ON_DISC, imageLoadingInfo.memoryCacheKey));

                if (imageLoadingInfo != null && imageLoadingInfo.targetSize != null)
                    if (DEBUG) Log.i(TAG, "tryloadbitmap imageLoadingInfo target size " + imageLoadingInfo.targetSize.toString());
                else
                    if (DEBUG) Log.i(TAG, "tryloadbitmap null target size");
                saveImageOnDisc(imageFile);
				configuration.discCache.put(imageLoadingInfo.uri, imageFile);
				imageUriForDecoding = imageFile.toURI();
			} else {
				imageUriForDecoding = new URI(imageLoadingInfo.uri);
			}

			bitmap = decodeImage(imageUriForDecoding);
			if (bitmap == null) {
				fireImageLoadingFailedEvent(FailReason.IO_ERROR);
			}
		} catch (IOException e) {
			Log.e(ImageLoader.TAG, e.getMessage(), e);
			fireImageLoadingFailedEvent(FailReason.IO_ERROR);
			if (imageFile.exists()) {
				imageFile.delete();
			}
		} catch (OutOfMemoryError e) {
			Log.e(ImageLoader.TAG, e.getMessage(), e);
			fireImageLoadingFailedEvent(FailReason.OUT_OF_MEMORY);
		} catch (Throwable e) {
			Log.e(ImageLoader.TAG, e.getMessage(), e);
			fireImageLoadingFailedEvent(FailReason.UNKNOWN);
		}
		return bitmap;
	}

	private Bitmap decodeImage(URI imageUri) throws IOException {
		Bitmap bmp = null;

		if (configuration.handleOutOfMemory) {
			bmp = decodeWithOOMHandling(imageUri);
		} else {
			ImageDecoder decoder = new ImageDecoder(imageUri, configuration.downloader);
			bmp = decoder.decode(imageLoadingInfo.targetSize, imageLoadingInfo.options.getImageScaleType(), imageLoadingInfo.imageView.getScaleType());
		}
		return bmp;
	}

	private Bitmap decodeWithOOMHandling(URI imageUri) throws IOException {
		Bitmap result = null;
		ImageDecoder decoder = new ImageDecoder(imageUri, configuration.downloader);
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

	private void saveImageOnDisc(File targetFile) throws IOException, URISyntaxException {
		int width = configuration.maxImageWidthForDiscCache;
		int height = configuration.maxImageHeightForDiscCache;
		if (width > 0 || height > 0) {
            if (imageLoadingInfo != null && imageLoadingInfo.targetSize != null) {
                width = imageLoadingInfo.targetSize.getWidth();
                height = imageLoadingInfo.targetSize.getHeight();
            }

			// Download, decode, compress and save image
			ImageSize targetImageSize = new ImageSize(width, height);
			ImageDecoder decoder = new ImageDecoder(new URI(imageLoadingInfo.uri), configuration.downloader);
			Bitmap bmp = decoder.decode(targetImageSize, ImageScaleType.EXACT);
            if (bmp == null) {
                Log.e(TAG, "Couldn't save bitmap, null decode: " + imageLoadingInfo.uri);
                return;
            }
            OutputStream os;
            try {
			    os = new BufferedOutputStream(new FileOutputStream(targetFile));
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't open output stream for file: " + targetFile, e);
                return;
            }
			boolean compressedSuccessfully = bmp.compress(configuration.imageCompressFormatForDiscCache, configuration.imageQualityForDiscCache, os);
			if (compressedSuccessfully) {
				bmp.recycle();
				return;
			}
		}

		// If previous compression wasn't needed or failed
		// Download and save original image
		InputStream is = configuration.downloader.getStream(new URI(imageLoadingInfo.uri));
		try {
			OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile));
			try {
				FileUtils.copyStream(is, os);
			} finally {
				os.close();
			}
		} finally {
			is.close();
		}
	}

	private void fireImageLoadingFailedEvent(final FailReason failReason) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				imageLoadingInfo.listener.onLoadingFailed(failReason);
			}
		});
	}
}
