package com.chanapps.four.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.chanapps.four.activity.R;
import com.chanapps.four.widget.WidgetPickBoardDialogFragment;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/15/13
 * Time: 11:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class WidgetConfigureActivity extends FragmentActivity {

    public static final String TAG = WidgetConfigureActivity.class.getSimpleName();

    private static final boolean DEBUG = false;

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetPickBoardDialogFragment fragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.widget_configure_layout);
        appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid app widget id received, exiting configuration");
            finish();
        }
        else {
            if (DEBUG) Log.i(TAG, "Configuring widget=" + appWidgetId);
        }
        fragment = new WidgetPickBoardDialogFragment(appWidgetId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!fragment.isVisible())
            fragment.show(getSupportFragmentManager(), WidgetPickBoardDialogFragment.TAG);
    }

    @Override
    protected void onPause() {
        super.onPause();

    }
}
