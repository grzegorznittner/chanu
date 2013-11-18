package com.chanapps.four.component;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.viewer.ThreadViewHolder;
import com.chanapps.four.viewer.ThreadViewer;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
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
public class ThreadImageExpander {

    private static final String TAG = ThreadImageExpander.class.getSimpleName();
    private static final boolean DEBUG = true;
    //private static final double MAX_EXPANDED_SCALE = 1.5;

    private ThreadViewHolder viewHolder;
    private View.OnClickListener expandedImageListener;
    private String postImageUrl = null;
    private int postW = 0;
    private int postH = 0;
    private String fullImagePath = null;
    private boolean withProgress;
    private int stub;

    public ThreadImageExpander(ThreadViewHolder viewHolder, final Cursor cursor,
                               View.OnClickListener expandedImageListener, boolean withProgress, int stub) {
        this.viewHolder = viewHolder;
        this.expandedImageListener = expandedImageListener;
        this.withProgress = withProgress;
        this.stub = stub;

        long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
        String postExt = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EXT));
        Uri uri = ChanFileStorage.getHiddenLocalImageUri(viewHolder.list_item.getContext(), boardCode, postId, postExt);

        postW = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_W));
        postH = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_H));
        postImageUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_FULL_IMAGE_URL));
        if (postImageUrl != null && postImageUrl.endsWith(".gif")) {
        	String thumbUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        	File thumbFile = ImageLoader.getInstance().getDiscCache().get(thumbUrl);
        	fullImagePath = thumbFile != null ? thumbFile.getAbsolutePath() : null;
        } else {
        	fullImagePath = (new File(URI.create(uri.toString()))).getAbsolutePath();
        }
        if (DEBUG) Log.i(TAG, "postUrl=" + postImageUrl + " postSize=" + postW + "x" + postH);
    }

    private void hideThumbnail() {
        if (DEBUG) Log.i(TAG, "hiding thumbnail...");
        if (viewHolder.list_item_image == null) {
            if (DEBUG) Log.i(TAG, "Couldn't hide thumbnail, null thumbnail image");
            return;
        }
        //viewHolder.list_item_image.setImageDrawable(null);
        viewHolder.list_item_image.setVisibility(View.INVISIBLE);
        if (viewHolder.list_item_image_wrapper == null) {
            if (DEBUG) Log.i(TAG, "Skipping adjusting thumbnail height, null thumbnail image wrapper");
            return;
        }
        if (viewHolder.list_item_left_spacer != null)
            viewHolder.list_item_left_spacer.setVisibility(View.VISIBLE);
        //viewHolder.list_item_image_wrapper.setVisibility(View.INVISIBLE);
    }

    private void setImageDimensions(Point targetSize) {
        ViewGroup.LayoutParams params = viewHolder.list_item_image_expanded.getLayoutParams();
        if (params == null) {
            if (DEBUG) Log.i(TAG, "setImageDimensions() null params, exiting");
            return;
        }
        params.width = targetSize.x;
        params.height = targetSize.y;
        if (DEBUG) Log.i(TAG, "setImageDimensions() to " + params.width + "x" + params.height);
        if (viewHolder.list_item_image_expanded_click_effect != null) {
            ViewGroup.LayoutParams params2 = viewHolder.list_item_image_expanded_click_effect.getLayoutParams();
            if (params2 != null) {
                params2.width = params.width;
                params2.height = params.height;
            }
        }
        if (viewHolder.list_item_image_expanded_wrapper != null) {
            ViewGroup.LayoutParams params3 = viewHolder.list_item_image_expanded_wrapper.getLayoutParams();
            if (params3 != null) {
                //params3.width = params.width; always match width
                params3.height = params.height;
            }
        }
    }

    public void displayImage() {
        if (withProgress) {
            viewHolder.list_item_image_expanded_wrapper.setVisibility(View.VISIBLE);
            viewHolder.list_item_expanded_progress_bar.setVisibility(View.VISIBLE);
        }
        else {
            hideThumbnail();
            viewHolder.list_item_expanded_progress_bar.setVisibility(View.GONE);
        }

        Point targetSize = ThreadViewer.sizeHeaderImage(postW, postH);
        if (DEBUG) Log.i(TAG, "inputSize=" + postW + "x" + postH + " targetSize=" + targetSize.x + "x" + targetSize.y);
        setImageDimensions(targetSize);

        if (DEBUG) Log.i(TAG, "Set expanded image to visible");
        if (viewHolder.list_item_expanded_progress_bar != null)
            viewHolder.list_item_expanded_progress_bar.setVisibility(withProgress ? View.VISIBLE : View.GONE);
        if (viewHolder.list_item_image_expanded_wrapper != null)
            viewHolder.list_item_image_expanded_wrapper.setVisibility(View.VISIBLE);
        if (viewHolder.list_item_image_expanded != null)
            viewHolder.list_item_image_expanded.setVisibility(View.VISIBLE);

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
                .showStubImage(stub)
                .build();

        // display image async
        ChanImageLoader
                .getInstance(viewHolder.list_item_image_expanded.getContext())
                .displayImage(postImageUrl, viewHolder.list_item_image_expanded,
                        expandedDisplayImageOptions, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                //collapseImageView();
                String reason = failReason.toString();
                /*
                Context context = viewHolder.list_item_image_expanded.getContext();
                NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
                String msg;
                if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD)
                    msg = context.getString(R.string.thread_couldnt_download_image_down);
                else if (failReason.getType() == FailReason.FailType.NETWORK_DENIED
                        || failReason.getType() == FailReason.FailType.IO_ERROR
                        || reason.equalsIgnoreCase("io_error"))
                    msg = context.getString(R.string.thread_couldnt_download_image);
                else
                    msg = String.format(context.getString(R.string.thread_couldnt_load_image), failReason.getType().toString().toLowerCase().replaceAll("_", " "));
                */
                if (DEBUG) Log.e(TAG, "Failed to download " + postImageUrl + " to file=" + fullImagePath + " reason=" + reason);
                //Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                if (viewHolder.list_item_expanded_progress_bar != null && withProgress)
                    viewHolder.list_item_expanded_progress_bar.setVisibility(View.GONE);
                if (viewHolder.list_item_image_expanded_click_effect != null) {
                    if (expandedImageListener != null) {
                        viewHolder.list_item_image_expanded_click_effect.setVisibility(View.VISIBLE);
                        viewHolder.list_item_image_expanded_click_effect.setOnClickListener(expandedImageListener);
                    }
                    else {
                        viewHolder.list_item_image_expanded_click_effect.setVisibility(View.GONE);
                        viewHolder.list_item_image_expanded_click_effect.setOnClickListener(null);
                    }
                }
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (DEBUG) Log.v(TAG, "onLoadingComplete uri=" + imageUri);
                if (viewHolder.list_item_expanded_progress_bar != null && withProgress)
                    viewHolder.list_item_expanded_progress_bar.setVisibility(View.GONE);
                if (viewHolder.list_item_image_expanded_click_effect != null) {
                    if (expandedImageListener != null) {
                        viewHolder.list_item_image_expanded_click_effect.setVisibility(View.VISIBLE);
                        viewHolder.list_item_image_expanded_click_effect.setOnClickListener(expandedImageListener);
                    }
                    else {
                        viewHolder.list_item_image_expanded_click_effect.setVisibility(View.GONE);
                        viewHolder.list_item_image_expanded_click_effect.setOnClickListener(null);
                    }
                }
                if (viewHolder.list_item_image_expansion_target != null) {
                    //viewHolder.list_item_image_expansion_target.setOnClickListener(null);
                    //viewHolder.list_item_image_expansion_target.setForeground(view.getResources().getDrawable(R.drawable.null_selector_bg));
                }
                if (withProgress)
                    hideThumbnail();
                if (viewHolder.list_item != null)
                    viewHolder.list_item.setTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED, Boolean.TRUE);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                if (viewHolder.list_item_expanded_progress_bar != null && withProgress)
                    viewHolder.list_item_expanded_progress_bar.setVisibility(View.GONE);
                //collapseImageView();
                //Context context = viewHolder.list_item_image_expanded.getContext();
                //Toast.makeText(context, R.string.thread_couldnt_load_image_cancelled, Toast.LENGTH_SHORT).show();
            }
        }); // load async
    }

    private boolean shouldExpandImage() {
        if (viewHolder.list_item_image_expanded != null
                && viewHolder.list_item_image_expanded.getVisibility() != View.GONE
                && viewHolder.list_item_image_expanded.getHeight() > 0) {
            if (DEBUG) Log.i(TAG, "Image already expanded, skipping");
            return false;
        }
        return true;
    }

    /*
    private void clearImage() {
        if (DEBUG) Log.i(TAG, "Clearing existing image");
        ThreadViewer.clearBigImageView(viewHolder.list_item_image_expanded); // clear old image
        if (DEBUG) Log.i(TAG, "Existing image cleared");
    }

    private void expandImage() {
        if (!shouldExpandImage()) {
            collapseImageView();
            return;
        }
        clearImage();
        //ThreadViewer.initStatics(viewHolder.list_item_image_expanded);
        //Point targetSize = sizeExpandedImage(postW, postH);
        Point targetSize = ThreadViewer.sizeHeaderImage(postW, postH);
        if (DEBUG) Log.i(TAG, "inputSize=" + postW + "x" + postH + " targetSize=" + targetSize.x + "x" + targetSize.y);
        setImageDimensions(targetSize);
        displayImage(targetSize, true);
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
