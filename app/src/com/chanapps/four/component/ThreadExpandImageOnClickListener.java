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
    private static final boolean DEBUG = true;
    private static final int NO_TEXT_PADDING_TOP_DP = 8;

    private ImageView itemThumbnailImage;
    private ImageView itemExpandedImage;
    private ProgressBar itemExpandedProgressBar;
    private TextView itemExifView;
    private TextView itemSubjectView;
    private TextView itemTextView;
    private String postImageUrl = null;
    private int postW = 0;
    private int postH = 0;
    private int listPosition = 0;
    private String fullImagePath = null;
    private int padding8Dp;
    private int flags;
    private String spoilerSubject;
    private String spoilerText;
    private String exifText;
    private ThreadActivity threadActivity;

    public ThreadExpandImageOnClickListener(ThreadActivity threadActivity, final Cursor cursor, final View itemView) {
        this.threadActivity = threadActivity;
        long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
        String postExt = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EXT));
        Uri uri = ChanFileStorage.getLocalImageUri(threadActivity.getApplicationContext(), boardCode, postId, postExt);
        spoilerSubject = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SPOILER_SUBJECT));
        spoilerText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SPOILER_TEXT));
        exifText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EXIF_TEXT));

        itemThumbnailImage = (ImageView)itemView.findViewById(R.id.list_item_image);
        itemExpandedImage = (ImageView)itemView.findViewById(R.id.list_item_image_expanded);
        itemExpandedProgressBar = (ProgressBar)itemView.findViewById(R.id.list_item_expanded_progress_bar);
        itemExifView = (TextView)itemView.findViewById(R.id.list_item_image_exif);
        itemSubjectView = (TextView)itemView.findViewById(R.id.list_item_subject);
        itemTextView = (TextView)itemView.findViewById(R.id.list_item_text);

        padding8Dp = ChanGridSizer.dpToPx(itemExpandedImage.getResources().getDisplayMetrics(), NO_TEXT_PADDING_TOP_DP);

        listPosition = cursor.getPosition();
        postW = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_W));
        postH = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_H));
        postImageUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_FULL_IMAGE_URL));
        fullImagePath = (new File(URI.create(uri.toString()))).getAbsolutePath();
        flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
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

        if ((flags & ChanPost.FLAG_HAS_IMAGE) > 0)
            expandImage();
        if ((flags & ChanPost.FLAG_HAS_EXIF) > 0)
            expandExif();
        if ((flags & ChanPost.FLAG_HAS_SPOILER) > 0)
            expandSpoiler();
    }

    private void expandImage() {
        if (DEBUG) Log.i(TAG, "Clearing existing image");
        ChanHelper.clearBigImageView(itemExpandedImage); // clear old image
        if (DEBUG) Log.i(TAG, "Existing image cleared");

        if (itemExpandedImage != null
                && itemExpandedImage.getVisibility() != View.GONE
                && itemExpandedImage.getHeight() > 0) {
            if (DEBUG) Log.i(TAG, "Image already expanded, skipping");
            return;
        }

        // calculate image dimensions
        if (DEBUG) Log.i(TAG, "post size " + postW + "x" + postH);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        threadActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //int padding = ChanGridSizer.dpToPx(displayMetrics, 16);
        int maxWidth = displayMetrics.widthPixels;
        int maxHeight = maxWidth; // to avoid excessively big images
        //int maxHeight = displayMetrics.heightPixels; // to avoid excessively big images
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
        //if (listPosition > 0) {
            ViewGroup.LayoutParams params = itemExpandedImage.getLayoutParams();
            if (params != null) {
                params.width = listPosition == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : width;
                params.height = listPosition == 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : height;
                if (DEBUG) Log.i(TAG, "set expanded image size=" + width + "x" + height);
            }
            int paddingTop = (flags & ChanPost.FLAG_HAS_TEXT) > 0 ? 0 : padding8Dp;
            int paddingBottom = padding8Dp;
            itemExpandedImage.setPadding(0, paddingTop, 0, paddingBottom);
        //}
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
                NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
                if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD)
                    msg = threadActivity.getString(R.string.thread_couldnt_download_image_down);
                else if (failReason.getType() == FailReason.FailType.NETWORK_DENIED
                        || failReason.getType() == FailReason.FailType.IO_ERROR
                        || reason.equalsIgnoreCase("io_error"))
                    msg = threadActivity.getString(R.string.thread_couldnt_download_image);
                else
                    msg = String.format(threadActivity.getString(R.string.thread_couldnt_load_image), failReason.getType().toString().toLowerCase().replaceAll("_", " "));
                if (DEBUG) Log.e(TAG, "Failed to download " + postImageUrl + " to file=" + fullImagePath + " reason=" + reason);
                Toast.makeText(threadActivity, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (itemExpandedProgressBar != null)
                    itemExpandedProgressBar.setVisibility(View.GONE);
                if (itemThumbnailImage != null && listPosition == 0) // thread header loads image over thumbnail
                    itemThumbnailImage.setImageDrawable(null);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                collapseImageView();
                Toast.makeText(threadActivity, R.string.thread_couldnt_load_image_cancelled, Toast.LENGTH_SHORT).show();
            }
        }); // load async
    }

    private void expandExif() {
        if (DEBUG) Log.i(TAG, "Expanding exifText=" + exifText);
        if (itemExifView != null && exifText != null && !exifText.isEmpty()) {
            itemExifView.setText(Html.fromHtml(exifText));
            itemExifView.setVisibility(View.VISIBLE);
        }
    }

    private void expandSpoiler() {
        if (DEBUG) Log.i(TAG, "Expanding spoiler subject=" + spoilerSubject + " text=" + spoilerText);
        if (itemSubjectView != null && spoilerSubject != null && !spoilerSubject.isEmpty()) {
            itemSubjectView.setText(Html.fromHtml(spoilerSubject));
            itemSubjectView.setVisibility(View.VISIBLE);
        }
        if (itemTextView != null && spoilerText != null && !spoilerText.isEmpty()) {
            itemTextView.setText(Html.fromHtml(spoilerText));
            itemTextView.setVisibility(View.VISIBLE);
        }
    }

}
