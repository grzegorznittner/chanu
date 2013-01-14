/**
 * 
 */
package com.chanapps.four.data;

import java.util.Date;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class DataTransfer {
	public DataTransfer (int duration, int size) {
		time = new Date();
		this.duration = duration;
		this.size = size;
		this.dataRate = size / duration;
		this.failed = false;
	}
	public DataTransfer() {
		time = new Date();
		failed = true;
	}
	public Date time;
	/** Fetch time in ms */
	public int duration;
	/** Fetched data size in bytes */
	public int size;
	/** Download rate B/ms, same as kB/s */
	public double dataRate;
	public boolean failed;
	
	public String toString() {
		if (failed) {
			return "failed at " + time;
		} else {
			return "size " + size + "b during " + duration + "ms at " + dataRate + "kB/s on " + time;
		}
	}
}
