package com.chanapps.four.data;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.WeakHashMap;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.chanapps.four.service.UserPreferences;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class ChanFileStorage {
	private static final String TAG = ChanFileStorage.class.getSimpleName();
	private static final boolean DEBUG = false;
	
	private static WeakHashMap<String, ChanBoard> boardCache = new WeakHashMap<String, ChanBoard>();
	private static WeakHashMap<Long, ChanThread> threadCache = new WeakHashMap<Long, ChanThread>();

    private static final String CACHE_ROOT = "Android";
    private static final String CACHE_DATA_DIR = "data";
    private static final String CACHE_PKG_DIR = "cache";
    private static final String FILE_SEP = "/";
    private static final String CACHE_EXT = ".txt";
    private static final String BITMAP_CACHE_EXT = ".jpg";
    private static final String USER_PREFS_FILENAME = "userprefs.txt";

    public static boolean isBoardCachedOnDisk(Context context, String boardCode) {
        File boardDir = getBoardCacheDirectory(context, boardCode);
        return boardDir != null && boardDir.exists();
    }

    public static boolean isUserPreferencesOnDisk(Context context) {
        File userPrefsFile = getUserPreferencesFile(context);
        return userPrefsFile != null && userPrefsFile.exists();
    }

    private static File getBoardCacheDirectory(Context context, String boardCode) {
        String cacheDir = CACHE_ROOT + FILE_SEP + CACHE_DATA_DIR + FILE_SEP + context.getPackageName() + FILE_SEP
                + CACHE_PKG_DIR + FILE_SEP + boardCode;
        File boardDir = StorageUtils.getOwnCacheDirectory(context, cacheDir);
        return boardDir;
    }

    private static File getUserPreferencesFile(Context context) {
        String cacheDir = CACHE_ROOT + FILE_SEP + CACHE_DATA_DIR + FILE_SEP + context.getPackageName() + FILE_SEP
                + CACHE_PKG_DIR;
        File cacheFolder = StorageUtils.getOwnCacheDirectory(context, cacheDir);
        if (cacheFolder != null) {
        	File userPrefsFile = new File(cacheFolder, USER_PREFS_FILENAME);
        	return userPrefsFile;
        } else {
        	Log.e(TAG, "Cache folder returned empty");
        	return null;
        }
    }

    public static void storeBoardData(Context context, ChanBoard board) throws IOException {
    	if (board.defData) {
    		// default data should never be stored
    		return;
    	}
		boardCache.put(board.link, board);
        File boardDir = getBoardCacheDirectory(context, board.link);
		if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
			ObjectMapper mapper = ChanHelper.getJsonMapper();
			mapper.writeValue(new File(boardDir, board.link + CACHE_EXT), board);
			if (DEBUG) Log.i(TAG, "Stored " + board.threads.length + " threads for board '" + board.link + "'");
		} else {
			Log.e(TAG, "Cannot create board cache folder. " + (boardDir == null ? "null" : boardDir.getAbsolutePath()));
		}
	}
    
    public static File getBoardFile(Context context, String boardName, int page) {
        File boardDir = getBoardCacheDirectory(context, boardName);
		if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
			File boardFile = new File(boardDir, boardName + "_page" + page + CACHE_EXT);
			return boardFile;
		} else {
			Log.w(TAG, "Board folder could not be created: " + boardName);
			return null;
		}
    }

    public static long storeBoardFile(Context context, String boardName, int page, BufferedReader reader) throws IOException {
		File boardFile = getBoardFile(context, boardName, page);
		FileWriter writer = new FileWriter(boardFile, false);
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				writer.write(line);
			}
		} finally {
            try {
			    writer.flush();
			    writer.close();
            }
            catch (Exception e) {
                Log.e(TAG, "Exception while flushing and closing board file: " + e.getMessage(), e);
            }
		}
		if (DEBUG) Log.i(TAG, "Stored file for board " + boardName + " page " + page);
		return boardFile.length();
	}
	
    public static File getThreadFile(Context context, String boardName, long threadNo) {
        File boardDir = getBoardCacheDirectory(context, boardName);
		if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
			File boardFile = new File(boardDir, "t_" + threadNo + "f" + CACHE_EXT);
			return boardFile;
		} else {
			Log.w(TAG, "Board folder could not be created: " + boardName);
			return null;
		}
    }
	
    public static long storeThreadFile(Context context, String boardName, long threadNo, BufferedReader reader) throws IOException {
		File threadFile = getThreadFile(context, boardName, threadNo);
		FileWriter writer = new FileWriter(threadFile, false);
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				writer.write(line);
			}
		} finally {
		    writer.flush();
		    writer.close();
		}
		if (DEBUG) Log.i(TAG, "Stored file for thread " + boardName + "/" + threadNo);
		return threadFile.length();
	}
	
	public static void storeThreadData(Context context, ChanThread thread) throws IOException {
    	if (thread.defData) {
    		// default data should never be stored
    		return;
    	}
		threadCache.put(thread.no, thread);
        File boardDir = getBoardCacheDirectory(context, thread.board);
		if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
			//File tempThreadFile = new File(boardDir, "t_" + thread.no + "tmp" + CACHE_EXT);
			File threadFile = new File(boardDir, "t_" + thread.no + CACHE_EXT);
			try {
				ObjectMapper mapper = ChanHelper.getJsonMapper();
				mapper.writeValue(threadFile, thread);
				
//				mapper.writeValue(tempThreadFile, thread);
//				
//				ObjectMapper loadMapper = ChanHelper.getJsonMapper();
//				loadMapper.readValue(tempThreadFile, ChanThread.class);
//				
//				FileUtils.copyFile(tempThreadFile, threadFile);
			} finally {
//				FileUtils.deleteQuietly(tempThreadFile);
			}
			if (DEBUG) Log.i(TAG, "Stored " + thread.posts.length + " posts for thread '" + thread.board + FILE_SEP + thread.no + "'");
		} else {
			Log.e(TAG, "Cannot create board cache folder. " + (boardDir == null ? "null" : boardDir.getAbsolutePath()));
		}
	}
	
	public static ChanBoard loadBoardData(Context context, String boardCode) {
		if (boardCode == null) {
			Log.e(TAG, "Trying to load 'null' board! Check stack trace why has it happened.", new Exception());
			throw new RuntimeException("Null board code was passed!");
		}
		if (boardCache.containsKey(boardCode)) {
			if (DEBUG) Log.i(TAG, "Retruning board " + boardCode + " data from cache");
			return boardCache.get(boardCode);
		}
		File boardFile = null;
		try {
            File boardDir = getBoardCacheDirectory(context, boardCode);
			if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
				boardFile = new File(boardDir, boardCode + CACHE_EXT);
				if (boardFile != null && boardFile.exists()) {
					ObjectMapper mapper = ChanHelper.getJsonMapper();
					ChanBoard board = mapper.readValue(boardFile, ChanBoard.class);
					if (DEBUG) Log.i(TAG, "Loaded " + board.threads.length + " threads for board '" + board.link + "'");
					return board;
				} else {
					if (DEBUG) Log.i(TAG, "File for board '" + boardCode + "' doesn't exist");
				}
			} else {
				Log.e(TAG, "Cannot create board cache folder. " + (boardDir == null ? "null" : boardDir.getAbsolutePath()));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while loading board '" + boardCode + "' data. ", e);
			if (boardFile != null) {
				boardFile.delete();
            }
		}
		return prepareDefaultBoardData(context, boardCode);
	}
	
	private static ChanBoard prepareDefaultBoardData(Context context, String boardCode) {
		ChanBoard board = ChanBoard.getBoardByCode(context, boardCode).copy();
		int boardImageId = ChanHelper.getImageResourceId(board.link);
		
		ChanThread thread = new ChanThread();
		thread.board = boardCode;
		thread.closed = 0;
		thread.created = new Date();
		thread.images = 1;
		thread.no = -100;
		thread.tim = thread.created.getTime() * 1000;
		thread.tn_w = 240;
		thread.tn_h = 240;
		thread.defData = true;
		
		board.threads = new ChanThread[] { thread };
		board.lastFetched = 0;
		board.defData = true;
		
		return board;
	}

	public static ChanThread loadThreadData(Context context, String boardCode, long threadNo) {
		if (boardCode == null || threadNo <= 0) {
			Log.w(TAG, "Trying to load '" + boardCode + FILE_SEP + threadNo + "' thread! Check stack trace why has it happened.", new Exception());
			return null;
		}
		if (threadCache.containsKey(threadNo)) {
			ChanThread thread = threadCache.get(threadNo);
			if (thread == null) {
				Log.w(TAG, "Null thread " + boardCode + "/" + threadNo + " stored in cache, removing key");
				threadCache.remove(threadNo);
			} else {
				if (DEBUG) Log.i(TAG, "Returning thread " + boardCode + FILE_SEP +  threadNo + " data from cache, posts: " + thread.posts.length);
				return thread;
			}
		}
		File threadFile = null;
		try {
			threadFile = new File(getBoardCacheDirectory(context, boardCode), "t_" + threadNo + CACHE_EXT);
			if (!threadFile.exists()) {
				if (DEBUG) Log.d(TAG, "Thread '" + boardCode + FILE_SEP + threadNo + "' doesn't exist.");
				return prepareDefaultThreadData(context, boardCode, threadNo);
			}
			ObjectMapper mapper = ChanHelper.getJsonMapper();
			ChanThread thread = mapper.readValue(threadFile, ChanThread.class);
			Log.w(TAG, "Loaded thread '" + boardCode + FILE_SEP + threadNo + "' with " + thread.posts.length + " posts");
			return thread;
		} catch (Exception e) {
			Log.w(TAG, "Error while loading thread '" + boardCode + FILE_SEP + threadNo + "' data. ", e);
			return prepareDefaultThreadData(context, boardCode, threadNo);
		}
	}
	
	private static ChanThread prepareDefaultThreadData(Context context, String boardCode, long threadNo) {
		ChanThread thread = new ChanThread();
		thread.board = boardCode;
		thread.closed = 0;
		thread.created = new Date();
		thread.images = 0;
		thread.replies = 0;
		thread.no = threadNo;
		thread.tim = thread.created.getTime() * 1000;
		thread.tn_w = 0;
		thread.tn_h = 0;
		
		ChanPost post = new ChanPost();
		post.no = threadNo;
		post.board = boardCode;
		post.closed = 0;
		post.created = new Date();
		post.images = 0;
		post.no = threadNo;
		post.tim = thread.created.getTime() * 1000;
		post.tn_w = 0;
		post.tn_h = 0;
		post.defData = true;
		
		thread.posts = new ChanPost[] { post };
		thread.lastFetched = 0;
		thread.defData = true;
		
		return thread;
	}
	
	public static void storeUserPreferences(Context context, UserPreferences userPrefs) {
		try {
            File userPrefsFile = getUserPreferencesFile(context);
			if (userPrefsFile != null) {
				try {
					userPrefs.lastStored = new Date();
					ObjectMapper mapper = ChanHelper.getJsonMapper();
					mapper.writeValue(userPrefsFile, userPrefs);
				} catch (Exception e) {
                    Log.e(TAG, "Exception while writing user preferences", e);
                }
				if (DEBUG) Log.i(TAG, "Stored user preferences to file, last updated " + userPrefs.lastUpdate);
			} else {
				Log.e(TAG, "Cannot store user preferences");
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while storing user preferences", e);
		}
	}

	public static UserPreferences loadUserPreferences(Context context) {
		try {
			File userPrefsFile = getUserPreferencesFile(context);
			if (userPrefsFile != null && userPrefsFile.exists()) {
				ObjectMapper mapper = ChanHelper.getJsonMapper();
				UserPreferences userPrefs = mapper.readValue(userPrefsFile, UserPreferences.class);
                if (userPrefs == null) {
                    Log.e(TAG, "Couldn't load user preferences, null returned");
                    return new UserPreferences();
                } else {
				    if (DEBUG) Log.i(TAG, "Loaded user preferences, last updated " + userPrefs.lastUpdate + ", last stored " + userPrefs.lastStored);
					return userPrefs;
                }
			} else {
				Log.w(TAG, "File for user preferences doesn't exist");
				return new UserPreferences();
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while loading user preferences", e);
		}
		return null;
	}

    public static String getBoardWidgetBitmapPath(Context context, String boardName, int index) {
        File boardDir = getBoardCacheDirectory(context, boardName);
        if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
            File boardFile = new File(boardDir, boardName + "_widgetbitmap_" + index + "." + BITMAP_CACHE_EXT);
            return boardFile.getAbsolutePath();
        } else {
            Log.w(TAG, "Board widget bitmap file could not be created: " + boardName);
            return null;
        }
    }

    public static Bitmap getBoardWidgetBitmap(Context context, String boardName, int index) {
        String boardPath = getBoardWidgetBitmapPath(context, boardName, index);
        Bitmap b = BitmapFactory.decodeFile(boardPath);
        return b;
    }

    public static final int BITMAP_BUFFER_SIZE = 512;

    public static long storeBoardWidgetBitmapFile(Context context, String boardName, int index, BufferedInputStream is)
            throws IOException
    {
        long totalBytes = 0;
        FileOutputStream fos = null;
        try {
	        String bitmapPath = getBoardWidgetBitmapPath(context, boardName, index);
	        fos = new FileOutputStream(bitmapPath, false);
	        totalBytes = IOUtils.copy(is, fos);
        } finally {
        	IOUtils.closeQuietly(fos);
        }
        if (DEBUG) Log.i(TAG, "Stored widget bitmap file for board " + boardName + " index " + index + " totalBytes=" + totalBytes);
        return totalBytes;
    }

}
