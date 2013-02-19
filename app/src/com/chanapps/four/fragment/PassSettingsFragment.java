package com.chanapps.four.fragment;

import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.service.NetworkProfileManager;


/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/22/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class PassSettingsFragment
    extends PreferenceFragment
    implements Preference.OnPreferenceChangeListener
{

    public static String PREF_PASS_CLOSE_BUTTON = "pref_pass_close_button";
    public static String PREF_PASS_AUTH_BUTTON = "pref_pass_auth_button";
    public static String TAG = PassSettingsFragment.class.getSimpleName();

    protected DialogInterface.OnDismissListener dismissListener;
    protected SharedPreferences prefs;
    protected Preference passEnabled;
    protected Preference authButton;
    protected Preference closeButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pass_preferences);

        passEnabled = findPreference(SettingsActivity.PREF_PASS_ENABLED);
        passEnabled.setOnPreferenceChangeListener(this);

        authButton = findPreference(PREF_PASS_AUTH_BUTTON);
        authButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast.makeText(getActivity(), "Authorized", Toast.LENGTH_SHORT).show();
                PassSettingsFragment.this.dismiss();
                return true;
            }
        });
        updateAuthorizeVisibility();

        closeButton = findPreference(PREF_PASS_CLOSE_BUTTON);
        closeButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ensurePrefs().edit().putBoolean(SettingsActivity.PREF_PASS_ENABLED, false).commit();
                PassSettingsFragment.this.dismiss();
                return true;
            }
        });
    }

    public void show(FragmentTransaction transaction, String tag) {
        transaction.add(this, tag);
        transaction.commit();
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    public void dismiss() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.remove(this);
        ft.commit();
        if (dismissListener != null)
            dismissListener.onDismiss(null);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (newValue instanceof Boolean && ((Boolean) newValue).booleanValue())
            updateAuthorizeVisibility(true);
        return true;
    }

    public SharedPreferences ensurePrefs() {
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return prefs;
    }

    private boolean isPassEnabled() {
        return ensurePrefs().getBoolean(SettingsActivity.PREF_PASS_ENABLED, false);
    }

    private boolean isPassAvailable() {
        switch (NetworkProfileManager.instance().getCurrentProfile().getConnectionType()) {
            case WIFI:
                return true;
            case MOBILE:
            case NO_CONNECTION:
            default:
                return false;
        }
    }

    private void updateAuthorizeVisibility() {
        updateAuthorizeVisibility(isPassEnabled());
    }

    private void updateAuthorizeVisibility(boolean passEnabled) {
        if (isPassAvailable() && passEnabled)
            authButton.setEnabled(true);
        else
            authButton.setEnabled(false);
    }

}
