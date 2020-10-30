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

import android.util.Log;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.Future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

// MediaSet is a directory-like data structure.
// It contains MediaItems and sub-MediaSets.
//
// The primary interface are:
// getMediaItemCount(), getMediaItem() and
// getSubMediaSetCount(), getSubMediaSet().
//
// getTotalMediaItemCount() returns the number of all MediaItems, including
// those in sub-MediaSets.
public abstract class MediaSet extends MediaObject {
    public static final int MEDIAITEM_BATCH_FETCH_COUNT = 500;
    public static final int INDEX_NOT_FOUND = -1;

    public static final int SYNC_RESULT_SUCCESS = 0;
    public static final int SYNC_RESULT_CANCELLED = 1;
    public static final int SYNC_RESULT_ERROR = 2;
    private static final Future<Integer> FUTURE_STUB = new Future<Integer>() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Integer get() {
            return SYNC_RESULT_SUCCESS;
        }

        @Override
        public void waitDone() {
        }
    };
    private WeakHashMap<ContentListener, Object> mListeners = new WeakHashMap<ContentListener, Object>();

    public MediaSet(Path path, long version) {
        super(path, version);
    }

    public int getMediaItemCount() {
        return 0;
    }

    // Returns the media items in the range [start, start + count).
    //
    // The number of media items returned may be less than the specified count
    // if there are not enough media items available. The number of
    // media items available may not be consistent with the return value of
    // getMediaItemCount() because the contents of database may have already
    // changed.
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        return new ArrayList<MediaItem>();
    }

    public int getSubMediaSetCount() {
        return 0;
    }

    public MediaSet getSubMediaSet(int index) {
        throw new IndexOutOfBoundsException();
    }

    public boolean isLeafAlbum() {
        return false;
    }

    public int getTotalMediaItemCount() {
        int total = getMediaItemCount();
        for (int i = 0, n = getSubMediaSetCount(); i < n; i++) {
            total += getSubMediaSet(i).getTotalMediaItemCount();
        }
        return total;
    }

    // TODO: we should have better implementation of sub classes
    public int getIndexOfItem(Path path, int hint) {
        // hint < 0 is handled below
        // first, try to find it around the hint
        int start = Math.max(0, hint - MEDIAITEM_BATCH_FETCH_COUNT / 2);
        ArrayList<MediaItem> list = getMediaItem(start, MEDIAITEM_BATCH_FETCH_COUNT);
        int index = getIndexOf(path, list);
        if (index != INDEX_NOT_FOUND) return start + index;

        // try to find it globally
        start = start == 0 ? MEDIAITEM_BATCH_FETCH_COUNT : 0;
        list = getMediaItem(start, MEDIAITEM_BATCH_FETCH_COUNT);
        while (true) {
            index = getIndexOf(path, list);
            if (index != INDEX_NOT_FOUND) return start + index;
            if (list.size() < MEDIAITEM_BATCH_FETCH_COUNT) return INDEX_NOT_FOUND;
            start += MEDIAITEM_BATCH_FETCH_COUNT;
            list = getMediaItem(start, MEDIAITEM_BATCH_FETCH_COUNT);
        }
    }

    protected int getIndexOf(Path path, ArrayList<MediaItem> list) {
        for (int i = 0, n = list.size(); i < n; ++i) {
            if (list.get(i).mPath.equals(path)) return i;
        }
        return INDEX_NOT_FOUND;
    }

    public abstract String getName();

    // NOTE: The MediaSet only keeps a weak reference to the listener. The
    // listener is automatically removed when there is no other reference to
    // the listener.
    public void addContentListener(ContentListener listener) {
        if (mListeners.containsKey(listener)) {
            throw new IllegalArgumentException();
        }
        mListeners.put(listener, null);
    }

    public void removeContentListener(ContentListener listener) {
        if (!mListeners.containsKey(listener)) {
            throw new IllegalArgumentException();
        }
        mListeners.remove(listener);
    }

    // This should be called by subclasses when the content is changed.
    public void notifyContentChanged() {
        for (ContentListener listener : mListeners.keySet()) {
            listener.onContentDirty();
        }
    }

    // Reload the content. Return the current data version. reload() should be called
    // in the same thread as getMediaItem(int, int) and getSubMediaSet(int).
    public abstract long reload();

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        details.addDetail(MediaDetails.INDEX_TITLE, getName());
        return details;
    }

    // Enumerate all media items in this media set (including the ones in sub
    // media sets), in an efficient order. ItemConsumer.consumer() will be
    // called for each media item with its index.
    public void enumerateMediaItems(ItemConsumer consumer) {
        enumerateMediaItems(consumer, 0);
    }

    public void enumerateTotalMediaItems(ItemConsumer consumer) {
        enumerateTotalMediaItems(consumer, 0);
    }

    // The default implementation uses getMediaItem() for enumerateMediaItems().
    // Subclasses may override this and use more efficient implementations.
    // Returns the number of items enumerated.
    protected int enumerateMediaItems(ItemConsumer consumer, int startIndex) {
        int total = getMediaItemCount();
        int start = 0;
        while (start < total) {
            int count = Math.min(MEDIAITEM_BATCH_FETCH_COUNT, total - start);
            ArrayList<MediaItem> items = getMediaItem(start, count);
            for (int i = 0, n = items.size(); i < n; i++) {
                MediaItem item = items.get(i);
                consumer.consume(startIndex + start + i, item);
            }
            start += count;
        }
        return total;
    }

    // Recursively enumerate all media items under this set.
    // Returns the number of items enumerated.
    protected int enumerateTotalMediaItems(ItemConsumer consumer, int startIndex) {
        int start = 0;
        start += enumerateMediaItems(consumer, startIndex);
        int m = getSubMediaSetCount();
        for (int i = 0; i < m; i++) {
            start += getSubMediaSet(i).enumerateTotalMediaItems(consumer, startIndex + start);
        }
        return start;
    }

    /**
     * Requests sync on this MediaSet. It returns a Future object that can be used by the caller
     * to query the status of the sync. The sync result code is one of the SYNC_RESULT_* constants
     * defined in this class and can be obtained by Future.get().
     * <p>
     * Subclasses should perform sync on a different thread.
     * <p>
     * The default implementation here returns a Future stub that does nothing and returns
     * SYNC_RESULT_SUCCESS by get().
     */
    public Future<Integer> requestSync(SyncListener listener) {
        return FUTURE_STUB;
    }

    protected Future<Integer> requestSyncOnEmptySets(MediaSet[] sets, SyncListener listener) {
        MultiSetSyncFuture future = new MultiSetSyncFuture(listener);
        future.requestSyncOnEmptySets(sets);
        return future;
    }

    /**
     * Listener to be used with requestSync(SyncListener).
     */
    public interface SyncListener {
        /**
         * Called when the sync task completed. Completion may be due to normal termination,
         * an exception, or cancellation.
         *
         * @param mediaSet   the MediaSet that's done with sync
         * @param resultCode one of the SYNC_RESULT_* constants
         */
        void onSyncDone(MediaSet mediaSet, int resultCode);
    }

    public interface ItemConsumer {
        void consume(int index, MediaItem item);
    }

    private class MultiSetSyncFuture implements Future<Integer>, SyncListener {
        private static final String TAG = "Gallery.MultiSetSync";

        private final HashMap<MediaSet, Future<Integer>> mMediaSetMap = new HashMap<MediaSet, Future<Integer>>();
        private final SyncListener mListener;

        private boolean mIsCancelled = false;
        private int mResult = -1;

        MultiSetSyncFuture(SyncListener listener) {
            mListener = listener;
        }

        synchronized void requestSyncOnEmptySets(MediaSet[] sets) {
            for (MediaSet set : sets) {
                if ((set.getMediaItemCount() == 0) && !mMediaSetMap.containsKey(set)) {
                    // Sync results are handled in this.onSyncDone().
                    Future<Integer> future = set.requestSync(this);
                    if (!future.isDone()) {
                        mMediaSetMap.put(set, future);
                        Log.d(TAG, "  request sync: " + Utils.maskDebugInfo(set.getName()));
                    }
                }
            }
            Log.d(TAG, "requestSyncOnEmptySets actual=" + mMediaSetMap.size());
        }

        @Override
        public synchronized void cancel() {
            if (mIsCancelled) return;
            mIsCancelled = true;
            for (Future<Integer> future : mMediaSetMap.values()) future.cancel();
            mMediaSetMap.clear();
            if (mResult < 0) mResult = SYNC_RESULT_CANCELLED;
        }

        @Override
        public synchronized boolean isCancelled() {
            return mIsCancelled;
        }

        @Override
        public synchronized boolean isDone() {
            return mMediaSetMap.isEmpty();
        }

        @Override
        public synchronized Integer get() {
            waitDone();
            return mResult;
        }

        @Override
        public synchronized void waitDone() {
            try {
                while (!isDone()) wait();
            } catch (InterruptedException e) {
                Log.d(TAG, "waitDone() interrupted");
            }
        }

        // SyncListener callback
        @Override
        public void onSyncDone(MediaSet mediaSet, int resultCode) {
            SyncListener listener = null;
            synchronized (this) {
                if (mMediaSetMap.remove(mediaSet) != null) {
                    Log.d(TAG, "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " #pending=" + mMediaSetMap.size());
                    if (resultCode == SYNC_RESULT_ERROR) {
                        mResult = SYNC_RESULT_ERROR;
                    }
                    if (mMediaSetMap.isEmpty()) {
                        if (mResult < 0) mResult = SYNC_RESULT_SUCCESS;
                        notifyAll();
                        listener = mListener;
                    }
                }
            }
            if (listener != null) listener.onSyncDone(MediaSet.this, mResult);
        }
    }
}
