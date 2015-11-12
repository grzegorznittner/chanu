/**
 * 
 */
package com.chanapps.four.data;

import java.util.Calendar;
import java.util.Date;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanBoardStat {
	public String board;
	public int usage;
	public long lastUsage;
	
	public ChanBoardStat() {
		this.usage = 0;
	}
	public ChanBoardStat(String board) {
		this.board = board;
		this.usage = 0;
	}
	public long use() {
		usage++;
		lastUsage = Calendar.getInstance().getTimeInMillis();
		return lastUsage;
	}
	
	public String toString() {
		return "board " + board + " used " + usage + " last at " + new Date(lastUsage);
	}
}
