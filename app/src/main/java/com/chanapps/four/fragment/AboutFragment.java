package com.chanapps.four.fragment;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.chanapps.four.activity.BuildConfig;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.URLFormatComponent;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/22/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class AboutFragment extends PreferenceFragment {
    protected static final boolean DEBUG = false;
    protected static final String VERSION_DATE_FORMAT = "yyyy.MM.dd";
    protected static String TAG = AboutFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.about_preferences);
        //this function link already existing preferences with their url when clicked

        versionPreference("pref_about_application_version");
        linkPreference("pref_about_icon", URLFormatComponent.getUrl(getActivity(), URLFormatComponent.MARKET_CORP_URL));
        linkPreference("pref_about_application_version", URLFormatComponent.getUrl(getActivity(), URLFormatComponent.MARKET_APP_URL));
//        linkPreference("pref_about_application_rate", URLFormatComponent.getUrl(getActivity(), URLFormatComponent.MARKET_APP_URL));
//        linkPreference("pref_about_application_gplus", URLFormatComponent.getUrl(getActivity(), URLFormatComponent.GOOGLE_PLUS_CHANU_URL));
        linkPreference("pref_about_data_4chan", URLFormatComponent.getUrl(getActivity(), URLFormatComponent.GITHUB_CHAN_API_URL));
        linkPreference("pref_about_data_uil", URLFormatComponent.getUrl(getActivity(), URLFormatComponent.GITHUB_UIL_URL));
        linkPreference("pref_about_data_pulltorefresh", URLFormatComponent.getUrl(getActivity(), URLFormatComponent.GITHUB_ABPTR_URL));
        linkPreference("pref_about_data_color", URLFormatComponent.getUrl(getActivity(), URLFormatComponent.GOOGLE_CODE_COLOR_PICKER_URL));
//        linkPreference("pref_about_store_chanapps", URLFormatComponent.getUrl(getActivity(), URLFormatComponent.SKREENED_CHANU_STORE_URL));
//        linkPreference("pref_about_translations_de", URLFormatComponent.getUrl(getActivity(), URLFormatComponent.GERMAN_TRANSLATOR_URL));
    }

    protected void versionPreference(final String pref) {
        if (DEBUG) Log.i(TAG, "versionPreference");
        Preference p = findPreference(pref);
        try {
            PackageInfo info = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            Date buildDate = new Date(BuildConfig.TIMESTAMP);

            String version = info.versionName;
            String dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(buildDate);

            String title = String.format(getString(R.string.pref_about_application_version), version);
            String summary = String.format(getString(R.string.pref_about_application_version_sum), dateStr);

            p.setTitle(title);
            p.setSummary(summary);

            if (DEBUG) Log.i(TAG, "set version title=" + title + " summary=" + summary);
        } catch (Exception e) {
            Log.e(TAG, "Exception setting version preference", e);
        }
    }

    protected void linkPreference(final String pref, final String url) {
        findPreference(pref).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ActivityDispatcher.launchUrlInBrowser(getActivity(), url);
                return true;
            }
        });
    }

}
