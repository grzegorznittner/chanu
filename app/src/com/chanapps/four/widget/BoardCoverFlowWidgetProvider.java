package com.chanapps.four.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.BoardSelectorTab;
import com.chanapps.four.data.ChanBoard;

/**
 * User: mpop
 * Date: 11/22/12
 * Time: 11:30 PM
 */
public class BoardCoverFlowWidgetProvider extends AbstractBoardWidgetProvider {

    public static final String TAG = BoardCoverFlowWidgetProvider.class.getSimpleName();

    private static final boolean DEBUG = true;

    @Override
    protected String getWidgetType() {
        return WidgetConstants.WIDGET_TYPE_COVER_FLOW;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            WidgetConf widgetConf = WidgetProviderUtils.loadWidgetConf(context, appWidgetId);
            if (widgetConf == null)
                widgetConf = new WidgetConf(appWidgetId); // new widget or no config;


            final Intent intent = new Intent(context, StackWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_board_coverflow_layout);
            remoteViews.setRemoteAdapter(appWidgetId, R.id.stack_view_coverflow, intent);
            remoteViews.setEmptyView(R.id.stack_view_coverflow, R.id.stack_view_empty);

            updateWidgetViews(R.layout.widget_board_coverflow_layout, R.id.widget_board_coverflow_container, remoteViews, context, widgetConf);

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);


        }
    }

    private void updateWidgetViews(int widgetLayout, int widgetContainer, RemoteViews views, Context context, WidgetConf widgetConf) {

        int containerBackground = widgetConf.roundedCorners ? R.drawable.widget_rounded_background : 0;
        views.setInt(widgetContainer, "setBackgroundResource", containerBackground);

        ChanBoard board = ChanBoard.getBoardByCode(context, widgetConf.boardCode);
        if (board == null)
            board = ChanBoard.getBoardByCode(context, ChanBoard.DEFAULT_BOARD_CODE);
        if (board == null) {
            android.util.Log.e(TAG, "Something very bad happened null board code=" + widgetConf.boardCode);
            return;
        }

        String boardTitle = ChanBoard.isVirtualBoard(board.link)
                ? board.name
                : board.name + " /" + board.link + "/";
        int boardTitleColor = widgetConf.boardTitleColor;
        int boardTitleVisibility = widgetConf.showBoardTitle ? View.VISIBLE : View.GONE;
        views.setTextViewText(R.id.board_title, boardTitle);
        views.setTextColor(R.id.board_title, boardTitleColor);
        views.setViewVisibility(R.id.board_title, boardTitleVisibility);
        if (widgetConf.showBoardTitle)
            views.setOnClickPendingIntent(R.id.board_title, makeBoardIntent(widgetConf,context));

        views.setInt(R.id.home_button, "setImageResource", R.drawable.app_icon);
        views.setOnClickPendingIntent(R.id.home_button, makeHomeIntent(widgetConf, context));

        int refreshBackground = widgetConf.showRefreshButton ? R.drawable.widget_refresh_gradient_bg : 0;
        int refreshDrawable = widgetConf.showRefreshButton ? R.drawable.widget_refresh_button_selector : 0;
        views.setInt(R.id.refresh, "setBackgroundResource", refreshBackground);
        views.setInt(R.id.refresh, "setImageResource", refreshDrawable);
        views.setOnClickPendingIntent(R.id.refresh, makeRefreshIntent());

        int configureBackground = widgetConf.showConfigureButton ? R.drawable.widget_configure_gradient_bg : 0;
        int configureDrawable = widgetConf.showConfigureButton ? R.drawable.widget_configure_button_selector : 0;
        views.setInt(R.id.configure, "setBackgroundResource", configureBackground);
        views.setInt(R.id.configure, "setImageResource", configureDrawable);
        views.setOnClickPendingIntent(R.id.configure, makeConfigureIntent(widgetConf, context));

        if (DEBUG)
            android.util.Log.i(TAG, "Updated widgetId=" + widgetConf.appWidgetId + " for board=" + widgetConf.boardCode);
    }

    private PendingIntent makeHomeIntent(WidgetConf widgetConf, Context context) {
        Intent intent = BoardSelectorActivity.createIntentForActivity(context, BoardSelectorTab.BOARDLIST);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int uniqueId = (100 * widgetConf.appWidgetId) + 1;
        return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent makeBoardIntent(WidgetConf widgetConf, Context context) {
        Intent intent;
        if (ChanBoard.isVirtualBoard(widgetConf.boardCode)) {
            BoardSelectorTab tab = ChanBoard.WATCHLIST_BOARD_CODE.equals(widgetConf.boardCode)
                    ? BoardSelectorTab.WATCHLIST
                    : BoardSelectorTab.RECENT;
            intent = BoardSelectorActivity.createIntentForActivity(context, tab);
        } else {
            intent = BoardActivity.createIntentForActivity(context, widgetConf.boardCode, "");
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int uniqueId = (100 * widgetConf.appWidgetId) + 2;
        return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent makeRefreshIntent() {
        return null;
    }

    private PendingIntent makeConfigureIntent(WidgetConf widgetConf, Context context) {
        Intent intent = new Intent(context, WidgetConfigureCoverFlowActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetConf.appWidgetId);
        int uniqueId = (100 * widgetConf.appWidgetId) + 4;
        return PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        super.onReceive(context, intent);    //To change body of overridden methods use File | Settings | File Templates.
    }
}