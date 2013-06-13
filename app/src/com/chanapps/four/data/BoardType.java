package com.chanapps.four.data;

import android.content.Context;
import com.chanapps.four.activity.R;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 4/19/13
* Time: 10:50 AM
* To change this template use File | Settings | File Templates.
*/
public enum BoardType {

    META (R.string.board_meta, R.string.board_meta_desc, 0, false, true, "meta"),
    POPULAR (R.string.board_selector_popular_title, R.string.board_selector_popular_desc, 0, false, true, "popular"),
    LATEST (R.string.board_selector_latest_title, R.string.board_selector_latest_desc, 0, false, true, "latest"),
    LATEST_IMAGES (R.string.board_selector_latest_images_title, R.string.board_selector_latest_images_desc, 0, false, true, "images"),
    JAPANESE_CULTURE (R.string.board_type_japanese_culture, R.string.board_type_japanese_culture_desc, 0, true, true, "japanese_culture"),
    INTERESTS (R.string.board_type_interests, R.string.board_type_interests_desc, 0, true, true, "interests"),
    CREATIVE (R.string.board_type_creative, R.string.board_type_creative_desc, 0, true, true, "creative"),
    OTHER (R.string.board_type_other, R.string.board_type_other_desc, 0, true, true, "other"),
    ADULT (R.string.board_type_adult, R.string.board_type_adult_desc, 0, true, false, "adult"),
    MISC (R.string.board_type_misc, R.string.board_type_misc_desc, 0, true, false, "misc"),
    WATCHLIST (R.string.board_watch, R.string.board_watch_desc, 0, false, true, "watchlist");

    private final int displayStringId;
    private final int descStringId;
    private final int drawableId;
    private final boolean isCategory;
    private final boolean isSFW;
    private final String boardCode;

    BoardType(int displayStringId, int descStringId, int drawableId, boolean isCategory, boolean isSFW, String boardCode) {
        this.displayStringId = displayStringId;
        this.descStringId = descStringId;
        this.drawableId = drawableId;
        this.isCategory = isCategory;
        this.isSFW = isSFW;
        this.boardCode = boardCode;
    }

    public static final BoardType valueOfDisplayString(Context context, String displayString) {
        for (BoardType boardType : BoardType.values())
            if (context.getString(boardType.displayStringId).equals(displayString))
                return boardType;
        return null;
    }

    public int displayStringId() {
        return displayStringId;
    }

    public int descStringId() {
        return descStringId;
    }

    public int drawableId() {
        return drawableId;
    }

    public boolean isCategory() {
        return isCategory;
    }

    public boolean isSFW() {
        return isSFW;
    }

    public String boardCode() {
        return boardCode;
    }

}
