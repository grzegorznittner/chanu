/**
 * 
 */
package com.chanapps.four.data;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanBoardStat {
	public String board;
	public int usage;
	
	public ChanBoardStat() {
		this.usage = 0;
	}
	public ChanBoardStat(String board) {
		this.board = board;
		this.usage = 0;
	}
	
	public String toString() {
		return "board " + board + " used " + usage;
	}
}
