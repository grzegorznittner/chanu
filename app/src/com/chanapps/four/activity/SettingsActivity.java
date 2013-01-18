package com.chanapps.four.activity;

import android.app.Activity;
import android.content.AsyncTaskLoader;
import android.database.Cursor;
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
    public static final String PREF_HIDE_ALL_TEXT = "pref_hide_all_text";
    public static final String PREF_AUTOMATICALLY_MANAGE_WATCHLIST = "pref_automatically_manage_watchlist";
    public static final String PREF_CLEAR_CACHE = "pref_clear_cache";
    public static final String PREF_RESET_TO_DEFAULTS = "pref_reset_to_defaults";

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
}

