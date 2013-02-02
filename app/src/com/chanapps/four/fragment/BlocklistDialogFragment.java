package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.util.Log;
import android.widget.Toast;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBlocklist;
import com.chanapps.four.data.ChanBoard;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class BlocklistDialogFragment extends DialogFragment {

    public static final String TAG = BlocklistDialogFragment.class.getSimpleName();

    private Context context;

    private String[] blocklistArray = null;

    public BlocklistDialogFragment(Context context) {
        this.context = context;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        blocklistArray = ChanBlocklist.getSorted(context);
        Log.e(TAG, "blocklist = " + Arrays.toString(blocklistArray));
        if (blocklistArray.length > 0) {
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.blocklist_title_remove)
                    .setItems(blocklistArray, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String id = blocklistArray[which];
                            ChanBlocklist.remove(context, id);
                            String msg = String.format(getString(R.string.blocklist_removed_id), id);
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    .create();
        }
        else {
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.blocklist_title)
                    .setMessage(R.string.blocklist_message_empty)
                    .setNeutralButton(R.string.dismiss,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    .create();
        }
    }

}
