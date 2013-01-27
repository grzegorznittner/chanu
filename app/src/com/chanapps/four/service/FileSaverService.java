/**
 * 
 */
package com.chanapps.four.service;

import java.util.Calendar;

import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.UserStatistics;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FileSaverService extends BaseChanService {
    private static final String TAG = FileSaverService.class.getSimpleName();
    private static final boolean DEBUG = true;
    
    public enum FileType {USER_STATISTICS};
    
    public static void startService(Context context, FileType fileType) {
        if (DEBUG) Log.i(TAG, "Start file saver service for " + fileType);
        Intent intent = new Intent(context, FileSaverService.class);
        intent.putExtra(ChanHelper.NAME, fileType.toString());
        context.startService(intent);
    }

    public FileSaverService() {
   		super("filesaver");
   	}

    protected FileSaverService(String name) {
   		super(name);
   	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
        long startTime = Calendar.getInstance().getTimeInMillis();
		try {
			String fileName = intent.getStringExtra(ChanHelper.NAME);
			FileType fileType = FileType.valueOf(fileName);
			if (DEBUG) Log.i(TAG, "Handling file saver service for " + fileType);
			
			switch(fileType) {
			case USER_STATISTICS:
				UserStatistics userStats = NetworkProfileManager.instance().getUserStatistics();
				if (userStats == null) {
					Log.w(TAG, "User statistics object not loaded!");
					return;
				}
				ChanFileStorage.storeUserPreferences(getBaseContext(), userStats);
				break;
			}
			
			long endTime = Calendar.getInstance().getTimeInMillis();
            if (DEBUG) Log.i(TAG, "Stored " + fileType + " in " + (endTime - startTime) + "ms.");            
		} catch (Exception e) { 
            Log.e(TAG, "Error in clean up service", e);
		}
	}
}
