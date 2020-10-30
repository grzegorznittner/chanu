/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.LruCache;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DownloadEntry.Columns;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.ThreadPool.CancelListener;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.nostra13.universalimageloader.utils.L;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.WeakHashMap;

public class DownloadCache {
    private static final String TAG = "DownloadCache";
    private static final int MAX_DELETE_COUNT = 16;
    private static final int LRU_CAPACITY = 4;

    private static final String TABLE_NAME = DownloadEntry.SCHEMA.getTableName();

    private static final String[] QUERY_PROJECTION = {Columns.ID, Columns.DATA};
    private static final String WHERE_HASH_AND_URL = String.format("%s = ? AND %s = ?", Columns.HASH_CODE, Columns.CONTENT_URL);
    private static final int QUERY_INDEX_ID = 0;
    private static final int QUERY_INDEX_DATA = 1;

    private static final String[] FREESPACE_PROJECTION = {Columns.ID, Columns.DATA, Columns.CONTENT_URL, Columns.CONTENT_SIZE};
    private static final String FREESPACE_ORDER_BY = String.format("%s ASC", Columns.LAST_ACCESS);
    private static final int FREESPACE_IDNEX_ID = 0;
    private static final int FREESPACE_IDNEX_DATA = 1;
    private static final int FREESPACE_INDEX_CONTENT_URL = 2;
    private static final int FREESPACE_INDEX_CONTENT_SIZE = 3;

    private static final String ID_WHERE = Columns.ID + " = ?";

    private static final String[] SUM_PROJECTION = {String.format("sum(%s)", Columns.CONTENT_SIZE)};
    private static final int SUM_INDEX_SUM = 0;

    private final LruCache<String, Entry> mEntryMap = new LruCache<String, Entry>(LRU_CAPACITY);
    private final HashMap<String, DownloadTask> mTaskMap = new HashMap<String, DownloadTask>();
    private final File mRoot;
    private final GalleryApp mApplication;
    private final SQLiteDatabase mDatabase;
    private final long mCapacity;

    private long mTotalBytes = 0;
    private boolean mInitialized = false;
    private WeakHashMap<Object, Entry> mAssociateMap = new WeakHashMap<Object, Entry>();

    public DownloadCache(GalleryApp application, File root, long capacity) {
        mRoot = Utils.checkNotNull(root);
        mApplication = Utils.checkNotNull(application);
        mCapacity = capacity;
        mDatabase = new DatabaseHelper(application.getAndroidContext()).getWritableDatabase();
    }

    private Entry findEntryInDatabase(String stringUrl) {
        long hash = Utils.crc64Long(stringUrl);
        String[] whereArgs = {String.valueOf(hash), stringUrl};
        Cursor cursor = mDatabase.query(TABLE_NAME, QUERY_PROJECTION, WHERE_HASH_AND_URL, whereArgs, null, null, null);
        try {
            if (cursor.moveToNext()) {
                File file = new File(cursor.getString(QUERY_INDEX_DATA));
                long id = cursor.getInt(QUERY_INDEX_ID);
                Entry entry = null;
                synchronized (mEntryMap) {
                    entry = mEntryMap.get(stringUrl);
                    if (entry == null) {
                        entry = new Entry(id, file);
                        mEntryMap.put(stringUrl, entry);
                    }
                }
                return entry;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public Entry lookup(URL url) {
        if (!mInitialized) initialize();
        String stringUrl = url.toString();

        // First find in the entry-pool
        synchronized (mEntryMap) {
            Entry entry = mEntryMap.get(stringUrl);
            if (entry != null) {
                updateLastAccess(entry.mId);
                return entry;
            }
        }

        // Then, find it in database
        TaskProxy proxy = new TaskProxy();
        synchronized (mTaskMap) {
            Entry entry = findEntryInDatabase(stringUrl);
            if (entry != null) {
                updateLastAccess(entry.mId);
                return entry;
            }
        }
        return null;
    }

    public Entry download(JobContext jc, URL url) {
        if (!mInitialized) initialize();

        String stringUrl = url.toString();

        // First find in the entry-pool
        synchronized (mEntryMap) {
            Entry entry = mEntryMap.get(stringUrl);
            if (entry != null) {
                updateLastAccess(entry.mId);
                return entry;
            }
        }

        // Then, find it in database
        TaskProxy proxy = new TaskProxy();
        synchronized (mTaskMap) {
            Entry entry = findEntryInDatabase(stringUrl);
            if (entry != null) {
                updateLastAccess(entry.mId);
                return entry;
            }

            // Finally, we need to download the file ....
            // First check if we are downloading it now ...
            DownloadTask task = mTaskMap.get(stringUrl);
            if (task == null) { // if not, start the download task now
                task = new DownloadTask(stringUrl);
                mTaskMap.put(stringUrl, task);
                task.mFuture = mApplication.getThreadPool().submit(task, task);
            }
            task.addProxy(proxy);
        }

        return proxy.get(jc);
    }

    private void updateLastAccess(long id) {
        ContentValues values = new ContentValues();
        values.put(Columns.LAST_ACCESS, System.currentTimeMillis());
        mDatabase.update(TABLE_NAME, values, ID_WHERE, new String[]{String.valueOf(id)});
    }

    private synchronized void freeSomeSpaceIfNeed(int maxDeleteFileCount) {
        if (mTotalBytes <= mCapacity) return;
        Cursor cursor = mDatabase.query(TABLE_NAME, FREESPACE_PROJECTION, null, null, null, null, FREESPACE_ORDER_BY);
        try {
            while (maxDeleteFileCount > 0 && mTotalBytes > mCapacity && cursor.moveToNext()) {
                long id = cursor.getLong(FREESPACE_IDNEX_ID);
                String url = cursor.getString(FREESPACE_INDEX_CONTENT_URL);
                long size = cursor.getLong(FREESPACE_INDEX_CONTENT_SIZE);
                String path = cursor.getString(FREESPACE_IDNEX_DATA);
                boolean containsKey;
                synchronized (mEntryMap) {
                    containsKey = mEntryMap.containsKey(url);
                }
                if (!containsKey) {
                    --maxDeleteFileCount;
                    mTotalBytes -= size;
                    new File(path).delete();
                    mDatabase.delete(TABLE_NAME, ID_WHERE, new String[]{String.valueOf(id)});
                } else {
                    // skip delete, since it is being used
                }
            }
        } finally {
            cursor.close();
        }
    }

    private synchronized long insertEntry(String url, File file) {
        long size = file.length();
        mTotalBytes += size;

        ContentValues values = new ContentValues();
        String hashCode = String.valueOf(Utils.crc64Long(url));
        values.put(Columns.DATA, file.getAbsolutePath());
        values.put(Columns.HASH_CODE, hashCode);
        values.put(Columns.CONTENT_URL, url);
        values.put(Columns.CONTENT_SIZE, size);
        values.put(Columns.LAST_UPDATED, System.currentTimeMillis());
        return mDatabase.insert(TABLE_NAME, "", values);
    }

    private synchronized void initialize() {
        if (mInitialized) return;
        mInitialized = true;
        if (!mRoot.isDirectory()) mRoot.mkdirs();
        if (!mRoot.isDirectory()) {
            throw new RuntimeException("cannot create " + mRoot.getAbsolutePath());
        }

        Cursor cursor = mDatabase.query(TABLE_NAME, SUM_PROJECTION, null, null, null, null, null);
        mTotalBytes = 0;
        try {
            if (cursor.moveToNext()) {
                mTotalBytes = cursor.getLong(SUM_INDEX_SUM);
            }
        } finally {
            cursor.close();
        }
        if (mTotalBytes > mCapacity) freeSomeSpaceIfNeed(MAX_DELETE_COUNT);
    }

    public static class TaskProxy {
        private DownloadTask mTask;
        private boolean mIsCancelled = false;
        private Entry mEntry;

        synchronized void setResult(Entry entry) {
            if (mIsCancelled) return;
            mEntry = entry;
            notifyAll();
        }

        public synchronized Entry get(JobContext jc) {
            jc.setCancelListener(new CancelListener() {
                public void onCancel() {
                    mTask.removeProxy(TaskProxy.this);
                    synchronized (TaskProxy.this) {
                        mIsCancelled = true;
                        TaskProxy.this.notifyAll();
                    }
                }
            });
            while (!mIsCancelled && mEntry == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.w(TAG, "ignore interrupt", e);
                }
            }
            jc.setCancelListener(null);
            return mEntry;
        }
    }

    private final class DatabaseHelper extends SQLiteOpenHelper {
        public static final String DATABASE_NAME = "download.db";
        public static final int DATABASE_VERSION = 2;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            DownloadEntry.SCHEMA.createTables(db);
            // Delete old files
            for (File file : mRoot.listFiles()) {
                if (!file.delete()) {
                    Log.w(TAG, "fail to remove: " + file.getAbsolutePath());
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //reset everything
            DownloadEntry.SCHEMA.dropTables(db);
            onCreate(db);
        }
    }

    public class Entry {
        public File cacheFile;
        protected long mId;

        Entry(long id, File cacheFile) {
            mId = id;
            this.cacheFile = Utils.checkNotNull(cacheFile);
        }

        public void associateWith(Object object) {
            mAssociateMap.put(Utils.checkNotNull(object), this);
        }
    }

    private class DownloadTask implements Job<File>, FutureListener<File> {
        private final String mUrl;
        private HashSet<TaskProxy> mProxySet = new HashSet<TaskProxy>();
        private Future<File> mFuture;

        public DownloadTask(String url) {
            mUrl = Utils.checkNotNull(url);
        }

        public void removeProxy(TaskProxy proxy) {
            synchronized (mTaskMap) {
                Utils.assertTrue(mProxySet.remove(proxy));
                if (mProxySet.isEmpty()) {
                    mFuture.cancel();
                    mTaskMap.remove(mUrl);
                }
            }
        }

        // should be used in synchronized block of mDatabase
        public void addProxy(TaskProxy proxy) {
            proxy.mTask = this;
            mProxySet.add(proxy);
        }

        public void onFutureDone(Future<File> future) {
            File file = future.get();
            long id = 0;
            if (file != null) { // insert to database
                id = insertEntry(mUrl, file);
            }

            if (future.isCancelled()) {
                Utils.assertTrue(mProxySet.isEmpty());
                return;
            }

            synchronized (mTaskMap) {
                Entry entry = null;
                synchronized (mEntryMap) {
                    if (file != null) {
                        entry = new Entry(id, file);
                        Utils.assertTrue(mEntryMap.put(mUrl, entry) == null);
                    }
                }
                for (TaskProxy proxy : mProxySet) {
                    proxy.setResult(entry);
                }
                mTaskMap.remove(mUrl);
                freeSomeSpaceIfNeed(MAX_DELETE_COUNT);
            }
        }

        public File run(JobContext jc) {
            // TODO: utilize etag
            jc.setMode(ThreadPool.MODE_NETWORK);
            File tempFile = null;
            try {
                URL url = new URL(mUrl);
                tempFile = File.createTempFile("cache", ".tmp", mRoot);
                try {
                    File f = new File(mRoot, ".nomedia");
                    if (!f.exists()) f.createNewFile();
                } catch (IOException e) {
                    L.i("Can't create \".nomedia\" file in gallery cache directory " + mRoot);
                }
                // download from url to tempFile
                jc.setMode(ThreadPool.MODE_NETWORK);
                boolean downloaded = DownloadUtils.requestDownload(jc, url, tempFile);
                jc.setMode(ThreadPool.MODE_NONE);
                if (downloaded) return tempFile;
            } catch (Exception e) {
                Log.e(TAG, String.format("fail to download %s", mUrl), e);
            } finally {
                jc.setMode(ThreadPool.MODE_NONE);
            }
            if (tempFile != null) tempFile.delete();
            return null;
        }
    }
}
