package com.chanapps.four.data;

import com.chanapps.four.activity.R;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 4/18/13
* Time: 3:01 PM
* To change this template use File | Settings | File Templates.
*/
public enum BoardSelectorTab {

    WATCHLIST (R.string.board_selector_watchlist_title),
    POPULAR (R.string.board_selector_popular_title),
    BOARDLIST (R.string.board_selector_boardlist_title);

    private final int displayStringId;

    BoardSelectorTab(int displayStringId) {
        this.displayStringId = displayStringId;
    }

    public int displayStringId() {
        return displayStringId;
    }

}
