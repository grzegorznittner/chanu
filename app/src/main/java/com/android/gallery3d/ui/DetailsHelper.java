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

import android.content.Context;
import android.content.DialogInterface;
import android.view.View.MeasureSpec;

import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.ui.DetailsAddressResolver.AddressResolvingListener;
import com.chanapps.four.gallery3d.R;

public class DetailsHelper {
    private static DetailsAddressResolver sAddressResolver;
    private DetailsViewContainer mContainer;

    public DetailsHelper(GalleryActivity activity, GLView rootPane, DetailsSource source) {
        mContainer = new DialogDetailsView(activity, source);
    }

    public static String resolveAddress(GalleryActivity activity, double[] latlng, AddressResolvingListener listener) {
        if (sAddressResolver == null) {
            sAddressResolver = new DetailsAddressResolver(activity);
        } else {
            sAddressResolver.cancel();
        }
        return sAddressResolver.resolveAddress(latlng, listener);
    }

    public static void pause() {
        if (sAddressResolver != null) sAddressResolver.cancel();
    }

    public static String getDetailsName(Context context, int key) {
        switch (key) {
            case MediaDetails.INDEX_TITLE:
                return context.getString(R.string.title);
            case MediaDetails.INDEX_DESCRIPTION:
                return context.getString(R.string.description);
            case MediaDetails.INDEX_DATETIME:
                return context.getString(R.string.time);
            case MediaDetails.INDEX_LOCATION:
                return context.getString(R.string.location);
            case MediaDetails.INDEX_PATH:
                return context.getString(R.string.path);
            case MediaDetails.INDEX_WIDTH:
                return context.getString(R.string.width);
            case MediaDetails.INDEX_HEIGHT:
                return context.getString(R.string.height);
            case MediaDetails.INDEX_ORIENTATION:
                return context.getString(R.string.orientation);
            case MediaDetails.INDEX_DURATION:
                return context.getString(R.string.duration);
            case MediaDetails.INDEX_MIMETYPE:
                return context.getString(R.string.mimetype);
            case MediaDetails.INDEX_SIZE:
                return context.getString(R.string.file_size);
            case MediaDetails.INDEX_MAKE:
                return context.getString(R.string.maker);
            case MediaDetails.INDEX_MODEL:
                return context.getString(R.string.model);
            case MediaDetails.INDEX_FLASH:
                return context.getString(R.string.flash);
            case MediaDetails.INDEX_APERTURE:
                return context.getString(R.string.aperture);
            case MediaDetails.INDEX_FOCAL_LENGTH:
                return context.getString(R.string.focal_length);
            case MediaDetails.INDEX_WHITE_BALANCE:
                return context.getString(R.string.white_balance);
            case MediaDetails.INDEX_EXPOSURE_TIME:
                return context.getString(R.string.exposure_time);
            case MediaDetails.INDEX_ISO:
                return context.getString(R.string.iso);
            default:
                return "Unknown key" + key;
        }
    }

    public void layout(int left, int top, int right, int bottom) {
        if (mContainer instanceof GLView) {
            GLView view = (GLView) mContainer;
            view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.makeMeasureSpec(bottom - top, MeasureSpec.AT_MOST));
            view.layout(0, top, view.getMeasuredWidth(), top + view.getMeasuredHeight());
        }
    }

    public void reloadDetails(int indexHint) {
        mContainer.reloadDetails(indexHint);
    }

    public void setClickListener(int stringId, DialogInterface.OnClickListener listener) {
        mContainer.setClickListener(stringId, listener);
    }

    public void setCloseListener(CloseListener listener) {
        mContainer.setCloseListener(listener);
    }

    public void show() {
        mContainer.show();
    }

    public void hide() {
        mContainer.hide();
    }

    public interface DetailsSource {
        int size();

        int getIndex();

        int findIndex(int indexHint);

        MediaDetails getDetails();
    }

    public interface CloseListener {
        void onClose();
    }

    public interface DetailsViewContainer {
        void reloadDetails(int indexHint);

        void setCloseListener(CloseListener listener);

        void setClickListener(int stringId, DialogInterface.OnClickListener listener);

        void show();

        void hide();
    }
}


