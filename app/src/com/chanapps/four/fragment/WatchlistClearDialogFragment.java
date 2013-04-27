package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
//import android.support.v4.app.DialogFragment;
import android.widget.BaseAdapter;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanWatchlist;
import com.chanapps.four.component.ToastRunnable;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class WatchlistClearDialogFragment extends DialogFragment {
    public static final String TAG = WatchlistClearDialogFragment.class.getSimpleName();
    private BoardGroupFragment fragment;
    public WatchlistClearDialogFragment() {
        super();
    }
    public WatchlistClearDialogFragment(BoardGroupFragment fragment) {
        super();
        this.fragment = fragment;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return (new AlertDialog.Builder(getActivity()))
                .setMessage(R.string.dialog_clear_watchlist)
                .setPositiveButton(R.string.dialog_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Context ctx = getActivity();
                                ChanWatchlist.clearWatchlist(ctx);
                                (new ToastRunnable(getActivity(), getString(R.string.dialog_cleared_watchlist))).run();
                                if (fragment != null)
                                    fragment.invalidate();
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
