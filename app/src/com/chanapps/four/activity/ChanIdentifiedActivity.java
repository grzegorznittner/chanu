package com.chanapps.four.activity;

import android.content.AsyncTaskLoader;
import android.database.Cursor;
import android.os.Handler;

public interface ChanIdentifiedActivity {
	/**
	 * Returns activity identifier
	 */
	ChanActivityId getChanActivityId();
	
	/**
	 * Returns cursor loader which can be used to reload data displayed on the activity page.
	 * If not available null will be returned.
	 */
	AsyncTaskLoader<Cursor> getChanCursorLoader();
	
	/**
	 * Returns handler which can be used to notify activity.
	 * If not available null will be returned.
	 */
	Handler getChanHandler();
}
