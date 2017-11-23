package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.activity.BoardActivity;
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

    public WatchlistDeleteDialogFragment(){}

    public WatchlistDeleteDialogFragment(Handler handler, ChanThread thread) {
        super();
        this.handler = handler;
        this.thread = thread;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
        TextView title = (TextView)layout.findViewById(R.id.title);
        TextView message = (TextView)layout.findViewById(R.id.message);
        title.setText(R.string.board_watch);
        message.setText(R.string.dialog_delete_watchlist_thread);
        setStyle(STYLE_NO_TITLE, 0);
        return (new AlertDialog.Builder(getActivity()))
                .setView(layout)
                .setPositiveButton(R.string.dialog_remove,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Context context = getActivity().getApplicationContext();
                                    ChanFileStorage.deleteWatchedThread(context, thread);
                                    BoardActivity.refreshWatchlist(context);
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
