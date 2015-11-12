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

import android.content.ContentProviderClient;
import android.net.Uri;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSource;
import com.android.gallery3d.data.Path;

public class ChanOffLineSource extends MediaSource {
    private static final String TAG = "ChanOffLineSource";
    public static final String KEY_BUCKET_ID = "bucketId";
    
    public static final String SOURCE_PREFIX = "chan-offline";

    private GalleryApp mApplication;

    private ContentProviderClient mClient;

    public ChanOffLineSource(GalleryApp context) {
        super(SOURCE_PREFIX);
        mApplication = context;
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        if (SOURCE_PREFIX.equals(path.getPrefix())) {
        	String[] elems = path.split();
        	if (elems.length == 1) {
        		return new ChanOffLineAlbumSet(path, mApplication);
        	} else if (elems.length == 2) {
        		return new ChanOffLineAlbum(path, mApplication, elems[1]);
        	} else if (elems.length == 3) {
        		return new ChanOffLineImage(mApplication, path, elems[1], elems[2]);
        	}
        }
        return null;
    }

    @Override
    public Path findPathByUri(Uri uri) {
        try {
        	String uriStr = uri != null ? uri.toString() : "";
        	if (uriStr.startsWith("/" + SOURCE_PREFIX + "/")) {
        		return Path.fromString(uriStr);
        	}
        } catch (Exception e) {
            Log.w(TAG, "uri: " + uri.toString(), e);
        }
        return null;
    }

    @Override
    public void pause() {
    	if (mClient != null) {
    		mClient.release();
    		mClient = null;
    	}
    }
}
