package com.chanapps.four.widget;

import com.chanapps.four.data.ChanBoard;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 4/17/13
 * Time: 6:18 PM
 */

public class WidgetConf implements Serializable {

    public static final String DELIM = "/";

    int appWidgetId;
    String boardCode;
    int boardTitleColor;
    boolean roundedCorners;
    boolean showBoardTitle;
    boolean showRefreshButton;
    boolean showConfigureButton;
    String widgetType;

    public WidgetConf(String serializedConf) {
        deserialize(serializedConf);
    }

    public WidgetConf(int appWidgetId) {
        this(appWidgetId, ChanBoard.DEFAULT_BOARD_CODE, 0xffffffff, false, true, true, true, WidgetConstants.WIDGET_TYPE_EMPTY);
    }

    public WidgetConf(int appWidgetId, String widgetType) {
        this(appWidgetId, ChanBoard.DEFAULT_BOARD_CODE, 0xffffffff, false, true, true, true, widgetType);
    }

    public WidgetConf(int appWidgetId,
                      String boardCode,
                      int boardTitleColor,
                      boolean roundedCorners,
                      boolean showBoardTitle,
                      boolean showRefreshButton,
                      boolean showConfigureButton,
                      String widgetType) {
        this.appWidgetId = appWidgetId;
        this.boardCode = boardCode;
        this.boardTitleColor = boardTitleColor;
        this.roundedCorners = roundedCorners;
        this.showBoardTitle = showBoardTitle;
        this.showRefreshButton = showRefreshButton;
        this.showConfigureButton = showConfigureButton;
        this.widgetType = widgetType;
    }

    public String serialize() {
        return appWidgetId + DELIM +
                boardCode + DELIM +
                boardTitleColor + DELIM +
                roundedCorners + DELIM +
                showBoardTitle + DELIM +
                showRefreshButton + DELIM +
                showConfigureButton + DELIM +
                widgetType + DELIM;
    }

    public void deserialize(String s) {
        if (s == null || s.isEmpty())
            return;
        String[] c = s.split(DELIM);
        appWidgetId = c.length > 0 ? Integer.parseInt(c[0]) : 0;
        boardCode = c.length > 1 ? c[1] : "";
        boardTitleColor = c.length > 2 ? Integer.parseInt(c[2]) : 0;
        roundedCorners = c.length > 3 ? Boolean.parseBoolean(c[3]) : true;
        showBoardTitle = c.length > 4 ? Boolean.parseBoolean(c[4]) : true;
        showRefreshButton = c.length > 5 ? Boolean.parseBoolean(c[5]) : true;
        showConfigureButton = c.length > 6 ? Boolean.parseBoolean(c[6]) : true;
        widgetType = c.length > 7 ? c[7] : WidgetConstants.WIDGET_TYPE_EMPTY;
    }

}
