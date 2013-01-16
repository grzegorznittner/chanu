package com.chanapps.four.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.BaseAdapter;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;


/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/22/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class SettingsFragment extends PreferenceFragment {

    public static String TAG = SettingsFragment.class.getSimpleName();

    public Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        Preference resetPrefsButton = (Preference)findPreference(SettingsActivity.PREF_RESET_TO_DEFAULTS);
        resetPrefsButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ResetPreferencesDialogFragment dialog = new ResetPreferencesDialogFragment(SettingsFragment.this);
                dialog.show(getFragmentManager(), SettingsFragment.TAG);
                return true;
            }
        });
    }

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
