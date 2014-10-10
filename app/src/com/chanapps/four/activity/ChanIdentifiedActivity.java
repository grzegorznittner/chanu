package com.chanapps.four.activity;

import android.content.Context;
import android.os.Handler;

public interface ChanIdentifiedActivity {
	/**
	 * Returns activity identifier
	 */
	ChanActivityId getChanActivityId();

	/**
	 * Returns handler which can be used to notify activity.
	 * If not available null will be returned.
	 */
	Handler getChanHandler();
	
	/**
	 * Returns activity's context.
	 */
	Context getBaseContext();

    // tell the search-launching activity to close the search action bar
    void closeSearch();

    // used for refreshing data
    void refresh();

    // tell activity we are starting fetch
    void setProgress(boolean on);

    public void switchBoard(String boardCode, String query); // for when we are already in this class

}
