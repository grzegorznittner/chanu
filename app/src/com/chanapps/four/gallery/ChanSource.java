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

package com.chanapps.four.gallery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.ContentProviderClient;
import android.content.UriMatcher;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MediaSet.ItemConsumer;
import com.android.gallery3d.data.MediaSource;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.PathMatcher;

public class ChanSource extends MediaSource {

    public static final String KEY_BUCKET_ID = "bucketId";

    private GalleryApp mApplication;
    private PathMatcher mMatcher;
    private static final int NO_MATCH = -1;
    private final UriMatcher mUriMatcher = new UriMatcher(NO_MATCH);
    public static final Comparator<PathId> sIdComparator = new IdComparator();

    private static final int CHAN_IMAGE_ITEM = 1;
    private static final int CHAN_VIDEO_ITEM = 2;
    private static final int CHAN_IMAGE_ALBUM = 3;
    private static final int CHAN_VIDEO_ALBUM = 4;

    private static final String TAG = "ChanSource";

    private ContentProviderClient mClient;

    public ChanSource(GalleryApp context) {
        super("chan");
        mApplication = context;
        mMatcher = new PathMatcher();
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        GalleryApp app = mApplication;

        if ("chan".equals(path.getPrefix())) {
        	String[] elems = path.split();
        	String board = elems[1];
        	String thread = elems[2];
        	if (elems.length == 3) {
        		// thread
        	} else {
        		// image
        	}
        }
        return null;
    }

    @Override
    public Path findPathByUri(Uri uri) {
        try {
        	String uriStr = uri != null ? uri.toString() : "";
        	if (uriStr.startsWith("/chan/")) {
        		return Path.fromString(uriStr);
        	}
        } catch (Exception e) {
            Log.w(TAG, "uri: " + uri.toString(), e);
        }
        return null;
    }

    @Override
    public Path getDefaultSetOf(Path item) {
        MediaObject object = mApplication.getDataManager().getMediaObject(item);
        /* TODO Greg
        if (object instanceof LocalMediaItem) {
            return Path.fromString("/local/all").getChild(
                    String.valueOf(((LocalMediaItem) object).getBucketId()));
        }
        */
        return null;
    }

    @Override
    public void mapMediaItems(ArrayList<PathId> list, ItemConsumer consumer) {
        ArrayList<PathId> imageList = new ArrayList<PathId>();
        ArrayList<PathId> videoList = new ArrayList<PathId>();
        int n = list.size();
        for (int i = 0; i < n; i++) {
            PathId pid = list.get(i);
            // We assume the form is: "/local/{image,video}/item/#"
            // We don't use mMatcher for efficiency's reason.
            Path parent = pid.path.getParent();
            /* TODO Greg
            if (parent == LocalImage.ITEM_PATH) {
                imageList.add(pid);
            } else if (parent == LocalVideo.ITEM_PATH) {
                videoList.add(pid);
            }*/
        }
        // TODO: use "files" table so we can merge the two cases.
        processMapMediaItems(imageList, consumer, true);
        processMapMediaItems(videoList, consumer, false);
    }

    private void processMapMediaItems(ArrayList<PathId> list,
            ItemConsumer consumer, boolean isImage) {
        // Sort path by path id
        Collections.sort(list, sIdComparator);
        int n = list.size();
        for (int i = 0; i < n; ) {
            PathId pid = list.get(i);

            // Find a range of items.
            ArrayList<Integer> ids = new ArrayList<Integer>();
            int startId = Integer.parseInt(pid.path.getSuffix());
            ids.add(startId);

            int j;
            for (j = i + 1; j < n; j++) {
                PathId pid2 = list.get(j);
                int curId = Integer.parseInt(pid2.path.getSuffix());
                if (curId - startId >= MediaSet.MEDIAITEM_BATCH_FETCH_COUNT) {
                    break;
                }
                ids.add(curId);
            }

            MediaItem[] items = LocalAlbum.getMediaItemById(
                    mApplication, isImage, ids);
            for(int k = i ; k < j; k++) {
                PathId pid2 = list.get(k);
                consumer.consume(pid2.id, items[k - i]);
            }

            i = j;
        }
    }

    // This is a comparator which compares the suffix number in two Paths.
    private static class IdComparator implements Comparator<PathId> {
        public int compare(PathId p1, PathId p2) {
            String s1 = p1.path.getSuffix();
            String s2 = p2.path.getSuffix();
            int len1 = s1.length();
            int len2 = s2.length();
            if (len1 < len2) {
                return -1;
            } else if (len1 > len2) {
                return 1;
            } else {
                return s1.compareTo(s2);
            }
        }
    }

    @Override
    public void resume() {
        mClient = mApplication.getContentResolver()
                .acquireContentProviderClient(MediaStore.AUTHORITY);
    }

    @Override
    public void pause() {
        mClient.release();
        mClient = null;
    }
}
