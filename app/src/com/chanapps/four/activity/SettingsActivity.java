package com.chanapps.four.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.SettingsFragment;

/**
 * User: mpop
 * Date: 11/20/12
 * Time: 11:50 PM
 */
public class SettingsActivity extends Activity implements ChanIdentifiedActivity {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String PREF_SHOW_NSFW_BOARDS = "pref_show_nsfw_boards";
    public static final String PREF_NOTIFICATIONS = "pref_notifications";
    public static final String PREF_HIDE_POST_NUMBERS = "pref_hide_post_numbers";
    public static final String PREF_USE_FRIENDLY_IDS = "pref_use_friendly_ids";
    public static final String PREF_USER_NAME = "pref_user_name";
    public static final String PREF_USER_EMAIL = "pref_user_email";
    public static final String PREF_USER_PASSWORD = "pref_user_password";
    public static final String PREF_CACHE_SIZE = "pref_cache_size";
    public static final String PREF_CLEAR_CACHE = "pref_clear_cache";
    public static final String PREF_CLEAR_WATCHLIST = "pref_clear_watchlist";
    public static final String PREF_RESET_TO_DEFAULTS = "pref_reset_to_defaults";
    public static final String PREF_BLOCKLIST_BUTTON = "pref_blocklist_button";
    public static final String PREF_ABOUT = "pref_about";
    public static final String PREF_PASS_TOKEN = "pref_pass_token";
    public static final String PREF_PASS_PIN = "pref_pass_pin";
    public static final String PREF_PASS_ENABLED = "pref_pass_enabled";

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

    @Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.SETTINGS_ACTIVITY);
	}

	@Override
	public Handler getChanHandler() {
		return null;
	}

    @Override
    public void refresh() {}

    @Override
    public void closeSearch() {}

}

