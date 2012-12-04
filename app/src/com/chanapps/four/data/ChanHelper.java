package com.chanapps.four.data;

import android.content.Context;
import android.util.DisplayMetrics;

public class ChanHelper {
	public static final String PREF_NAME = "4Channer";

	public static final String BOARD_TYPE = "boardType";
	public static final String BOARD_CODE = "boardCode";
	public static final String PAGE = "pageNo";
	public static final String THREAD_NO = "threadNo";
    public static final String POST_NO = "postNo";
    public static final String TEXT = "text";
    public static final String IMAGE_URL = "imageUrl";
    public static final String IMAGE_WIDTH = "imageWidth";
    public static final String IMAGE_HEIGHT = "imageHeight";
    public static final String FAVORITE_BOARDS = "favoriteBoards";
    public static final String THREAD_WATCHLIST = "threadWatchlist";

    public enum Orientation {
        PORTRAIT,
        LANDSCAPE
    }

    public static final Orientation getOrientation(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        if (metrics.widthPixels > metrics.heightPixels) {
            return Orientation.LANDSCAPE;
        }
        else {
            return Orientation.PORTRAIT;
        }
    }
}
