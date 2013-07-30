package com.chanapps.four.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.widget.BaseAdapter;
import android.widget.Toast;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.activity.AboutActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.UserStatistics;
import com.chanapps.four.service.NetworkProfileManager;


/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/22/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class SettingsFragment
        extends PreferenceFragment
//        implements SharedPreferences.OnSharedPreferenceChangeListener
{

    public static String TAG = SettingsFragment.class.getSimpleName();

    public Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPreferenceScreen();
//        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public void initPreferenceScreen() {
        addPreferencesFromResource(R.xml.preferences);

        Preference blocklistButton = findPreference(SettingsActivity.PREF_BLOCKLIST_BUTTON);
        blocklistButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                (new BlocklistSelectToViewDialogFragment(SettingsFragment.this))
                        .show(getFragmentManager(), SettingsFragment.TAG);
                return true;
            }
        });

        Preference resetPrefsButton = findPreference(SettingsActivity.PREF_RESET_TO_DEFAULTS);
        resetPrefsButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                (new ResetPreferencesDialogFragment(SettingsFragment.this))
                        .show(getFragmentManager(), SettingsFragment.TAG);
                return true;
            }
        });

        Preference clearCacheButton = findPreference(SettingsActivity.PREF_CLEAR_CACHE);
        clearCacheButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                (new ClearCacheDialogFragment(SettingsFragment.this))
                        .show(getFragmentManager(), SettingsFragment.TAG);
                return true;
            }
        });

        Preference clearWatchlistButton = findPreference(SettingsActivity.PREF_CLEAR_WATCHLIST);
        clearWatchlistButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                (new WatchlistClearDialogFragment())
                        .show(getFragmentManager(), WatchlistClearDialogFragment.TAG);
                return true;
            }
        });

        Preference clearFavoritesButton = findPreference(SettingsActivity.PREF_CLEAR_FAVORITES);
        clearFavoritesButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                (new FavoritesClearDialogFragment())
                        .show(getFragmentManager(), FavoritesClearDialogFragment.TAG);
                return true;
            }
        });

        Preference aboutButton = findPreference(SettingsActivity.PREF_ABOUT);
        aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), AboutActivity.class);
                startActivity(intent);
                return true;
            }
        });

        Preference.OnPreferenceClickListener namesListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.SETTINGS_NAMES);
                return false;
            }
        };
        Preference name = findPreference(SettingsActivity.PREF_USER_NAME);
        Preference email = findPreference(SettingsActivity.PREF_USER_EMAIL);
        Preference password = findPreference(SettingsActivity.PREF_USER_PASSWORD);
        name.setOnPreferenceClickListener(namesListener);
        email.setOnPreferenceClickListener(namesListener);
        password.setOnPreferenceClickListener(namesListener);

        Preference.OnPreferenceChangeListener themeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SettingsActivity activity = (SettingsActivity)getActivity();
                activity.recreate();
                return true;
            }
        };
        Preference theme = findPreference(SettingsActivity.PREF_THEME);
        theme.setOnPreferenceChangeListener(themeListener);
    }

/*
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        boolean passEnabled = prefs.getBoolean(SettingsActivity.PREF_PASS_ENABLED, false);
        int stringId = validatePass(prefs);
        boolean passInvalid = stringId > 0;
        Toast.makeText(getActivity(), "key: " + key + " invalid:" + passInvalid, Toast.LENGTH_SHORT).show();
        if (SettingsActivity.PREF_PASS_TOKEN.equals(key) && passEnabled && passInvalid) {
            disablePass(prefs, stringId);
        }
        else if (SettingsActivity.PREF_PASS_PIN.equals(key) && passEnabled && passInvalid) {
            disablePass(prefs, stringId);
        }
        else if (SettingsActivity.PREF_PASS_ENABLED.equals(key) && passEnabled && passInvalid) {
            disablePass(prefs, stringId);
        }
    }

    private int validatePass(SharedPreferences prefs) {
        String token = prefs.getString(SettingsActivity.PREF_PASS_TOKEN, null);
        String pin = prefs.getString(SettingsActivity.PREF_PASS_PIN, null);
        if ("".equals(token) || token.length() != 10) {
            return R.string.pref_pass_token_invalid;
        }
        else if ("".equals(pin) || pin.length() != 6) {
            return R.string.pref_pass_pin_invalid;
        }
        else {
            return 0;
        }
    }

    private void disablePass(SharedPreferences prefs, int stringId) {
        prefs.edit().putBoolean(SettingsActivity.PREF_PASS_ENABLED, false).commit();
        if (getActivity() != null)
            Toast.makeText(getActivity(), stringId, Toast.LENGTH_SHORT).show();
        ensureHandler().sendEmptyMessageDelayed(0, 1000);
    }
*/
    public Handler ensureHandler() {
        if (handler == null)
            handler = new ReloadPrefsHandler(this);
        return handler;
    }

    protected class ReloadPrefsHandler extends Handler {
        SettingsFragment fragment;
        public ReloadPrefsHandler() {}
        public ReloadPrefsHandler(SettingsFragment fragment) {
            this.fragment = fragment;
        }
        @Override
        public void handleMessage(Message msg) {
            ((BaseAdapter)fragment.getPreferenceScreen().getRootAdapter()).notifyDataSetInvalidated();
        }
    }

}
