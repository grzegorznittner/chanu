package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanBlocklist;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanPost;

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

    private boolean useFriendlyIds = true;
    private String[] blocklistArray = null;
    private String[] formattedBlocklistArray = null;

    public BlocklistDialogFragment(Context context) {
        this.context = context;
    }

    private void initBlocklistArrays() {
        useFriendlyIds = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        blocklistArray = ChanBlocklist.getSorted(context);
        formattedBlocklistArray = new String[blocklistArray.length];
        for (int i = 0; i < blocklistArray.length; i++) {
            formattedBlocklistArray[i] = ChanPost.getUserId(blocklistArray[i], useFriendlyIds);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        initBlocklistArrays();
        Log.e(TAG, "blocklist = " + Arrays.toString(formattedBlocklistArray));
        if (formattedBlocklistArray.length > 0) {
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.blocklist_title_remove)
                    .setItems(formattedBlocklistArray, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String formattedId = formattedBlocklistArray[which];
                            String id = blocklistArray[which];
                            ChanBlocklist.remove(context, id);
                            String msg = String.format(getString(R.string.blocklist_removed_id), formattedId);
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
