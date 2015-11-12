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

package com.android.gallery3d.ui;

import com.android.gallery3d.common.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

// ResourceTexture is a texture whose Bitmap is decoded from a resource.
// By default ResourceTexture is not opaque.
public class ResourceTexture extends UploadedTexture {

    private static final String TAG = ResourceTexture.class.getSimpleName();

    protected final Context mContext;
    protected final int mResId;

    public ResourceTexture(Context context, int resId) {
        mContext = Utils.checkNotNull(context);
        mResId = resId;
        setOpaque(false);
    }

    @Override
    protected Bitmap onGetBitmap() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap b = null;
        try {
            b = BitmapFactory.decodeResource(
                mContext.getResources(), mResId, options);
        }
        catch (OutOfMemoryError e) {
            Log.e(TAG, "Couldn't get memory allocated for bitmap resId=" + mResId, e);
        }
        return b;
    }

    @Override
    protected BitmapFactory.Options onGetBitmapBounds() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            BitmapFactory.decodeResource(
                    mContext.getResources(), mResId, options);
        }
        catch (OutOfMemoryError e) {
            Log.e(TAG, "Couldn't get memory allocated for bitmap bounds resId=" + mResId, e);
        }
        return options;
    }

    @Override
    protected void onFreeBitmap(Bitmap bitmap) {
        if (!inFinalizer()) {
            bitmap.recycle();
        }
    }
}
