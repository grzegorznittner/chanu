package com.chanapps.four.component;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.google.analytics.tracking.android.EasyTracker;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 8/22/13
* Time: 11:47 AM
* To change this template use File | Settings | File Templates.
*/
public class AnalyticsComponent {

    protected static final String TAG = AnalyticsComponent.class.getSimpleName();
    protected static final boolean DEBUG = false;

    private AnalyticsComponent(Context context) {
    }

    public static void onStart(final Activity activity) {
        if (PreferenceManager
                .getDefaultSharedPreferences(activity)
                .getBoolean(SettingsActivity.PREF_USE_GOOGLE_ANALYTICS, true))
            EasyTracker.getInstance().activityStart(activity);

    }

    public static void onStop(final Activity activity) {
        if (PreferenceManager
                .getDefaultSharedPreferences(activity)
                .getBoolean(SettingsActivity.PREF_USE_GOOGLE_ANALYTICS, true))
            EasyTracker.getInstance().activityStop(activity);
    }

}
