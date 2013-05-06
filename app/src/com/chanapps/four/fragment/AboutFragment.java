package com.chanapps.four.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.BaseAdapter;
import com.chanapps.four.activity.AboutActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.IconPreferenceScreen;
import com.chanapps.four.data.ChanHelper;


/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/22/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class AboutFragment extends PreferenceFragment
{

    public static String TAG = AboutFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.about_preferences);

        linkPreference("pref_about_icon", "market://search?q=pub:Chanapps Software");
        linkPreference("pref_about_application_version", "market://search?q=pub:Chanapps Software");
        linkPreference("pref_about_application_rate", "market://search?q=pub:Chanapps Software");
        linkPreference("pref_about_data_4chan", "https://github.com/4chan/4chan-API");
        linkPreference("pref_about_data_uil", "https://github.com/nostra13/Android-Universal-Image-Loader");
        linkPreference("pref_about_data_color", "https://code.google.com/p/color-picker-view/");
        linkPreference("pref_about_developer_chanapps", "market://search?q=pub:Chanapps Software");

        emailPreference("pref_about_developer_burns", getString(R.string.pref_about_developer_burns_sum));
        emailPreference("pref_about_developer_nittner", getString(R.string.pref_about_developer_nittner_sum));
        emailPreference("pref_about_developer_pop", getString(R.string.pref_about_developer_pop_sum));
        emailPreference("pref_about_developer_milas", getString(R.string.pref_about_developer_milas_sum));

    }

    protected void linkPreference(final String pref, final String url) {
        findPreference(pref).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        ChanHelper.launchUrlInBrowser(getActivity(), url);
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
                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.application_name));
                        getActivity().startActivity(
                                Intent.createChooser(intent, getString(R.string.pref_about_send_email)));
                        return true;
                    }
                });
    }

}
