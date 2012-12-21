package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ToastRunnable;
import com.chanapps.four.handler.LoaderHandler;
import com.chanapps.four.data.ChanWatchlist;

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
    private long tim = 0;
    public WatchlistDeleteDialogFragment(Handler handler, long tim) {
        super();
        this.handler = handler;
        this.tim = tim;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return (new AlertDialog.Builder(getActivity()))
                .setMessage(R.string.dialog_delete_watchlist_thread)
                .setPositiveButton(R.string.dialog_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Context ctx = getActivity();
                                ChanWatchlist.deleteThreadFromWatchlist(ctx, tim);
                                //Message m = Message.obtain(handler, LoaderHandler.RESTART_LOADER_MSG);
                                //handler.sendMessageDelayed(m, 200);
                                (new ToastRunnable(getActivity(), getString(R.string.dialog_deleted_from_watchlist))).run();
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
