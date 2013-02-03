package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.RefreshableActivity;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.Dismissable;
import com.chanapps.four.data.ChanBlocklist;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanWatchlist;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class BlocklistAddDialogFragment extends DialogFragment {

    public static final String TAG = BlocklistAddDialogFragment.class.getSimpleName();

    Dismissable dismissable;
    RefreshableActivity activity;
    String userId;
    boolean useFriendlyIds;
    String formattedUserId;

    public BlocklistAddDialogFragment(Dismissable dismissable, RefreshableActivity activity, String userId) {
        super();
        this.dismissable = dismissable;
        this.activity = activity;
        this.userId = userId;
        useFriendlyIds = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext()).getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        formattedUserId = ChanPost.getUserId(userId, useFriendlyIds);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String msg = String.format(activity.getBaseContext().getString(R.string.blocklist_add_id_confirm), formattedUserId);
        return (new AlertDialog.Builder(getActivity()))
                .setMessage(msg)
                .setPositiveButton(R.string.dialog_add,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ChanBlocklist.add(activity.getBaseContext(), userId);
                                activity.refreshActivity();
                                dismissable.dismiss();
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
