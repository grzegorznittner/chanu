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
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;

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

    /*
    public static boolean isDispatchable(ChanIdentifiedActivity activity) {
        return PreferenceManager
                .getDefaultSharedPreferences(activity.getBaseContext())
                .getString(LAST_ACTIVITY, null)
                != null;
    }
    */

    public static void dispatch(final ChanIdentifiedActivity activity) {
        if (activity == null || !(activity instanceof Activity)) {
            Log.e(TAG, "dispatch() prematurely terminating since called with invalid activity=" + activity);
            return;
        }
        /*
        boolean startWithFavorites = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext())
                .getBoolean(SettingsActivity.PREF_START_WITH_FAVORITES, true);
        if (startWithFavorites) {
        */
        if (DEBUG) Log.i(TAG, "dispatch() startWithFavorites=true, starting async dispatch");
            asyncDispatch((Activity)activity);
    /*
    }
        else {
            if (DEBUG) Log.i(TAG, "dispatch() startWithFavorites=false, starting dispatch process immediately");
            syncDispatch(activity);
        }
        */
    }

    protected static void asyncDispatch(final Activity activity) { // assume only called if startWithFavorites is true
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChanBoard board = ChanFileStorage.loadBoardData(activity, ChanBoard.FAVORITES_BOARD_CODE);
                if (board != null && board.hasData()) {
                    if (DEBUG) Log.i(TAG, "asyncDispatch found non-empty favorites board, dispatching to favorites");
                    dispatchToBoard(activity, ChanBoard.FAVORITES_BOARD_CODE);
                }
                else {
                    if (DEBUG) Log.i(TAG, "asyncDispatch found empty favorites board, dispatching to allBoards");
                    dispatchToBoard(activity, ChanBoard.ALL_BOARDS_BOARD_CODE);
                }
            }
        }).start();
    }

    /*
    protected static boolean syncDispatch(final ChanIdentifiedActivity activity) {
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
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (DEBUG) Log.i(TAG, "dispatch() created intent=" + newIntent
                + " boardCode=" + newIntent.getStringExtra("boardCode"));
        ((Activity)activity).startActivity(newIntent);
        ((Activity)activity).finish();
        return true;
    }
    */

    protected static void dispatchToBoard(Activity activity, String boardCode) {
        Intent newIntent = BoardActivity.createIntent(activity, boardCode, "");
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (DEBUG) Log.i(TAG, "dispatchToBoard() /" + boardCode + "/");
        activity.startActivity(newIntent);
        activity.finish();
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
