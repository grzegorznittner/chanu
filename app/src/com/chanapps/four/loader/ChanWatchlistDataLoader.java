package com.chanapps.four.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.ChanWatchlist;

public class ChanWatchlistDataLoader {

    private static final String TAG = ChanWatchlistDataLoader.class.getSimpleName();
    private static final boolean DEBUG = false;

    protected Context context;

    public ChanWatchlistDataLoader(Context context) {
        this.context = context;
    }
    
    public Context getContext() {
    	return context;
    }

    private boolean hideAllText;
    private boolean hidePostNumbers;
    private boolean useFriendlyIds;

    public List<ChanThread> getWatchedThreads() {
    	List<ChanThread> result = new ArrayList<ChanThread>();
    	
        List<String> savedWatchlist = ChanWatchlist.getSortedWatchlistFromPrefs(context);
//        if (savedWatchlist == null || savedWatchlist.isEmpty()) {
//            return null;
//        }
        if (DEBUG) Log.i(TAG, "Parsing watchlist: " + Arrays.toString(savedWatchlist.toArray()));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, true);
        useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        for (String threadPath : savedWatchlist) {
            if (DEBUG) Log.i(TAG, "Parsing threadpath: " + threadPath);

            try {
                String[] threadComponents = ChanWatchlist.getThreadPathComponents(threadPath);
                String boardCode = threadComponents[1];
                long threadNo = Long.valueOf(threadComponents[2]);
                ChanThread thread = null;
                ChanPost threadPost = null;
                try {
                    if (DEBUG) Log.i(TAG, "trying to load thread " + boardCode + "/" + threadNo + " from storage");
                    thread = ChanFileStorage.loadThreadData(getContext(), boardCode, threadNo);
                }
                catch (Exception e) {
                    Log.e(TAG, "Error loading thread data from storage", e);
                }

                if (thread != null && thread.defData) {
                    if (DEBUG) Log.i(TAG, "Found defdata on thread post, skipping");
                } else { // pull from cache, it will have the latest data
                    if (DEBUG) Log.i(TAG, "Found cached watchlist thread " + boardCode + "/" + threadNo + ", updating from cache");
                    result.add(thread);
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Error parsing watch preferences ", e);
            }
        }
  		return result;
    }

}
