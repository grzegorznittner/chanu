package com.chanapps.four.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.BaseAdapter;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.widget.WidgetAlarmReceiver;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/22/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class SettingsFragment extends PreferenceFragment {

    public static String TAG = SettingsFragment.class.getSimpleName();

    public Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        Preference resetPrefsButton = (Preference)findPreference(SettingsActivity.PREF_RESET_TO_DEFAULTS);
        resetPrefsButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ResetPreferencesDialogFragment dialog = new ResetPreferencesDialogFragment(SettingsFragment.this);
                dialog.show(getFragmentManager(), SettingsFragment.TAG);
                return true;
            }
        });

        List<ChanBoard> boards = ChanBoard.getSortedBoards(this.getActivity().getApplicationContext());
        int numBoards = boards.size();
        String[] entries = new String[numBoards];
        String[] entryValues = new String[numBoards];
        for (int i = 0; i < numBoards; i++) {
            ChanBoard board = boards.get(i);
            entries[i] = "/" + board.link + " " + board.name;
            entryValues[i] = board.link;
        }

        ListPreference widgetBoard = (ListPreference)findPreference(SettingsActivity.PREF_WIDGET_BOARD);
        updateWidgetSummary(widgetBoard, widgetBoard.getValue());
        widgetBoard.setEntries(entries);
        widgetBoard.setEntryValues(entryValues);
        widgetBoard.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateWidgetSummary(preference, (String)newValue);
                WidgetAlarmReceiver.refreshWidget(SettingsFragment.this.getActivity().getApplicationContext());
                return true;
            }
        });

    }

    private void updateWidgetSummary(Preference preference, String boardCode) {
        String newSummary = getString(R.string.pref_widget_board_summ) + " /" + boardCode;
        preference.setSummary(newSummary);
    }

    public Handler ensureHandler() {
        if (handler == null)
            handler = new ReloadPrefsHandler(this);
        return handler;
    }

    protected class ReloadPrefsHandler extends Handler {
        SettingsFragment fragment;
        public ReloadPrefsHandler() {}
        public ReloadPrefsHandler(SettingsFragment fragment) {
            this.fragment = fragment;
        }
        @Override
        public void handleMessage(Message msg) {
            ((BaseAdapter)fragment.getPreferenceScreen().getRootAdapter()).notifyDataSetInvalidated();
        }
    }
}
