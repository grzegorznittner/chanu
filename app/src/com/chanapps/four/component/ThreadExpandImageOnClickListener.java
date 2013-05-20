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
import android.widget.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
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
public class ThreadExpandImageOnClickListener implements View.OnClickListener {

    private static final String TAG = ThreadExpandImageOnClickListener.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int NO_TEXT_PADDING_TOP_DP = 8;

    private ViewGroup itemHeaderWrapper;
    private ViewGroup itemThumbnailImageWrapper;
    private ImageView itemThumbnailImage;
    private ViewGroup itemExpandedWrapper;
    private ImageView itemExpandedImage;
    private View itemExpandedImageClickEffect;
    private ProgressBar itemExpandedProgressBar;
    private TextView itemSubjectView;
    private TextView itemHeadlineView;
    private TextView itemTextView;
    private String postImageUrl = null;
    private String thumbImageUrl = null;
    private int postW = 0;
    private int postH = 0;
    private int listPosition = 0;
    private String fullImagePath = null;
    private int padding8Dp;
    private int itemThumbWidth;
    private int itemThumbEmptyWidth;
    private int flags;
    private String spoilerSubject;
    private String spoilerText;

    public ThreadExpandImageOnClickListener(Context context, final Cursor cursor, final View itemView) {
        long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
        String postExt = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EXT));
        Uri uri = ChanFileStorage.getLocalImageUri(context, boardCode, postId, postExt);
        thumbImageUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        spoilerSubject = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SPOILER_SUBJECT));
        spoilerText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_SPOILER_TEXT));

        itemHeaderWrapper = (ViewGroup)itemView.findViewById(R.id.list_item_header_wrapper);
        itemThumbnailImageWrapper = (ViewGroup)itemView.findViewById(R.id.list_item_image_wrapper);
        itemThumbnailImage = (ImageView)itemView.findViewById(R.id.list_item_image);
        itemExpandedWrapper = (ViewGroup)itemView.findViewById(R.id.list_item_image_expanded_wrapper);
        itemExpandedImage = (ImageView)itemView.findViewById(R.id.list_item_image_expanded);
        itemExpandedImageClickEffect = itemView.findViewById(R.id.list_item_image_expanded_click_effect);
        itemExpandedProgressBar = (ProgressBar)itemView.findViewById(R.id.list_item_expanded_progress_bar);
        itemSubjectView = (TextView)itemView.findViewById(R.id.list_item_subject);
        itemHeadlineView = (TextView)itemView.findViewById(R.id.list_item_header);
        itemTextView = (TextView)itemView.findViewById(R.id.list_item_text);

        DisplayMetrics displayMetrics = itemExpandedImage.getResources().getDisplayMetrics();
        padding8Dp = ChanGridSizer.dpToPx(displayMetrics, NO_TEXT_PADDING_TOP_DP);
        itemThumbWidth = ChanGridSizer.dpToPx(displayMetrics, ThreadViewer.ITEM_THUMB_WIDTH_DP);
        itemThumbEmptyWidth = ChanGridSizer.dpToPx(displayMetrics, ThreadViewer.ITEM_THUMB_EMPTY_DP);

        listPosition = cursor.getPosition();
        postW = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_W));
        postH = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_H));
        postImageUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_FULL_IMAGE_URL));
        fullImagePath = (new File(URI.create(uri.toString()))).getAbsolutePath();
        flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
        if (DEBUG) Log.i(TAG, "postUrl=" + postImageUrl + " postSize=" + postW + "x" + postH);
    }

    private void collapseImageView() {
        if (DEBUG) Log.i(TAG, "collapsed pos=" + listPosition);
        //ChanHelper.clearBigImageView(itemExpandedImage);
        if (itemExpandedWrapper != null)
            itemExpandedWrapper.setVisibility(View.GONE);
        if (itemExpandedProgressBar != null)
            itemExpandedProgressBar.setVisibility(View.GONE);
        //itemExpandedImage.setVisibility(View.GONE);
        //showThumbnail();
    }

    private void hideThumbnail() {
        if (DEBUG) Log.i(TAG, "hiding thumbnail...");
        if (itemThumbnailImage == null) {
            if (DEBUG) Log.i(TAG, "Couldn't hide thumbnail, null thumbnail image");
            return;
        }
        itemThumbnailImage.setImageDrawable(null);
        itemThumbnailImage.setVisibility(View.GONE);
        if (itemThumbnailImageWrapper == null) {
            if (DEBUG) Log.i(TAG, "Skipping adjusting thumbnail height, null thumbnail image wrapper");
            return;
        }
        itemThumbnailImageWrapper.setVisibility(View.GONE);
        /*
        ViewGroup.LayoutParams params = itemThumbnailImageWrapper.getLayoutParams();
        if (params == null)
            return;
        params.width = itemThumbEmptyWidth;

        if (itemHeaderWrapper == null) {
            if (DEBUG) Log.i(TAG, "Skipping adjusting thumbnail height, no header wrapper");
            return;
        }
        ViewGroup.LayoutParams params2 = itemHeaderWrapper.getLayoutParams();
        if (params2 == null)
            return;

        // find wrapper height, wrap_content doesn't work in this case
        int subjectHeight = measureTextViewHeight(itemSubjectView);
        int spacer = subjectHeight > 0 ? padding8Dp : 0;
        int headlineHeight = measureTextViewHeight(itemHeadlineView);
        int wrapperHeight = padding8Dp/2 + subjectHeight + spacer + headlineHeight + padding8Dp;
        if (DEBUG) Log.i(TAG, "subjectHeight=" + subjectHeight + " headlineHeight=" + headlineHeight + " wrapperHeight=" + wrapperHeight);
        params2.height = wrapperHeight; // for some reason doesn't respect wrap_content
        */
    }

    /*
    private int measureTextViewHeight(TextView tv) {
        Context context = tv.getContext();
        CharSequence text = tv.getText();
        if (text == null || text.length() == 0)
            return 0;
        TextPaint paint = tv.getPaint();
        int width = context.getResources().getDisplayMetrics().widthPixels - 4 * padding8Dp;
        StaticLayout staticLayout = new StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 1.0f, false);
        return staticLayout.getHeight();
    }

    private void showThumbnail() {
        if (itemThumbnailImageWrapper == null || itemThumbnailImage == null)
            return;
        ViewGroup.LayoutParams params = itemThumbnailImageWrapper.getLayoutParams();
        if (params == null)
            return;
        params.width = itemThumbWidth;
        ChanImageLoader.getInstance(itemThumbnailImage.getContext()).displayImage(thumbImageUrl, itemThumbnailImage);
    }
    */
    @Override
    public void onClick(View v) {
        if (DEBUG) Log.i(TAG, "expanding pos=" + listPosition);
        if ((flags & ChanPost.FLAG_HAS_IMAGE) > 0)
            expandImage();
        if ((flags & ChanPost.FLAG_HAS_SPOILER) > 0)
            expandSpoiler();
    }

    /*
    private Point calculateImageDimensions() {
        if (DEBUG) Log.i(TAG, "post size " + postW + "x" + postH);
        DisplayMetrics displayMetrics = itemExpandedImage.getResources().getDisplayMetrics();
        int maxWidth = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        int maxHeight = maxWidth; // to avoid excessively big images
        //itemExpandedImage.setMaxWidth(maxWidth);
        //itemExpandedImage.setMaxHeight(maxHeight);
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
        return new Point(width, height);
    }
    */
    private void setImageDimensions(Point targetSize) {
        ViewGroup.LayoutParams params = itemExpandedImage.getLayoutParams();
        if (params != null) {
            /*
            if ((flags & ChanPost.FLAG_IS_HEADER) > 0
                    && itemThumbnailImage != null
                    && itemThumbnailImage.getLayoutParams() != null) { // for thread header use existing params
                ViewGroup.LayoutParams thumbParams = itemThumbnailImage.getLayoutParams();
                params.width = thumbParams.width;
                params.height = thumbParams.height;
            }
            else {
            */
            params.width = targetSize.x;
            params.height = targetSize.y;
            //}
            if (DEBUG) Log.i(TAG, "set expanded image size=" + params.width + "x" + params.height);
            ViewGroup.LayoutParams params2 = itemExpandedImageClickEffect.getLayoutParams();
            if (params2 != null) {
                params2.width = params.width;
                params2.height = params.height;
                if (DEBUG) Log.i(TAG, "set expanded image click effect size=" + params2.width + "x" + params2.height);
            }
        }
    }

    /*
    private void scheduleScrollIfNeeded(Point targetSize) {
        int lastPosition = threadActivity.getAbsListView().getLastVisiblePosition();
        boolean shouldMove = listPosition > 0 && listPosition >= lastPosition - 1;
        if (shouldMove && threadActivity.getChanHandler() != null) {
            final int scrollDistance = targetSize.y;
            threadActivity.getChanHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    threadActivity.getAbsListView().smoothScrollBy(scrollDistance, 250);
                }
            }, 250);
        }
    }
    */

    private void displayImage(Point targetSize) {
        if (DEBUG) Log.i(TAG, "Set expanded image to visible");
        if (itemExpandedProgressBar != null)
            itemExpandedProgressBar.setVisibility(View.VISIBLE);

        int width = targetSize.x; // may need to adjust to avoid out of mem
        int height = targetSize.y;
        ImageSize imageSize = new ImageSize(width, height); // load image at half res to avoid out of mem
        if (DEBUG) Log.i(TAG, "Downsampling image to size=" + imageSize.getWidth() + "x" + imageSize.getHeight());
        DisplayImageOptions expandedDisplayImageOptions = new DisplayImageOptions.Builder()
                .imageSize(imageSize)
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .cacheOnDisc()
                .fullSizeImageLocation(fullImagePath)
                .resetViewBeforeLoading()
                .build();

        // display image async
        ChanImageLoader
                .getInstance(itemExpandedImage.getContext())
                .displayImage(postImageUrl, itemExpandedImage, expandedDisplayImageOptions, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                collapseImageView();
                String reason = failReason.toString();
                String msg;
                Context context = itemExpandedImage.getContext();
                NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
                if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD)
                    msg = context.getString(R.string.thread_couldnt_download_image_down);
                else if (failReason.getType() == FailReason.FailType.NETWORK_DENIED
                        || failReason.getType() == FailReason.FailType.IO_ERROR
                        || reason.equalsIgnoreCase("io_error"))
                    msg = context.getString(R.string.thread_couldnt_download_image);
                else
                    msg = String.format(context.getString(R.string.thread_couldnt_load_image), failReason.getType().toString().toLowerCase().replaceAll("_", " "));
                if (DEBUG) Log.e(TAG, "Failed to download " + postImageUrl + " to file=" + fullImagePath + " reason=" + reason);
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (itemExpandedProgressBar != null)
                    itemExpandedProgressBar.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
                if (itemExpandedImageClickEffect != null)
                    itemExpandedImageClickEffect.setVisibility(View.VISIBLE);
                if (itemExpandedWrapper != null)
                    itemExpandedWrapper.setVisibility(View.VISIBLE);
                hideThumbnail();
                //if (itemThumbnailImage != null && listPosition == 0) // thread header loads image over thumbnail
                //    itemThumbnailImage.setImageDrawable(null);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                collapseImageView();
                Context context = itemExpandedImage.getContext();
                Toast.makeText(context, R.string.thread_couldnt_load_image_cancelled, Toast.LENGTH_SHORT).show();
            }
        }); // load async
    }

    private boolean shouldExpandImage() {
        if (itemExpandedImage != null
                && itemExpandedImage.getVisibility() != View.GONE
                && itemExpandedImage.getHeight() > 0) {
            if (DEBUG) Log.i(TAG, "Image already expanded, skipping");
            return false;
        }
        return true;
    }

    private void clearImage() {
        if (DEBUG) Log.i(TAG, "Clearing existing image");
        ChanHelper.clearBigImageView(itemExpandedImage); // clear old image
        if (DEBUG) Log.i(TAG, "Existing image cleared");
    }

    private void expandImage() {
        if (!shouldExpandImage()) {
            collapseImageView();
            return;
        }
        clearImage();
        ThreadViewer.initStatics(itemExpandedImage);
        Point targetSize = ThreadViewer.sizeHeaderImage(postW, postH);
        //calculateImageDimensions();
        if (DEBUG) Log.i(TAG, "inputSize=" + postW + "x" + postH + " targetSize=" + targetSize.x + "x" + targetSize.y);
        setImageDimensions(targetSize);
        //scheduleScrollIfNeeded(targetSize);
        displayImage(targetSize);
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
