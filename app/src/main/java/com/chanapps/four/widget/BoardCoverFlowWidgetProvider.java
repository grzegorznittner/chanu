package com.chanapps.four.widget;

/**
 * User: mpop
 * Date: 11/22/12
 * Time: 11:30 PM
 */
public class BoardCoverFlowWidgetProvider extends AbstractBoardWidgetProvider {

    public static final String TAG = BoardCoverFlowWidgetProvider.class.getSimpleName();
    public static final int MAX_THREADS = 20;

    @Override
    protected String getWidgetType() {
        return WidgetConstants.WIDGET_TYPE_COVER_FLOW;
    }

}