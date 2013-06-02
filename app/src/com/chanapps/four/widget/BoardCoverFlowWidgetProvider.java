package com.chanapps.four.widget;

/**
 * User: mpop
 * Date: 11/22/12
 * Time: 11:30 PM
 */
public class BoardCoverFlowWidgetProvider extends AbstractBoardWidgetProvider {

    public static final String TAG = BoardCoverFlowWidgetProvider.class.getSimpleName();

    private static final boolean DEBUG = false;

    @Override
    protected String getWidgetType() {
        return WidgetConstants.WIDGET_TYPE_COVER_FLOW;
    }
}