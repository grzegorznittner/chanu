package com.chanapps.four.component;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chanapps.four.activity.*;
import com.chanapps.four.data.ChanHelper;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 12/18/12
 * Time: 10:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class DispatcherHelper {

    private static final String TAG = DispatcherHelper.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static void saveActivityToPrefs(Activity activity) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
        ChanHelper.LastActivity lastActivity;
        if (activity instanceof GalleryViewActivity)
            lastActivity = ChanHelper.LastActivity.GALLERY_ACTIVITY;
        else if (activity instanceof ThreadActivity)
            lastActivity = ChanHelper.LastActivity.THREAD_ACTIVITY;
        else if (activity instanceof PostReplyActivity)
            lastActivity = ChanHelper.LastActivity.POST_REPLY_ACTIVITY;
        else if (activity instanceof SettingsActivity)
            lastActivity = ChanHelper.LastActivity.SETTINGS_ACTIVITY;
        else
            lastActivity = ChanHelper.LastActivity.BOARD_ACTIVITY;
        editor.putString(ChanHelper.LAST_ACTIVITY, lastActivity.toString());
        editor.commit();
    }

    public static ChanHelper.LastActivity getLastActivity(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return ChanHelper.LastActivity.valueOf(
            prefs.getString(ChanHelper.LAST_ACTIVITY, ChanHelper.LastActivity.INVALID_ACTIVITY.toString()));
    }

    public static void dispatchIfNecessaryFromPrefsState(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent.hasExtra(ChanHelper.IGNORE_DISPATCH) && intent.getBooleanExtra(ChanHelper.IGNORE_DISPATCH, false))
            return;
        ChanHelper.LastActivity lastActivity = getLastActivity(activity);
        Class activityClass;
        switch (lastActivity) {
            case GALLERY_ACTIVITY:
                activityClass = GalleryViewActivity.class;
                break;
            case THREAD_ACTIVITY:
                activityClass = ThreadActivity.class;
                break;
            case POST_REPLY_ACTIVITY:
                activityClass = PostReplyActivity.class;
                break;
            case SETTINGS_ACTIVITY:
                activityClass = SettingsActivity.class;
                break;
            case BOARD_ACTIVITY:
            default:
                activityClass = BoardActivity.class;
        }
        if (activity.getClass() != activityClass) {
            if (DEBUG) Log.i(TAG, "Dispatching to activity:" + lastActivity);
            intent = new Intent(activity, activityClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
            activity.finish();
        }
        else {
            if (DEBUG) Log.i(TAG, "Activity already active, not dispatching");
        }
    }

}
