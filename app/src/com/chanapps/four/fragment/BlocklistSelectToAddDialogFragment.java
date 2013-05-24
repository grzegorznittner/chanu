package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.RefreshableActivity;
import com.chanapps.four.adapter.ThreadListCursorAdapter;
import com.chanapps.four.data.ChanBlocklist;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.viewer.ThreadListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

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
    private String[] displayBlockTypes;

    public BlocklistSelectToAddDialogFragment(RefreshableActivity activity,
                                              Map<ChanBlocklist.BlockType, List<String>> blocklist)
    {
        this.activity = activity;
        this.blocklist = blocklist;
        List<ChanBlocklist.BlockType> blockTypes = new ArrayList<ChanBlocklist.BlockType>();
        for (ChanBlocklist.BlockType blockType : ChanBlocklist.BlockType.values())
            if (blocklist.containsKey(blockType) && blocklist.get(blockType).size() > 0)
                blockTypes.add(blockType);
        this.displayBlockTypes = new String[blockTypes.size()];
        for (int i = 0; i < blockTypes.size(); i++)
            this.displayBlockTypes[i] = blockTypes.get(i).displayString();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.e(TAG, "blocktypes = " + Arrays.toString(displayBlockTypes));
        if (displayBlockTypes.length > 0)
            return listDialog();
        else
            return emptyListDialog();
    }

    private Dialog listDialog() {
        setStyle(STYLE_NO_TITLE, 0);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.items_dialog_fragment, null);
        TextView title = (TextView)layout.findViewById(R.id.title);
        title.setText(R.string.blocklist_select_type);
        ListView items = (ListView)layout.findViewById(R.id.items);
        ArrayAdapter<String> adapter = new ArrayAdapter(getActivity().getApplicationContext(),
                R.layout.items_dialog_item, displayBlockTypes);
        items.setAdapter(adapter);
        items.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String s = ((TextView)view).getText().toString();
                ChanBlocklist.BlockType blockType = null;
                for (ChanBlocklist.BlockType b : ChanBlocklist.BlockType.values())
                    if (b.displayString().equals(s)) {
                        blockType = b;
                        break;
                    }
                if (blockType != null) {
                    List<String> blocks = blocklist.get(blockType);
                    (new BlocklistAddDialogFragment(activity, blockType, blocks))
                            .show(activity.getSupportFragmentManager(),TAG
                            );
                    dismiss();
                }
            }
        });
        return new AlertDialog.Builder(getActivity())
                .setView(layout)
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .create();
    }

    private Dialog emptyListDialog() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
        TextView title = (TextView)layout.findViewById(R.id.title);
        TextView message = (TextView)layout.findViewById(R.id.message);
        title.setText(R.string.dialog_blocklist);
        message.setText(R.string.blocklist_no_types_found);
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
