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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.ImageCacheService.ImageData;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;

public abstract class ImageCacheRequest implements Job<Bitmap> {
    private static final String TAG = "ImageCacheRequest";

    protected GalleryApp mApplication;
    private Path mPath;
    private int mType;
    private int mTargetSize;

    public ImageCacheRequest(GalleryApp application, Path path, int type, int targetSize) {
        mApplication = application;
        mPath = path;
        mType = type;
        mTargetSize = targetSize;
    }

    public Bitmap run(JobContext jc) {
        String debugTag = mPath + "," + ((mType == MediaItem.TYPE_THUMBNAIL) ? "THUMB" : (mType == MediaItem.TYPE_MICROTHUMBNAIL) ? "MICROTHUMB" : "?");
        ImageCacheService cacheService = mApplication.getImageCacheService();

        ImageData data = cacheService.getImageData(mPath, mType);
        if (jc.isCancelled()) return null;

        if (data != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = DecodeUtils.requestDecode(jc, data.mData, data.mOffset, data.mData.length - data.mOffset, options);
            if (bitmap == null && !jc.isCancelled()) {
                Log.w(TAG, "decode cached failed " + debugTag);
            }
            return bitmap;
        } else {
            Bitmap bitmap = onDecodeOriginal(jc, mType);
            if (jc.isCancelled()) return null;

            if (bitmap == null) {
                Log.w(TAG, "decode orig failed " + debugTag);
                return null;
            }

            if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
                bitmap = BitmapUtils.resizeDownAndCropCenter(bitmap, mTargetSize, true);
            } else {
                bitmap = BitmapUtils.resizeDownBySideLength(bitmap, mTargetSize, true);
            }
            if (jc.isCancelled()) return null;

            byte[] array = BitmapUtils.compressBitmap(bitmap);
            if (jc.isCancelled()) return null;

            cacheService.putImageData(mPath, mType, array);
            return bitmap;
        }
    }

    public abstract Bitmap onDecodeOriginal(JobContext jc, int targetSize);
}
