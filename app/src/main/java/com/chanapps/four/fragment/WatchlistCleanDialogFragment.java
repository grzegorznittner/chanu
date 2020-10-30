package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanFileStorage;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 12/14/12
 * Time: 12:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class WatchlistCleanDialogFragment extends DialogFragment {

    public static final String TAG = WatchlistCleanDialogFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    public WatchlistCleanDialogFragment() {
        super();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.WATCHLIST_DELETE);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
        TextView title = layout.findViewById(R.id.title);
        TextView message = layout.findViewById(R.id.message);
        title.setText(R.string.board_watch);
        message.setText(R.string.dialog_clean_watchlist);
        setStyle(STYLE_NO_TITLE, 0);
        return (new AlertDialog.Builder(getActivity())).setView(layout).setPositiveButton(R.string.dialog_clean, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    if (DEBUG) Log.i(TAG, "Cleaning watchlist...");
                    Context context = getActivity().getApplicationContext();
                    ChanFileStorage.cleanDeadWatchedThreads(context);
                    BoardActivity.refreshWatchlist(context);
                    //Toast.makeText(getActivity().getApplicationContext(),
                    //        R.string.thread_watchlist_cleared, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't clear watchlist", e);
                    Toast.makeText(getActivity().getApplicationContext(), R.string.thread_watchlist_not_cleared, Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }
        }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        }).create();
    }
}
