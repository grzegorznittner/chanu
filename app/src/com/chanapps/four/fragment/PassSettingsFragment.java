package com.chanapps.four.fragment;

import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import com.chanapps.four.activity.PostReplyActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.RefreshableActivity;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.task.AuthorizePassTask;


/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/22/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class PassSettingsFragment extends PreferenceFragment
{

    public static String TAG = PassSettingsFragment.class.getSimpleName();

    public static String PREF_PASS_AUTH_BUTTON = "pref_pass_auth_button";
    public static String PREF_PASS_PURCHASE_BUTTON = "pref_pass_purchase_button";
    public static String PREF_PASS_CLOSE_BUTTON = "pref_pass_close_button";
    public static String PASS_PURCHASE_URL = "https://www.4chan.org/pass";

    protected DialogInterface.OnDismissListener dismissListener;
    protected RefreshableActivity refreshableActivity;
    protected SharedPreferences prefs;
    protected Preference authButton;
    protected Preference purchaseButton;
    protected Preference closeButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pass_preferences);

        authButton = findPreference(PREF_PASS_AUTH_BUTTON);
        authButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                authorizePass();
                PassSettingsFragment.this.dismiss();
                return true;
            }
        });
        updateAuthorizeVisibility();

        purchaseButton = findPreference(PREF_PASS_PURCHASE_BUTTON);
        purchaseButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // we're cheating
                ChanHelper.launchUrlInBrowser((PostReplyActivity)refreshableActivity, PASS_PURCHASE_URL);
                return true;
            }
        });

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

    public void setRefreshableActivity(RefreshableActivity activity) {
        refreshableActivity = activity;
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
        if (isPassAvailable())
            authButton.setEnabled(true);
        else
            authButton.setEnabled(false);
    }

    private void authorizePass() {
        String passToken = ensurePrefs().getString(SettingsActivity.PREF_PASS_TOKEN, "");
        String passPIN = ensurePrefs().getString(SettingsActivity.PREF_PASS_PIN, "");
        AuthorizePassTask authorizePassTask = new AuthorizePassTask(refreshableActivity, passToken, passPIN);
        AuthorizingPassDialogFragment passDialogFragment = new AuthorizingPassDialogFragment(authorizePassTask);
        passDialogFragment.show(getFragmentManager(), AuthorizingPassDialogFragment.TAG);
        if (!authorizePassTask.isCancelled()) {
            authorizePassTask.execute(passDialogFragment);
        }
    }

}
