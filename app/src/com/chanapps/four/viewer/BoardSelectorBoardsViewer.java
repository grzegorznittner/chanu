package com.chanapps.four.viewer;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.loader.ChanImageLoader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/11/13
 * Time: 1:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardSelectorBoardsViewer {

    private static ImageLoader imageLoader;
    private static DisplayImageOptions displayImageOptions;

    private static void initStatics(View view) {
        imageLoader = ChanImageLoader.getInstance(view.getContext());
        displayImageOptions = new DisplayImageOptions.Builder()
                .resetViewBeforeLoading()
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .cacheInMemory()
                .cacheOnDisc()
                .build();
    }

    static public boolean setViewValue(View view, Cursor cursor) {
        if (imageLoader == null)
            initStatics(view);
        switch (view.getId()) {
            case R.id.grid_item_thread_subject:
                return setThreadSubject((TextView) view, cursor);
            case R.id.grid_item_thread_thumb:
                return setThreadThumb((ImageView) view, cursor);
            case R.id.grid_item_country_flag:
                return setThreadCountryFlag((ImageView) view, cursor);
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

    static protected boolean setThreadThumb(ImageView iv, Cursor cursor) {
        int threadFlags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((threadFlags & ChanThread.THREAD_FLAG_TITLE) > 0) {
            iv.setImageBitmap(null);
        }
        else {
            imageLoader.displayImage(
                    cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL)),
                    iv,
                    displayImageOptions,
                    thumbLoadingListener);
        }
        return true;
    }

    static protected boolean setThreadCountryFlag(ImageView iv, Cursor cursor) {
        imageLoader.displayImage(
                cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_COUNTRY_FLAG_URL)),
                iv,
                displayImageOptions);
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
