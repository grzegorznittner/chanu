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

import android.net.Uri;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class UriSource extends MediaSource {
    @SuppressWarnings("unused")
    private static final String TAG = "UriSource";

    private GalleryApp mApplication;

    public UriSource(GalleryApp context) {
        super("uri");
        mApplication = context;
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        String[] segment = path.split();
        if (segment.length != 2) {
            throw new RuntimeException("bad path: " + path);
        }
        String decoded;
        try {
            decoded = URLDecoder.decode(segment[1], "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported encoding for url:" + segment[1]);
            decoded = segment[1];
        }
        return new UriImage(mApplication, path, Uri.parse(decoded));
    }

    @Override
    public Path findPathByUri(Uri uri) {
        String type = mApplication.getContentResolver().getType(uri);
        // Assume the type is image if the type cannot be resolved
        // This could happen for "http" URI.
        if (type == null || type.startsWith("image/")) {
            String encoded;
            try {
                encoded = URLEncoder.encode(uri.toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding for url:" + uri.toString());
                encoded = uri.toString();
            }
            return Path.fromString("/uri/" + encoded);
        }
        return null;
    }
}
