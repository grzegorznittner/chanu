package com.chanapps.four.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.chanapps.four.component.ChanGridSizer;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
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

    static public synchronized ImageLoader getInstance(Context context) {
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
                            //.memoryCacheExtraOptions(MAX_MEMORY_WIDTH, MAX_MEMORY_HEIGHT)
                            .discCacheExtraOptions(maxWidth, maxHeight, Bitmap.CompressFormat.JPEG, 85)
                                    //.imageDownloader(new ExtendedImageDownloader(context))
                                    //.threadPriority(Thread.MIN_PRIORITY+1)
                            .threadPoolSize(5)
                            .discCacheFileNameGenerator(new FileNameGenerator() {
                                @Override
                                public String generate(String imageUri) {
                                    return String.valueOf(Math.abs(imageUri.hashCode())) + ".jpg";
                                }
                            })
                            .build());
        }
        return imageLoader;
    }

}
