/**
 * 
 */
package com.chanapps.four.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.service.FileSaverService;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class UserStatistics {
	public static final String TAG = "UserPreferences";
	public static final boolean DEBUG = true;
	
	public static final int MIN_TOP_BOARDS = 5;
	private static final long MIN_STORE_DELAY = 15000;  // 15s
	
	/*
	 * board code -> number of board visits
	 */
	public Map<String, ChanBoardStat> boardOpen = new HashMap<String, ChanBoardStat>();
	/*
	 * board code -> number of threads opened
	 */
	public Map<String, ChanBoardStat> boardThread = new HashMap<String, ChanBoardStat>();
	/*
	 * board code -> number of images downloaded
	 */
	public Map<String, ChanBoardStat> boardFullImage = new HashMap<String, ChanBoardStat>();
	
	public long lastUpdate;
	public long lastStored;
	
	public void registerActivity(ChanIdentifiedActivity activity) {
		ChanActivityId activityId = activity.getChanActivityId();
		switch(activityId.activity) {
		case BOARD_ACTIVITY:
			boardOpened(activityId.boardCode);
			break;
		case THREAD_ACTIVITY:
			threadOpened(activityId.boardCode, activityId.threadNo);
			break;
		case FULL_SCREEN_IMAGE_ACTIVITY:
			imageDownloaded(activityId.boardCode, activityId.postNo);
			break;
		case POST_REPLY_ACTIVITY:
			break;
		default:
			// we don't register other activities
		}
		if (new Date().getTime() - lastStored > MIN_STORE_DELAY) {
			FileSaverService.startService(activity.getBaseContext(), FileSaverService.FileType.USER_STATISTICS);
		}
	}
	
	public void boardOpened(String boardCode) {
		if (!boardOpen.containsKey(boardCode)) {
			boardOpen.put(boardCode, new ChanBoardStat(boardCode));
		}
		boardOpen.get(boardCode).usage++;
		lastUpdate = new Date().getTime();
	}
	
	public void threadOpened(String boardCode, long threadNo) {
		if (!boardThread.containsKey(boardCode)) {
			boardThread.put(boardCode, new ChanBoardStat(boardCode));
		}
		boardThread.get(boardCode).usage++;
		lastUpdate = new Date().getTime();
	}
	
	public void imageDownloaded(String boardCode, long postNo) {
		if (!boardFullImage.containsKey(boardCode)) {
			boardFullImage.put(boardCode, new ChanBoardStat(boardCode));
		}
		boardFullImage.get(boardCode).usage++;
		lastUpdate = new Date().getTime();
	}
	
	/**
	 * Returns short list of top used boards.
	 */
	public List<ChanBoardStat> getTopBoards() {
		List<ChanBoardStat> topBoards = new ArrayList<ChanBoardStat>(boardOpen.values());
		int sumOfUsages = 0;
		for(ChanBoardStat board : topBoards) {
			if (boardThread.containsKey(board.board)) {
				board.usage += boardThread.get(board.board).usage;
			}
			if (boardFullImage.containsKey(board.board)) {
				board.usage += boardFullImage.get(board.board).usage;
			}
			sumOfUsages += board.usage;
		}
		// sorting by last modification date desc order
        Collections.sort(topBoards, new Comparator<ChanBoardStat>() {
            public int compare(ChanBoardStat o1, ChanBoardStat o2) {
                return o1.usage > o2.usage ? 1
                		: o1.usage < o2.usage ? -1 : 0;
            }
        });
		if (topBoards.size() < MIN_TOP_BOARDS) {
			if (DEBUG) {
				Log.d(TAG, "Top boards: " + logStats(topBoards));
			}
			return topBoards;
		}
		int averageUsage = sumOfUsages / topBoards.size();
		int numOfTopBoards = 0;
        for(ChanBoardStat board : topBoards) {
        	numOfTopBoards++;
        	if (board.usage < averageUsage) {
        		break;
        	}
        }
        if (numOfTopBoards < MIN_TOP_BOARDS) {
        	numOfTopBoards = topBoards.size() < MIN_TOP_BOARDS ? topBoards.size() : MIN_TOP_BOARDS;
        }
        topBoards = topBoards.subList(0, numOfTopBoards);
        if (DEBUG) {
			Log.d(TAG, "Top boards: " + logStats(topBoards));
		}
		return topBoards;
	}
	
	private String logStats(List<ChanBoardStat> boards) {
		StringBuffer buf = new StringBuffer();
		for(ChanBoardStat board : boards) {
			buf.append(board.board + ": " + board.usage + ", ");
		}
		return buf.toString();
	}
}
