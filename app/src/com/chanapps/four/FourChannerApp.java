package com.chanapps.four;

import android.util.Log;
import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.data.DataManager;
import com.chanapps.four.component.AnalyticsExceptionParser;
import com.chanapps.four.component.BillingComponent;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.UserStatistics;
import com.chanapps.four.gallery.ChanOffLineSource;
import com.chanapps.four.gallery.ChanSource;
import com.chanapps.four.service.NetworkProfileManager;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionReporter;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */

public class FourChannerApp extends GalleryAppImpl {

    private static final boolean DEBUG = true;
    private static final String TAG = FourChannerApp.class.getSimpleName();

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

}
