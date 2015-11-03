package com.chanapps.four.data;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.chanapps.four.activity.*;
import com.chanapps.four.component.NotificationComponent;
import com.chanapps.four.service.BoardParserService;
import com.chanapps.four.widget.WidgetProviderUtils;
import com.nostra13.universalimageloader.utils.L;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.chanapps.four.service.FileSaverService;
import com.chanapps.four.service.FileSaverService.FileType;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class ChanFileStorage {
    private static final String TAG = ChanFileStorage.class.getSimpleName();
    private static final boolean DEBUG = false;

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

    private static final String ANDROID_ROOT = "Android";
    private static final String ANDROID_DATA_DIR = "data";
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
        return ANDROID_ROOT + FILE_SEP
                + ANDROID_DATA_DIR + FILE_SEP
                + context.getPackageName() + FILE_SEP
                + CACHE_PKG_DIR;
    }

    private static String getRootPersistentDirectory(Context context) {
        return ANDROID_ROOT + FILE_SEP
                + ANDROID_DATA_DIR + FILE_SEP
                + context.getPackageName();
    }

    public static File getCacheDirectory(Context context) {
        String cacheDir = getRootCacheDirectory(context);
        return StorageUtils.getOwnCacheDirectory(context, cacheDir);
    }

    private static File getPersistentDirectory(Context context) {
        String persistentDir = getRootPersistentDirectory(context);
        return getOwnPersistentDirectory(context, persistentDir);
    }

    private static File getOwnPersistentDirectory(Context context, String persistentDir) {
        File appPersistentDir = null;
        if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            appPersistentDir = new File(Environment.getExternalStorageDirectory(), persistentDir);
        }
        if (appPersistentDir == null || (!appPersistentDir.exists() && !appPersistentDir.mkdirs())) {
            appPersistentDir = context.getFilesDir();
        }
        return appPersistentDir;
    }

    private static File getWallpaperCacheDirectory(Context context) {
        String cacheDir = getRootCacheDirectory(context) + FILE_SEP + WALLPAPER_DIR;
        return StorageUtils.getOwnCacheDirectory(context, cacheDir);
    }

    private static String getLegacyBoardCachePath(Context context, String boardCode) {
        return getRootCacheDirectory(context) + FILE_SEP + boardCode;
    }

    private static File getLegacyBoardCacheFile(Context context, String boardCode) {
        String cacheDir = getLegacyBoardCachePath(context, boardCode);
        File boardDir = null;
        if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            boardDir = new File(Environment.getExternalStorageDirectory(), cacheDir);
        }
        if (boardDir == null || !boardDir.exists()) {
            boardDir = context.getCacheDir();
        }
        File boardFile = boardDir != null && boardDir.exists() ? new File(boardDir, boardCode + CACHE_EXT) : null;
        return boardFile;
    }

    public static File getBoardCacheDirectory(Context context, String boardCode) {
        File boardDir;
        if (ChanBoard.isPersistentBoard(boardCode)) {
            String persistentDir = getRootPersistentDirectory(context) + FILE_SEP + boardCode;
            boardDir = getOwnPersistentDirectory(context, persistentDir);
        }
        else {
            String cacheDir = getRootCacheDirectory(context) + FILE_SEP + boardCode;
            boardDir = StorageUtils.getOwnCacheDirectory(context, cacheDir);
        }
        return boardDir;
    }

    public static File getHiddenBoardCacheDirectory(Context context, String boardCode) {
        final File boardDir = getBoardCacheDirectory(context, boardCode);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!boardDir.exists()) {
                    if (DEBUG) Log.i(TAG, "created board cache directory " + boardDir);
                    return;
                }
                try {
                    File f = new File(boardDir, ".nomedia");
                    if (!f.exists()) {
                        if (!f.createNewFile()) {
                            Log.e(TAG, "couldn't create .nomedia in board cache directory " + boardDir);
                            return;
                        }
                        if (DEBUG) Log.i(TAG, "created .nomedia in board cache directory " + boardDir);
                    }
                    else {
                        if (DEBUG) Log.i(TAG, "file .nomedia already exists in board cache directory " + boardDir);
                    }
                } catch (IOException e) {
                    L.i("Can't create \".nomedia\" file in board cache dir " + boardDir);
                }
            }
        }).start();
        return boardDir;
    }

    private static File getLegacyUserStatsFile(Context context) {
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

    private static File getUserStatsFile(Context context) {
        File cacheFolder = getPersistentDirectory(context);
        if (cacheFolder != null) {
            File userPrefsFile = new File(cacheFolder, USER_STATS_FILENAME);
            return userPrefsFile;
        } else {
            Log.e(TAG, "Cache folder returned empty");
            return null;
        }
    }

    public static void storeBoardData(Context context, ChanBoard board) throws IOException {
        storeBoardData(context, board, -1);
    }

    public static void storeBoardData(Context context, ChanBoard board, long threadNo) throws IOException {
        if (board.defData) {
            Log.i(TAG, "Default data found, not storing board=" + board.link);
            // default data should never be stored
            return;
        }
        File boardDir = getBoardCacheDirectory(context, board.link);
        if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
            ObjectMapper mapper = BoardParserService.getJsonMapper();
            mapper.writeValue(new File(boardDir, board.link + CACHE_EXT), board);
            if (DEBUG) Log.i(TAG, "Stored " + board.threads.length + " threads for board '" + board.link + "'");
            if (!board.isVirtualBoard()) {
                updateWatchedThread(context, board);
            }
            if (DEBUG) Log.i(TAG, "updating board /" + board.link + "/" + (threadNo > -1 ? threadNo : ""));
        } else {
            Log.e(TAG, "Cannot create board cache folder. " + (boardDir == null ? "null" : boardDir.getAbsolutePath()));
        }
        if (!board.isVirtualBoard())
            addMissingWatchedThreads(context, board);
        boardCache.put(board.link, board);
        if (DEBUG) Log.i(TAG, "put cached board=" + board.link + " threadCount=" + board.threads.length);
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

    public static long storeBoardFile(Context context, String boardName, int page, BufferedInputStream stream) throws IOException {
        File boardFile = getBoardFile(context, boardName, page);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(boardFile, false));
            IOUtils.copy(stream, writer);
        } finally {
            IOUtils.closeQuietly(stream);
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

    public static long storeThreadFile(Context context, String boardName, long threadNo, BufferedInputStream stream) throws IOException {
        File threadFile = getThreadFile(context, boardName, threadNo);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(threadFile, false));
            IOUtils.copy(stream, writer);
        } finally {
            IOUtils.closeQuietly(stream);
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
            if (DEBUG)
                Log.i(TAG, "skipping thread cached time=" + currentThread.lastFetched + " newer than storing time=" + thread.lastFetched);
            return;
        }
        threadCache.put(thread.board + "/" + thread.no, thread);
        File boardDir = getBoardCacheDirectory(context, thread.board);
        if (boardDir != null && (boardDir.exists() || boardDir.mkdirs())) {
            File threadFile = new File(boardDir, "t_" + thread.no + CACHE_EXT);
            try {
                ObjectMapper mapper = BoardParserService.getJsonMapper();
                mapper.writeValue(threadFile, thread);
            } finally {
            }
            updateBoardThread(context, thread);
            updateWatchedThread(context, thread);
            if (DEBUG)
                Log.i(TAG, "Stored " + thread.posts.length + " posts for thread '" + thread.board + FILE_SEP + thread.no + "'");
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
                    && cachedBoard.threads.length > 0 && !cachedBoard.defData) {
                if (DEBUG) Log.i(TAG, "Returning board " + boardCode
                        + " data from cache threads=" + cachedBoard.threads.length
                        + " loadedthreads=" + cachedBoard.loadedThreads.length
                        + " newThreads=" + cachedBoard.newThreads
                        + " updatedThreads=" + cachedBoard.updatedThreads
                );
                return cachedBoard;
            } else {
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
                    ObjectMapper mapper = BoardParserService.getJsonMapper();
                    ChanBoard board = mapper.readValue(boardFile, ChanBoard.class);
                    if (DEBUG) Log.i(TAG, "Loaded " + board.threads.length + " threads for board '" + board.link
                            + "' isFile=" + boardFile.isFile() + " size=" + boardFile.length() / 1000 + "KB");
                    /*
                    if (board.hasNewBoardData()) {
                        board.swapLoadedThreads();
                        boardCache.put(boardCode, board);
                        FileSaverService.startService(context, FileType.BOARD_SAVE, boardCode);
                    }
                    else {
                    */
                    if (!board.isVirtualBoard())
                        addMissingWatchedThreads(context, board);
                    boardCache.put(boardCode, board);
                    //}
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
        ChanBoard board = prepareDefaultBoardData(context, boardCode);
        if (board != null && !board.isVirtualBoard())
            addMissingWatchedThreads(context, board);
        return board;
    }

    private static void addMissingWatchedThreads(Context context, ChanBoard board) {
        if (DEBUG) Log.i(TAG, "addMissingWatchedThreads /" + board.link + "/ start #threads = " + board.threads.length);
        ChanBoard watchlist = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
        if (watchlist == null || watchlist.defData
                || watchlist.threads == null || watchlist.threads.length == 0)
            return;
        Set<Long> watchedNos = new HashSet<Long>();
        Map<Long, ChanThread> watchedThreads = new HashMap<Long, ChanThread>();
        Map<Long, ChanThread> watchedLoadedThreads = new HashMap<Long, ChanThread>();
        for (ChanThread thread : watchlist.threads) {
            if (thread.board.equals(board.link)) {
                watchedNos.add(thread.no);
                watchedThreads.put(thread.no, thread);
                watchedLoadedThreads.put(thread.no, thread);
            }
        }
        if (DEBUG) Log.i(TAG, "addMissingWatchedThreads size=" + watchedNos.size());
        if (watchedNos.size() == 0)
            return;
        synchronized (board) {
            for (ChanThread thread : board.threads) {
                if (watchedNos.contains(thread.no))
                    watchedThreads.remove(thread.no);
            }
            for (ChanThread thread : board.loadedThreads) {
                if (watchedNos.contains(thread.no))
                    watchedLoadedThreads.remove(thread.no);
            }
            if (DEBUG) Log.i(TAG, "addMissingWatchedThreads missing size=" + watchedThreads.size());
            // watchedThreads is now the list of missing board threads
            if (watchedThreads.size() > 0) { // add to end of board.threads
                List<ChanThread> threads = new ArrayList<ChanThread>(Arrays.asList(board.threads));
                threads.addAll(watchedThreads.values());
                board.threads = threads.toArray(board.threads);
            }
            if (DEBUG) Log.i(TAG, "addMissingWatchedThreads missing loaded size=" + watchedLoadedThreads.size());
            if (board.loadedThreads.length > 0 && watchedLoadedThreads.size() > 0) { // add to end of board.loadedThreads
                List<ChanThread> threads = new ArrayList<ChanThread>(Arrays.asList(board.loadedThreads));
                threads.addAll(watchedLoadedThreads.values());
                board.loadedThreads = threads.toArray(board.loadedThreads);
            }
        }
        if (DEBUG) Log.i(TAG, "addMissingWatchedThreads /" + board.link + "/ end #threads = " + board.threads.length);
    }

    public static boolean hasNewBoardData(Context context, String boardCode) {
        ChanBoard board = loadBoardData(context, boardCode);
        return board == null ? false : board.hasNewBoardData();
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
        thread.defData = true;

        board.defData = true;
        board.threads = new ChanThread[]{thread};
        board.lastFetched = 0;

        return board;
    }

    public static ChanThread getCachedThreadData(Context context, String boardCode, long threadNo) {
        // WARNING: loads only cached copy of the data
        // data may be stale or thread may be null, handle this situation
        // only call if you are in a non-backgroundable UI mode and must avoid file access
        return threadCache.get(boardCode + "/" + threadNo);
    }

    public static ChanThread loadThreadData(Context context, String boardCode, long threadNo) {
        if (boardCode == null || threadNo <= 0) {
            if (DEBUG)
                Log.w(TAG, "Trying to load '" + boardCode + FILE_SEP + threadNo + "' thread! Check stack trace why has it happened.", new Exception());
            return null;
        }
        if (threadCache.containsKey(boardCode + "/" + threadNo)) {
            ChanThread thread = threadCache.get(boardCode + "/" + threadNo);
            if (thread == null || thread.defData) {
                if (DEBUG) Log.w(TAG, "Null thread " + boardCode + "/" + threadNo + " stored in cache, removing key");
                threadCache.remove(boardCode + "/" + threadNo);
            } else {
                if (DEBUG)
                    Log.i(TAG, "Returning thread " + boardCode + FILE_SEP + threadNo + " data from cache, posts: " + thread.posts.length);
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
            ObjectMapper mapper = BoardParserService.getJsonMapper();
            ChanThread thread = mapper.readValue(threadFile, ChanThread.class);
            thread.loadedFromBoard = false;
            threadCache.put(thread.board + "/" + thread.no, thread);
            if (DEBUG)
                Log.i(TAG, "Loaded thread '" + boardCode + FILE_SEP + threadNo + "' with " + thread.posts.length + " posts detail=" + thread);
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

        thread.posts = new ChanPost[]{post};
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
                    ObjectMapper mapper = BoardParserService.getJsonMapper();
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
            if (userStatsFile != null && userStatsFile.exists() && userStatsFile.canRead() && userStatsFile.length() > 0) {
                ObjectMapper mapper = BoardParserService.getJsonMapper();
                UserStatistics userPrefs = mapper.readValue(userStatsFile, UserStatistics.class);
                if (userPrefs == null) {
                    Log.e(TAG, "Couldn't load user statistics, null returned");
                    return new UserStatistics();
                } else {
                    if (DEBUG)
                        Log.i(TAG, "Loaded user statistics, last updated " + userPrefs.lastUpdate + ", last stored " + userPrefs.lastStored);
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
        //Set<String> savedWidgetConf = WidgetProviderUtils.getActiveWidgetPref(context);
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
            } else {
                Log.e(TAG, "Error deleting cache exitValue=" + exitVal + " output=" + outputStr);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception deleting cache", e);
            return false;
        } finally {
            // add back user data
            //if (savedWidgetConf != null && savedWidgetConf.size() > 0)
            //    WidgetProviderUtils.saveWidgetBoardPref(context, savedWidgetConf);
        }
    }

    public static String getLocalGalleryImageFilename(ChanPost post) {
        return post.board + "_" + post.imageName();
    }

    public static Uri getHiddenLocalImageUri(Context context, String boardCode, long postNo, String ext) {
        return Uri.parse("file://" + getHiddenBoardCacheDirectory(context, boardCode) + FILE_SEP + postNo + ext);
    }

    private static final String CHANU_FOLDER = "Chanu";

    public static File getDownloadFolder(Context context, String boardCode, long threadNo, boolean isSingleImage) {
        String configuredPath = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsActivity.PREF_DOWNLOAD_LOCATION, null);
        String defaultPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + FILE_SEP + CHANU_FOLDER;
        String suffix = getDownloadSubfolder(context, boardCode, threadNo, isSingleImage);
        String downloadPath = configuredPath != null ? configuredPath + suffix : defaultPath + suffix;
        return new File(downloadPath);
    }

    private static String getDownloadSubfolder(Context context, String boardCode, long threadNo, boolean isSingleImage) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SettingsActivity.DownloadImages downloadType = SettingsActivity.DownloadImages.valueOf(prefs.getString(
                SettingsActivity.PREF_DOWNLOAD_IMAGES, SettingsActivity.DownloadImages.STANDARD.toString()));
        switch(downloadType) {
            case ALL_IN_ONE:
                return "";
            case PER_BOARD:
                return FILE_SEP + "board_" + boardCode;
            case PER_THREAD:
//				Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
//				String now = formatter.format(scheduleTime);
                if (threadNo > 0) {
                    return FILE_SEP + boardCode + "_" + threadNo;
                } else {
                    // offline mode doesn't provide thread info so download defaults to PER_BOARD
                    return FILE_SEP + "board_" + boardCode;
                }
            case STANDARD:
            default:
                return (!isSingleImage && boardCode != null && !boardCode.isEmpty() && threadNo > 0)
                        ? FILE_SEP + boardCode + "_" + threadNo
                        : "";
        }
	}

    public static File createWallpaperFile(Context context) {
        File dir = getWallpaperCacheDirectory(context);
        String name = UUID.randomUUID().toString() + WALLPAPER_EXT;
        return new File(dir, name);
    }

    public static int deletePosts(Context context, String boardCode, long threadNo, long[] postNos, boolean imageOnly) {
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
            } else if (found && imageOnly) {
                post.clearImageInfo();
                postList.add(post);
            } else {
                postList.add(post);
            }
        }

        ChanPost[] survivingPosts = new ChanPost[postList.size()];
        int i = 0;
        for (ChanPost post : postList)
            survivingPosts[i++] = post;
        thread.posts = survivingPosts;

        try {
            if (DEBUG) Log.i(TAG, "After delete calling storeThreadData for /" + thread.board + "/" + thread.no);
            storeThreadData(context, thread);
        } catch (IOException e) {
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
        List<ChanPost> newThreads = null;
        if (board.defData || board.threads == null || board.threads.length == 0 || board.threads[0].defData) {
            newThreads = new ArrayList<ChanPost>();
            board.defData = false;
        } else {
            newThreads = new ArrayList<ChanPost>(Arrays.asList(board.threads));
        }
        if (DEBUG) Log.i(TAG, "Before adding to watchlist: " + thread);
        newThreads.add(0, thread.cloneForWatchlist());
        board.threads = newThreads.toArray(new ChanThread[]{});

        if (DEBUG) {
            Log.i(TAG, "After adding to watchlist: " + board.threads[board.threads.length - 1]);
            Log.i(TAG, "After adding to watchlist threads: " + board.threads[0]);
            Log.i(TAG, "After adding to watchlist defData: " + board.threads[0].defData);
        }

        storeBoardData(context, board);
        WidgetProviderUtils.updateAll(context, ChanBoard.WATCHLIST_BOARD_CODE);
    }

    public static void addFavoriteBoard(Context context, ChanThread thread) throws IOException {
        if (DEBUG) Log.i(TAG, "addFavoriteBoard /" + thread.board + "/");
        ChanBoard board = loadBoardData(context, ChanBoard.FAVORITES_BOARD_CODE);
        if (isFavoriteBoard(board, thread)) {
            if (DEBUG) Log.i(TAG, "addFavoriteBoard /" + thread.board + "/ already favorite, exiting");
            return;
        }
        List<ChanPost> newThreads = null;
        if (board.defData || board.threads == null || board.threads.length == 0 || board.threads[0].defData) {
            newThreads = new ArrayList<ChanPost>();
            board.defData = false;
        } else {
            newThreads = new ArrayList<ChanPost>(Arrays.asList(board.threads));
        }
        if (DEBUG) Log.i(TAG, "Before adding to favorites: " + thread);
        newThreads.add(0, thread);
        board.threads = newThreads.toArray(new ChanThread[]{});

        if (DEBUG) {
            Log.i(TAG, "After adding to favorites: " + board.threads[board.threads.length - 1]);
            Log.i(TAG, "After adding to favorites threads: " + board.threads[0]);
            Log.i(TAG, "After adding to favorites defData: " + board.threads[0].defData);
        }
        storeBoardData(context, board);
    }

    public static void deleteWatchedThread(Context context, ChanThread thread) throws IOException {
        ChanBoard board = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
        List<ChanPost> newThreads = new ArrayList<ChanPost>(Arrays.asList(board.threads));
        for (ChanPost post : board.threads) {
            if (post.board != null && post.board.equals(thread.board) && post.no == thread.no) {
                newThreads.remove(post);
            }
        }
        board.threads = newThreads.toArray(new ChanThread[]{});

        storeBoardData(context, board);
        WidgetProviderUtils.updateAll(context, ChanBoard.WATCHLIST_BOARD_CODE);
    }

    public static void deleteFavoritesBoard(Context context, ChanThread thread) throws IOException {
        ChanBoard board = loadBoardData(context, ChanBoard.FAVORITES_BOARD_CODE);
        List<ChanPost> newThreads = new ArrayList<ChanPost>(Arrays.asList(board.threads));
        for (ChanPost post : board.threads) {
            if (post.board != null && post.board.equals(thread.board)) {
                newThreads.remove(post);
            }
        }
        board.threads = newThreads.toArray(new ChanThread[]{});
        storeBoardData(context, board);
    }

    public static void clearWatchedThreads(Context context) throws IOException {
        ChanBoard board = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
        board.threads = new ChanThread[]{};
        storeBoardData(context, board);
        WidgetProviderUtils.updateAll(context, ChanBoard.WATCHLIST_BOARD_CODE);
    }

    public static void cleanDeadWatchedThreads(Context context) throws IOException {
        ChanBoard board = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
        List<ChanThread> cleanedThreads = new ArrayList<ChanThread>();
        for (ChanThread thread : board.threads) {
            if (!thread.isDead)
                cleanedThreads.add(thread);
        }
        board.threads = cleanedThreads.size() == 0 ? new ChanThread[]{} : cleanedThreads.toArray(new ChanThread[cleanedThreads.size()]);
        storeBoardData(context, board);
        WidgetProviderUtils.updateAll(context, ChanBoard.WATCHLIST_BOARD_CODE);
    }

    public static void clearFavorites(Context context) throws IOException {
        ChanBoard board = loadBoardData(context, ChanBoard.FAVORITES_BOARD_CODE);
        board.threads = new ChanThread[]{};
        storeBoardData(context, board);
    }

    private static void updateBoardThread(Context context, ChanThread loadedThread) throws IOException {
        // store updated status into board thread record
        ChanBoard board = loadBoardData(context, loadedThread.board);
        if (board == null || board.threads == null)
            return;
        int found = -1;
        for (int i = 0; i < board.threads.length; i++) {
            if (board.threads[i] != null && board.threads[i].no == loadedThread.no) {
                found = i;
                break;
            }
        }
        if (found >= 0) {
            if (DEBUG) Log.i(TAG, "updateBoardThread found thread=[" + board.threads[found] + "] merging=[" + loadedThread + "]");
            board.threads[found].copyUpdatedInfoFields(loadedThread);
            storeBoardData(context, board, board.threads[found].no);
        }
    }

    private static void updateWatchedThread(Context context, ChanThread loadedThread) throws IOException {
        ChanBoard watchlistBoard = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
        for (int i = 0; i < watchlistBoard.threads.length; i++) {
            ChanThread watchedThread = watchlistBoard.threads[i];
            if (watchedThread.no == loadedThread.no && watchedThread.board.equals(loadedThread.board)) {
                NotificationComponent.notifyNewReplies(context, watchedThread, loadedThread);
                watchlistBoard.threads[i].updateThreadData(loadedThread);
                if (DEBUG) Log.i(TAG, "Updating watched thread " + watchedThread.board + "/" + watchedThread.no
                        + " replies: " + watchedThread.replies + " images: " + watchedThread.images);
                storeBoardData(context, watchlistBoard);
                BoardActivity.refreshWatchlist(context);
            }
        }
    }

    private static void updateWatchedThread(Context context, ChanBoard loadedBoard) throws IOException {
        if (loadedBoard.defData || loadedBoard.loadedThreads == null || loadedBoard.loadedThreads.length == 0
                || loadedBoard.loadedThreads[0].defData) {
            return;
        }
        ChanBoard watchlist = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
        if (watchlist == null || watchlist.threads == null || watchlist.threads.length == 0)
            return;
        boolean updateWatchlist = false;
        for (int i = 0; i < watchlist.threads.length; i++) {
            ChanThread watchedThread = watchlist.threads[i];
            if (watchedThread.board.equals(loadedBoard.link)) {
                for (int t = 0; t < loadedBoard.loadedThreads.length; t++) {
                    ChanThread loadedThread = loadedBoard.loadedThreads[t];
                    if (watchedThread.no == loadedThread.no) {
                        NotificationComponent.notifyNewReplies(context, watchedThread, loadedThread);
                        watchedThread.updateThreadDataWithPost(loadedThread);
                        if (DEBUG) Log.i(TAG, "Updating watched thread " + watchedThread.board + "/" + watchedThread.no
                                + " replies: " + watchedThread.replies + " images: " + watchedThread.images);
                        updateWatchlist = true;
                    }
                }
            }
        }
        if (updateWatchlist) {
            storeBoardData(context, watchlist);
            if (PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getBoolean(SettingsActivity.PREF_AUTOMATICALLY_MANAGE_WATCHLIST, true))
                ChanFileStorage.cleanDeadWatchedThreads(context);
            BoardActivity.refreshWatchlist(context);
        }
    }

    private static boolean isThreadWatched(ChanBoard board, ChanThread thread) {
        if (board == null || board.threads == null || thread == null)
            return false;
        for (ChanPost post : board.threads) {
            if (post.board != null && post.board.equals(thread.board) && post.no == thread.no) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFavoriteBoard(ChanBoard board, ChanThread thread) {
        if (board == null || board.threads == null)
            return false;
        for (ChanPost post : board.threads) {
            if (post.board != null && post.board.equals(thread.board)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isThreadWatched(Context context, ChanThread thread) {
        ChanBoard board = loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
        return isThreadWatched(board, thread);
    }

    public static void migrateIfNecessary(Context context) {
        migrateUserStats(context);
        migrateBoard(context, ChanBoard.WATCHLIST_BOARD_CODE);
        migrateBoard(context, ChanBoard.FAVORITES_BOARD_CODE);
    }
    
    private static void migrateUserStats(Context context) {
        File legacyUserStatsFile = getLegacyUserStatsFile(context);
        if (legacyUserStatsFile != null && legacyUserStatsFile.exists()) {
            File userStatsDir = getPersistentDirectory(context);
            moveFileToDir(legacyUserStatsFile, userStatsDir);
        }
    }
    
    private static void migrateBoard(Context context, String boardCode) {
        File legacyBoardFile = getLegacyBoardCacheFile(context, boardCode);
        if (legacyBoardFile == null || !legacyBoardFile.exists())
            return;
        File boardDir = getBoardCacheDirectory(context, boardCode);
        if (!moveFileToDir(legacyBoardFile, boardDir))
            return;
        String cacheDir = getLegacyBoardCachePath(context, boardCode);
        if (!Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return;
        File legacyBoardDir = new File(Environment.getExternalStorageDirectory(), cacheDir);
        if (legacyBoardDir == null || !legacyBoardDir.exists())
            return;
        legacyBoardDir.delete();
    }
    
    private static boolean moveFileToDir(File sourceFile, File destDir) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel in = null;
        FileChannel out = null;

        File destFile = null;
        try
        {
            destFile = new File(destDir.getAbsolutePath() + FILE_SEP + sourceFile.getName());
            if (!destFile.createNewFile())
                return false;

            fis = new FileInputStream(sourceFile);
            fos = new FileOutputStream(destFile);
            in = fis.getChannel();
            out = fos.getChannel();

            long size = in.size();
            long bytes = in.transferTo(0, size, out);
            if (bytes < size) { // transfer failed
                Log.e(TAG, "didn't transfer full size of file " + sourceFile + " to " + destDir + ", deleting");
                destFile.delete();    
                return false;
            }
            if (!destFile.exists()) // transfer failed
                return false;
            sourceFile.delete();
            if (sourceFile.exists()) // delete failed
                return false;
            return true;
        }
        catch (Throwable e)
        {
            Log.e(TAG, "Exception moving file " + sourceFile + " to " + destDir);
            if (destFile != null)
                destFile.delete();
            return false;
        }
        finally
        {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(fis);
        }
    }

}
