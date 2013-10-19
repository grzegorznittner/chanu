package com.chanapps.four.component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import com.nostra13.universalimageloader.core.download.URLConnectionImageDownloader;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class ExtendedImageDownloader extends URLConnectionImageDownloader {

    private static final boolean DEBUG = false;

    public static final String PROTOCOL_ASSETS = "assets";
    public static final String PROTOCOL_DRAWABLE = "drawable";

    private static final String PROTOCOL_ASSETS_PREFIX = PROTOCOL_ASSETS + "://";
    private static final String PROTOCOL_DRAWABLE_PREFIX = PROTOCOL_DRAWABLE + "://";

    private Context context;

    public ExtendedImageDownloader(Context context) {
    	super(context);
        this.context = context;
    }

    @Override
    protected InputStream getStreamFromOtherSource(String imageUri, Object extra) throws IOException {
    	URI imageUrl = null;
		try {
			imageUrl = new URI(imageUri);
		} catch (URISyntaxException e) {
            Log.e(TAG, "Exception with uri syntax", e);
		}
        String protocol = imageUrl.getScheme();
        if (PROTOCOL_ASSETS.equals(protocol)) {
            return getStreamFromAssets(imageUrl);
        } else if (PROTOCOL_DRAWABLE.equals(protocol)) {
            return getStreamFromDrawable(imageUrl);
        } else {
            return super.getStreamFromOtherSource(imageUri, extra);
        }
    }

    private InputStream getStreamFromAssets(URI imageUri) throws IOException {
        String filePath = imageUri.toString().substring(PROTOCOL_ASSETS_PREFIX.length()); // Remove "assets://" prefix from image URI
        return context.getAssets().open(filePath);
    }

    private InputStream getStreamFromDrawable(URI imageUri) {
        String drawableIdString = imageUri.toString().substring(PROTOCOL_DRAWABLE_PREFIX.length()); // Remove "drawable://" prefix from image URI
        int drawableId = Integer.parseInt(drawableIdString);
        BitmapDrawable drawable = (BitmapDrawable) context.getResources().getDrawable(drawableId);
        Bitmap bitmap = drawable.getBitmap();
        if (DEBUG) Log.d(TAG, "Getting drawable from stream: " + imageUri + " has value " + drawable);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 0, os);
        return new ByteArrayInputStream(os.toByteArray());
    }
}