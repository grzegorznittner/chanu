package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanBlocklist;
import com.chanapps.four.data.ChanPost;

import java.util.*;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class BlocklistViewDialogFragment extends DialogFragment {

    public static final String TAG = BlocklistViewDialogFragment.class.getSimpleName();

    ChanBlocklist.BlockType blockType;
    List<String> blocks;
    final Map<Integer, Boolean> checkedBlocks = new HashMap<Integer, Boolean>();

    public BlocklistViewDialogFragment(ChanBlocklist.BlockType blockType,
                                         List<String> blocks)
    {
        super();
        this.blockType = blockType;
        this.blocks = blocks;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<String> formattedBlocks;
        boolean useFriendlyIds = PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        switch (blockType) {
            case TRIPCODE:
                List<String> formattedTrips = new ArrayList<String>(blocks.size());
                for (String trip : blocks) {
                    String formattedUserTrip = ChanPost.formattedUserTrip(trip, useFriendlyIds);
                    formattedTrips.add(formattedUserTrip);
                }
                formattedBlocks = formattedTrips;
                break;
            case ID:
                List<String> formattedUserIds = new ArrayList<String>(blocks.size());
                for (String userId : blocks) {
                    String formattedUserId = ChanPost.formattedUserId(userId, useFriendlyIds);
                    formattedUserIds.add(formattedUserId);
                }
                formattedBlocks = formattedUserIds;
                break;
            default:
                formattedBlocks = blocks;
        }
        String[] formattedBlocksArray = new String[formattedBlocks.size()];
        for (int i = 0; i < formattedBlocks.size(); i++)
            formattedBlocksArray[i] = formattedBlocks.get(i);

        Log.e(TAG, "formattedBlocks = " + Arrays.toString(formattedBlocksArray));
        if (formattedBlocksArray.length > 0) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.blocklist_title)
                    .setMultiChoiceItems(formattedBlocksArray, null, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            checkedBlocks.put(which, isChecked);
                        }
                    })
                    .setPositiveButton(R.string.dialog_remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            List<String> removeBlocks = new ArrayList<String>();
                            for (int i : checkedBlocks.keySet())
                                if (checkedBlocks.get(i))
                                    removeBlocks.add(blocks.get(i));
                            ChanBlocklist.removeAll(getActivity(), blockType, removeBlocks);
                        }
                    })
                    .setNegativeButton(R.string.dialog_close,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    .create();
        }
        else {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
            TextView title = (TextView)layout.findViewById(R.id.title);
            TextView message = (TextView)layout.findViewById(R.id.message);
            title.setText(R.string.dialog_blocklist);
            message.setText(R.string.blocklist_empty);
            setStyle(STYLE_NO_TITLE, 0);
            return new AlertDialog.Builder(getActivity())
                    .setView(layout)
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
