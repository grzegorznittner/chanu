package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.*;
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
public class BlocklistViewDialogFragment
        extends ListDialogFragment
        implements DialogInterface.OnClickListener,
        ListView.OnItemClickListener
{

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

        String title = String.format(getString(R.string.blocklist_title_with_type), blockType.displayString());
        Dialog d = createListDialog(title, title, getString(R.string.blocklist_empty), formattedBlocksArray,
                this,
                null,
                getString(R.string.dialog_remove),
                this);
        adapter = checkableArrayAdapter();
        items.setAdapter(adapter);
        items.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        return d;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            List<String> removeBlocks = new ArrayList<String>();
            for (int i : checkedBlocks.keySet())
                if (checkedBlocks.get(i))
                    removeBlocks.add(blocks.get(i));
            ChanBlocklist.removeAll(getActivity(), blockType, removeBlocks);
        }
        else if (which >= 0) {
            if (checkedBlocks.containsKey(which) && checkedBlocks.get(which))
                setChecked(which, false);
            else
                setChecked(which, true);
        }

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (checkedBlocks.containsKey(position) && checkedBlocks.get(position))
            setChecked(position, false);
        else
            setChecked(position, true);
    }

    /*
    @Override
    public void onResume() {
        super.onResume();
        for (int i : checkedBlocks.keySet())
            setChecked(i, checkedBlocks.get(i));
    }
    */
    protected void setChecked(int i, boolean checked) {
        checkedBlocks.put(i, checked);
        items.setItemChecked(i, checked);
    }

    ArrayAdapter<String> checkableArrayAdapter() {
        return new ArrayAdapter(getActivity().getApplicationContext(),
                R.layout.items_dialog_checkable_item, array);
    }

}
