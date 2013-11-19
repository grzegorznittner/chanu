/**
 *
 */
package com.chanapps.four.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.CacheSizePreference;
import com.chanapps.four.data.*;
import com.chanapps.four.widget.AbstractBoardWidgetProvider;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 */
public class CleanUpService extends BaseChanService {
    protected static final String TAG = CleanUpService.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final long MIN_DELAY_BETWEEN_CLEANUPS = 4 * 60 * 1000; // 2min
    private static final long MAX_DELAY_BETWEEN_CLEANUPS = 30 * 60 * 1000; // 10min
    private static long scheduleDelay = MIN_DELAY_BETWEEN_CLEANUPS;
    private static long lastScheduled = Calendar.getInstance().getTimeInMillis();

    public static void startService(Context context) {
        if (lastScheduled > Calendar.getInstance().getTimeInMillis() - scheduleDelay) {
            if (DEBUG) Log.w(TAG, "Cleanup service was called less than " + (scheduleDelay / 1000) + "s ago");
            return;
        }
        scheduleDelay += MIN_DELAY_BETWEEN_CLEANUPS;
        if (scheduleDelay > MAX_DELAY_BETWEEN_CLEANUPS) {
        	scheduleDelay = MIN_DELAY_BETWEEN_CLEANUPS;
        }
        if (DEBUG) Log.w(TAG, "Scheduling clean up service");
        lastScheduled = Calendar.getInstance().getTimeInMillis();
        Intent intent = new Intent(context, CleanUpService.class);
        intent.putExtra(PRIORITY_MESSAGE_FETCH, 1);
        context.startService(intent);
    }

    private File cacheFolder = null;
    private List<FileDesc> otherFiles = null;
    private Map<String, Long> sizeByBoard = null;
    private Map<String, List<FileDesc>> filesByBoard = null;
    private long sizeOfWidget = 0;
    private List<FileDesc> filesOfWidget = null;
    List<String> watchedOrTopBoardCode = new ArrayList<String>();
    Map<String, List<Long>> watchedThreads = new HashMap<String, List<Long>>();

    private long targetCacheSize = 0;
    private long totalSize = 0;
    private int totalFiles = 0;
    private int totalDeletedFiles = 0;
    private long otherSize = 0;

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
            cacheFolder = ChanFileStorage.getCacheDirectory(context);
            if (DEBUG) Log.w(TAG, "Cache dir: " + cacheFolder.getAbsolutePath());
            otherFiles = new ArrayList<FileDesc>();
            sizeByBoard = new HashMap<String, Long>();
            filesByBoard = new HashMap<String, List<FileDesc>>();
            sizeOfWidget = 0;
            filesOfWidget = null;

            totalSize = 0;
            totalFiles = 0;
            totalDeletedFiles = 0;
            otherSize = 0;

            scanFiles();

            long endTime = Calendar.getInstance().getTimeInMillis();
            logCacheFileInfo("ClearUp init.", startTime, endTime);
            if (DEBUG) {
                toastUI("" + totalFiles + " files of size " + (totalSize / 1024)
                        + " kB. Calculated in " + ((endTime - startTime) / 1000) + "s.");
            }

            startTime = Calendar.getInstance().getTimeInMillis();
            long maxCacheSize = getPreferredCacheSize() * 1024 * 1024;
            if (DEBUG) Log.w(TAG, "Preferred cache size is " + (maxCacheSize / (1024 * 1024)) + "MB");
            targetCacheSize = maxCacheSize * 80 / 100;

            if (totalSize > targetCacheSize) {
                // delete other files older than 3 days
                int numDeletedFiles = trimByDate(otherFiles, 3L * 24L * 60L * 60L * 1000L);
                totalDeletedFiles += numDeletedFiles;
                if (DEBUG) Log.w(TAG, "Deleted " + numDeletedFiles + " 'other' files.");

                // clean up non top nor watched boards first
                for (String board : filesByBoard.keySet()) {
                    if (totalSize < targetCacheSize) {
                        break;
                    }
                    if (watchedOrTopBoardCode.contains(board)) {
                        continue;
                    }

                    List<FileDesc> boardFiles = filesByBoard.get(board);
                    // delete old files first
                    numDeletedFiles = trimByDate(boardFiles, 3L * 24L * 60L * 60L * 1000L);
                    if (DEBUG && numDeletedFiles > 0)
                        Log.i(TAG, "Deleted " + numDeletedFiles + " old files from board " + board);
                    if (totalSize < targetCacheSize) {
                        break;
                    }
                    // then by file size
                    numDeletedFiles = trimBySize(boardFiles, 300 * 1024); // delete files larger than 300kB
                    if (DEBUG && numDeletedFiles > 0)
                        Log.i(TAG, "Deleted " + numDeletedFiles + " large files from board " + board);
                    totalDeletedFiles += numDeletedFiles;
                }

                // clean up top or watched boards except thread files
                for (String board : watchedOrTopBoardCode) {
                    if (totalSize < targetCacheSize) {
                        break;
                    }
                    List<FileDesc> preBoardFiles = filesByBoard.get(board);
                    List<Long> threads = watchedThreads.get(board);
                    List<FileDesc> boardFiles = new ArrayList<FileDesc>();
                    for (FileDesc d : preBoardFiles) {
                        String THREAD_FILE_PATTERN = ".*/t_([0-9]+)\\.txt";
                        Pattern threadFilePattern = Pattern.compile(THREAD_FILE_PATTERN);
                        Matcher m = threadFilePattern.matcher(d.path);
                        if (!m.matches()) {
                            boardFiles.add(d); // not a thread file
                            continue;
                        }
                        long threadNo = 0;
                        try {
                            String threadStr = m.group(1);
                            threadNo = Long.valueOf(threadNo);
                            if (!threads.contains(threadNo))
                                boardFiles.add(d);
                        }
                        catch (IllegalStateException e) {
                            Log.i(TAG, "bad match thread number, adding file to cleanup: " + d.path, e);
                            boardFiles.add(d);
                            continue;
                        }
                        catch (NumberFormatException e) {
                            Log.i(TAG, "unmatched thread number, adding file to cleanup: " + d.path, e);
                            boardFiles.add(d);
                            continue;
                        }
                    }

                    // delete old files first
                    numDeletedFiles = trimByDate(boardFiles, 5L * 24L * 60L * 60L * 1000L);
                    if (DEBUG && numDeletedFiles > 0)
                        Log.i(TAG, "Deleted " + numDeletedFiles + " old files from board " + board);
                    if (totalSize < targetCacheSize) {
                        break;
                    }
                    // then by file size
                    numDeletedFiles = trimBySize(boardFiles, 500 * 1024);
                    if (DEBUG && numDeletedFiles > 0)
                        Log.i(TAG, "Deleted " + numDeletedFiles + " large files from board " + board);
                    totalDeletedFiles += numDeletedFiles;
                }

                // clean up top or watched boards
                for (String board : watchedOrTopBoardCode) {
                    if (totalSize < targetCacheSize) {
                        break;
                    }
                    List<FileDesc> boardFiles = filesByBoard.get(board);
                    // delete old files first
                    numDeletedFiles = trimByDate(boardFiles, 5L * 24L * 60L * 60L * 1000L);
                    if (DEBUG && numDeletedFiles > 0)
                        Log.i(TAG, "Deleted " + numDeletedFiles + " old files from board " + board);
                    if (totalSize < targetCacheSize) {
                        break;
                    }
                    // then by file size
                    numDeletedFiles = trimBySize(boardFiles, 500 * 1024);
                    if (DEBUG && numDeletedFiles > 0)
                        Log.i(TAG, "Deleted " + numDeletedFiles + " large files from board " + board);
                    totalDeletedFiles += numDeletedFiles;
                }

                // finally clean widgets if we absolutely have to
                if (totalSize > targetCacheSize
                        && sizeOfWidget > 0 && filesOfWidget != null && filesOfWidget.size() > 0) {
                    List<FileDesc> widgetFiles = filesOfWidget;
                    numDeletedFiles = trimByDate(widgetFiles, 0);
                    if (DEBUG && numDeletedFiles > 0) Log.i(TAG, "Deleted " + numDeletedFiles + " files from widgets");
                    totalDeletedFiles += numDeletedFiles;
                }
            }

            endTime = Calendar.getInstance().getTimeInMillis();
            logCacheFileInfo("Deletion report.", startTime, endTime);
            if (totalDeletedFiles > 0) {
                toastUI("" + totalFiles + " files of size " + (totalSize / 1024) + " kB."
                        + totalDeletedFiles + " files has been deleted in " + ((endTime - startTime) / 1000) + "s.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in clean up service", e);
        }
    }

    private int getPreferredCacheSize() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int prefSize = prefs.getInt(SettingsActivity.PREF_CACHE_SIZE, CacheSizePreference.DEFAULT_VALUE);
        return prefSize < CacheSizePreference.DEFAULT_VALUE ? CacheSizePreference.DEFAULT_VALUE : prefSize;
    }

    private void prepareTopWatchedBoards(Context context) {
        watchedOrTopBoardCode = new ArrayList<String>();
        watchedThreads = new HashMap<String, List<Long>>();
        ChanBoard board = ChanFileStorage.loadBoardData(context, ChanBoard.WATCHLIST_BOARD_CODE);
        if (board != null && board.threads != null) {
            for (ChanPost thread : board.threads) {
                if (!watchedOrTopBoardCode.contains(thread.board)) {
                    watchedOrTopBoardCode.add(thread.board);
                }
                if (!watchedThreads.containsKey(thread.board))
                    watchedThreads.put(thread.board, new ArrayList<Long>());
                if (!watchedThreads.get(thread.board).contains(thread.no))
                    watchedThreads.get(thread.board).add(thread.no);
            }
        }
        UserStatistics userStats = NetworkProfileManager.instance().getUserStatistics();
        for (ChanBoardStat stat : userStats.topBoards()) {
            if (!watchedOrTopBoardCode.contains(stat.board)) {
                watchedOrTopBoardCode.add(stat.board);
            }
        }
        board = ChanFileStorage.loadBoardData(context, ChanBoard.FAVORITES_BOARD_CODE);
        if (board != null && board.threads != null) {
            for (ChanPost thread : board.threads) {
                if (!watchedOrTopBoardCode.contains(thread.board)) {
                    watchedOrTopBoardCode.add(thread.board);
                }
            }
        }
    }

    private void logCacheFileInfo(String logMsg, long startTime, long endTime) {
        if (DEBUG) {
            if (DEBUG)
                Log.i(TAG, logMsg + " Cache folder contains " + totalFiles + " files of size " + (totalSize / 1024)
                        + " kB. Calculated in " + (endTime - startTime) + "ms.");
            for (Map.Entry<String, Long> entry : sizeByBoard.entrySet()) {
                if (filesByBoard.get(entry.getKey()).size() == 0) {
                    continue;
                }
                if (DEBUG) Log.i(TAG, "Board " + entry.getKey() + " size " + (entry.getValue() / 1024) + "kB "
                        + filesByBoard.get(entry.getKey()).size() + " files.");
            }
            if (DEBUG) Log.i(TAG, "Other files' size " + (otherSize / 1024) + "kB "
                    + otherFiles.size() + " files.");
        }
    }

    private int trimByDate(List<FileDesc> files, long timeOffset) {
        if (files == null || files.size() == 0) {
            return 0;
        }
        Collections.sort(files, new Comparator<FileDesc>() {
            public int compare(FileDesc o1, FileDesc o2) {
                return o1.lastModified > o2.lastModified ? 1
                        : o1.lastModified < o2.lastModified ? -1 : 0;
            }
        });
        int i = 0;
        long olderThan3Days = new Date().getTime() - timeOffset;
        for (FileDesc file : files.toArray(new FileDesc[]{})) {
            if (olderThan3Days > file.lastModified) {
                new File(file.path).delete();
                totalSize -= file.size;
                files.remove(file);
                i++;
            }
            if (totalSize < targetCacheSize) {
                break;
            }
        }
        return i;
    }

    private int trimBySize(List<FileDesc> files, long size) {
        if (files == null || files.size() == 0) {
            return 0;
        }
        Collections.sort(files, new Comparator<FileDesc>() {
            public int compare(FileDesc o1, FileDesc o2) {
                return o1.size > o2.size ? -1
                        : o1.size < o2.size ? 1 : 0;
            }
        });
        int i = 0;
        for (FileDesc file : files.toArray(new FileDesc[]{})) {
            if (size < file.size) {
                new File(file.path).delete();
                totalSize -= file.size;
                files.remove(file);
                i++;
            }
            if (totalSize < targetCacheSize) {
                break;
            }
        }
        return i;
    }

    private void scanFiles() {
        File[] children = cacheFolder.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    if (ChanBoard.getBoardByCode(getBaseContext(), child.getName()) != null) {
                        List<FileDesc> boardData = new ArrayList<FileDesc>();
                        long boardSize = addFiles(child, boardData);

                        sizeByBoard.put(child.getName(), boardSize);
                        filesByBoard.put(child.getName(), boardData);
                        totalSize += boardSize;
                        totalFiles += boardData.size();
                    } else if (AbstractBoardWidgetProvider.WIDGET_CACHE_DIR.equals(child.getName())) {
                        List<FileDesc> widgetData = new ArrayList<FileDesc>();
                        long widgetSize = addFiles(child, widgetData);
                        sizeOfWidget = widgetSize;
                        filesOfWidget = widgetData;
                        totalSize += widgetSize;
                        totalFiles += widgetData.size();
                    } else {
                        long folderSize = addFiles(child, otherFiles);
                        otherSize += folderSize;
                        totalSize += folderSize;
                    }
                } else {
                    FileDesc desc = new FileDesc(child);
                    totalSize += desc.size;
                    otherSize += desc.size;
                    otherFiles.add(desc);
                }
            }
        }
        totalFiles += otherFiles.size();
    }

    private long addFiles(File file, List<FileDesc> all) {
        long totalSize = 0;
        if (DEBUG) Log.i(TAG, "Checking folder " + file.getAbsolutePath());
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    totalSize += addFiles(child, all);
                } else {
                    FileDesc desc = new FileDesc(child);
                    totalSize += desc.size;
                    all.add(desc);
                }
            }
        }
        return totalSize;
    }
}
