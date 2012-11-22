package com.chanapps.four.test;

import android.app.Activity;
import android.os.Bundle;
import com.chanapps.four.component.SettingsFragment;

/**
 * User: mpop
 * Date: 11/20/12
 * Time: 11:50 PM
 */
public class SettingsActivity extends Activity {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String PREF_HIDE_ALL_TEXT = "pref_hide_all_text";
    public static final String PREF_HIDE_TEXT_ONLY_POSTS = "pref_hide_text_only_posts";
    public static final String PREF_NOTIFICATIONS = "pref_notifications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
}

