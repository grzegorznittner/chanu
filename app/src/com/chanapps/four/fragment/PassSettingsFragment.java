package com.chanapps.four.fragment;

import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.chanapps.four.activity.R;


/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/22/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class PassSettingsFragment extends PreferenceFragment
{

    public static String PREF_PASS_CLOSE_BUTTON = "pref_pass_close_button";
    public static String TAG = PassSettingsFragment.class.getSimpleName();

    protected DialogInterface.OnDismissListener dismissListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pass_preferences);

        Preference closeButton = findPreference(PREF_PASS_CLOSE_BUTTON);
        closeButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
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

}
