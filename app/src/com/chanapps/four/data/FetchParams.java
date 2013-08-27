/**
 * 
 */
package com.chanapps.four.data;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FetchParams {
	public long refreshDelay;
	public long forceRefreshDelay;
	public int readTimeout;
	public int connectTimeout;
    public int maxAutoLoadFSize; // in bytes
    public int maxThumbnailPrefetches;

	public FetchParams(long refreshDelay, long forceRefreshDelay, int readTimeout, int connectTimeout, int maxAutoLoadFSize, int maxThumbnailPrefetches) {
		this.refreshDelay = refreshDelay * 1000;
		this.forceRefreshDelay = forceRefreshDelay * 1000;
		this.readTimeout = readTimeout * 1000;
		this.connectTimeout = connectTimeout * 1000;
        this.maxAutoLoadFSize = maxAutoLoadFSize;
        this.maxThumbnailPrefetches = maxThumbnailPrefetches;
	}
}
