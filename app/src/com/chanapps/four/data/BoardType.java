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

    WATCHLIST (R.string.board_watch, R.string.board_watch_desc, 0, false, true),
    POPULAR (R.string.board_selector_popular_title, R.string.board_selector_popular_desc, 0, false, true),
    LATEST (R.string.board_selector_latest_title, R.string.board_selector_latest_desc, 0, false, true),
    LATEST_IMAGES (R.string.board_selector_latest_images_title, R.string.board_selector_latest_images_desc, 0, false, true),
    JAPANESE_CULTURE (R.string.board_type_japanese_culture, R.string.board_type_japanese_culture_desc, 0, true, true),
    INTERESTS (R.string.board_type_interests, R.string.board_type_interests_desc, 0, true, true),
    CREATIVE (R.string.board_type_creative, R.string.board_type_creative_desc, 0, true, true),
    OTHER (R.string.board_type_other, R.string.board_type_other_desc, 0, true, true),
    ADULT (R.string.board_type_adult, R.string.board_type_adult_desc, 0, true, false),
    MISC (R.string.board_type_misc, R.string.board_type_misc_desc, 0, true, false);

    private final int displayStringId;
    private final int descStringId;
    private final int drawableId;
    private final boolean isCategory;
    private final boolean isSFW;

    BoardType(int displayStringId, int descStringId, int drawableId, boolean isCategory, boolean isSFW) {
        this.displayStringId = displayStringId;
        this.descStringId = descStringId;
        this.drawableId = drawableId;
        this.isCategory = isCategory;
        this.isSFW = isSFW;
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

}
