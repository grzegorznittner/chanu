/**
 * 
 */
package com.chanapps.four.data;

import java.util.ArrayList;
import java.util.Calendar;
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
	public static final boolean DEBUG = false;
	
	public static final int MIN_TOP_BOARDS = 5;
	public static final int MAX_TOP_THREADS = 50;
	private static final long MIN_STORE_DELAY = 15000;  // 15s
	
	/**
	 * board code -> number of visits (including threads and image view)
	 */
	public Map<String, ChanBoardStat> boardStats = new HashMap<String, ChanBoardStat>();
	/**
	 * thread num -> number of visits (including image view)
	 * @deprecated boardThreadStats should be used now
	 */
	public Map<Long, ChanThreadStat> threadStats = new HashMap<Long, ChanThreadStat>();
	/*
	 * board '/' thread num -> number of visits (including image view)
	 */
	public Map<String, ChanThreadStat> boardThreadStats = new HashMap<String, ChanThreadStat>();
	
	public long lastUpdate;
	public long lastStored;
	
	public boolean convertThreadStats() {
		if (threadStats.size() > 0) {
			int threadsToConvert = threadStats.size();
			for (ChanThreadStat stat : threadStats.values()) {
				boardThreadStats.put(stat.board + "/" + stat.no, stat);
			}
			threadStats.clear();
			if (DEBUG) Log.i(TAG, "" + threadsToConvert + " thread stats has been converted to new format.");
			return true;
		}
		return false;
	}
	
	public void registerActivity(ChanIdentifiedActivity activity) {
		ChanActivityId activityId = activity.getChanActivityId();
		switch(activityId.activity) {
		case BOARD_ACTIVITY:
			boardUse(activityId.boardCode);
			break;
		case THREAD_ACTIVITY:
		case FULL_SCREEN_IMAGE_ACTIVITY:
		case POST_REPLY_ACTIVITY:
			boardUse(activityId.boardCode);
			threadUse(activityId.boardCode, activityId.threadNo);
			break;
		default:
			// we don't register other activities
		}
		if (new Date().getTime() - lastStored > MIN_STORE_DELAY) {
			FileSaverService.startService(activity.getBaseContext(), FileSaverService.FileType.USER_STATISTICS);
		}
	}
	
	public void boardUse(String boardCode) {
		if (boardCode == null) {
			return;
		}
		if (!boardStats.containsKey(boardCode)) {
			boardStats.put(boardCode, new ChanBoardStat(boardCode));
		}
		lastUpdate = boardStats.get(boardCode).use();
	}
	
	public void threadUse(String boardCode, long threadNo) {
		if (boardCode == null || threadNo <= 0) {
			return;
		}
		String threadKey = boardCode + "/" + threadNo;
		if (!boardThreadStats.containsKey(threadKey)) {
			boardThreadStats.put(threadKey, new ChanThreadStat(boardCode, threadNo));
		}
		ChanThreadStat stat = boardThreadStats.get(threadKey);
		lastUpdate = stat.use();
	}
	
	/**
	 * Returns short list of top used boards.
	 */
	public List<ChanBoardStat> topBoards() {
		List<ChanBoardStat> topBoards = new ArrayList<ChanBoardStat>(boardStats.values());
		int sumOfUsages = 0;
		// sorting by last modification date desc order
        Collections.sort(topBoards, new Comparator<ChanBoardStat>() {
            public int compare(ChanBoardStat o1, ChanBoardStat o2) {
                return o1.usage > o2.usage ? 1
                		: o1.usage < o2.usage ? -1 : 0;
            }
        });
		if (topBoards.size() < MIN_TOP_BOARDS) {
			if (DEBUG) Log.d(TAG, "Top boards: " + logBoardStats(topBoards));
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
        if (DEBUG) Log.d(TAG, "Top boards: " + logBoardStats(topBoards));
		return topBoards;
	}
	
	/**
	 * Returns short list of top used boards.
	 */
	public List<ChanThreadStat> topThreads() {
		List<ChanThreadStat> topThreads = new ArrayList<ChanThreadStat>(boardThreadStats.values());
		int sumOfUsages = 0;
		// sorting by usage desc
        Collections.sort(topThreads, new Comparator<ChanThreadStat>() {
            public int compare(ChanThreadStat o1, ChanThreadStat o2) {
                return o1.usage > o2.usage ? 1
                		: o1.usage < o2.usage ? -1 : 0;
            }
        });
		if (topThreads.size() < MAX_TOP_THREADS) {
			if (DEBUG) Log.d(TAG, "Top threads: " + logThreadStats(topThreads));
			return topThreads;
		}
		int averageUsage = sumOfUsages / topThreads.size();
		int numOfTopThreads = 0;
        for(ChanThreadStat board : topThreads) {
        	numOfTopThreads++;
        	if (board.usage < averageUsage) {
        		break;
        	}
        }
        topThreads = topThreads.subList(0, numOfTopThreads);
        if (DEBUG) Log.d(TAG, "Top threads: " + logThreadStats(topThreads));
		return topThreads;
	}
	
	public void compactThreads() {
		long weekAgo = Calendar.getInstance().getTimeInMillis() - 7 * 24 * 60 * 60 * 1000;
		List<ChanThreadStat> topThreads = new ArrayList<ChanThreadStat>(boardThreadStats.values());
		for (ChanThreadStat threadStat : topThreads) {
			if (threadStat.lastUsage < weekAgo) {
				boardThreadStats.remove(threadStat.no);
			}
		}
	}

	private String logBoardStats(List<ChanBoardStat> boards) {
		StringBuffer buf = new StringBuffer();
		for(ChanBoardStat board : boards) {
			buf.append(board.board + ": " + board.usage + ", ");
		}
		return buf.toString();
	}

	private String logThreadStats(List<ChanThreadStat> threads) {
		StringBuffer buf = new StringBuffer();
		for(ChanThreadStat thread : threads) {
			buf.append(thread.board + "/" + thread.no + ": " + thread.usage + ", ");
		}
		return buf.toString();
	}
}
