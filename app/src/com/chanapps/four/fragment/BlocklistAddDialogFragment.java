package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.RefreshableActivity;
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
public class BlocklistAddDialogFragment extends DialogFragment {

    public static final String TAG = BlocklistAddDialogFragment.class.getSimpleName();

    RefreshableActivity activity;
    ChanBlocklist.BlockType blockType;
    List<String> blocks;

    public BlocklistAddDialogFragment(RefreshableActivity activity,
                                      ChanBlocklist.BlockType blockType,
                                      List<String> blocks)
    {
        super();
        this.activity = activity;
        this.blockType = blockType;
        this.blocks = blocks;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        List<String> formattedBlocks;
        boolean useFriendlyIds = PreferenceManager
                .getDefaultSharedPreferences(activity.getBaseContext())
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
        Set<String> uniqueBlocks = new HashSet<String>(formattedBlocks);
        List<String> sortedBlocks = new ArrayList<String>(uniqueBlocks);
        Collections.sort(sortedBlocks);
        String formattedBlocklist = Arrays
                .toString(sortedBlocks.toArray())
                .replaceAll("^\\[", "")
                .replaceAll("\\]$", "");
        String plural = sortedBlocks.size() > 1 ? "s" : "";
        String msg = String.format(activity.getBaseContext().getString(R.string.blocklist_add_id_confirm),
                blockType.toString().toLowerCase(),
                plural,
                formattedBlocklist);

        return (new AlertDialog.Builder(getActivity()))
                .setMessage(msg)
                .setPositiveButton(R.string.dialog_add,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ChanBlocklist.addAll(activity.getBaseContext(), blockType, blocks);
                                activity.refresh();
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
