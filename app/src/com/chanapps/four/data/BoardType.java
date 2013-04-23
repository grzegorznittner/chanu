package com.chanapps.four.data;

import com.chanapps.four.activity.R;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 4/19/13
* Time: 10:50 AM
* To change this template use File | Settings | File Templates.
*/
public enum BoardType {

    WATCHLIST (R.string.board_watch, 0, false, true),
    POPULAR (R.string.board_selector_popular_title, 0, false, true),
    LATEST (R.string.board_selector_latest_title, 0, false, true),
    LATEST_IMAGES (R.string.board_selector_latest_images_title, 0, false, true),
    JAPANESE_CULTURE (R.string.board_type_japanese_culture, 0, true, true),
    INTERESTS (R.string.board_type_interests, 0, true, true),
    CREATIVE (R.string.board_type_creative, 0, true, true),
    OTHER (R.string.board_type_other, 0, true, true),
    ADULT (R.string.board_type_adult, 0, true, false),
    MISC (R.string.board_type_misc, 0, true, false);

    private final int displayStringId;
    private final int drawableId;
    private final boolean isCategory;
    private final boolean isSFW;

    BoardType(int displayStringId, int drawableId, boolean isCategory, boolean isSFW) {
        this.displayStringId = displayStringId;
        this.drawableId = drawableId;
        this.isCategory = isCategory;
        this.isSFW = isSFW;
    }

    public int displayStringId() {
        return displayStringId;
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

}
