package com.chanapps.four.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.fragment.SettingsFragment;

/**
 * User: mpop
 * Date: 11/20/12
 * Time: 11:50 PM
 */
public class SettingsActivity extends Activity {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String PREF_SHOW_NSFW_BOARDS = "pref_show_nsfw_boards";
    public static final String PREF_USE_FAVORITES = "pref_use_favorites";
    public static final String PREF_NOTIFICATIONS = "pref_notifications";
    public static final String PREF_HIDE_ALL_TEXT = "pref_hide_all_text";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveInstanceState();
    }

    protected void saveInstanceState() {
        DispatcherHelper.saveActivityToPrefs(this);
    }

}

