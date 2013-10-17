package com.chanapps.four.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;
import com.chanapps.four.activity.AboutActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.BillingComponent;
import org.apache.commons.lang3.time.DateUtils;

import java.text.DateFormat;
import java.util.Date;

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

        addPurchasePreferences();
        //addPurchasePreference();
        //addConsumePreference();

        linkPreference("pref_about_store_chanapps", "http://www.skreened.com/chanapps/");
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
                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.application_name));
                        getActivity().startActivity(
                                Intent.createChooser(intent, getString(R.string.pref_about_send_email)));
                        return true;
                    }
                });
    }

    protected void addPurchasePreferences() {
        boolean hasProkey = BillingComponent.getInstance(getActivity().getApplicationContext()).hasProkey();
        if (hasProkey) {
            removePurchasePreference();
            addInstalledProkeyPreference();
        }
        else {
            addPurchasePreference();
        }
    }

    protected void removePurchasePreference() {
        PreferenceCategory purchaseCategory = (PreferenceCategory)findPreference(AboutActivity.PREF_PURCHASE_CATEGORY);
        Preference purchaseProkeyButton = findPreference(AboutActivity.PREF_PURCHASE_PROKEY);
        purchaseCategory.removePreference(purchaseProkeyButton);
    }

    protected void addInstalledProkeyPreference() {
        Preference installedProkey = new Preference(getActivity());
        installedProkey.setKey(AboutActivity.PREF_INSTALLED_PROKEY);
        installedProkey.setTitle(R.string.pref_installed_prokey);
        installedProkey.setSummary(R.string.pref_installed_prokey_summ);
        PreferenceCategory purchaseCategory = (PreferenceCategory)findPreference(AboutActivity.PREF_PURCHASE_CATEGORY);
        purchaseCategory.addPreference(installedProkey);
    }

    protected void addPurchasePreference() {
        Preference purchaseProkeyButton = findPreference(AboutActivity.PREF_PURCHASE_PROKEY);
        purchaseProkeyButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (DEBUG) Log.i(TAG, "onPreferenceClick() purchase prokey");
                //DialogFragment outerFragment = null;
                try {
                    //final DialogFragment fragment = new ConnectingToBillingDialogFragment();
                    //outerFragment = fragment;
                    //fragment.show(getFragmentManager(), SettingsFragment.TAG);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Activity activity = getActivity();
                            if (activity == null)
                                return;
                            BillingComponent
                                    .getInstance(activity.getApplicationContext())
                                    .purchaseProkey(getActivity()); //, fragment);
                            //if (activity instanceof ChanIdentifiedActivity) {
                            //    Handler handler = ((ChanIdentifiedActivity) activity).getChanHandler();
                            //    if (handler != null)
                            //        handler.post(new Runnable() {
                            //            @Override
                            //            public void run() {
                            //                fragment.dismiss();
                            //            }
                            //        });
                            //}
                        }
                    }).start();
                }
                catch (Exception e) {
                    Log.e(TAG, "onPreferenceClick() exception purchasing prokey", e);
                    Toast.makeText(getActivity(), R.string.purchase_error, Toast.LENGTH_LONG).show();
                    //if (outerFragment != null)
                    //    outerFragment.dismiss();
                }
                return true;
            }
        });
    }

}