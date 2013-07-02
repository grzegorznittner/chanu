package com.chanapps.four.activity;

import android.content.Context;

public interface ChanIdentifiedService {
	/**
	 * Returns activity identifier
	 */
	ChanActivityId getChanActivityId();
	
	/**
	 * Returns service's context.
	 */
	Context getApplicationContext ();

}
