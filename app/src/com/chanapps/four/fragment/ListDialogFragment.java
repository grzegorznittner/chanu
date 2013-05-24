package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.chanapps.four.activity.R;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public abstract class ListDialogFragment extends DialogFragment {

    private DialogInterface.OnCancelListener cancelListener = null;

    public Dialog createListDialog(int titleStringId, int emptyTitleStringId, int emptyStringId, String[] array,
                                   ListView.OnItemClickListener listener) {
        return createListDialog(titleStringId, emptyTitleStringId, emptyStringId, array, listener, null);
    }

    public Dialog createListDialog(int titleStringId, int emptyTitleStringId, int emptyStringId, String[] array,
                             ListView.OnItemClickListener listener, final DialogInterface.OnCancelListener cancelListener) {
        this.cancelListener = cancelListener;
        if (array.length > 0) {
            setStyle(STYLE_NO_TITLE, 0);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View layout = inflater.inflate(R.layout.items_dialog_fragment, null);
            TextView title = (TextView)layout.findViewById(R.id.title);
            title.setText(titleStringId);
            ListView items = (ListView)layout.findViewById(R.id.items);
            ArrayAdapter<String> adapter = new ArrayAdapter(getActivity().getApplicationContext(),
                    R.layout.items_dialog_item, array);
            items.setAdapter(adapter);
            items.setOnItemClickListener(listener);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(layout);
            if (cancelListener != null) {
                builder
                        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                cancelListener.onCancel(dialog);
                            }
                        });
            }
            else {
                builder.setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
            }
            Dialog d = builder.create();
            return d;
        }
        else {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
            TextView title = (TextView)layout.findViewById(R.id.title);
            TextView message = (TextView)layout.findViewById(R.id.message);
            title.setText(emptyTitleStringId);
            message.setText(emptyStringId);
            setStyle(STYLE_NO_TITLE, 0);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(layout);
            if (cancelListener != null) {
                builder
                        .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                cancelListener.onCancel(dialog);
                            }
                        });
            }
            else {
                builder.setNegativeButton(R.string.dismiss,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
            }
            Dialog d = builder.create();
            return d;
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        if (cancelListener != null)
            cancelListener.onCancel(dialogInterface);
    }

}
