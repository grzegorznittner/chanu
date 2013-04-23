package com.nostra13.universalimageloader.core.download;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;

import com.android.gallery3d.ui.Log;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.nostra13.universalimageloader.core.assist.FlushedInputStream;

/**
 * Default implementation of ImageDownloader. Uses {@link URLConnection} for image stream retrieving.
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class URLConnectionImageDownloader extends BaseImageDownloader {

    public static final String TAG = URLConnectionImageDownloader.class.getSimpleName();

	/** {@value} */
	public static final int DEFAULT_HTTP_CONNECT_TIMEOUT = 5 * 1000; // milliseconds
	/** {@value} */
	public static final int DEFAULT_HTTP_READ_TIMEOUT = 20 * 1000; // milliseconds

	private int connectTimeout;
	private int readTimeout;

	public URLConnectionImageDownloader(Context context) {
		this(context, DEFAULT_HTTP_CONNECT_TIMEOUT, DEFAULT_HTTP_READ_TIMEOUT);
	}

	public URLConnectionImageDownloader(Context context, int connectTimeout, int readTimeout) {
		super(context, connectTimeout, readTimeout);
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}

	@Override
	public InputStream getStreamFromNetwork(String imageUri, Object extra) throws IOException {
        NetworkProfile activeProfile = NetworkProfileManager.instance().getCurrentProfile();
        if (activeProfile.getConnectionType() == NetworkProfile.Type.NO_CONNECTION
                || activeProfile.getConnectionHealth() == NetworkProfile.Health.NO_CONNECTION) {
            Log.e(TAG, "Slow network, bypassed downloading url=" + imageUri);
            return null;
        }
        else {
            URLConnection conn = new URL(imageUri).openConnection();
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            FlushedInputStream fis;
            try {
                fis = new FlushedInputStream(new BufferedInputStream(conn.getInputStream()));
                return fis;
            }
            catch (FileNotFoundException e) {
                Log.e(TAG, "Couldn't get image from network", e);
                NetworkProfileManager.instance().failedFetchingData(
                        null, NetworkProfile.Failure.NETWORK);
                return null;
            }
        }
    }
}