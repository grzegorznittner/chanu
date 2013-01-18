package com.chanapps.four.component;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.fragment.ClearCacheDialogFragment;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 1/18/13
* Time: 1:16 PM
* To change this template use File | Settings | File Templates.
*/
public enum ChanNotificationManager {

    INSTANCE; // forces singleton; see http://stackoverflow.com/questions/70689/what-is-an-efficient-way-to-implement-a-singleton-pattern-in-java

    public static final int CLEAR_CACHE_NOTIFY_ID = 0x1; // a unique notify idea is needed for each notify to "clump" together

    private Context context;

    private NotificationManager manager;

    public static ChanNotificationManager getInstance(Context context) { // use this whenever you need to use the manager
        INSTANCE.context = context;
        INSTANCE.manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        return INSTANCE;
    }

    public boolean isEnabled() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(SettingsActivity.PREF_NOTIFICATIONS, true);
    }

    public void sendNotification(int id, Notification notification) {
        manager.notify(id, notification);
    }
}
