package com.chanapps.four.component;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
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
public class ThreadExpandImageOnClickListener implements View.OnClickListener {

    private static final String TAG = ThreadExpandImageOnClickListener.class.getSimpleName();
    private static final boolean DEBUG = false;

    private int expandable = 0;
    private ImageView itemExpandedImage;
    private ProgressBar itemExpandedProgressBar;
    private String postImageUrl = null;
    int postW = 0;
    int postH = 0;
    int listPosition = 0;
    String fullImagePath = null;
    private ThreadActivity threadActivity;

    public ThreadExpandImageOnClickListener(ThreadActivity threadActivity, final Cursor cursor, final int expandable, final View itemView) {
        this.threadActivity = threadActivity;
        long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_CODE));
        long postTim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
        String postExt = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_EXT));
        Uri uri = ChanFileStorage.getLocalImageUri(threadActivity.getApplicationContext(), boardCode, postId, postExt);

        this.expandable = expandable;
        itemExpandedImage = (ImageView)itemView.findViewById(R.id.list_item_image_expanded);
        itemExpandedProgressBar = (ProgressBar)itemView.findViewById(R.id.list_item_expanded_progress_bar);

        listPosition = cursor.getPosition();
        postW = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_W));
        postH = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_H));
        postImageUrl = postTim > 0 ? ChanPost.imageUrl(boardCode, postTim, postExt) : null;
        fullImagePath = (new File(URI.create(uri.toString()))).getAbsolutePath();
    }

    private void collapseImageView() {
        if (DEBUG) Log.i(TAG, "collapsed pos=" + listPosition);
        ChanHelper.clearBigImageView(itemExpandedImage);
        if (itemExpandedProgressBar != null)
            itemExpandedProgressBar.setVisibility(View.GONE);
        itemExpandedImage.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (DEBUG) Log.i(TAG, "expanding pos=" + listPosition);

        if (DEBUG) Log.i(TAG, "Clearing existing image");
        ChanHelper.clearBigImageView(itemExpandedImage); // clear old image
        if (DEBUG) Log.i(TAG, "Existing image cleared");

        if (DEBUG) Log.i(TAG, "Found postImageUrl=" + postImageUrl);
        if ((expandable & ThreadActivity.IMAGE_EXPANDABLE) == 0 || postImageUrl == null || postImageUrl.isEmpty()) {// no image to display
            if (DEBUG) Log.i(TAG, "No image found to expand, hiding image and exiting");
            itemExpandedImage.setVisibility(View.GONE);
            return;
        }

        // calculate image dimensions
        if (DEBUG) Log.i(TAG, "post size " + postW + "x" + postH);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        threadActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //int padding = ChanGridSizer.dpToPx(displayMetrics, 16);
        int maxWidth = displayMetrics.widthPixels;
        int maxHeight = maxWidth; // to avoid excessively big images
        itemExpandedImage.setMaxWidth(maxWidth);
        itemExpandedImage.setMaxHeight(maxHeight);
        if (DEBUG) Log.i(TAG, "max size " + maxWidth + "x" + maxHeight);
        float scaleFactor = 1;
        if (postW >= postH) {
            // square or wide image, base sizing on width
            if (postW > maxWidth)
                scaleFactor = (float)maxWidth / (float)postW;
        }
        else {
            // tall image
            if (postH > maxHeight)
                scaleFactor = (float)maxHeight / (float)postH;
        }
        int width = Math.round(scaleFactor * (float)postW);
        int height = Math.round(scaleFactor * (float)postH);
        if (DEBUG) Log.i(TAG, "target size " + width + "x" + height);
        // set layout dimensions
        ViewGroup.LayoutParams params = itemExpandedImage.getLayoutParams();
        if (params != null) {
            params.width = width;
            params.height = height;
            if (DEBUG) Log.i(TAG, "set expanded image size=" + width + "x" + height);
        }
        itemExpandedImage.setVisibility(View.VISIBLE);
        if (DEBUG) Log.i(TAG, "Set expanded image to visible");

        int lastPosition = threadActivity.getAbsListView().getLastVisiblePosition();
        boolean shouldMove = listPosition >= lastPosition - 1;
        final int parentOffset = shouldMove ? 100 : 0; // allow for margin

        // set visibility delayed
        if (itemExpandedProgressBar != null)
            itemExpandedProgressBar.setVisibility(View.VISIBLE);
        if (threadActivity.getChanHandler() != null)
            threadActivity.getChanHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    threadActivity.getAbsListView().smoothScrollBy(parentOffset, 250);
                }
            }, 250);

        ImageSize imageSize = new ImageSize(width, height);
        DisplayImageOptions expandedDisplayImageOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.EXACTLY)
                .cacheOnDisc()
                .fullSizeImageLocation(fullImagePath)
                .resetViewBeforeLoading()
                .build();

        // display image async
        ChanImageLoader.getInstance(threadActivity).displayImage(postImageUrl, itemExpandedImage, expandedDisplayImageOptions, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                collapseImageView();
                String reason = failReason.toString();
                String msg;
                NetworkProfile profile = NetworkProfileManager.instance().getCurrentProfile();
                if (profile.getConnectionType() == NetworkProfile.Type.NO_CONNECTION
                        || profile.getConnectionHealth() == NetworkProfile.Health.NO_CONNECTION
                        || profile.getConnectionHealth() == NetworkProfile.Health.BAD)
                    msg = threadActivity.getString(R.string.thread_couldnt_download_image_down);
                else if (reason.equalsIgnoreCase("io_error"))
                    msg = threadActivity.getString(R.string.thread_couldnt_download_image);
                else
                    msg = String.format(threadActivity.getString(R.string.thread_couldnt_load_image), failReason.toString().toLowerCase().replaceAll("_", " "));
                if (DEBUG) Log.e(TAG, "Failed to download " + postImageUrl + " to file=" + fullImagePath + " reason=" + reason);
                Toast.makeText(threadActivity, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (itemExpandedProgressBar != null)
                    itemExpandedProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                collapseImageView();
                Toast.makeText(threadActivity, R.string.thread_couldnt_load_image_cancelled, Toast.LENGTH_SHORT).show();
            }
        }); // load async
    }
}
