package com.chanapps.four.widget;

/**
 * User: mpop
 * Date: 11/22/12
 * Time: 11:30 PM
 */
public class BoardWidgetProvider extends AbstractBoardWidgetProvider {

    public static final String TAG = BoardWidgetProvider.class.getSimpleName();

    private static final boolean DEBUG = false;

    @Override
    protected String getWidgetType() {
        return WidgetConstants.WIDGET_TYPE_BOARD;
    }
}