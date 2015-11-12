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
import com.chanapps.four.component.GlobalAlarmReceiver;
import com.chanapps.four.loader.ChanImageLoader;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/15/13
 * Time: 11:07 PM
 */
public class WidgetConfigureActivity extends AbstractWidgetConfigureActivity {

    public static final String TAG = WidgetConfigureActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    @Override
    protected int getContentViewLayout() {
        return R.layout.widget_configure_layout;
    }

    @Override
    protected String getWidgetType() {
        return WidgetConstants.WIDGET_TYPE_BOARD;
    }

    @Override
    protected void setBoardImages() {
        final Context context = getApplicationContext();
        final String boardCode = widgetConf.boardCode;
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int[] imageIds = {R.id.image_left, R.id.image_center, R.id.image_right};
                final String[] urls = boardThreadUrls(context, boardCode, imageIds.length);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < imageIds.length; i++) {
                            final int imageResourceId = imageIds[i];
                            final ImageView iv = (ImageView) findViewById(imageResourceId);
                            iv.setImageBitmap(null);
                            if (DEBUG) Log.i(TAG, "Calling displayImage i=" + i + " url=" + urls[i]);
                            ChanImageLoader.getInstance(context).displayImage(urls[i], iv);
                        }
                    }
                });
            }
        }).start();
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
                WidgetProviderUtils.storeWidgetConf(WidgetConfigureActivity.this, widgetConf);
                Intent intent = new Intent();
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(WidgetProviderUtils.WIDGET_PROVIDER_UTILS, WidgetConfigureActivity.this.getWidgetType());
                WidgetConfigureActivity.this.setResult(Activity.RESULT_OK, intent);
                Intent updateWidget = new Intent(WidgetConfigureActivity.this, BoardWidgetProvider.class);
                updateWidget.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = {appWidgetId};
                updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                updateWidget.putExtra(WidgetProviderUtils.WIDGET_PROVIDER_UTILS, WidgetConfigureActivity.this.getWidgetType());
                WidgetConfigureActivity.this.sendBroadcast(updateWidget);
                WidgetConfigureActivity.this.finish();
                GlobalAlarmReceiver.scheduleGlobalAlarm(getApplicationContext()); // will deschedule if appropriate
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

}
