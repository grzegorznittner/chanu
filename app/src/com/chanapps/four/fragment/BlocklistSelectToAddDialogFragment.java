package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.RefreshableActivity;
import com.chanapps.four.data.ChanBlocklist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class BlocklistSelectToAddDialogFragment extends DialogFragment {

    public static final String TAG = BlocklistSelectToAddDialogFragment.class.getSimpleName();

    private RefreshableActivity activity;
    private Map<ChanBlocklist.BlockType, List<String>> blocklist;
    private String[] blockTypes;

    public BlocklistSelectToAddDialogFragment(RefreshableActivity activity,
                                              Map<ChanBlocklist.BlockType, List<String>> blocklist)
    {
        this.activity = activity;
        this.blocklist = blocklist;
        List<String> blockTypes = new ArrayList<String>();
        for (ChanBlocklist.BlockType blockType : ChanBlocklist.BlockType.values())
            if (blocklist.containsKey(blockType) && blocklist.get(blockType).size() > 0)
                blockTypes.add(blockType.toString());
        this.blockTypes = new String[blockTypes.size()];
        for (int i = 0; i < blockTypes.size(); i++)
            this.blockTypes[i] = blockTypes.get(i);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.e(TAG, "blocktypes = " + Arrays.toString(blockTypes));
        if (blockTypes.length > 0) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.blocklist_select_type)
                    .setItems(blockTypes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ChanBlocklist.BlockType blockType = ChanBlocklist.BlockType.valueOf(blockTypes[which]);
                            List<String> blocks = blocklist.get(blockType);
                            (new BlocklistAddDialogFragment(activity, blockType, blocks))
                                    .show(activity.getSupportFragmentManager(), TAG);
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
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.blocklist_title)
                    .setMessage(R.string.blocklist_no_types_found)
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
