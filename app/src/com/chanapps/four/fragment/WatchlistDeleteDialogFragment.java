package com.chanapps.four.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanThread;
import java.io.IOException;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class WatchlistDeleteDialogFragment extends DialogFragment {

    public static final String TAG = WatchlistDeleteDialogFragment.class.getSimpleName();

    private Handler handler;
    private ChanThread thread;

    public WatchlistDeleteDialogFragment(Handler handler, ChanThread thread) {
        super();
        this.handler = handler;
        this.thread = thread;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return (new AlertDialog.Builder(getActivity()))
                .setMessage(R.string.dialog_delete_watchlist_thread)
                .setPositiveButton(R.string.dialog_remove,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    ChanFileStorage.deleteWatchedThread(getActivity().getApplicationContext(), thread);
                                    if (handler != null)
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Activity a = getActivity();
                                                if (a instanceof ChanIdentifiedActivity)
                                                    ((ChanIdentifiedActivity)a).refresh();
                                            }
                                        });
                                }
                                catch (IOException e) {
                                    Log.e(TAG, "Exception deleting watchlist thread=" + thread, e);
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            R.string.thread_watchlist_not_deleted_thread, Toast.LENGTH_SHORT).show();
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
