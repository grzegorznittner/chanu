package com.chanapps.four.viewer;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanThread;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/11/13
 * Time: 1:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardlistViewer {

    static public boolean setViewValue(View view, Cursor cursor, int columnIndex,
                                                   ImageLoader imageLoader, DisplayImageOptions options) {
        switch (view.getId()) {
            case R.id.grid_item_thread_subject:
                return setThreadSubject((TextView) view, cursor);
            case R.id.grid_item_thread_title:
                return setThreadTitle((TextView) view, cursor);
            case R.id.grid_item_thread_thumb:
                return setThreadThumb((ImageView) view, cursor, imageLoader, options);
            case R.id.grid_item_country_flag:
                return setThreadCountryFlag((ImageView) view, cursor, imageLoader, options);
        }
        return false;
    }

    static protected boolean setThreadSubject(TextView tv, Cursor cursor) {
        tv.setVisibility(View.GONE);
        int threadFlags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((threadFlags & ChanThread.THREAD_FLAG_TITLE) > 0) {
            tv.setText("");
        }
        else {
            tv.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT))));
        }
        return true;
    }

    static protected boolean setThreadTitle(TextView tv, Cursor cursor) {
        int threadFlags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((threadFlags & ChanThread.THREAD_FLAG_TITLE) > 0) {
            tv.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TITLE))));
            tv.setVisibility(View.VISIBLE);
        }
        else {
            tv.setVisibility(View.GONE);
            tv.setText("");
        }
        return true;
    }

    static protected boolean setThreadThumb(ImageView iv, Cursor cursor,
                                            ImageLoader imageLoader, DisplayImageOptions options) {
        int threadFlags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((threadFlags & ChanThread.THREAD_FLAG_TITLE) > 0) {
            iv.setImageBitmap(null);
        }
        else {
            imageLoader.displayImage(
                    cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL)),
                    iv,
                    options.modifyCenterCrop(true),
                    thumbLoadingListener);
        }
        return true;
    }

    static protected boolean setThreadCountryFlag(ImageView iv, Cursor cursor,
                                                  ImageLoader imageLoader, DisplayImageOptions options) {
        imageLoader.displayImage(
                cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_COUNTRY_FLAG_URL)),
                iv,
                options);
        return true;
    }

    static protected ImageLoadingListener thumbLoadingListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            ViewParent parent = view.getParent();
            if (parent != null && parent instanceof ViewGroup) {
                ViewGroup parentView = (ViewGroup)parent;
                TextView subjectView = (TextView)parentView.findViewById(R.id.grid_item_thread_subject);
                if (subjectView != null && subjectView.getText() != null && subjectView.getText().length() > 0)
                    subjectView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    };

}
