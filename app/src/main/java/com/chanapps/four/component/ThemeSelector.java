package com.chanapps.four.component;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 7/29/13
 * Time: 4:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThemeSelector {

    public static final String ACTION_THEME_CHANGED = "themeChangedAction";
    public static final String EXTRA_THEME_ID = "themeId";
    protected static final String TAG = ThemeSelector.class.getSimpleName();
    protected static final boolean DEBUG = false;
    protected static final int LIGHT_THEME = R.style.AppTheme;
    protected static final int DARK_THEME = R.style.AppTheme_Dark;
    public static final int DEFAULT_THEME = LIGHT_THEME;
    protected static ThemeSelector themeSelector;

    protected String themeType;
    protected int themeId = LIGHT_THEME;
    protected Context context;
    protected SharedPreferences.OnSharedPreferenceChangeListener themeChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (!SettingsActivity.PREF_THEME.equals(key)) return;
            themeType = sharedPreferences.getString(SettingsActivity.PREF_THEME, context.getString(R.string.pref_theme_default_value));
            int newThemeId = calcThemeId();
            if (themeId == newThemeId) return;
            updateTheme(newThemeId);
            if (DEBUG)
                Log.i(TAG, "themeChangeListener set to " + (themeId == DARK_THEME ? "dark" : "light") + " theme");
        }
    };

    protected ThemeSelector(Context context) {
        this.context = context;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        themeType = prefs.getString(SettingsActivity.PREF_THEME, context.getString(R.string.pref_theme_default_value));
        themeId = calcThemeId();
        if (DEBUG)
            Log.i(TAG, "ThemeSelector() set to " + (themeId == DARK_THEME ? "dark" : "light") + " theme");
        prefs.registerOnSharedPreferenceChangeListener(themeChangeListener);

    }

    public static ThemeSelector instance(Context context) {
        if (themeSelector == null) themeSelector = new ThemeSelector(context);
        return themeSelector;
    }

    protected void updateTheme(int newTheme) {
        if (themeId == newTheme) return;
        themeId = newTheme;
        Intent intent = new Intent(ACTION_THEME_CHANGED);
        intent.putExtra(EXTRA_THEME_ID, themeId);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    protected int calcThemeId() {
        int id;
        if (context.getString(R.string.pref_theme_light_value).equals(themeType)) id = LIGHT_THEME;
        else if (context.getString(R.string.pref_theme_dark_value).equals(themeType))
            id = DARK_THEME;
        else id = DEFAULT_THEME;
        if (DEBUG) Log.i(TAG, "calcThemeId('" + themeType + "') = " + id);
        return id;
    }

    public boolean isDark() {
        return themeId == R.style.AppTheme_Dark;
    }

    public interface ThemeActivity {
        int getThemeId();

        void setThemeId(int themeId);

        void setTheme(int themeId);

        void recreate();

        Context getApplicationContext();
    }

    public static class ThemeReceiver extends BroadcastReceiver {
        protected ThemeActivity activity;

        public ThemeReceiver() {
            super();
        }

        public ThemeReceiver(ThemeActivity activity) {
            this();
            this.activity = activity;
            int currentTheme = ThemeSelector.instance(activity.getApplicationContext()).themeId;
            if (activity.getThemeId() != currentTheme) {
                activity.setThemeId(currentTheme);
                activity.setTheme(currentTheme);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (activity == null || intent == null || intent.getAction() == null || !intent.getAction().equals(ThemeSelector.ACTION_THEME_CHANGED) || !intent.hasExtra(ThemeSelector.EXTRA_THEME_ID))
                return;
            int newThemeId = intent.getIntExtra(ThemeSelector.EXTRA_THEME_ID, ThemeSelector.DEFAULT_THEME);
            if (activity.getThemeId() != newThemeId) activity.recreate();
        }

        public void register() {
            IntentFilter intentFilter = new IntentFilter(ThemeSelector.ACTION_THEME_CHANGED);
            LocalBroadcastManager.getInstance(activity.getApplicationContext()).registerReceiver(this, intentFilter);
        }

        public void unregister() {
            LocalBroadcastManager.getInstance(activity.getApplicationContext()).unregisterReceiver(this);
        }
    }

}
