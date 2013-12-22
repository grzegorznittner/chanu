package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.UserStatistics;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.widget.WidgetProviderUtils;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 12/14/12
 * Time: 12:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResetPreferencesDialogFragment extends DialogFragment {
    public static final String TAG = ResetPreferencesDialogFragment.class.getSimpleName();
    private SettingsFragment fragment;

    public ResetPreferencesDialogFragment(){}

    public ResetPreferencesDialogFragment(SettingsFragment fragment) {
        super();
        this.fragment = fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
        TextView title = (TextView) layout.findViewById(R.id.title);
        TextView message = (TextView) layout.findViewById(R.id.message);
        title.setText(R.string.dialog_reset_preferences);
        message.setText(R.string.dialog_reset_preferences_confirm);
        setStyle(STYLE_NO_TITLE, 0);
        return (new AlertDialog.Builder(getActivity()))
                .setView(layout)
                .setPositiveButton(R.string.pref_reset_to_defaults,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Context context = getActivity();

                                // do this jazz to save widget conf even on clear because you can't programmatically remove widgets
                                Set<String> savedWidgetConf = WidgetProviderUtils.getActiveWidgetPref(context);

                                PreferenceManager.getDefaultSharedPreferences(context)
                                        .edit()
                                        .clear()
                                        .putStringSet(SettingsActivity.PREF_WIDGET_BOARDS, savedWidgetConf)
                                        .commit();

                                UserStatistics stats = NetworkProfileManager.instance().getUserStatistics();
                                if (stats != null)
                                    stats.reset();
                                fragment.setPreferenceScreen(null);
                                fragment.initPreferenceScreen();
                                Toast.makeText(getActivity(), R.string.dialog_reset_preferences, Toast.LENGTH_SHORT).show();
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // ignore
                            }
                        })
                .create();
    }
}
