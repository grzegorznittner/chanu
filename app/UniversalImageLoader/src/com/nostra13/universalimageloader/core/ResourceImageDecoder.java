package com.nostra13.universalimageloader.core;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.widget.ImageView.ScaleType;

import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.download.ImageDownloader;

/**
 * Decodes resource images to {@link Bitmap}
 * 
 * @author Grzegorz Nittner (grzegorz.nittner[at]gmail[dot]com)
 * 
 * @see ImageScaleType
 * @see ImageDownloader
 */
public class ResourceImageDecoder {

	private final Context context;
	private final int resourceId;

	/**
	 * @param imageUri
	 *            Image URI (<b>i.e.:</b> "http://site.com/image.png", "file:///mnt/sdcard/image.png")
	 * @param imageDownloader
	 *            Image downloader
	 * 
	 */
	public ResourceImageDecoder(Context context, int resourceId) {
		this.context = context;
		this.resourceId = resourceId;
	}

	/**
	 * Decodes image from URI into {@link Bitmap}. Image is scaled close to incoming {@link ImageSize image size} during
	 * decoding (depend on incoming image scale type).
	 * 
	 * @param targetSize
	 *            Image size to scale to during decoding
	 * @param scaleType
	 *            {@link ImageScaleType Image scale type}
	 * 
	 * @return Decoded bitmap
	 * @throws IOException
	 */
	public Bitmap decode(ImageSize targetSize, ImageScaleType scaleType) throws IOException {
		return decode(targetSize, scaleType, ScaleType.CENTER_INSIDE);
	}

	/**
	 * Decodes image from URI into {@link Bitmap}. Image is scaled close to incoming {@link ImageSize image size} during
	 * decoding (depend on incoming image scale type).
	 * 
	 * @param targetSize
	 *            Image size to scale to during decoding
	 * @param scaleType
	 *            {@link ImageScaleType Image scale type}
	 * @param viewScaleType
	 *            {@link ScaleType ImageView scale type}
	 * 
	 * @return Decoded bitmap
	 * @throws IOException
	 */
	public Bitmap decode(ImageSize targetSize, ImageScaleType scaleType, ScaleType viewScaleType) throws IOException {
		Options decodeOptions = getBitmapOptionsForImageDecoding(targetSize, scaleType, viewScaleType);
		return BitmapFactory.decodeResource(context.getResources(), resourceId, decodeOptions);
	}

	private Options getBitmapOptionsForImageDecoding(ImageSize targetSize, ImageScaleType scaleType, ScaleType viewScaleType) throws IOException {
		Options options = new Options();
		options.inSampleSize = computeImageScale(targetSize, scaleType, viewScaleType);
		return options;
	}

	private int computeImageScale(ImageSize targetSize, ImageScaleType scaleType, ScaleType viewScaleType) throws IOException {
		int targetWidth = targetSize.getWidth();
		int targetHeight = targetSize.getHeight();

		// decode image size
		Options options = new Options();
		options.inJustDecodeBounds = true;

		int scale = 1;
		int imageWidth = options.outWidth;
		int imageHeight = options.outHeight;
		int widthScale = imageWidth / targetWidth;
		int heightScale = imageWidth / targetHeight;
		switch (viewScaleType) {
			case FIT_XY:
			case FIT_START:
			case FIT_END:
			case CENTER_INSIDE:
				switch (scaleType) {
					default:
					case POWER_OF_2:
						while (imageWidth / 2 >= targetWidth || imageHeight / 2 >= targetHeight) { // ||
							imageWidth /= 2;
							imageHeight /= 2;
							scale *= 2;
						}
						break;
					case EXACT:
						scale = Math.max(widthScale, heightScale); // max
						break;
				}
				break;
			case MATRIX:
			case CENTER:
			case CENTER_CROP:
			default:
				switch (scaleType) {
					default:
					case POWER_OF_2:
						while (imageWidth / 2 >= targetWidth && imageHeight / 2 >= targetHeight) { // &&
							imageWidth /= 2;
							imageHeight /= 2;
							scale *= 2;
						}
						break;
					case EXACT:
						scale = Math.min(widthScale, heightScale); // min
						break;
				}
		}

		if (scale < 1) {
			scale = 1;
		}

		return scale;
	}
}