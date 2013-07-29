package com.chanapps.four.component;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
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

    protected static final String TAG = ThemeSelector.class.getSimpleName();
    protected static final boolean DEBUG = true;
    protected String themeValue = "-1"; // not initialized
    protected Context context;
    
    public ThemeSelector(Context context) {
        this.context = context;    
    }

    public void initTheme() { // must run in onCreate BEFORE call to setContentView()
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = prefs.getString(SettingsActivity.PREF_THEME,
                context.getString(R.string.pref_theme_default_value));
        if (themeValue.equals(theme)) {
            if (DEBUG) Log.i(TAG, "selectTheme() theme=" + themeValue + " leaving unchanged");
            return;
        }
        themeValue = theme;
        int themeId = transformThemeSettingToId(theme);
        context.setTheme(themeId);
        if (DEBUG) Log.i(TAG, "selectTheme() changed to theme=" + themeValue);
    }

    protected int transformThemeSettingToId(String theme) {
        int themeId;
        if (context.getString(R.string.pref_theme_light_value).equals(themeValue))
            themeId = R.style.AppTheme;
        else if (context.getString(R.string.pref_theme_dark_value).equals(themeValue))
            themeId = R.style.AppTheme_Dark;
        else if (context.getString(R.string.pref_theme_auto_value).equals(themeValue))
            themeId = R.style.AppTheme; // should be fixed to check sensor lux levels
        else
            themeId = R.style.AppTheme;
        return themeId;
    }

    public boolean themeMatchesPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = prefs.getString(SettingsActivity.PREF_THEME,
                context.getString(R.string.pref_theme_default_value));
        if (themeValue.equals(theme)) {
            if (DEBUG) Log.i(TAG, "themeMatchesPrefs() theme=" + theme + " leaving unchanged");
            return true;
        }
        else {
            if (DEBUG) Log.i(TAG, "themeMatchesPrefs() should change to new theme=" + theme);
            return false;
        }
    }

    public boolean isDark() {
        int themeId = transformThemeSettingToId(themeValue);
        return themeId == R.style.AppTheme_Dark;
    }

}
