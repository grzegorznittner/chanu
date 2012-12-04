package com.chanapps.four.data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.util.WeakHashMap;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class ChanFileStorage {
	public static final String TAG = ChanFileStorage.class.getSimpleName();
	
	private static WeakHashMap<String, ChanBoard> boardCache = new WeakHashMap<String, ChanBoard>();
	private static WeakHashMap<Long, ChanThread> threadCache = new WeakHashMap<Long, ChanThread>();
	
	public static void storeBoardData(Context context, ChanBoard board) {
		try {
			boardCache.put(board.link, board);
			String cacheDir = "Android/data/" + context.getPackageName() + "/cache/" + board.link;
			File boardDir = StorageUtils.getOwnCacheDirectory(context, cacheDir);
			if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
				FileWriter writer = new FileWriter(new File(boardDir, board.link + ".txt"), false);
				try {
					board.lastFetched = new Date().getTime();
					Gson gson = new GsonBuilder().create();
					gson.toJson(board, writer);
				} finally {
					writer.flush();
					writer.close();
				}
				Log.w(TAG, "Stored " + board.threads.length + " threads for board '" + board.link + "'");
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
			String cacheDir = "Android/data/" + context.getPackageName() + "/cache/" + thread.board + "/" + thread.no;
			File boardDir = StorageUtils.getOwnCacheDirectory(context, cacheDir);
			if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
				FileWriter writer = new FileWriter(new File(boardDir, thread.no + ".txt"), false);
				try {
					thread.lastFetched = new Date().getTime();
					Gson gson = new GsonBuilder().create();
					gson.toJson(thread, writer);
				} finally {
					writer.flush();
					writer.close();
				}
				Log.w(TAG, "Stored " + thread.posts.length + " posts for thread '" + thread.board + "/" + thread.no + "'");
			} else {
				Log.e(TAG, "Cannot create board cache folder. " + (boardDir == null ? "null" : boardDir.getAbsolutePath()));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while storing thread '" + thread.board + "/" + thread.no + "' data. ", e);
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
		try {
			String cacheDir = "Android/data/" + context.getPackageName() + "/cache/" + boardCode;
			File boardDir = StorageUtils.getOwnCacheDirectory(context, cacheDir);
			if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
				File boardFile = new File(boardDir, boardCode + ".txt");
				if (boardFile != null && boardFile.exists()) {
					FileReader reader = new FileReader(boardFile);
					Gson gson = new GsonBuilder().create();
					ChanBoard board = gson.fromJson(reader, ChanBoard.class);
					Log.w(TAG, "Loaded " + board.threads.length + " threads for board '" + board.link + "'");
					return board;
				} else {
					Log.w(TAG, "File for board '" + boardCode + "' doesn't exist");
				}
			} else {
				Log.e(TAG, "Cannot create board cache folder. " + (boardDir == null ? "null" : boardDir.getAbsolutePath()));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while loading board '" + boardCode + "' data. ", e);
		}
		return ChanBoard.getBoardByCode(context, boardCode);
	}
	
	public static ChanThread loadThreadData(Context context, String boardCode, long threadNo) {
		if (boardCode == null || threadNo <= 0) {
			Log.w(TAG, "Trying to load '" + boardCode + "/" + threadNo + "' thread! Check stack trace why has it happened.", new Exception());
			return null;
		}
		if (threadCache.containsKey(threadNo)) {
			Log.i(TAG, "Retruning thread " + boardCode + "/" +  threadNo + " data from cache");
			return threadCache.get(threadNo);
		}
		try {
			String cacheDir = "Android/data/" + context.getPackageName() + "/cache/" + boardCode + "/" + threadNo;
			File boardDir = StorageUtils.getOwnCacheDirectory(context, cacheDir);
			if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
				File boardFile = new File(boardDir, "" + threadNo + ".txt");
				if (boardFile != null && boardFile.exists()) {
					FileReader reader = new FileReader(boardFile);
					Gson gson = new GsonBuilder().create();
					ChanThread thread = gson.fromJson(reader, ChanThread.class);
					Log.w(TAG, "Loaded " + thread.posts.length + " posts for board '" + boardCode + "/" + threadNo + "'");
					return thread;
				} else {
					Log.w(TAG, "File for thread '" + boardCode + "/" + threadNo + "' doesn't exist");
				}
			} else {
				Log.e(TAG, "Cannot create thread cache folder. " + (boardDir == null ? "null" : boardDir.getAbsolutePath()));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while loading thread '" + boardCode + "/" + threadNo + "' data. ", e);
		}
		return null;
	}

}
