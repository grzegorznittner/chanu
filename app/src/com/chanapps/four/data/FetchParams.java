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

	public FetchParams(long refreshDelay, long forceRefreshDelay, int readTimeout, int connectTimeout) {
		this.refreshDelay = refreshDelay * 1000;
		this.forceRefreshDelay = forceRefreshDelay * 1000;
		this.readTimeout = readTimeout * 1000;
		this.connectTimeout = connectTimeout * 1000;
	}
}
