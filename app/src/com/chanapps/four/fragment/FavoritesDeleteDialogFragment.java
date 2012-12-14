package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.BaseAdapter;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBoard;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class FavoritesDeleteDialogFragment extends DialogFragment {
    public static final String TAG = FavoritesDeleteDialogFragment.class.getSimpleName();
    private BoardGroupFragment fragment;
    private String boardCode;
    public FavoritesDeleteDialogFragment(BoardGroupFragment fragment, String boardCode) {
        super();
        this.fragment = fragment;
        this.boardCode = boardCode;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return (new AlertDialog.Builder(getActivity()))
                .setMessage(R.string.dialog_delete_favorites_thread)
                .setPositiveButton(R.string.dialog_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Context ctx = getActivity();
                                ChanBoard.deleteBoardFromFavorites(ctx, boardCode);
                                BaseAdapter adapter = fragment.getAdapter();
                                if (adapter != null)
                                    adapter.notifyDataSetChanged();
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
