package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.widget.BaseAdapter;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ToastRunnable;
import com.chanapps.four.data.ChanWatchlist;
import com.chanapps.four.data.UserStatistics;
import com.chanapps.four.service.NetworkProfileManager;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class WatchlistCleanDialogFragment extends DialogFragment {
    public static final String TAG = WatchlistCleanDialogFragment.class.getSimpleName();
    final private BoardGroupFragment fragment;
    public WatchlistCleanDialogFragment() {
        super();
        this.fragment = null;
    }
    public WatchlistCleanDialogFragment(BoardGroupFragment fragment) {
        super();
        this.fragment = fragment;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.WATCHLIST_CLEAN);
        return (new AlertDialog.Builder(getActivity()))
                .setMessage(R.string.dialog_clean_watchlist)
                .setPositiveButton(R.string.dialog_clean,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                (new ChanWatchlist.CleanWatchlistTask(getActivity(), true, fragment)).execute();
                                dismiss();
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            	dismiss();
                            }
                        })
                .create();
    }
}
