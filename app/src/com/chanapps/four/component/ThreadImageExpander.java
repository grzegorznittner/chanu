package com.chanapps.four.component;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import android.webkit.WebView;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.gallery.ChanImage;
import com.chanapps.four.viewer.ThreadViewHolder;
import com.chanapps.four.viewer.ThreadViewer;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

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
    private static final boolean DEBUG = false;

    public static final String WEBVIEW_BLANK_URL = "about:blank";
    private static final int BIG_IMAGE_SIZE_BYTES = 1024 * 250; // more than 250kb, show in web view
    private static final int BYTES_PER_PIXEL = 4; // Bitmap.Config.ARGB_8888;
    //private static final double MAX_EXPANDED_SCALE = 1.5;

    private ThreadViewHolder viewHolder;
    private String thumbUrl = null;
    private String postImageUrl = null;
    private int thumbW = 0;
    private int thumbH = 0;
    private int postW = 0;
    private int postH = 0;
    private String fullImagePath = null;
    private boolean withProgress;
    private int stub;
    private Point targetSize;
    private String postExt;
    private int fsize;
    private long postNo;
    private long threadNo;
    private String boardCode;
    private View.OnClickListener expandedImageListener;
    private boolean showContextMenu = true;
    private boolean isVideo = false;

    public ThreadImageExpander(ThreadViewHolder viewHolder, final Cursor cursor, boolean withProgress, int stub,
                               View.OnClickListener expandedImageListener, boolean showContextMenu) {
        this.viewHolder = viewHolder;
        this.withProgress = withProgress;
        this.stub = stub;
        this.expandedImageListener = expandedImageListener;

        boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
        long resto = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
        postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        threadNo = resto > 0 ? postNo : resto;

        postExt = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EXT));
        fsize = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FSIZE));
        Uri uri = ChanFileStorage.getHiddenLocalImageUri(viewHolder.list_item.getContext(), boardCode, postNo, postExt);

        thumbW = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_W));
        thumbH = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_TN_H));
        postW = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_W));
        postH = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_H));
        postImageUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_FULL_IMAGE_URL));
        thumbUrl = cursor.getString(cursor.getColumnIndex(ChanPost.POST_IMAGE_URL));
        if (postImageUrl != null && postImageUrl.endsWith(".gif")) {
        	File thumbFile = ImageLoader.getInstance().getDiscCache().get(thumbUrl);
        	fullImagePath = thumbFile != null ? thumbFile.getAbsolutePath() : null;
        } else {
        	fullImagePath = (new File(URI.create(uri.toString()))).getAbsolutePath();
        }
        this.showContextMenu = showContextMenu;
        if (DEBUG) Log.i(TAG, "postUrl=" + postImageUrl + " postSize=" + postW + "x" + postH);
    }

    static public void setImageDimensions(ThreadViewHolder viewHolder, Point targetSize) {
        //ViewGroup.LayoutParams params = viewHolder.list_item_image_expanded.getLayoutParams();
        if (DEBUG) Log.i(TAG, "setImageDimensions() to " + targetSize.x + "x" + targetSize.y);
        if (viewHolder.list_item_image_expanded_click_effect != null) {
            ViewGroup.LayoutParams params2 = viewHolder.list_item_image_expanded_click_effect.getLayoutParams();
            if (params2 != null) {
                params2.width = targetSize.x;
                params2.height = targetSize.y;
            }
        }
        if (viewHolder.list_item_image_expanded_webview != null) {
            ViewGroup.LayoutParams params3 = viewHolder.list_item_image_expanded_webview.getLayoutParams();
            if (params3 != null) {
                params3.width = targetSize.x;
                params3.height = targetSize.y;
            }
        }
        if (viewHolder.list_item_image_expanded_wrapper != null) {
            ViewGroup.LayoutParams params4 = viewHolder.list_item_image_expanded_wrapper.getLayoutParams();
            if (params4 != null) {
                //params3.width = params.width; always match width
                params4.height = targetSize.y;
            }
        }
    }

    public void displayImage() {
        viewHolder.isWebView = true;
        isVideo = ChanImage.isVideo(postExt, fsize, postW, postH);
        int width = isVideo ? thumbW : postW;
        int height = isVideo ? thumbH : postH;
        targetSize = ThreadViewer.sizeHeaderImage(width, height, showContextMenu);
        if (DEBUG) Log.i(TAG, "inputSize=" + width + "x" + height + " targetSize=" + targetSize.x + "x" + targetSize.y);
        setImageDimensions(viewHolder, targetSize);
        displayWebView(width, height);
        /*
        viewHolder.isWebView = isAnimatedGif() || isBigImage(targetSize);
        if (viewHolder.isWebView)
            displayWebView();
        else
            displayImageView();
        */
    }
    /*
    protected boolean isAnimatedGif() {
        return ChanImage.isAnimatedGif(postExt, fsize, postW, postH);
    }

    protected boolean isBigImage(Point targetSize) {
        int targetSizeBytes = targetSize.x * targetSize.y * BYTES_PER_PIXEL;
        return fsize > BIG_IMAGE_SIZE_BYTES
                || targetSizeBytes > BIG_IMAGE_SIZE_BYTES;
    }
    */
    protected void displayWebView(int width, int height) {
        if (viewHolder.list_item_image_expanded_wrapper != null)
            viewHolder.list_item_image_expanded_wrapper.setVisibility(View.VISIBLE);
        /*
        if (viewHolder.list_item_expanded_progress_bar != null)
            viewHolder.list_item_expanded_progress_bar.setVisibility(View.GONE);
        if (viewHolder.list_item_image_expanded != null)
            viewHolder.list_item_image_expanded.setVisibility(View.GONE);
        */
        if (viewHolder.list_item_image_wrapper != null)
            viewHolder.list_item_image_wrapper.setVisibility(View.GONE);
        if (viewHolder.list_item_image_header != null)
            viewHolder.list_item_image_header.setVisibility(View.GONE);

        WebView v = viewHolder.list_item_image_expanded_webview;
        if (v == null)
            return;
        //v.loadUrl(WEBVIEW_BLANK_URL); // needed so we don't get old image showing
        v.setVisibility(View.INVISIBLE);
        //v.setWebViewClient(webViewClient);

        if (DEBUG) Log.i(TAG, "Loading anim gif webview url=" + postImageUrl);
        int scale = calcScale(width, height);
        v.setInitialScale(scale);
        displayClickEffect();
        if (DEBUG) Log.i(TAG, "displayWebView() imageSize=" + width + "x" + height
                + " targetSize=" + targetSize.x + "x" + targetSize.y
                + " scale=" + scale);

        if (isVideo) {
            v.loadUrl(thumbUrl);
        }
        else {
            v.loadUrl(postImageUrl);
        }
    }
    private int calcScale(int width, int height) {
        float maxWidth = width > 1 ? width : 250;
        float maxHeight = height > 1 ? height : 250;
        float itemWidth = targetSize.x;
        float itemHeight = targetSize.y;
        return (int)Math.min(Math.ceil(itemWidth * 100 / maxWidth), Math.ceil(itemWidth * 100 / maxWidth));
    }

    /*
    protected WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            if (view != null)
                view.setVisibility(View.VISIBLE);
        }
    };
    */

    /*
    protected void displayImageView() {
        if (viewHolder.list_item_image_expanded_wrapper != null)
            viewHolder.list_item_image_expanded_wrapper.setVisibility(View.VISIBLE);
        if (viewHolder.list_item_expanded_progress_bar != null)
            viewHolder.list_item_expanded_progress_bar.setVisibility(withProgress ? View.VISIBLE : View.GONE);
        if (viewHolder.list_item_image_expanded != null)
            viewHolder.list_item_image_expanded.setVisibility(View.VISIBLE);
        if (viewHolder.list_item_image_expanded_webview != null)
            viewHolder.list_item_image_expanded_webview.setVisibility(View.GONE);
        if (viewHolder.list_item_image_header != null)
            viewHolder.list_item_image_header.setVisibility(View.GONE);
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
                        expandedDisplayImageOptions, expandedImageLoadingListener);
    }
    */

    ImageLoadingListener expandedImageLoadingListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {
            displayClickEffect();
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            String reason = failReason.toString();
            if (DEBUG) Log.e(TAG, "Failed to download " + postImageUrl + " to file=" + fullImagePath + " reason=" + reason);
            //if (viewHolder.list_item_expanded_progress_bar != null && withProgress)
            //    viewHolder.list_item_expanded_progress_bar.setVisibility(View.GONE);
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (DEBUG) Log.v(TAG, "onLoadingComplete uri=" + imageUri);
            //if (viewHolder.list_item_expanded_progress_bar != null && withProgress)
            //    viewHolder.list_item_expanded_progress_bar.setVisibility(View.GONE);
            displayClickEffect();
            if (viewHolder.list_item_image_expansion_target != null) {
                //viewHolder.list_item_image_expansion_target.setOnClickListener(null);
                //viewHolder.list_item_image_expansion_target.setForeground(view.getResources().getDrawable(R.drawable.null_selector_bg));
            }

            //if (withProgress)
            //     ThreadViewer.toggleExpandedImage(viewHolder);
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            //if (viewHolder.list_item_expanded_progress_bar != null && withProgress)
            //    viewHolder.list_item_expanded_progress_bar.setVisibility(View.GONE);
        }
    };

    private void displayClickEffect() {
        if (viewHolder.list_item_image_expanded_click_effect != null) {
            viewHolder.list_item_image_expanded_click_effect.setVisibility(View.VISIBLE);
            //viewHolder.list_item_image_expanded_click_effect.setOnClickListener(collapseImageListener);
            if (isVideo) {
                setVideoListener();
            }
            else {
                setGalleryListener();
            }
        }
    }

    private void setVideoListener() {
        viewHolder.list_item_image_expanded_click_effect.setOnClickListener(videoListener);
    }

    private View.OnClickListener videoListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Context c = viewHolder.list_item != null ? viewHolder.list_item.getContext() : null;
            if (c == null) {
                return;
            }
            Activity a = c instanceof Activity ? (Activity)c : null;
            String mimeType = ChanImage.videoMimeType(postExt);
            Uri uri = Uri.parse(postImageUrl);
            ChanImage.startViewer(a, uri, mimeType);
        }
    };

    private void setGalleryListener() {
        viewHolder.list_item_image_expanded_click_effect.setOnClickListener(expandedImageListener);
    }

    /*
    private View.OnClickListener collapseImageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ThreadViewer.toggleExpandedImage(viewHolder);
        }
    };
    */

    private boolean shouldExpandImage() {
        /*
        if (viewHolder.list_item_image_expanded != null
                && viewHolder.list_item_image_expanded.getVisibility() != View.GONE
                && viewHolder.list_item_image_expanded.getHeight() > 0) {
            if (DEBUG) Log.i(TAG, "Image already expanded, skipping");
            return false;
        }
        else
        */
        if (viewHolder.list_item_image_expanded_webview != null
                && viewHolder.list_item_image_expanded_webview.getVisibility() != View.GONE
                && viewHolder.list_item_image_expanded_webview.getHeight() > 0) {
            if (DEBUG) Log.i(TAG, "Image webview already expanded, skipping");
            return false;
        }
        else {
            return true;
        }
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
