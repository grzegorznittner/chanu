package com.chanapps.four.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.ExtendedImageDownloader;
import com.chanapps.four.data.ChanHelper;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 3/14/13
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChanImageLoader {

    static private final String TAG = ChanImageLoader.class.getSimpleName();
    static private final boolean DEBUG = false;
    
    static private final int FULL_SCREEN_IMAGE_PADDING_DP = 8;
    static private final int MAX_MEMORY_WIDTH = 125;
    static private final int MAX_MEMORY_HEIGHT = 125;

    static private ImageLoader imageLoader = null;
    protected static final int IMAGE_URL_HASHCODE_KEY = R.id.grid_item_image;

    static public ImageLoader getInstance(Context context) {
        if (imageLoader == null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            manager.getDefaultDisplay().getMetrics(displayMetrics);
            int padding = ChanGridSizer.dpToPx(displayMetrics, FULL_SCREEN_IMAGE_PADDING_DP);
            final int maxWidth = ChanGridSizer.dpToPx(displayMetrics, displayMetrics.widthPixels) - 2 * padding;
            final int maxHeight = ChanGridSizer.dpToPx(displayMetrics, displayMetrics.heightPixels) - 2 * padding;
            imageLoader = ImageLoader.getInstance();
            imageLoader.init(
                    new ImageLoaderConfiguration
                            .Builder(context)
                            .memoryCacheExtraOptions(MAX_MEMORY_WIDTH, MAX_MEMORY_HEIGHT)
                            .discCacheExtraOptions(maxWidth, maxHeight, Bitmap.CompressFormat.JPEG, 85)
                            .imageDownloader(new ExtendedImageDownloader(context))
                            .build());
        }
        return imageLoader;
    }

    public static void smartSetImageView(ImageView iv,
                                         String imageUrl,
                                         DisplayImageOptions displayImageOptions,
                                         int imageResourceId)
    {
        try {
            if (imageResourceId == 0 && (imageUrl == null || imageUrl.isEmpty())) {
                ChanHelper.safeClearImageView(iv);
                return;
            }
            ImageLoader imageLoader = getInstance(iv.getContext());
            Integer viewHashCodeInt = (Integer)iv.getTag(IMAGE_URL_HASHCODE_KEY);
            int viewHashCode = viewHashCodeInt != null ? viewHashCodeInt : 0;
            int urlHashCode = imageUrl != null && !imageUrl.isEmpty() ? imageUrl.hashCode() : imageResourceId;
            if (DEBUG) Log.i(TAG, "iv urlhash=" + urlHashCode + " viewhash=" + viewHashCode);
            if (iv.getDrawable() == null || viewHashCode != urlHashCode) {
            	if (imageResourceId > 0) // load from board
            		imageUrl = "drawable://" + imageResourceId;
                if (DEBUG) Log.i(TAG, "calling imageloader for " + imageUrl);
                ChanHelper.safeClearImageView(iv);
                iv.setTag(IMAGE_URL_HASHCODE_KEY, urlHashCode);
                imageLoader.displayImage(imageUrl, iv, displayImageOptions); // load async
            }
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Couldn't set image view after number format exception with url=" + imageUrl, nfe);
            ChanHelper.safeClearImageView(iv);
        }
        catch (Exception e) {
            Log.e(TAG, "Exception setting image view with url=" + imageUrl, e);
            ChanHelper.safeClearImageView(iv);
        }
    }
}
