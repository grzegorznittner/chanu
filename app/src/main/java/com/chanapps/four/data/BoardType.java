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

    ALL_BOARDS (R.string.board_all_boards,
            R.drawable.grid_block, R.drawable.grid_block_light,
            true, false, true, "all_boards", R.string.board_empty_all_boards, R.string.board_all_boards),
    POPULAR (R.string.board_popular,
            R.drawable.men, R.drawable.men_light,
            false, false, true, "popular", R.string.board_empty_popular, R.string.board_popular),
    LATEST (R.string.board_latest,
            R.drawable.clock, R.drawable.clock_light,
            false, false, true, "latest", R.string.board_empty_latest, R.string.board_latest),
    LATEST_IMAGES (R.string.board_latest_images,
            0, 0,
            false, false, true, "images", R.string.board_empty_images, R.string.board_latest_images),
    JAPANESE_CULTURE (R.string.board_type_japanese_culture,
            R.drawable.japanese_culture, R.drawable.japanese_culture_light,
            true, true, true, "japanese_culture", R.string.board_empty_all_boards, R.string.board_type_japanese_culture),
    INTERESTS (R.string.board_type_interests,
            R.drawable.game_controller, R.drawable.game_controller_light,
            true, true, true, "interests", R.string.board_empty_all_boards, R.string.board_type_interests),
    CREATIVE (R.string.board_type_creative,
            R.drawable.camera, R.drawable.camera_light,
            true, true, true, "creative", R.string.board_empty_all_boards, R.string.board_type_creative),
    OTHER (R.string.board_type_other,
            R.drawable.speech_bubble_ellipsis, R.drawable.speech_bubble_ellipsis_light,
            true, true, true, "other", R.string.board_empty_all_boards, R.string.board_type_other),
    ADULT (R.string.board_type_adult,
            R.drawable.heart, R.drawable.heart_light,
            true, true, false, "adult", R.string.board_empty_all_boards, R.string.board_type_adult),
    MISC (R.string.board_type_misc,
            R.drawable.eightball, R.drawable.eightball_light,
            true, true, false, "misc", R.string.board_empty_all_boards, R.string.board_type_misc),
    WATCHLIST (R.string.board_watch,
            R.drawable.document_magnify, R.drawable.document_magnify_light,
            false, false, true, "watchlist", R.string.board_empty_watchlist, R.string.board_watch),
    FAVORITES (R.string.board_favorites,
            R.drawable.star, R.drawable.star_light,
            false, false, true, "favorites", R.string.board_empty_favorites, R.string.board_favorites),
    TRIAL (R.string.board_type_other,
            R.drawable.speech_bubble_ellipsis, R.drawable.speech_bubble_ellipsis_light,
            true, true, true, "trial", R.string.board_empty_all_boards, R.string.board_type_other),;


    private final int displayStringId;
    private final int drawableId;
    private final int darkDrawableId;
    private final boolean isCategory;
    private final boolean isSubCategory;
    private final boolean isSFW;
    private final String boardCode;
    private final int emptyStringId;
    private final int drawerStringId;

    BoardType(int displayStringId,
              int drawableId, int darkDrawableId,
              boolean isCategory, boolean isSubCategory,
              boolean isSFW, String boardCode, int emptyStringId, int drawerStringId) {
        this.displayStringId = displayStringId;
        this.drawableId = drawableId;
        this.darkDrawableId = darkDrawableId;
        this.isCategory = isCategory;
        this.isSubCategory = isSubCategory;
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

    public int drawableId() {
        return drawableId;
    }

    public int darkDrawableId() {
        return darkDrawableId;
    }

    public boolean isCategory() {
        return isCategory;
    }

    public boolean isSubCategory() {
        return isSubCategory;
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
