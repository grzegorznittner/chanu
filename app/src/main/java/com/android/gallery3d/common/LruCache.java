/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.gallery3d.common;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache which stores recently inserted entries and all entries ever
 * inserted which still has a strong reference elsewhere.
 */
public class LruCache<K, V> {

    private final HashMap<K, V> mLruMap;
    private final HashMap<K, Entry<K, V>> mWeakMap = new HashMap<K, Entry<K, V>>();
    private ReferenceQueue<V> mQueue = new ReferenceQueue<V>();

    @SuppressWarnings("serial")
    public LruCache(final int capacity) {
        mLruMap = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void cleanUpWeakMap() {
        Entry<K, V> entry = (Entry<K, V>) mQueue.poll();
        while (entry != null) {
            mWeakMap.remove(entry.mKey);
            entry = (Entry<K, V>) mQueue.poll();
        }
    }

    public synchronized boolean containsKey(K key) {
        cleanUpWeakMap();
        return mWeakMap.containsKey(key);
    }

    public synchronized V put(K key, V value) {
        cleanUpWeakMap();
        mLruMap.put(key, value);
        Entry<K, V> entry = mWeakMap.put(key, new Entry<K, V>(key, value, mQueue));
        return entry == null ? null : entry.get();
    }

    public synchronized V get(K key) {
        cleanUpWeakMap();
        V value = mLruMap.get(key);
        if (value != null) return value;
        Entry<K, V> entry = mWeakMap.get(key);
        return entry == null ? null : entry.get();
    }

    public synchronized void clear() {
        mLruMap.clear();
        mWeakMap.clear();
        mQueue = new ReferenceQueue<V>();
    }

    private static class Entry<K, V> extends WeakReference<V> {
        K mKey;

        public Entry(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            mKey = key;
        }
    }
}
