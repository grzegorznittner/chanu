package com.chanapps.four.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chanapps.four.activity.R;
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

    public List<ChanPost> getWatchedThreads() {
    	List<ChanPost> result = new ArrayList<ChanPost>();
    	
        List<String> savedWatchlist = ChanWatchlist.getSortedWatchlistFromPrefs(context);
        if (DEBUG) Log.i(TAG, "Parsing watchlist: " + Arrays.toString(savedWatchlist.toArray()));
        for (String threadPath : savedWatchlist) {
            if (DEBUG) Log.i(TAG, "Parsing threadpath: " + threadPath);

            try {
                String[] threadComponents = ChanWatchlist.getThreadPathComponents(threadPath);
                String boardCode = threadComponents[1];
                long threadNo = Long.valueOf(threadComponents[2]);
                ChanThread thread = null;
                try {
                    if (DEBUG) Log.i(TAG, "trying to load thread " + boardCode + "/" + threadNo + " from storage");
                    thread = ChanFileStorage.loadThreadData(getContext(), boardCode, threadNo);
                    if (thread == null) {
                        if (DEBUG) Log.v(TAG, "Couldn't load watched thread, null");
                    }
                    else if (thread.defData) {
                        if (DEBUG) Log.v(TAG, "Couldn't load watched thread, defData");
                    }
                    else if (thread.posts == null || thread.posts.length == 0) {
                        if (DEBUG) Log.v(TAG, "Couldn't load watched thread, no posts found in thread");
                    }
                    else {
                        thread.posts[0].mergeIntoThreadList(result);
                        if (DEBUG) Log.v(TAG, "Loaded watched thread: " + thread.no);
                    }
                }
                catch (Exception e) {
                    Log.e(TAG, "Error loading thread data from storage", e);
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Error parsing watch preferences ", e);
            }
        }
  		return result;
    }

}
