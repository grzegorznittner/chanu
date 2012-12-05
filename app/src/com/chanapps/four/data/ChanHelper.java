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
    public static final String POST_ID = "_id";
    public static final String POST_BOARD_NAME = "board_name";
    public static final String POST_NOW = "now";
    public static final String POST_TIME = "time";
    public static final String POST_NAME = "name";
    public static final String POST_SUB = "sub";
    public static final String POST_COM = "com";
    public static final String POST_TIM = "tim";
    public static final String POST_FILENAME = "filename";
    public static final String POST_EXT = "ext";
    public static final String POST_W = "w";
    public static final String POST_H = "h";
    public static final String POST_TN_W = "tn_w";
    public static final String POST_TN_H = "tn_h";
    public static final String POST_FSIZE = "fsize";
    public static final String POST_RESTO = "resto";
    public static final String POST_LAST_UPDATE = "last_update";
    // version 2
    public static final String POST_STICKY = "sticky";
    public static final String POST_CLOSED = "closed";
    public static final String POST_OMITTED_POSTS = "omitted_posts";
    public static final String POST_OMITTED_IMAGES = "omitted_images";
    // version 6
    public static final String POST_SHORT_TEXT = "shortText"; // we construct and filter this
    public static final String POST_TEXT = "text"; // we construct and filter this
    public static final String POST_IMAGE_URL = "image_url"; // we construct this from board and tim

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
