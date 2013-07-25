package com.chanapps.four.component;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanFileStorage;
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
    private static final boolean DEBUG = true;
    private static final double MAX_EXPANDED_SCALE = 1.5;

    private View itemView;
    private View listItemLeftSpacer;
    private View.OnClickListener expandedImageListener;
    private ViewGroup itemThumbnailImageWrapper;
    private ImageView itemThumbnailImage;
    private ViewGroup itemExpandedWrapper;
    private ImageView itemExpandedImage;
    private View itemExpandedImageClickEffect;
    private ProgressBar itemExpandedProgressBar;
    private String postImageUrl = null;
    private int postW = 0;
    private int postH = 0;
    private int listPosition = 0;
    private String fullImagePath = null;
    private int flags;

    public ThreadExpandImageOnClickListener(Context context, final Cursor cursor, final View itemView,
                                            View.OnClickListener expandedImageListener) {
        long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
        String postExt = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EXT));
        Uri uri = ChanFileStorage.getLocalImageUri(context, boardCode, postId, postExt);

        this.itemView = itemView;
        this.expandedImageListener = expandedImageListener;
        itemThumbnailImageWrapper = (ViewGroup)itemView.findViewById(R.id.list_item_image_wrapper);
        itemThumbnailImage = (ImageView)itemView.findViewById(R.id.list_item_image);
        itemExpandedWrapper = (ViewGroup)itemView.findViewById(R.id.list_item_image_expanded_wrapper);
        listItemLeftSpacer = itemView.findViewById(R.id.list_item_left_spacer);
        itemExpandedImage = (ImageView)itemView.findViewById(R.id.list_item_image_expanded);
        itemExpandedImageClickEffect = itemView.findViewById(R.id.list_item_image_expanded_click_effect);
        itemExpandedProgressBar = (ProgressBar)itemView.findViewById(R.id.list_item_expanded_progress_bar);

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
        if (itemExpandedWrapper != null)
            itemExpandedWrapper.setVisibility(View.GONE);
        if (itemExpandedProgressBar != null)
            itemExpandedProgressBar.setVisibility(View.GONE);
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
        if (listItemLeftSpacer != null)
            listItemLeftSpacer.setVisibility(View.VISIBLE);
        itemThumbnailImageWrapper.setVisibility(View.GONE);
    }


    @Override
    public void onClick(View v) {
        if (DEBUG) Log.i(TAG, "expanding pos=" + listPosition);
        if ((flags & ChanPost.FLAG_HAS_IMAGE) > 0)
            expandImage();
    }

    private void setImageDimensions(Point targetSize) {
        ViewGroup.LayoutParams params = itemExpandedImage.getLayoutParams();
        if (params == null) {
            if (DEBUG) Log.i(TAG, "setImageDimensions() null params, exiting");
            return;
        }
        params.width = targetSize.x;
        params.height = targetSize.y;
        if (DEBUG) Log.i(TAG, "setImageDimensions() to " + params.width + "x" + params.height);
        if (itemExpandedImageClickEffect != null) {
            ViewGroup.LayoutParams params2 = itemExpandedImageClickEffect.getLayoutParams();
            if (params2 != null) {
                params2.width = params.width;
                params2.height = params.height;
            }
        }
        if (itemExpandedWrapper != null) {
            ViewGroup.LayoutParams params3 = itemExpandedWrapper.getLayoutParams();
            if (params3 != null) {
                //params3.width = params.width; always match width
                params3.height = params.height;
            }
        }
    }

    private void displayImage(Point targetSize, final boolean withProgress) {
        if (DEBUG) Log.i(TAG, "Set expanded image to visible");
        if (itemExpandedProgressBar != null)
            itemExpandedProgressBar.setVisibility(withProgress ? View.VISIBLE : View.GONE);
        if (itemExpandedWrapper != null)
            itemExpandedWrapper.setVisibility(View.VISIBLE);
        if (itemExpandedImage != null)
            itemExpandedImage.setVisibility(View.VISIBLE);

        int width = targetSize.x; // may need to adjust to avoid out of mem
        int height = targetSize.y;
        ImageSize imageSize = new ImageSize(width, height); // load image at half res to avoid out of mem
        if (DEBUG) Log.i(TAG, "Downsampling image to size=" + imageSize.getWidth() + "x" + imageSize.getHeight());
        DisplayImageOptions expandedDisplayImageOptions = new DisplayImageOptions.Builder()
                .imageSize(imageSize)
                //.imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
                .cacheOnDisc()
                .fullSizeImageLocation(fullImagePath)
                .resetViewBeforeLoading()
                .showStubImage(R.drawable.stub_image_background)
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
                //collapseImageView();
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
                //Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                if (itemExpandedProgressBar != null && withProgress)
                    itemExpandedProgressBar.setVisibility(View.GONE);
                if (itemExpandedImageClickEffect != null) {
                    itemExpandedImageClickEffect.setVisibility(View.VISIBLE);
                    itemExpandedImageClickEffect.setOnClickListener(expandedImageListener);
                }
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (itemExpandedProgressBar != null && withProgress)
                    itemExpandedProgressBar.setVisibility(View.GONE);
                if (itemExpandedImageClickEffect != null) {
                    itemExpandedImageClickEffect.setVisibility(View.VISIBLE);
                    itemExpandedImageClickEffect.setOnClickListener(expandedImageListener);
                }
                if (withProgress)
                    hideThumbnail();
                if (itemView != null)
                    itemView.setTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED, Boolean.TRUE);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                if (itemExpandedProgressBar != null && withProgress)
                    itemExpandedProgressBar.setVisibility(View.GONE);
                //collapseImageView();
                //Context context = itemExpandedImage.getContext();
                //Toast.makeText(context, R.string.thread_couldnt_load_image_cancelled, Toast.LENGTH_SHORT).show();
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
        ThreadViewer.clearBigImageView(itemExpandedImage); // clear old image
        if (DEBUG) Log.i(TAG, "Existing image cleared");
    }

    private void expandImage() {
        if (!shouldExpandImage()) {
            collapseImageView();
            return;
        }
        clearImage();
        ThreadViewer.initStatics(itemExpandedImage);
        //Point targetSize = sizeExpandedImage(postW, postH);
        Point targetSize = ThreadViewer.sizeHeaderImage(postW, postH);
        if (DEBUG) Log.i(TAG, "inputSize=" + postW + "x" + postH + " targetSize=" + targetSize.x + "x" + targetSize.y);
        setImageDimensions(targetSize);
        displayImage(targetSize, true);
    }

    public void displayAutoExpandedImage() {
        itemExpandedProgressBar.setVisibility(View.VISIBLE);
        ThreadViewer.initStatics(itemExpandedImage);
        //Point targetSize = sizeExpandedImage(postW, postH);
        Point targetSize = ThreadViewer.sizeHeaderImage(postW, postH);
        if (DEBUG) Log.i(TAG, "inputSize=" + postW + "x" + postH + " targetSize=" + targetSize.x + "x" + targetSize.y);
        setImageDimensions(targetSize);
        displayImage(targetSize, true);
    }

    public void displayCachedExpandedImage() {
        hideThumbnail();
        itemExpandedProgressBar.setVisibility(View.GONE);
        ThreadViewer.initStatics(itemExpandedImage);
        //Point targetSize = sizeExpandedImage(postW, postH);
        Point targetSize = ThreadViewer.sizeHeaderImage(postW, postH);
        if (DEBUG) Log.i(TAG, "inputSize=" + postW + "x" + postH + " targetSize=" + targetSize.x + "x" + targetSize.y);
        setImageDimensions(targetSize);
        displayImage(targetSize, false);
    }

    private static Point sizeExpandedImage(final int actualWidth, final int actualHeight) {
        Point imageSize = new Point();
        double aspectRatio = (double) actualWidth / (double) actualHeight;

        if (aspectRatio < 1) { // tall image, restrict by height
            int desiredHeight =
                    //powerOfTwoReduce(
                    //        actualHeight,
                            Math.min(ThreadViewer.cardMaxImageHeight(), (int)(actualHeight * MAX_EXPANDED_SCALE))
                    //)
                    ;
            imageSize.x = (int) (aspectRatio * (double) desiredHeight);
            imageSize.y = desiredHeight;
        } else {
            int desiredWidth =
                    //powerOfTwoReduce(
                    //        actualWidth,
                            Math.min(ThreadViewer.cardMaxImageWidth(), (int)(actualWidth * MAX_EXPANDED_SCALE))
                    //)
                    ;
            imageSize.x = desiredWidth; // restrict by width normally
            imageSize.y = (int) ((double) desiredWidth / aspectRatio);
        }
        if (DEBUG) com.android.gallery3d.ui.Log.v(TAG, "Input size=" + actualWidth + "x" + actualHeight + " output size=" + imageSize.x + "x" + imageSize.y);
        return imageSize;
    }
    /*
    private static int powerOfTwoReduce(double a, double d) { // actual, desired INT_SAMPLE_POWER_OF_2
        if (a <= d)
            return (int)a;
        /* a > d

           a/2^n <= d
           a <= d * 2^n
           a/d <= 2^n
           ln(a/d)/ln(2) <= n
           n >= ln(a/d)/ln(2)
           p = ceil(n)

           s = 2^p
           o = a / s
        */
        /*
        double n = Math.log(a/d) / Math.log(2);
        int p = (int)Math.floor(n);
        int s = (int)Math.pow(2, p); // scale
        int o = (int)(a / s);
        if (DEBUG) Log.i(TAG, "powerOfTwoReduce(a=" + a + ", d=" + d + ") n=" + n + " p=" + p + " s=" + s + " o=" + o);
        return o;
    }
    */
}
