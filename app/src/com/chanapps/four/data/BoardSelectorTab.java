package com.chanapps.four.data;

import com.chanapps.four.activity.R;
import com.chanapps.four.component.TutorialOverlay;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 4/18/13
* Time: 3:01 PM
* To change this template use File | Settings | File Templates.
*/
public enum BoardSelectorTab {

    BOARDLIST (R.string.board_selector_boardlist_title,
            "",
            R.string.board_empty_default,
            TutorialOverlay.Page.BOARDLIST),

    RECENT (R.string.board_selector_recent_title,
            ChanBoard.POPULAR_BOARD_CODE,
            R.string.board_empty_recent,
            TutorialOverlay.Page.RECENT),

    WATCHLIST (R.string.board_selector_watchlist_title,
            ChanBoard.WATCH_BOARD_CODE,
            R.string.board_empty_watchlist,
            TutorialOverlay.Page.WATCHLIST);

    private final int displayStringId;
    private final String boardCode;
    private final int emptyStringId;
    private final TutorialOverlay.Page tutorialPage;

    BoardSelectorTab(int displayStringId, String boardCode, int emptyStringId, TutorialOverlay.Page tutorialPage) {
        this.displayStringId = displayStringId;
        this.boardCode = boardCode;
        this.emptyStringId = emptyStringId;
        this.tutorialPage = tutorialPage;
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

    public TutorialOverlay.Page tutorialPage() {
        return tutorialPage;
    }

}
