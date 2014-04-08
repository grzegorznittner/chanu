package com.chanapps.four;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.data.DataManager;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.AnalyticsExceptionParser;
import com.chanapps.four.component.BillingComponent;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.UserStatistics;
import com.chanapps.four.gallery.ChanOffLineSource;
import com.chanapps.four.gallery.ChanSource;
import com.chanapps.four.service.NetworkProfileManager;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionReporter;

import java.util.Locale;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */

public class FourChannerApp extends GalleryAppImpl {

    private static final boolean DEBUG = false;
    private static final String TAG = FourChannerApp.class.getSimpleName();
    private static Locale locale = null;

    public synchronized DataManager getDataManager() {
        if (mDataManager == null) {
            mDataManager = new DataManager(this);
            mDataManager.initializeSourceMap();
            mDataManager.addSource(new ChanSource(this));
            mDataManager.addSource(new ChanOffLineSource(this));
        }
        return mDataManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupEnhancedExceptions();
        ChanFileStorage.migrateIfNecessary(getApplicationContext());
        BillingComponent.getInstance(getApplicationContext()).checkForNewPurchases();
        forceLocaleIfConfigured();
        if (DEBUG) Log.i(TAG, "onCreate() activity=" + NetworkProfileManager.instance().getActivityId());
    }

    protected void setupEnhancedExceptions() {
        EasyTracker.getInstance().setContext(this);
        // Change uncaught exception parser...
        // Note: Checking uncaughtExceptionHandler type can be useful if clearing ga_trackingId during development to disable analytics - avoid NullPointerException.
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (uncaughtExceptionHandler instanceof ExceptionReporter) {
            ExceptionReporter exceptionReporter = (ExceptionReporter) uncaughtExceptionHandler;
            exceptionReporter.setExceptionParser(new AnalyticsExceptionParser());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (locale != null) {
            newConfig.locale = locale;
            Locale.setDefault(locale);
            getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
        }
    }

    private void forceLocaleIfConfigured() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration config = getBaseContext().getResources().getConfiguration();
        boolean forceEnglish = settings.getBoolean(SettingsActivity.PREF_FORCE_ENGLISH, false);
        String lang = getString(R.string.pref_force_english_lang);
        if (forceEnglish && ! "".equals(lang) && ! config.locale.getLanguage().equals(lang)) {
            locale = new Locale(lang);
            Locale.setDefault(locale);
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }
    }
}
