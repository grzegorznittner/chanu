package com.chanapps.four.component;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64OutputStream;
import android.util.Log;
import com.chanapps.four.activity.*;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.LastActivity;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 12/18/12
 * Time: 10:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ActivityDispatcher {

    private static final String TAG = ActivityDispatcher.class.getSimpleName();
    private static final String LAST_ACTIVITY = "ActivityDispatcherLastActivity";
    private static final boolean DEBUG = true;

    public static void store(ChanIdentifiedActivity activity) {
        ChanActivityId activityId = activity.getChanActivityId();
        String serialized = activityId.serialize();
        if (serialized == null || serialized.isEmpty()) {
            if (DEBUG) Log.e(TAG, "store() serialize empty");
            return;
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext()).edit();
        editor.putString(LAST_ACTIVITY, serialized);
        editor.commit();
        if (DEBUG) Log.i(TAG, "store() stored " + activityId);
    }

    public static void dispatch(ChanIdentifiedActivity activity) {

        Intent intent = ((Activity)activity).getIntent();
        if (intent.hasExtra(ChanHelper.IGNORE_DISPATCH) && intent.getBooleanExtra(ChanHelper.IGNORE_DISPATCH, false)) {
            if (DEBUG) Log.i(TAG, "dispatch ignored by intent");
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
        String serialized = prefs.getString(LAST_ACTIVITY, null);
        if (serialized == null || serialized.isEmpty()) {
            if (DEBUG) Log.e(TAG, "dispatch() deserialize empty");
            return;
        }

        ChanActivityId activityId = ChanActivityId.deserialize(serialized);
        if (activityId == null) {
            if (DEBUG) Log.e(TAG, "dispatch() deserialize null");
            return;
        }

        if (DEBUG) Log.i(TAG, "dispatch() deserialized " + activityId);
        Intent newIntent = activityId.createIntent((Activity)activity);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ((Activity)activity).startActivity(newIntent);
        ((Activity)activity).finish();
    }

}
