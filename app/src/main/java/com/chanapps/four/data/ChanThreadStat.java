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
public class ChanThreadStat {
	public String board;
	public long no;
	public int usage;
	public long lastUsage;
	
	public ChanThreadStat() {
		this.usage = 0;
	}
	public ChanThreadStat(String board, long threadNo) {
		this.board = board;
		this.no = threadNo;
		this.usage = 0;
	}
	public long use() {
		usage++;
		lastUsage = Calendar.getInstance().getTimeInMillis();
		return lastUsage;
	}
	
	public String toString() {
		return "thread " + board + "/" + no + " used " + usage + " last " + new Date(lastUsage);
	}
}
