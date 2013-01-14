/**
 * 
 */
package com.chanapps.four.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class UserPreferences {
	public Map<String, Integer> boardOpen = new HashMap<String, Integer>();
	public Map<String, Integer> boardThread = new HashMap<String, Integer>();
	public Map<String, Integer> boardFullImage = new HashMap<String, Integer>();
	
	public Date lastUpdate;
	public Date lastStored;
	
	public void boardOpened(String boardCode) {
		boardOpen.put(boardCode, boardOpen.get(boardCode) + 1);
		lastUpdate = new Date();
	}
	
	public void threadOpened(String boardCode, long threadNo) {
		boardThread.put(boardCode, boardThread.get(boardCode) + 1);
		lastUpdate = new Date();
	}
	
	public void imageDownloaded(String boardCode, long postNo) {
		boardFullImage.put(boardCode, boardFullImage.get(boardCode) + 1);
		lastUpdate = new Date();
	}
}
