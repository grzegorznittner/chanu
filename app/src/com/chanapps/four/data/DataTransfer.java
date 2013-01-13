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
	public int duration;
	public int size;
	public double dataRate;
	public boolean failed;
}
