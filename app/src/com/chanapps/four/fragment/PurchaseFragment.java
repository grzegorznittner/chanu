package com.chanapps.four.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.URLFormatComponent;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/22/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class PurchaseFragment extends PreferenceFragment
{
    protected static final boolean DEBUG = false;
    protected static String TAG = PurchaseFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.purchase_preferences);

        linkPreference("pref_about_store_chanapps",
                URLFormatComponent.getUrl(getActivity(), URLFormatComponent.SKREENED_CHANU_STORE_URL));
        emailPreference("pref_about_contact_us", getString(R.string.pref_about_contact_email));
    }

    protected void linkPreference(final String pref, final String url) {
        findPreference(pref).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        ActivityDispatcher.launchUrlInBrowser(getActivity(), url);
                        return true;
                    }
                });
    }

    protected void emailPreference(final String pref, final String email) {
        findPreference(pref).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Uri uri = Uri.fromParts("mailto", email, null);
                        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                        getActivity().startActivity(
                                Intent.createChooser(intent, getString(R.string.pref_about_send_email)));
                        return true;
                    }
                });
    }
}
