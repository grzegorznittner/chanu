/**
 * 
 */
package com.chanapps.four.service;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class CleanUpService extends BaseChanService {

    protected static final String TAG = CleanUpService.class.getSimpleName();
    private static final boolean DEBUG = true;
	
    public static void startService(Context context) {
        if (DEBUG) Log.i(TAG, "Start clean up service");
        Intent intent = new Intent(context, CleanUpService.class);
        context.startService(intent);
    }

    public CleanUpService() {
   		super("cleanup");
   	}

    protected CleanUpService(String name) {
   		super(name);
   	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
        long startTime = Calendar.getInstance().getTimeInMillis();
		try {
            Context context = getBaseContext();
            
            ChanFileStorage.getSizeOfCacheDirectory(context);

            long endTime = Calendar.getInstance().getTimeInMillis();
            if (DEBUG) Log.i(TAG, "");
		} catch (Exception e) {
            Log.e(TAG, "Error in clean up service", e);
		}
	}
	
	private void addTree(File file, Collection<File> all) {
	    File[] children = file.listFiles();
	    if (children != null) {
	        for (File child : children) {
	            all.add(child);
	            if (child.isDirectory()) {
	            	addTree(child, all);
	            }
	        }
	    }
	}
}
