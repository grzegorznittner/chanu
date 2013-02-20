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

    public static void saveActivityToPrefs(Activity activity) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
        ChanHelper.LastActivity lastActivity;
        if (activity instanceof GalleryViewActivity)
            lastActivity = ChanHelper.LastActivity.FULL_SCREEN_IMAGE_ACTIVITY;
        else if (activity instanceof ThreadActivity)
            lastActivity = ChanHelper.LastActivity.THREAD_ACTIVITY;
        else if (activity instanceof BoardActivity)
            lastActivity = ChanHelper.LastActivity.BOARD_ACTIVITY;
        else if (activity instanceof PostReplyActivity)
            lastActivity = ChanHelper.LastActivity.POST_REPLY_ACTIVITY;
        else if (activity instanceof SettingsActivity)
            lastActivity = ChanHelper.LastActivity.SETTINGS_ACTIVITY;
        else
            lastActivity = ChanHelper.LastActivity.BOARD_SELECTOR_ACTIVITY;
        editor.putString(ChanHelper.LAST_ACTIVITY, lastActivity.toString());
        editor.commit();
    }

    public static ChanHelper.LastActivity getLastActivity(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return ChanHelper.LastActivity.valueOf(
            prefs.getString(ChanHelper.LAST_ACTIVITY, ChanHelper.LastActivity.BOARD_SELECTOR_ACTIVITY.toString()));
    }

    public static void dispatchIfNecessaryFromPrefsState(Activity activity) {
        ChanHelper.LastActivity lastActivity = getLastActivity(activity);
        Class activityClass;
        switch (lastActivity) {
            case FULL_SCREEN_IMAGE_ACTIVITY:
                activityClass = GalleryViewActivity.class;
                break;
            case BOARD_ACTIVITY:
                activityClass = BoardActivity.class;
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
            case BOARD_SELECTOR_ACTIVITY:
            default:
                activityClass = BoardSelectorActivity.class;
        }
        if (activity.getClass() != activityClass) {
            Log.i(BoardSelectorActivity.TAG, "Dispatching to activity:" + lastActivity);
            Intent intent = new Intent(activity, activityClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        }
        else {
            Log.i(BoardSelectorActivity.TAG, "Activity already active, not dispatching");
        }
    }

}
