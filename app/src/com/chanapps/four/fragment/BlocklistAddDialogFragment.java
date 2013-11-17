package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.chanapps.four.activity.ChanIdentifiedActivity;
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
public class BlocklistAddDialogFragment extends DialogFragment {

    public static final String TAG = BlocklistAddDialogFragment.class.getSimpleName();

    ChanBlocklist.BlockType blockType;
    List<String> blocks;

    public BlocklistAddDialogFragment() {
    }

    public BlocklistAddDialogFragment(ChanBlocklist.BlockType blockType,
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
        Set<String> uniqueBlocks = new HashSet<String>(formattedBlocks);
        List<String> sortedBlocks = new ArrayList<String>(uniqueBlocks);
        Collections.sort(sortedBlocks);
        String formattedBlocklist = Arrays
                .toString(sortedBlocks.toArray())
                .replaceAll("^\\[", "")
                .replaceAll("\\]$", "");
        String plural = sortedBlocks.size() > 1 ? "s" : "";
        String msg = String.format(getString(R.string.blocklist_add_id_confirm),
                blockType.toString().toLowerCase(),
                plural,
                formattedBlocklist);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
        TextView title = (TextView)layout.findViewById(R.id.title);
        TextView message = (TextView)layout.findViewById(R.id.message);
        title.setText(R.string.dialog_blocklist);
        message.setText(msg);
        setStyle(STYLE_NO_TITLE, 0);

        return (new AlertDialog.Builder(getActivity()))
                .setView(layout)
                .setPositiveButton(R.string.dialog_add,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ChanBlocklist.addAll(getActivity(), blockType, blocks);
                                ((ChanIdentifiedActivity)getActivity()).refresh();
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
