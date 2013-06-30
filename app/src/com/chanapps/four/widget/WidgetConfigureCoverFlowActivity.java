package com.chanapps.four.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.chanapps.four.activity.R;
import com.chanapps.four.loader.ChanImageLoader;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/15/13
 * Time: 11:07 PM
 */
public class WidgetConfigureCoverFlowActivity extends AbstractWidgetConfigureActivity {

    public static final String TAG = WidgetConfigureCoverFlowActivity.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final long DELAY_BOARD_IMAGE_MS = 5 * 1000; // give board fetch time to finish

    @Override
    protected int getContentViewLayout() {
        return R.layout.widget_configure_coverflow_layout;
    }

    protected void setBoardImages() {
        Log.d(TAG,"Set Board Images");
    }

    @Override
    protected String getWidgetType() {
        return WidgetConstants.WIDGET_TYPE_COVER_FLOW;
    }

    @Override
    protected void addDoneClickHandler() {
        Button doneButton = (Button) findViewById(R.id.done);
        if (doneButton == null)
            return;
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG)
                    Log.i(TAG, "Configured widget=" + appWidgetId + " configuring for board=" + widgetConf.boardCode);
                WidgetProviderUtils.storeWidgetConf(WidgetConfigureCoverFlowActivity.this, widgetConf);
                Intent intent = new Intent();
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(WidgetProviderUtils.WIDGET_PROVIDER_UTILS, WidgetConfigureCoverFlowActivity.this.getWidgetType());
                WidgetConfigureCoverFlowActivity.this.setResult(Activity.RESULT_OK, intent);
                Intent updateWidget = new Intent(WidgetConfigureCoverFlowActivity.this, BoardCoverFlowWidgetProvider.class);
                updateWidget.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = {appWidgetId};
                updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                updateWidget.putExtra(WidgetProviderUtils.WIDGET_PROVIDER_UTILS, WidgetConfigureCoverFlowActivity.this.getWidgetType());
                WidgetConfigureCoverFlowActivity.this.sendBroadcast(updateWidget);
                WidgetConfigureCoverFlowActivity.this.finish();
            }
        });


    }
}
