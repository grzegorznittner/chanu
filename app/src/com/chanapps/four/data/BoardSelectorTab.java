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

    BOARDLIST (R.string.board_selector_boardlist_title, "", R.string.board_empty_default),
    RECENT (R.string.board_selector_recent_title, ChanBoard.POPULAR_BOARD_CODE, R.string.board_empty_recent),
    WATCHLIST (R.string.board_selector_watchlist_title, ChanBoard.WATCH_BOARD_CODE, R.string.board_empty_watchlist);

    private final int displayStringId;
    private final String boardCode;
    private final int emptyStringId;

    BoardSelectorTab(int displayStringId, String boardCode, int emptyStringId) {
        this.displayStringId = displayStringId;
        this.boardCode = boardCode;
        this.emptyStringId = emptyStringId;
    }

    public int displayStringId() {
        return displayStringId;
    }

    public String boardCode() {
        return boardCode;
    }

    public int emptyStringId() {
        return emptyStringId;
    }

}
