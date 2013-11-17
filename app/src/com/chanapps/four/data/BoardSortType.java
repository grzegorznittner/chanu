package com.chanapps.four.data;

import android.content.Context;
import com.chanapps.four.activity.R;

public enum BoardSortType {

    BUMP_ORDER(R.string.sort_order_bump_order),

    REPLY_COUNT(R.string.sort_order_reply_count),

    IMAGE_COUNT(R.string.sort_order_image_count),

    CREATION_DATE(R.string.sort_order_creation_date);

    private final int displayStringId;

    BoardSortType(int displayStringId) {
        this.displayStringId = displayStringId;
    }

    public static final BoardSortType valueOfDisplayString(Context context, String displayString) {
        for (BoardSortType boardType : BoardSortType.values())
            if (context.getString(boardType.displayStringId).equals(displayString))
                return boardType;
        return null;
    }

    public int displayStringId() {
        return displayStringId;
    }

}
