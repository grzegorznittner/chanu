package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.util.Log;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBlocklist;

import java.util.*;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class BlocklistSelectToViewDialogFragment extends DialogFragment {

    public static final String TAG = BlocklistViewDialogFragment.class.getSimpleName();

    private Map<ChanBlocklist.BlockType, Set<String>> blocklist;
    private String[] blockTypes;

    public BlocklistSelectToViewDialogFragment(SettingsFragment fragment)
    {
        this.blocklist = ChanBlocklist.getBlocklist(fragment.getActivity().getApplicationContext());
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
                    .setTitle(R.string.blocklist_select_view_type)
                    .setItems(blockTypes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ChanBlocklist.BlockType blockType = ChanBlocklist.BlockType.valueOf(blockTypes[which]);
                            List<String> blocks = ChanBlocklist.getSorted(getActivity(), blockType);
                            (new BlocklistViewDialogFragment(blockType, blocks)).show(getFragmentManager(), TAG);
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
                    .setMessage(R.string.blocklist_empty)
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
