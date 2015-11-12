/**
 * 
 */
package com.chanapps.four.data;

import java.util.*;

import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.component.TutorialOverlay;
import com.chanapps.four.service.FileSaverService;
import com.chanapps.four.service.NetworkProfileManager;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class UserStatistics {
	public static final String TAG = UserStatistics.class.getSimpleName();
	public static final boolean DEBUG = false;
	
	public static final int MIN_TOP_BOARDS = 5;
	public static final int MAX_TOP_THREADS = 50;
	private static final long MIN_STORE_DELAY = 5000;  // 15s
	private static final long MIN_DELAY_FOR_TIPS = 5 * 60 * 1000; // 5min

	public static enum ChanFeature {
		NONE,
	    INTRO_DESC
        //,
	    //SECTION_DESC,
        //BOARD_DESC,
        //THREAD_DESC,
        //POST_DESC,
        //FINISHED_DESC
        //POPULAR_DESC,
        //WATCHLIST_DESC,
        //,WATCHLIST_CLEAN, WATCHLIST_DELETE,
		//BOARD_SELECT, MANUAL_REFRESH, SEARCH_BOARD, SEARCH_THREAD, BOARD_LIST_VIEW, ADD_THREAD, POST,
		//CACHED_BOARD_IMAGES, ALL_CACHED_IMAGES, BOARD_RULES, WATCH_THREAD, GALLERY_VIEW,
		//PLAY_THREAD, PRELOAD_ALL_IMAGES, DOWNLOAD_ALL_IMAGES_TO_GALLERY,
		//SETTINGS_NAMES, SETTINGS_4CHAN_PASS, SETTINGS_CACHE_SIZE, SETTINGS_WATCHLIST
	}
    public static ChanFeature[] MAIN_TUTORIAL_FEATURES = new ChanFeature[]{
            ChanFeature.INTRO_DESC
            //,
            //ChanFeature.SECTION_DESC,
            //ChanFeature.BOARD_DESC,
            //ChanFeature.THREAD_DESC,
            //ChanFeature.POST_DESC,
            //ChanFeature.FINISHED_DESC
    };
	/*
	public static ChanFeature[] BOARDSELECTOR_FEATURES = new ChanFeature[]{
		ChanFeature.BOARDSELECTOR_DESC, ChanFeature.BOARD_SELECT, ChanFeature.ALL_CACHED_IMAGES };
	public static ChanFeature[] POPULAR_FEATURES = new ChanFeature[]{
            ChanFeature.POPULAR_DESC
            ,ChanFeature.MANUAL_REFRESH
    };
	public static ChanFeature[] WATCHLIST_FEATURES = new ChanFeature[]{
            ChanFeature.WATCHLIST_DESC
            , ChanFeature.WATCHLIST_CLEAN, ChanFeature.WATCHLIST_DELETE
    };
	public static ChanFeature[] BOARD_FEATURES = new ChanFeature[]{
            ChanFeature.BOARD_DESC
            //, ChanFeature.BOARD_SELECT,
            //ChanFeature.SEARCH_BOARD, ChanFeature.BOARD_LIST_VIEW, ChanFeature.ADD_THREAD, ChanFeature.BOARD_RULES,
            //ChanFeature.SETTINGS_CACHE_SIZE, ChanFeature.SETTINGS_NAMES, ChanFeature.SETTINGS_WATCHLIST,
            //ChanFeature.SETTINGS_4CHAN_PASS
    };
    public static ChanFeature[] THREAD_FEATURES = new ChanFeature[]{
            ChanFeature.THREAD_DESC
            //ChanFeature.SEARCH_THREAD
            //, ChanFeature.POST, ChanFeature.GALLERY_VIEW, ChanFeature.PLAY_THREAD,
            //ChanFeature.WATCH_THREAD, ChanFeature.PRELOAD_ALL_IMAGES, ChanFeature.DOWNLOAD_ALL_IMAGES_TO_GALLERY,
            //ChanFeature.SETTINGS_CACHE_SIZE, ChanFeature.SETTINGS_NAMES, ChanFeature.SETTINGS_WATCHLIST,
            //ChanFeature.SETTINGS_4CHAN_PASS
    };
	public static ChanFeature[] GALLERY_FEATURES = new ChanFeature[]{};
    */
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
	
	/*
	 * list of used features
	 */
	public Set<ChanFeature> usedFeatures = new HashSet<ChanFeature>();
	/*
	 *  list of displayed tips for features
	 */
	public Set<ChanFeature> displayedTips = new HashSet<ChanFeature>();
	
	public long tipDisplayed = 0;
	
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
		case GALLERY_ACTIVITY:
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

    public void reset() {
        boardStats.clear();
        threadStats.clear();
        boardThreadStats.clear();
        usedFeatures.clear();
        displayedTips.clear();
        tipDisplayed = 0;
        lastUpdate = 0;
        ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        if (activity != null)
            FileSaverService.startService(activity.getBaseContext(), FileSaverService.FileType.USER_STATISTICS);
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

	/**
	 * Marks feature as used, tip for it won't be displayed
	 */
	public void featureUsed(ChanFeature feature) {
		if (!usedFeatures.contains(feature)) {
			Log.e(TAG, "Feature " + feature + " marked as used");
			usedFeatures.add(feature);
		}
		String used = "";
		for (ChanFeature f : usedFeatures) {
			used += f + ", ";
		}
		if (DEBUG) Log.i(TAG, "Used features: " + used);
	}

	/**
	 * Returns feature for which tip should be displayed.
	 * If ChanFeature.NONE is returned then tip should not be displayed
	 */
	public ChanFeature nextTipForPage(TutorialOverlay.Page tutorialPage) {		
		ChanFeature[] tipSet = null;
		switch (tutorialPage) {
		    /*
            case BOARDLIST:
			if (!displayedTips.contains(ChanFeature.BOARDSELECTOR_DESC)) {
				return ChanFeature.BOARDSELECTOR_DESC;
			}
			tipSet = BOARDSELECTOR_FEATURES;
			break;
		case POPULAR:
			if (!displayedTips.contains(ChanFeature.POPULAR_DESC)) {
				return ChanFeature.POPULAR_DESC;
			}
			tipSet = POPULAR_FEATURES;
			break;
		case WATCHLIST:
			if (!displayedTips.contains(ChanFeature.WATCHLIST_DESC)) {
				return ChanFeature.WATCHLIST_DESC;
			}
			tipSet = WATCHLIST_FEATURES;
			break;
        */
		case BOARD:
            tipSet = MAIN_TUTORIAL_FEATURES;
            break;
            /*
			if (!displayedTips.contains(ChanFeature.BOARD_DESC)) {
				return ChanFeature.BOARD_DESC;
			}
			tipSet = BOARD_FEATURES;
			break;
			*/
		/*
		case THREAD:
			if (!displayedTips.contains(ChanFeature.THREAD_DESC)) {
				return ChanFeature.THREAD_DESC;
			}
			tipSet = THREAD_FEATURES;
			break;
        case RECENT:
			if (!displayedTips.contains(ChanFeature.POPULAR_DESC)) {
				return ChanFeature.POPULAR_DESC;
			}
			tipSet = POPULAR_FEATURES;
			break;
		case WATCHLIST:
			if (!displayedTips.contains(ChanFeature.WATCHLIST_DESC)) {
				return ChanFeature.WATCHLIST_DESC;
			}
			tipSet = WATCHLIST_FEATURES;
			break;
		*/
        }
        /*
		if (!tipShouldBeDisplayed()) {
			return ChanFeature.NONE;
		}
        */
        if (DEBUG) Log.i(TAG, "nextTipForPage tipSet=" + Arrays.toString(tipSet) + " displayedTips=" + Arrays.toString(displayedTips.toArray()));
		for (ChanFeature feature : tipSet) {
			//if (!usedFeatures.contains(feature) && !displayedTips.contains(feature)) {
			if (!displayedTips.contains(feature)) {
				return feature;
			}
		}
		return ChanFeature.NONE;
	}
	
	/**
	 * Marks tip of the feature as being displayed.
	 * Tip for it won't be displayed anymore.
	 */
	public void tipDisplayed(ChanFeature feature) {
		displayedTips.add(feature);
		tipDisplayed = new Date().getTime();
		
		Log.e(TAG, "tipDisplayed " + feature);
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null) { // && new Date().getTime() - lastStored > MIN_STORE_DELAY) {
			FileSaverService.startService(activity.getBaseContext(), FileSaverService.FileType.USER_STATISTICS);
			Log.e(TAG, "User stats scheduled for save tip " + feature);
		}
	}

    public void disableTips() {
        for (ChanFeature feature : ChanFeature.values()) {
            displayedTips.add(feature);
        }
        tipDisplayed = new Date().getTime();
        
		ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
		if (activity != null) { // && new Date().getTime() - lastStored > MIN_STORE_DELAY) {
			FileSaverService.startService(activity.getBaseContext(), FileSaverService.FileType.USER_STATISTICS);
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
