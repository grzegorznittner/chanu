package com.chanapps.four.data;

import android.content.Context;
import android.preference.PreferenceManager;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;

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

    public static BoardSortType loadFromPrefs(Context context) {
        BoardSortType sortType = BoardSortType.valueOfDisplayString(context, PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(SettingsActivity.PREF_BOARD_SORT_TYPE,
                        context.getString(R.string.sort_order_bump_order)));
        if (sortType == null)
            sortType = BoardSortType.BUMP_ORDER;
        return sortType;
    }

    public static void saveToPrefs(Context context, BoardSortType boardSortType) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(SettingsActivity.PREF_BOARD_SORT_TYPE,
                        context.getString(boardSortType.displayStringId()))
                .commit();
    }

}
