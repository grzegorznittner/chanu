package com.chanapps.four.component;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chanapps.four.activity.*;

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
    public static final String IGNORE_DISPATCH = "ignoreDispatch";

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

    public static boolean isDispatchable(ChanIdentifiedActivity activity) {
        return PreferenceManager
                .getDefaultSharedPreferences(activity.getBaseContext())
                .getString(LAST_ACTIVITY, null)
                != null;
    }

    public static boolean dispatch(ChanIdentifiedActivity activity) {

        Intent intent = ((Activity)activity).getIntent();
        if (intent.hasExtra(IGNORE_DISPATCH) && intent.getBooleanExtra(IGNORE_DISPATCH, false)) {
            if (DEBUG) Log.i(TAG, "dispatch ignored by intent");
            return false;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
        String serialized = prefs.getString(LAST_ACTIVITY, null);
        if (serialized == null || serialized.isEmpty()) {
            if (DEBUG) Log.e(TAG, "dispatch() deserialize empty");
            return false;
        }

        ChanActivityId activityId = ChanActivityId.deserialize(serialized);
        if (activityId == null) {
            if (DEBUG) Log.e(TAG, "dispatch() deserialize null");
            return false;
        }

        if (DEBUG) Log.i(TAG, "dispatch() deserialized " + activityId);
        Intent newIntent = activityId.createIntent((Activity)activity);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (DEBUG) Log.i(TAG, "dispatch() created intent=" + newIntent
                + " boardCode=" + newIntent.getStringExtra("boardCode"));
        ((Activity)activity).startActivity(newIntent);
        ((Activity)activity).finish();
        return true;
    }

    public static void launchUrlInBrowser(Context context, String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        context.startActivity(i);
    }

    public static void exitApplication(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean onUIThread() {
        return Looper.getMainLooper().equals(Looper.myLooper());
    }
}
