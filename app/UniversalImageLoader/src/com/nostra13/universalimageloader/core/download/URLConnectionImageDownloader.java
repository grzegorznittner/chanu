package com.nostra13.universalimageloader.core.download;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

import android.nfc.Tag;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.nostra13.universalimageloader.core.assist.FlushedInputStream;

/**
 * Default implementation of ImageDownloader. Uses {@link URLConnection} for image stream retrieving.
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class URLConnectionImageDownloader extends ImageDownloader {

    public static final String TAG = URLConnectionImageDownloader.class.getSimpleName();

	/** {@value} */
	public static final int DEFAULT_HTTP_CONNECT_TIMEOUT = 5 * 1000; // milliseconds
	/** {@value} */
	public static final int DEFAULT_HTTP_READ_TIMEOUT = 20 * 1000; // milliseconds

	private int connectTimeout;
	private int readTimeout;

	public URLConnectionImageDownloader() {
		this(DEFAULT_HTTP_CONNECT_TIMEOUT, DEFAULT_HTTP_READ_TIMEOUT);
	}

	public URLConnectionImageDownloader(int connectTimeout, int readTimeout) {
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}

	@Override
	public InputStream getStreamFromNetwork(URI imageUri) throws IOException {
        NetworkProfile activeProfile = NetworkProfileManager.instance().getCurrentProfile();
        if (activeProfile.getConnectionType() == NetworkProfile.Type.NO_CONNECTION
                || activeProfile.getConnectionHealth() == NetworkProfile.Health.BAD
                || activeProfile.getConnectionHealth() == NetworkProfile.Health.NO_CONNECTION) {
            Log.e(TAG, "Slow network, bypassed downloading url=" + imageUri);
            return null;
        }
        else {
            URLConnection conn = imageUri.toURL().openConnection();
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            return new FlushedInputStream(new BufferedInputStream(conn.getInputStream()));
        }
    }
}