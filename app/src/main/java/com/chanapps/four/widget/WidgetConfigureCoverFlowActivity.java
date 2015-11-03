package com.chanapps.four.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.loader.ChanImageLoader;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/15/13
 * Time: 11:07 PM
 */
public class WidgetConfigureCoverFlowActivity extends AbstractWidgetConfigureActivity {

    public static final String TAG = WidgetConfigureCoverFlowActivity.class.getSimpleName();
    private static final boolean DEBUG = false;
    protected static final int MAX_CONFIG_THREADS = 6;

    private StackView stackView = null;
    private View emptyView = null;
    private String[] urls = {};
    private BaseAdapter adapter = null;
    private LayoutInflater inflater = null;

    @Override
    protected int getContentViewLayout() {
        return R.layout.widget_configure_coverflow_layout;
    }

    @Override
    protected void setBoardImages() {
        final Context context = this;
        final Handler handler = new Handler();
        if (DEBUG) Log.i(TAG, "setBoardImages() /" + widgetConf.boardCode + "/");
        new Thread(new Runnable() {
            @Override
            public void run() {
                urls = boardThreadUrls(context, widgetConf.boardCode, MAX_CONFIG_THREADS);
                if (DEBUG) Log.i(TAG, "setBoardImages() /" + widgetConf.boardCode + "/ found " + urls.length
                        + " urls=" + Arrays.toString(urls));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (emptyView == null)
                            emptyView = findViewById(R.id.stack_view_empty);
                        if (emptyView != null)
                            emptyView.setVisibility(View.VISIBLE);
                        if (stackView == null)
                            stackView = (StackView)findViewById(R.id.stack_view_coverflow);
                        if (stackView == null)
                            return;
                        if (adapter == null) {
                            if (DEBUG) Log.i(TAG, "setBoardImages() /" + widgetConf.boardCode + "/ stackView.setAdapter");
                            adapter = createAdapter();
                            stackView.setAdapter(adapter);
                        }
                        else {
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected String getWidgetType() {
        return WidgetConstants.WIDGET_TYPE_COVER_FLOW;
    }

    protected Class getWidgetProviderClass() {
        return BoardCoverFlowWidgetProvider.class;
    }

    @Override
    protected void addDoneClickHandler() {
        Button doneButton = (Button) findViewById(R.id.done);
        if (doneButton == null)
            return;
        final WidgetConfigureCoverFlowActivity activity = this;
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
                Intent updateWidget = new Intent(activity, getWidgetProviderClass());
                updateWidget.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = {appWidgetId};
                updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                updateWidget.putExtra(WidgetProviderUtils.WIDGET_PROVIDER_UTILS, activity.getWidgetType());
                activity.sendBroadcast(updateWidget);
                activity.finish();
            }
        });
    }

    protected BaseAdapter createAdapter() {
        return new CoverflowStackAdapter(this, R.layout.widget_coverflow_item, R.id.image_coverflow_item);
    }

    protected class CoverflowStackAdapter extends BaseAdapter {
        protected Context context;
        protected int layoutId;
        protected int imageId;

        public CoverflowStackAdapter(Context context, int layoutId, int imageId) {
            this.context = context;
            this.layoutId = layoutId;
            this.imageId = imageId;
        }

        @Override
        public int getCount() {
            if (DEBUG) Log.i(TAG, "getCount()=" + urls.length);
            return urls.length;
        }

        @Override
        public Object getItem(int position) {
            return urls[position];
        }

        @Override
        public long getItemId(int position) {
            return (new String(widgetConf.boardCode + "/" + position)).hashCode();
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (DEBUG) Log.i(TAG, "getView() pos=" + position + " url=" + urls[position]);
            if (view == null) {
                if (inflater == null)
                    inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(layoutId, parent, false);
            }
            ImageView imageView = (ImageView) view.findViewById(imageId);
            imageView.setImageDrawable(null);
            ChanImageLoader.getInstance(context).displayImage(urls[position], imageView);
            if (emptyView != null && emptyView.getVisibility() != View.GONE)
                emptyView.setVisibility(View.GONE);
            return view;
        }
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
