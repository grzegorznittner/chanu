/**
 * 
 */
package com.chanapps.four.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.UserStatistics;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FileSaverService extends BaseChanService {
    private static final String TAG = FileSaverService.class.getSimpleName();
    private static final boolean DEBUG = false;
    
    private static final String PARAM_DATE = "paramDate";
    private static final String PARAM_TYPE = "paramType";
    private static final String PARAM_VALUE = "paramValue";
    private static final String PARAM_STACKTRACE = "paramStack";
    public static final String NAME = "name";

    public enum FileType {USER_STATISTICS, LOG_EVENT, BOARD_SAVE};
    
    public static void startService(Context context, FileType fileType) {
        if (DEBUG) Log.i(TAG, "Start file saver service for " + fileType);
        Intent intent = new Intent(context, FileSaverService.class);
        intent.putExtra(NAME, fileType.toString());
        context.startService(intent);
    }
    
    public static void startService(Context context, FileType fileType, String value) {
        if (DEBUG) Log.i(TAG, "Start file saver service for " + fileType);
        Intent intent = new Intent(context, FileSaverService.class);
        intent.putExtra(NAME, fileType.toString());
    	intent.putExtra(PARAM_VALUE, value);
    	
        context.startService(intent);
    }
    
    public static void startService(Context context, FileType fileType, String type, String value) {
        if (DEBUG) Log.i(TAG, "Start file saver service for " + fileType);
        Intent intent = new Intent(context, FileSaverService.class);
        intent.putExtra(NAME, fileType.toString());
        intent.putExtra(PARAM_DATE, Calendar.getInstance().getTimeInMillis());
    	intent.putExtra(PARAM_TYPE, type);
    	intent.putExtra(PARAM_VALUE, value);
    	
    	StringWriter sw = new StringWriter();
    	PrintWriter pw = new PrintWriter(sw);
    	new Exception("Stacktrace").printStackTrace(pw);
    	intent.putExtra(PARAM_STACKTRACE, sw.toString());
    	
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
			String fileName = intent.getStringExtra(NAME);
			FileType fileType = FileType.valueOf(fileName);
			if (DEBUG) Log.i(TAG, "Handling file saver service for " + fileType);
			
			switch(fileType) {
			case USER_STATISTICS:
				UserStatistics userStats = NetworkProfileManager.instance().getUserStatistics();
				if (userStats == null) {
                    if (DEBUG) Log.w(TAG, "User statistics object not loaded!");
					return;
				}
				ChanFileStorage.storeUserStats(getBaseContext(), userStats);
				break;
			case LOG_EVENT:
				long logDate = intent.getLongExtra(PARAM_DATE, 0);
				String type = intent.getStringExtra(PARAM_TYPE);
				String value = intent.getStringExtra(PARAM_VALUE);
				String stack = intent.getStringExtra(PARAM_STACKTRACE);
				break;
			case BOARD_SAVE:
				String boardCode = intent.getStringExtra(PARAM_VALUE);
				ChanBoard board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
				ChanFileStorage.storeBoardData(getBaseContext(), board);
				break;
			}
			
			long endTime = Calendar.getInstance().getTimeInMillis();
            if (DEBUG) Log.i(TAG, "Stored " + fileType + " in " + (endTime - startTime) + "ms.");            
		} catch (Exception e) { 
            Log.e(TAG, "Error in file saver service", e);
		}
	}
}
