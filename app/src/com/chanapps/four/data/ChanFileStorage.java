package com.chanapps.four.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.util.WeakHashMap;

import android.content.Context;
import android.util.Log;

import com.chanapps.four.service.UserPreferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class ChanFileStorage {
	public static final String TAG = ChanFileStorage.class.getSimpleName();
	
	private static WeakHashMap<String, ChanBoard> boardCache = new WeakHashMap<String, ChanBoard>();
	private static WeakHashMap<Long, ChanThread> threadCache = new WeakHashMap<Long, ChanThread>();

    private static final String CACHE_ROOT = "Android";
    private static final String CACHE_DATA_DIR = "data";
    private static final String CACHE_PKG_DIR = "cache";
    private static final String FILE_SEP = "/";
    private static final String CACHE_EXT = ".txt";
    private static final String USER_PREFS_FILENAME = "userprefs.txt";

    public static boolean isBoardCachedOnDisk(Context context, String boardCode) {
        File boardDir = getBoardCacheDirectory(context, boardCode);
        return boardDir != null && boardDir.exists();
    }

    public static boolean isThreadCachedOnDisk(Context context, String boardCode, long threadNo) {
        File threadDir = getThreadCacheDirectory(context, boardCode, threadNo);
        return threadDir != null && threadDir.exists();
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

    private static File getThreadCacheDirectory(Context context, String boardCode, long threadNo) {
        String cacheDir = CACHE_ROOT + FILE_SEP + CACHE_DATA_DIR + FILE_SEP + context.getPackageName() + FILE_SEP
                + CACHE_PKG_DIR + FILE_SEP + boardCode + FILE_SEP + threadNo;
        File threadDir = StorageUtils.getOwnCacheDirectory(context, cacheDir);
        return threadDir;
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

    public static void storeBoardData(Context context, ChanBoard board) {
		try {
			boardCache.put(board.link, board);
            File boardDir = getBoardCacheDirectory(context, board.link);
			if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
				FileWriter writer = new FileWriter(new File(boardDir, board.link + CACHE_EXT), false);
				try {
					board.lastFetched = new Date().getTime();
					Gson gson = new GsonBuilder().create();
					gson.toJson(board, writer);
				} finally {
                    try {
					    writer.flush();
					    writer.close();
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Exception while writing and closing board cache:" + e.getMessage(), e);
                    }
				}
				Log.i(TAG, "Stored " + board.threads.length + " threads for board '" + board.link + "'");
			} else {
				Log.e(TAG, "Cannot create board cache folder. " + (boardDir == null ? "null" : boardDir.getAbsolutePath()));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while storing board '" + board.link + "' data. ", e);
		}
	}
	
    public static File storeBoardFile(Context context, String boardName, int page, BufferedReader reader) {
		try {
            File boardDir = getBoardCacheDirectory(context, boardName);
			if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
				File boardFile = new File(boardDir, boardName + "_page" + page + CACHE_EXT);
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
                        Log.e(TAG, "Exception while writing and closing board cache:" + e.getMessage(), e);
                    }
				}
				Log.i(TAG, "Stored file for board " + boardName + " page " + page);
				return boardFile;
			} else {
				Log.e(TAG, "Cannot create board cache folder. " + (boardDir == null ? "null" : boardDir.getAbsolutePath()));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while storing board " + boardName + " page " + page, e);
		}
		return null;
	}
	
    public static File storeThreadFile(Context context, String boardName, long threadNo, BufferedReader reader) {
		try {
            File boardDir = getBoardCacheDirectory(context, boardName);
			if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
				File boardFile = new File(boardDir, "t_" + threadNo + "f" + CACHE_EXT);
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
                        Log.e(TAG, "Exception while writing and closing thread cache:" + e.getMessage(), e);
                    }
				}
				Log.i(TAG, "Stored file for thread " + boardName + "/" + threadNo);
				return boardFile;
			} else {
				Log.e(TAG, "Cannot create board cache folder. " + (boardDir == null ? "null" : boardDir.getAbsolutePath()));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while storing thread " + boardName + "/" + threadNo, e);
		}
		return null;
	}
	
	public static void storeThreadData(Context context, ChanThread thread) {
		try {
			threadCache.put(thread.no, thread);
            File threadDir = getThreadCacheDirectory(context, thread.board, thread.no);
			if (threadDir != null && (threadDir.exists() || threadDir.mkdirs())) {
				FileWriter writer = new FileWriter(new File(threadDir, thread.no + CACHE_EXT), false);
				try {
					thread.lastFetched = new Date().getTime();
					Gson gson = new GsonBuilder().create();
					gson.toJson(thread, writer);
				}
                catch (Exception e) {
                    Log.e(TAG, "Exception while writing thread", e);
                }
                finally {
                    try {
                        writer.flush();
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Exception while flushing thread", e);
                    }
                    try {
                        writer.close();
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Exception while closing thread", e);
                    }
                }
				Log.i(TAG, "Stored " + thread.posts.length + " posts for thread '" + thread.board + FILE_SEP + thread.no + "'");
			} else {
				Log.e(TAG, "Cannot create board cache folder. " + (threadDir == null ? "null" : threadDir.getAbsolutePath()));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while storing thread '" + thread.board + FILE_SEP + thread.no + "' data. ", e);
		}
	}
	
	public static ChanBoard loadBoardData(Context context, String boardCode) {
		if (boardCode == null) {
			Log.w(TAG, "Trying to load 'null' board! Check stack trace why has it happened.", new Exception());
			return null;
		}
		if (boardCache.containsKey(boardCode)) {
			Log.i(TAG, "Retruning board " + boardCode + " data from cache");
			return boardCache.get(boardCode);
		}
		File boardFile = null;
		try {
            File boardDir = getBoardCacheDirectory(context, boardCode);
			if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
				boardFile = new File(boardDir, boardCode + CACHE_EXT);
				if (boardFile != null && boardFile.exists()) {
					FileReader reader = new FileReader(boardFile);
					Gson gson = new GsonBuilder().create();
					ChanBoard board = gson.fromJson(reader, ChanBoard.class);
					Log.i(TAG, "Loaded " + board.threads.length + " threads for board '" + board.link + "'");
					return board;
				} else {
					Log.i(TAG, "File for board '" + boardCode + "' doesn't exist");
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
		return ChanBoard.getBoardByCode(context, boardCode);
	}

	public static ChanThread loadThreadData(Context context, String boardCode, long threadNo) {
		if (boardCode == null || threadNo <= 0) {
			Log.w(TAG, "Trying to load '" + boardCode + FILE_SEP + threadNo + "' thread! Check stack trace why has it happened.", new Exception());
			return null;
		}
		if (threadCache.containsKey(threadNo)) {
			Log.i(TAG, "Retruning thread " + boardCode + FILE_SEP +  threadNo + " data from cache");
			return threadCache.get(threadNo);
		}
		File threadFile = null;
		try {
            File threadDir = getThreadCacheDirectory(context, boardCode, threadNo);
			if (threadDir != null && (threadDir.exists() || threadDir.mkdirs())) {
				threadFile = new File(threadDir, "" + threadNo + CACHE_EXT);
				if (threadFile != null && threadFile.exists()) {
					FileReader reader = new FileReader(threadFile);
					Gson gson = new GsonBuilder().create();
					ChanThread thread = gson.fromJson(reader, ChanThread.class);
                    if (thread == null)
                        Log.e(TAG, "Couldn't load thread, null thread returned for " + boardCode + FILE_SEP + threadNo);
                    else
					    Log.i(TAG, "Loaded " + thread.posts.length + " posts for board '" + boardCode + FILE_SEP + threadNo + "'");
					return thread;
				} else {
					Log.w(TAG, "File for thread '" + boardCode + FILE_SEP + threadNo + "' doesn't exist");
				}
			} else {
				Log.e(TAG, "Cannot create thread cache folder. " + (threadDir == null ? "null" : threadDir.getAbsolutePath()));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while loading thread '" + boardCode + FILE_SEP + threadNo + "' data. ", e);
			if (threadFile != null) {
				threadFile.delete();
            }
		}
		return null;
	}
	
	public static void storeUserPreferences(Context context, UserPreferences userPrefs) {
		try {
            File userPrefsFile = getUserPreferencesFile(context);
			if (userPrefsFile != null) {
				FileWriter writer = new FileWriter(userPrefsFile, false);
				try {
					userPrefs.lastStored = new Date();
					Gson gson = new GsonBuilder().create();
					gson.toJson(userPrefs, writer);
				}
                catch (Exception e) {
                    Log.e(TAG, "Exception while writing user preferences", e);
                }
                finally {
                    try {
                        writer.flush();
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Exception while flushing user preferences", e);
                    }
                    try {
                        writer.close();
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Exception while closing user preferences", e);
                    }
                }
				Log.i(TAG, "Stored user preferences to file, last updated " + userPrefs.lastUpdate);
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
				FileReader reader = new FileReader(userPrefsFile);
				Gson gson = new GsonBuilder().create();
				UserPreferences userPrefs = gson.fromJson(reader, UserPreferences.class);
                if (userPrefs == null) {
                    Log.e(TAG, "Couldn't load user preferences, null returned");
                    return new UserPreferences();
                } else {
				    Log.i(TAG, "Loaded user preferences, last updated " + userPrefs.lastUpdate + ", last stored " + userPrefs.lastStored);
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

}
