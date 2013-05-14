package com.chanapps.four.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.chanapps.four.service.FileSaverService;
import com.chanapps.four.service.FileSaverService.FileType;
import com.chanapps.four.widget.BoardWidgetProvider;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class ChanFileStorage {
	private static final String TAG = ChanFileStorage.class.getSimpleName();
	private static final boolean DEBUG = true;
	
	private static final int MAX_BOARDS_IN_CACHE = 100;
	private static final int MAX_THREADS_IN_CACHE = 200;
	
	@SuppressWarnings("serial")
	private static Map<String, ChanBoard> boardCache = new LinkedHashMap<String, ChanBoard>(MAX_BOARDS_IN_CACHE + 1, .75F, true) {
	    // This method is called just after a new entry has been added
	    public boolean removeEldestEntry(Map.Entry<String, ChanBoard> eldest) {
	        return size() > MAX_BOARDS_IN_CACHE;
	    }
	};
	
	@SuppressWarnings("serial")
	private static Map<String, ChanThread> threadCache = new LinkedHashMap<String, ChanThread>(MAX_THREADS_IN_CACHE + 1, .75F, true) {
	    // This method is called just after a new entry has been added
	    public boolean removeEldestEntry(Map.Entry<String, ChanThread> eldest) {
	        return size() > MAX_THREADS_IN_CACHE;
	    }
	};

    private static final String CACHE_ROOT = "Android";
    private static final String CACHE_DATA_DIR = "data";
    private static final String CACHE_PKG_DIR = "cache";
    private static final String WALLPAPER_DIR = "wallpapers";
    private static final String FILE_SEP = "/";
    private static final String CACHE_EXT = ".txt";
    private static final String WALLPAPER_EXT = ".jpg";
    private static final String USER_STATS_FILENAME = "userstats.txt";

    public static boolean isBoardCachedOnDisk(Context context, String boardCode) {
        File boardDir = getBoardCacheDirectory(context, boardCode);
        return boardDir != null && boardDir.exists();
    }

    private static String getRootCacheDirectory(Context context) {
        return CACHE_ROOT + FILE_SEP
                + CACHE_DATA_DIR + FILE_SEP
                + context.getPackageName() + FILE_SEP
                + CACHE_PKG_DIR ;
    }
    
    public static File getCacheDirectory(Context context) {
        String cacheDir = getRootCacheDirectory(context);
        return StorageUtils.getOwnCacheDirectory(context, cacheDir);
    }

    private static File getWallpaperCacheDirectory(Context context) {
        String cacheDir = getRootCacheDirectory(context) + FILE_SEP + WALLPAPER_DIR;
        return StorageUtils.getOwnCacheDirectory(context, cacheDir);
    }

    public static File getBoardCacheDirectory(Context context, String boardCode) {
        String cacheDir = getRootCacheDirectory(context) + FILE_SEP + boardCode;
        File boardDir = StorageUtils.getOwnCacheDirectory(context, cacheDir);
        return boardDir;
    }

    private static File getUserStatsFile(Context context) {
        String cacheDir = getRootCacheDirectory(context);
        File cacheFolder = StorageUtils.getOwnCacheDirectory(context, cacheDir);
        if (cacheFolder != null) {
        	File userPrefsFile = new File(cacheFolder, USER_STATS_FILENAME);
        	return userPrefsFile;
        } else {
        	Log.e(TAG, "Cache folder returned empty");
        	return null;
        }
    }

    public static void storeBoardData(Context context, ChanBoard board) throws IOException {
    	if (board.defData) {
            Log.i(TAG, "Default data found, not storing board=" + board.link);
    		// default data should never be stored
    		return;
    	}
        ChanBoard cachedBoard = boardCache.get(board.link);
        if (cachedBoard == null) {
            if (DEBUG) Log.i(TAG, "null board cache for board=" + board.link);
            boardCache.put(board.link, board);
        }
        else {
            cachedBoard = board;
            if (DEBUG) Log.i(TAG, "found cached board=" + board.link + " threadCount=" + cachedBoard.threads.length);
        }
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
			if (page >= 0) {
				return new File(boardDir, boardName + "_page" + page + CACHE_EXT);
			} else {
				return new File(boardDir, boardName + "_catalog" + CACHE_EXT);
			}
		} else {
			if (DEBUG) Log.w(TAG, "Board folder could not be created: " + boardName);
			return null;
		}
    }

    public static long storeBoardFile(Context context, String boardName, int page, Reader reader) throws IOException {
		File boardFile = getBoardFile(context, boardName, page);
		FileWriter writer = null;
		try {
			writer = new FileWriter(boardFile, false);
			IOUtils.copy(reader, writer);
		} finally {
            IOUtils.closeQuietly(reader);
            writer.flush();
            IOUtils.closeQuietly(writer);
		}
		if (DEBUG) Log.i(TAG, "Stored file for board " + boardName + (page == -1 ? " catalog" : " page " + page));
		return boardFile.length();
	}
	
    public static File getThreadFile(Context context, String boardName, long threadNo) {
        File boardDir = getBoardCacheDirectory(context, boardName);
		if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
			File boardFile = new File(boardDir, "t_" + threadNo + "f" + CACHE_EXT);
			return boardFile;
		} else {
			if (DEBUG) Log.w(TAG, "Board folder could not be created: " + boardName);
			return null;
		}
    }
	
    public static long storeThreadFile(Context context, String boardName, long threadNo, Reader reader) throws IOException {
		File threadFile = getThreadFile(context, boardName, threadNo);
		FileWriter writer = null;
		try {
			writer = new FileWriter(threadFile, false);
			IOUtils.copy(reader, writer);
		} finally {
            IOUtils.closeQuietly(reader);
            writer.flush();
            IOUtils.closeQuietly(writer);
		}
		if (DEBUG) Log.i(TAG, "Stored file for thread " + boardName + "/" + threadNo);
		return threadFile.length();
	}
    
    public static void resetLastFetched(String boardCode, long threadNo) {
    	ChanThread currentThread = threadCache.get(boardCode + "/" + threadNo);
    	if (currentThread != null) {
    		currentThread.lastFetched = 0;
    	}
    }
    
    public static void resetLastFetched(String boardCode) {
    	ChanBoard currentBoard = boardCache.get(boardCode);
    	if (currentBoard != null) {
    		currentBoard.lastFetched = 0;
    	}
    }
	
	public static void storeThreadData(Context context, ChanThread thread) throws IOException {
    	if (thread.defData) {
    		// default data should never be stored
    		return;
    	}
    	ChanThread currentThread = threadCache.get(thread.board + "/" + thread.no);
    	if (currentThread != null && currentThread.lastFetched > thread.lastFetched) {
            if (DEBUG) Log.i(TAG, "skipping thread cached time=" + currentThread.lastFetched + " newer than storing time=" + thread.lastFetched);
    		return;
    	}
		threadCache.put(thread.board + "/" + thread.no, thread);
        File boardDir = getBoardCacheDirectory(context, thread.board);
		if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
			File threadFile = new File(boardDir, "t_" + thread.no + CACHE_EXT);
			try {
				ObjectMapper mapper = ChanHelper.getJsonMapper();
				mapper.writeValue(threadFile, thread);
			} finally {
			}
			updateWatchedThread(context, thread);
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
            ChanBoard cachedBoard = boardCache.get(boardCode);
            if (cachedBoard != null && cachedBoard.threads != null
                    && cachedBoard.threads.length > 0 && !cachedBoard.defData)
            {
                if (DEBUG) Log.i(TAG, "Returning board " + boardCode
                        + " data from cache threads=" + cachedBoard.threads.length
                        + " loadedthreads=" + cachedBoard.loadedThreads.length
                        + " newThreads=" + cachedBoard.newThreads
                        + " updatedThreads=" + cachedBoard.updatedThreads
                );
                return cachedBoard;
            }
            else {
                if (DEBUG) Log.i(TAG, "Ignoring missing data cached board " + boardCode
                        + " data from cache threads=" + cachedBoard.threads.length
                        + " loadedthreads=" + cachedBoard.loadedThreads.length
                        + " newThreads=" + cachedBoard.newThreads
                        + " updatedThreads=" + cachedBoard.updatedThreads
                        );
            }
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
					boardCache.put(boardCode, board);
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

    public static boolean hasNewBoardData(Context context, String boardCode) {
        ChanBoard board = loadBoardData(context, boardCode);
        return board == null ? false : board.hasNewBoardData();
    }

	public static ChanBoard loadFreshBoardData(Context context, String boardCode) {
        if (DEBUG) Log.i(TAG, "loadFreshBoardData code=" + boardCode);
        ChanBoard board = loadBoardData(context, boardCode);
        if (hasNewBoardData(context, boardCode)) {
            board.swapLoadedThreads();
			FileSaverService.startService(context, FileType.BOARD_SAVE, boardCode);
		}
        return board;
	}

	private static ChanBoard prepareDefaultBoardData(Context context, String boardCode) {
		ChanBoard board = ChanBoard.getBoardByCode(context, boardCode);
        if (board == null) {
            ChanBoard.initBoards(context);
            board = ChanBoard.getBoardByCode(context, boardCode);
        }
        if (board == null)
            return null;
        board = board.copy();
		ChanThread thread = new ChanThread();
		thread.board = boardCode;
		thread.closed = 0;
		thread.created = new Date();
		thread.images = 1;
		thread.no = -100;
		thread.tim = thread.created.getTime() * 1000;
		thread.tn_w = 240;
		thread.tn_h = 240;
		thread.defData = !ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode);
		
		board.threads = new ChanThread[] { thread };
		board.lastFetched = 0;
		
		return board;
	}

	public static ChanThread loadThreadData(Context context, String boardCode, long threadNo) {
		if (boardCode == null || threadNo <= 0) {
			if (DEBUG) Log.w(TAG, "Trying to load '" + boardCode + FILE_SEP + threadNo + "' thread! Check stack trace why has it happened.", new Exception());
			return null;
		}
		if (threadCache.containsKey(boardCode + "/" + threadNo)) {
			ChanThread thread = threadCache.get(boardCode + "/" + threadNo);
			if (thread == null || thread.defData) {
				if (DEBUG) Log.w(TAG, "Null thread " + boardCode + "/" + threadNo + " stored in cache, removing key");
				threadCache.remove(boardCode + "/" + threadNo);
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
				return getThreadFromBoard(context, boardCode, threadNo);
			}
			ObjectMapper mapper = ChanHelper.getJsonMapper();
			ChanThread thread = mapper.readValue(threadFile, ChanThread.class);
            thread.loadedFromBoard = false;
			if (DEBUG) Log.i(TAG, "Loaded thread '" + boardCode + FILE_SEP + threadNo + "' with " + thread.posts.length + " posts");
			return thread;
		} catch (Exception e) {
			if (DEBUG) Log.w(TAG, "Error while loading thread '" + boardCode + FILE_SEP + threadNo + "' data. ", e);
			return getThreadFromBoard(context, boardCode, threadNo);
		}
	}
	
	private static ChanThread getThreadFromBoard(Context context, String boardCode, long threadNo) {
		ChanThread thread = makeFirstThreadFromBoard(context, boardCode, threadNo);
		if (thread == null) {
			thread = makeFirstThreadFromBoard(context, ChanBoard.POPULAR_BOARD_CODE, threadNo);
		}
		if (thread == null) {
			thread = makeFirstThreadFromSpecialBoard(context, ChanBoard.LATEST_BOARD_CODE, threadNo);
		}
		if (thread == null) {
			thread = makeFirstThreadFromSpecialBoard(context, ChanBoard.LATEST_IMAGES_BOARD_CODE, threadNo);
		}
		if (thread == null) {
			thread = prepareDefaultThreadData(context, boardCode, threadNo);
		}
        thread.loadedFromBoard = true;
        return thread;
	}

	private static ChanThread makeFirstThreadFromBoard(Context context, String boardCode, long threadNo) {
		ChanBoard board = loadBoardData(context, boardCode);
		if (board != null && !board.defData && board.threads != null) {
			for (ChanPost post : board.threads) {
				if (post.no == threadNo) {
					ChanThread thread = new ChanThread();
					thread.board = boardCode;
					thread.no = threadNo;
					thread.lastFetched = 0;
					thread.posts = new ChanPost[]{post};
                    thread.closed = post.closed;
                    thread.loadedFromBoard = true;
                    return thread;
				}
			}
		}
		return null;
	}

	private static ChanThread makeFirstThreadFromSpecialBoard(Context context, String boardCode, long threadNo) {
		ChanBoard board = loadBoardData(context, boardCode);
		if (board != null && !board.defData && board.threads != null) {
			for (ChanPost post : board.threads) {
				if (post.no == threadNo) {
					ChanThread thread = new ChanThread();
					thread.board = boardCode;
					thread.tim = post.tim;
					thread.ext = post.ext;
					thread.no = threadNo;
					thread.lastFetched = 0;
					thread.posts = new ChanPost[]{post};
                    thread.closed = post.closed;
                    thread.loadedFromBoard = true;
                    return thread;
				}
			}
		}
		return null;
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
	
	public static void storeUserStats(Context context, UserStatistics userStats) {
		try {
            File userPrefsFile = getUserStatsFile(context);
			if (userPrefsFile != null) {
				try {
					userStats.compactThreads();
					userStats.lastStored = new Date().getTime();
					ObjectMapper mapper = ChanHelper.getJsonMapper();
					mapper.writeValue(userPrefsFile, userStats);
				} catch (Exception e) {
                    Log.e(TAG, "Exception while writing user preferences", e);
                }
				if (DEBUG) Log.i(TAG, "Stored user statistics to file, last updated " + userStats.lastUpdate);
			} else {
				Log.e(TAG, "Cannot store user statistics");
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while storing user statistics", e);
		}
	}

	public static UserStatistics loadUserStats(Context context) {
		try {
			File userStatsFile = getUserStatsFile(context);
			if (userStatsFile != null && userStatsFile.exists()) {
				ObjectMapper mapper = ChanHelper.getJsonMapper();
				UserStatistics userPrefs = mapper.readValue(userStatsFile, UserStatistics.class);
                if (userPrefs == null) {
                    Log.e(TAG, "Couldn't load user statistics, null returned");
                    return new UserStatistics();
                } else {
				    if (DEBUG) Log.i(TAG, "Loaded user statistics, last updated " + userPrefs.lastUpdate + ", last stored " + userPrefs.lastStored);
				    if (userPrefs.convertThreadStats()) {
				    	FileSaverService.startService(context, FileType.USER_STATISTICS);
				    }
					return userPrefs;
                }
			} else {
				if (DEBUG) Log.w(TAG, "File for user statistics doesn't exist");
				return new UserStatistics();
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while loading user statistics", e);
		}
		return new UserStatistics();
	}

    private static final String RM_CMD = "/system/bin/rm -r";

    public static boolean deleteCacheDirectory(Context context) {
        // do this jazz to save widget conf even on clear because you can't programmatically remove widgets
        Set<String> savedWidgetConf = BoardWidgetProvider.getActiveWidgetPref(context);

        try {
            String cacheDir = getRootCacheDirectory(context);
            File cacheFolder = StorageUtils.getOwnCacheDirectory(context, cacheDir);

            String cmd = RM_CMD + " " + cacheFolder.getAbsolutePath();
            if (DEBUG) Log.i(TAG, "Running delete cache command: " + cmd);
            Process process = Runtime.getRuntime().exec(cmd);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            process.waitFor();
            int exitVal = process.exitValue();
            String outputStr = output.toString();
            if (DEBUG) Log.i(TAG, "Finished deleting cache exitValue=" + exitVal + " output=" + outputStr);

            if (exitVal == 0) {
                return true;
            }
            else {
                Log.e(TAG, "Error deleting cache exitValue=" + exitVal + " output=" + outputStr);
                return false;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Exception deleting cache", e);
            return false;
        }
        finally {
            // add back widget conf
            BoardWidgetProvider.saveWidgetBoardPref(context, savedWidgetConf);
        }
    }
    
    public static String getLocalGalleryImageFilename(ChanPost post) {
        return post.board + "_" + post.imageName();
    }

    public static Uri getLocalImageUri(Context context, String boardCode, long postNo, String ext) {
        return Uri.parse("file://" + getBoardCacheDirectory(context, boardCode) + FILE_SEP + postNo + ext);
    }

    public static Uri getLocalImageUri(Context context, ChanPost post) {
        return Uri.parse("file://" + getBoardCacheDirectory(context, post.board) + FILE_SEP + post.imageName());
    }

    public static File createWallpaperFile(Context context) {
        File dir = getWallpaperCacheDirectory(context);
        String name = UUID.randomUUID().toString() + WALLPAPER_EXT;
        return new File(dir, name);
    }

    public static int deletePosts(Context context, String boardCode, long threadNo, long[] postNos, boolean imageOnly)
    {
        ChanThread thread = loadThreadData(context, boardCode, threadNo);
        if (thread == null)
            return 1;
        ChanPost[] posts = thread.posts;
        if (posts == null)
            return 2;

        Set<Long> deletePostNos = new HashSet<Long>(postNos.length);
        for (long postNo : postNos)
            deletePostNos.add(postNo);

        List<ChanPost> postList = new ArrayList<ChanPost>(posts.length);
        for (ChanPost post : posts) {
            boolean found = deletePostNos.contains(post.no);
            if (found && !imageOnly) {
                // don't add it, thus it will be deleted
            }
            else if (found && imageOnly) {
                post.clearImageInfo();
                postList.add(post);
            }
            else {
                postList.add(post);
            }
        }

        ChanPost[] survivingPosts = new ChanPost[postList.size()];
        int i = 0;
        for (ChanPost post: postList)
            survivingPosts[i++] = post;
        thread.posts = survivingPosts;

        try {
            if (DEBUG) Log.i(TAG, "After delete calling storeThreadData for /" + thread.board + "/" + thread.no);
            storeThreadData(context, thread);
        }
        catch (IOException e) {
            Log.e(TAG, "Couldn't store thread data after post delete", e);
            return 4;
        }
        return 0;
    }

    public static void addWatchedThread(Context context, ChanThread thread) throws IOException {
    	ChanBoard board = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
    	if (isThreadWatched(board, thread)) {
    		return;
    	}
    	List<ChanPost> newThreads = new ArrayList<ChanPost>(Arrays.asList(board.threads));
    	newThreads.add(thread);
    	board.threads = newThreads.toArray(new ChanPost[]{});
    	
    	storeBoardData(context, board);
    }
    
    public static void deleteWatchedThread(Context context, ChanThread thread) throws IOException {
    	ChanBoard board = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
    	List<ChanPost> newThreads = new ArrayList<ChanPost>(Arrays.asList(board.threads));
    	for (ChanPost post : board.threads) {
    		if (post.no == thread.no) {
    			newThreads.remove(post);
    		}
    	}
    	board.threads = newThreads.toArray(new ChanPost[]{});
    	
    	storeBoardData(context, board);
    }

    public static void clearWatchedThreads(Context context) throws IOException {
    	ChanBoard board = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
    	board.threads = new ChanPost[]{};
    	
    	storeBoardData(context, board);
    }
    
    private static void updateWatchedThread(Context context, ChanThread watchedThread) throws IOException {
    	ChanBoard board = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
    	for (int i = 0; i < board.threads.length; i++) {
    		ChanPost thread = board.threads[i];
    		if (thread.no == watchedThread.no) {
    			board.threads[i] = watchedThread;
    	    	storeBoardData(context, board);
    		}
    	}
    }
    
	private static boolean isThreadWatched(ChanBoard board, ChanThread thread) {
        if (board == null || board.threads == null)
            return false;
		for (ChanPost post : board.threads) {
			if (post.no == thread.no) {
				return true;
			}
		}
		return false;
	}

    public static boolean isThreadWatched(Context context, ChanThread thread) {
        ChanBoard board = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
        return isThreadWatched(board, thread);
    }
}
