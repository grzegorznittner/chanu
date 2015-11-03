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
import com.chanapps.four.component.ThemeSelector;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public abstract class ListDialogFragment extends DialogFragment {

    protected String[] array = {};
    protected ArrayAdapter<String> adapter = null;
    protected ListView items = null;
    private DialogInterface.OnCancelListener cancelListener = null;

    public ListDialogFragment(){}

    public Dialog createListDialog(int titleStringId, int emptyTitleStringId, int emptyStringId, String[] array,
                                   ListView.OnItemClickListener listener) {
        return createListDialog(titleStringId, emptyTitleStringId, emptyStringId, array, listener, null);
    }

    public Dialog createListDialog(int titleStringId, int emptyTitleStringId, int emptyStringId, String[] array,
                                   ListView.OnItemClickListener listener, final DialogInterface.OnCancelListener cancelListener) {
        return createListDialog(getString(titleStringId), getString(emptyTitleStringId), getString(emptyStringId),
                array, listener, cancelListener, null, null);
    }

    public Dialog createListDialog(String title, String emptyTitle, String empty, String[] array,
                             ListView.OnItemClickListener listener,
                             final DialogInterface.OnCancelListener cancelListener,
                             String positiveLabel,
                             final DialogInterface.OnClickListener positiveListener) {
        this.array = array;
        this.cancelListener = cancelListener;
        if (array.length > 0) {
            setStyle(STYLE_NO_TITLE, 0);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View layout = inflater.inflate(R.layout.items_dialog_fragment, null);
            TextView titleView = (TextView)layout.findViewById(R.id.title);
            titleView.setText(title);
            items = (ListView)layout.findViewById(R.id.items);
            int itemLayoutId = ThemeSelector.instance(getActivity()).isDark()
                    ? R.layout.items_dialog_item_dark
                    : R.layout.items_dialog_item;
            adapter = new ArrayAdapter(getActivity().getApplicationContext(),
                    itemLayoutId, array);
            items.setAdapter(adapter);
            if (listener != null)
                items.setOnItemClickListener(listener);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(layout);
            if (positiveListener != null) {
                builder.setPositiveButton(positiveLabel, positiveListener);
            }
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
            TextView titleView = (TextView)layout.findViewById(R.id.title);
            TextView message = (TextView)layout.findViewById(R.id.message);
            titleView.setText(emptyTitle);
            message.setText(empty);
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
