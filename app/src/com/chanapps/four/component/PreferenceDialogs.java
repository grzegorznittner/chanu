package com.chanapps.four.component;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/17/14
 * Time: 6:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class PreferenceDialogs {

    Activity mActivity;

    public PreferenceDialogs() {}

    public PreferenceDialogs(Activity activity) {
        mActivity = activity;
    }

    public void showNSFWDialog() {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity);
        final boolean showNSFW = pref.getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.pref_show_nsfw_boards)
                .setMessage(R.string.pref_show_nsfw_boards_summ_message)
                .setNegativeButton(R.string.cancel, null);
        if (showNSFW)
            builder.setPositiveButton(R.string.pref_show_nsfw_boards_summ_neg, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    pref.edit().putBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false).commit();
                    mActivity.recreate();
                }
            });
        else
            builder.setPositiveButton(R.string.pref_show_nsfw_boards_summ_pos, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    pref.edit().putBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, true).commit();
                    mActivity.recreate();
                }
            });
        AlertDialog d = builder.create();
        d.show();
    }


    /*
    <ListPreference
        android:key="pref_font_size"
        android:title="@string/font_size_menu"
        android:entries="@array/font_sizes"
        android:entryValues="@array/font_sizes"
        android:defaultValue="@string/font_size_medium" />

*/
    public void showFontSizeDialog() {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity);
        final String fontSize = pref.getString(SettingsActivity.PREF_FONT_SIZE, mActivity.getString(R.string.font_size_medium));
        final String[] fontSizes = mActivity.getResources().getStringArray(R.array.font_sizes);
        int checkedItem = 0;
        for (int i = 0; i < fontSizes.length; i++)
            if (fontSizes[i].equals(fontSize))
                checkedItem = i;
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.font_size_menu)
                .setNeutralButton(R.string.cancel, null);
        builder.setSingleChoiceItems(R.array.font_sizes, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newFontSize = fontSizes[which];
                pref.edit().putString(SettingsActivity.PREF_FONT_SIZE, newFontSize).commit();
                dialog.dismiss();
                if (mActivity != null)
                    mActivity.recreate();
            }
        });
        AlertDialog d = builder.create();
        d.show();
    }

    /*
            <ListPreference
                android:key="pref_autoload_images"
                android:title="@string/pref_autoload_images_title"
                android:entries="@array/pref_autoload_images_entries"
                android:entryValues="@array/pref_autoload_images_entry_values"
                android:defaultValue="@string/pref_autoload_images_auto_value" />

     */
    public void showAutoloadImagesDialog() {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity);
        final String autoloadValue = pref.getString(SettingsActivity.PREF_AUTOLOAD_IMAGES,
                mActivity.getString(R.string.pref_autoload_images_auto_value));
        final String[] autoloadValues = mActivity.getResources().getStringArray(R.array.pref_autoload_images_entry_values);
        int checkedItem = 0;
        for (int i = 0; i < autoloadValues.length; i++)
            if (autoloadValues[i].equals(autoloadValue))
                checkedItem = i;
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.pref_autoload_images_title)
                .setNeutralButton(R.string.cancel, null);
        builder.setSingleChoiceItems(R.array.pref_autoload_images_entries, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newAutoloadValue = autoloadValues[which];
                pref.edit().putString(SettingsActivity.PREF_AUTOLOAD_IMAGES, newAutoloadValue).commit();
                dialog.dismiss();
                if (mActivity != null)
                    mActivity.recreate();
            }
        });
        AlertDialog d = builder.create();
        d.show();
    }

    /*
            <ListPreference
                    android:key="pref_theme"
                    android:title="@string/pref_theme_title"
                    android:entries="@array/pref_theme_entries"
                    android:entryValues="@array/pref_theme_entry_values"
                    android:defaultValue="@string/pref_theme_default_value" />


     */
    public void showThemeDialog() {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity);
        final String value = pref.getString(SettingsActivity.PREF_THEME,
                mActivity.getString(R.string.pref_theme_default_value));
        final String[] values = mActivity.getResources().getStringArray(R.array.pref_theme_entry_values);
        int checkedItem = 0;
        for (int i = 0; i < values.length; i++)
            if (values[i].equals(value))
                checkedItem = i;
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.pref_theme_title)
                .setNeutralButton(R.string.cancel, null);
        builder.setSingleChoiceItems(R.array.pref_theme_entries, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newAutoloadValue = values[which];
                pref.edit().putString(SettingsActivity.PREF_THEME, newAutoloadValue).commit();
                dialog.dismiss();
                if (mActivity != null)
                    mActivity.recreate();
            }
        });
        AlertDialog d = builder.create();
        d.show();
    }

}
