package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.widget.Toast;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.UserStatistics;
import com.chanapps.four.service.NetworkProfileManager;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class WatchlistClearDialogFragment extends DialogFragment {

    public static final String TAG = WatchlistClearDialogFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    WeakReference<BoardGroupFragment> fragmentRef = null;

    public WatchlistClearDialogFragment() {
        super();
    }

    public WatchlistClearDialogFragment(BoardGroupFragment fragment) {
        super();
        fragmentRef = new WeakReference<BoardGroupFragment>(fragment);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.WATCHLIST_DELETE);
        return (new AlertDialog.Builder(getActivity()))
                .setMessage(R.string.dialog_clear_watchlist)
                .setPositiveButton(R.string.dialog_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    if (DEBUG) Log.i(TAG, "Clearing watchlist...");
                                    ChanFileStorage.clearWatchedThreads(getActivity().getApplicationContext());
                                    if (fragmentRef != null && fragmentRef.get() != null && fragmentRef.get().handler != null)
                                        fragmentRef.get().handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                fragmentRef.get().refresh();
                                            }
                                        });
                                    else
                                        BoardGroupFragment.scheduleWatchlistRefresh();
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            R.string.thread_watchlist_cleared, Toast.LENGTH_SHORT).show();
                                }
                                catch (IOException e) {
                                    Log.e(TAG, "Couldn't clear watchlist", e);
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            R.string.thread_watchlist_not_cleared, Toast.LENGTH_SHORT).show();
                                }
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
