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

	public FetchParams(long refreshDelay, long forceRefreshDelay, int readTimeout) {
		this.refreshDelay = refreshDelay;
		this.forceRefreshDelay = forceRefreshDelay;
		this.readTimeout = readTimeout;
	}
}
