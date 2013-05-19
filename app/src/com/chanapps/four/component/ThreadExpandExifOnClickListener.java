package com.chanapps.four.component;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.text.Html;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.ThreadViewer;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import java.io.File;
import java.net.URI;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 4/9/13
* Time: 10:28 AM
* To change this template use File | Settings | File Templates.
*/
public class ThreadExpandExifOnClickListener implements View.OnClickListener {

    private static final String TAG = ThreadExpandExifOnClickListener.class.getSimpleName();
    private static final boolean DEBUG = true;

    private TextView itemExifView;
    private int listPosition = 0;
    private int flags;
    private String exifText;

    public ThreadExpandExifOnClickListener(final Cursor cursor, final View itemView) {
        exifText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EXIF_TEXT));
        itemExifView = (TextView)itemView.findViewById(R.id.list_item_image_exif);
        listPosition = cursor.getPosition();
        flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
    }

    private void collapseExif() {
        if (DEBUG) Log.i(TAG, "collapsed pos=" + listPosition);
        if (itemExifView != null)
            itemExifView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (DEBUG) Log.i(TAG, "expanding pos=" + listPosition);
        if ((flags & ChanPost.FLAG_HAS_EXIF) > 0)
            expandExif();
    }


    private boolean shouldExpandExif() {
        if (itemExifView != null && itemExifView.getVisibility() != View.GONE) {
            if (DEBUG) Log.i(TAG, "Exif already expanded, skipping");
            return false;
        }
        return true;
    }

    private void expandExif() {
        if (!shouldExpandExif()) {
            collapseExif();
            return;
        }
        if (itemExifView.getVisibility() == View.VISIBLE) {
            itemExifView.setVisibility(View.GONE);
            return;
        }
        if (DEBUG) Log.i(TAG, "Expanding exifText=" + exifText);
        if (itemExifView != null && exifText != null && !exifText.isEmpty()) {
            itemExifView.setText(Html.fromHtml(exifText));
            itemExifView.setVisibility(View.VISIBLE);
        }
    }

}
