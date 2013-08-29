package com.chanapps.four.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.chanapps.four.activity.R;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/15/13
 * Time: 11:07 PM
 */
public class WidgetConfigureCoverFlowCardActivity extends AbstractWidgetConfigureActivity {

    public static final String TAG = WidgetConfigureCoverFlowCardActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    @Override
    protected int getContentViewLayout() {
        return R.layout.widget_configure_coverflowcard_layout;
    }

    protected void setBoardImages() {
        Log.d(TAG,"Set Board Images");
    }

    @Override
    protected String getWidgetType() {
        return WidgetConstants.WIDGET_TYPE_COVER_FLOW_CARD;
    }

    @Override
    protected void addDoneClickHandler() {
        Button doneButton = (Button) findViewById(R.id.done);
        if (doneButton == null)
            return;
        final WidgetConfigureCoverFlowCardActivity activity = this;
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG)
                    Log.i(TAG, "Configured widget=" + appWidgetId + " configuring for board=" + widgetConf.boardCode);
                WidgetProviderUtils.storeWidgetConf(activity, widgetConf);
                Intent intent = new Intent();
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(WidgetProviderUtils.WIDGET_PROVIDER_UTILS, activity.getWidgetType());
                activity.setResult(Activity.RESULT_OK, intent);
                Intent updateWidget = new Intent(activity, BoardCoverFlowCardWidgetProvider.class);
                updateWidget.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = {appWidgetId};
                updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                updateWidget.putExtra(WidgetProviderUtils.WIDGET_PROVIDER_UTILS, activity.getWidgetType());
                activity.sendBroadcast(updateWidget);
                activity.finish();
            }
        });


    }
}
