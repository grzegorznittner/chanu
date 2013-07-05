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

    ALL_BOARDS (R.string.board_selector_all_boards_title, R.string.board_selector_all_boards_desc, 0, true, true, "all_boards", R.string.board_empty_all_boards, R.string.board_all_boards),
    POPULAR (R.string.board_selector_popular_title, R.string.board_selector_popular_desc, 0, false, true, "popular", R.string.board_empty_popular, R.string.board_popular),
    LATEST (R.string.board_selector_latest_title, R.string.board_selector_latest_desc, 0, false, true, "latest", R.string.board_empty_latest, R.string.board_latest),
    LATEST_IMAGES (R.string.board_selector_latest_images_title, R.string.board_selector_latest_images_desc, 0, false, true, "images", R.string.board_empty_images, R.string.board_latest_images),
    META (R.string.board_meta, R.string.board_meta_desc, 0, false, true, "meta", R.string.board_empty_meta, R.string.board_meta),
    JAPANESE_CULTURE (R.string.board_type_japanese_culture, R.string.board_type_japanese_culture_desc, 0, true, true, "japanese_culture", R.string.board_empty_meta, R.string.board_type_japanese_culture),
    INTERESTS (R.string.board_type_interests, R.string.board_type_interests_desc, 0, true, true, "interests", R.string.board_empty_meta, R.string.board_type_interests),
    CREATIVE (R.string.board_type_creative, R.string.board_type_creative_desc, 0, true, true, "creative", R.string.board_empty_meta, R.string.board_type_creative),
    OTHER (R.string.board_type_other, R.string.board_type_other_desc, 0, true, true, "other", R.string.board_empty_meta, R.string.board_type_other),
    ADULT (R.string.board_type_adult, R.string.board_type_adult_desc, 0, true, false, "adult", R.string.board_empty_meta, R.string.board_type_adult),
    MISC (R.string.board_type_misc, R.string.board_type_misc_desc, 0, true, false, "misc", R.string.board_empty_meta, R.string.board_type_misc),
    WATCHLIST (R.string.board_watch, R.string.board_watch_desc, 0, false, true, "watchlist", R.string.board_empty_watchlist, R.string.board_watch);

    private final int displayStringId;
    private final int descStringId;
    private final int drawableId;
    private final boolean isCategory;
    private final boolean isSFW;
    private final String boardCode;
    private final int emptyStringId;
    private final int drawerStringId;

    BoardType(int displayStringId, int descStringId, int drawableId, boolean isCategory, boolean isSFW, String boardCode, int emptyStringId, int drawerStringId) {
        this.displayStringId = displayStringId;
        this.descStringId = descStringId;
        this.drawableId = drawableId;
        this.isCategory = isCategory;
        this.isSFW = isSFW;
        this.boardCode = boardCode;
        this.emptyStringId = emptyStringId;
        this.drawerStringId = drawerStringId;
    }

    public static final BoardType valueOfDrawerString(Context context, String drawerString) {
        for (BoardType boardType : BoardType.values())
            if (context.getString(boardType.drawerStringId).equals(drawerString))
                return boardType;
        return null;
    }

    public static final BoardType valueOfBoardCode(String boardCode) {
        for (BoardType boardType : BoardType.values())
            if (boardType.boardCode.equals(boardCode))
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

    public int emptyStringId() {
        return emptyStringId;
    }

    public int drawerStringId() {
        return drawerStringId;
    }

}
