package com.chanapps.four.data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.util.WeakHashMap;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chanapps.four.service.BoardLoadService;
import com.chanapps.four.service.ThreadLoadService;
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

    public static boolean isBoardCachedOnDisk(Context context, String boardCode) {
        File boardDir = getBoardCacheDirectory(context, boardCode);
        return boardDir != null && boardDir.exists();
    }

    public static boolean isThreadCachedOnDisk(Context context, String boardCode, long threadNo) {
        File threadDir = getThreadCacheDirectory(context, boardCode, threadNo);
        return threadDir != null && threadDir.exists();
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

}
