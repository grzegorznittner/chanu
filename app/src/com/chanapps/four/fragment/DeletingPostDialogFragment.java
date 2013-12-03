package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.task.DeletePostTask;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class DeletingPostDialogFragment extends DialogFragment {
    public static final String TAG = DeletingPostDialogFragment.class.getSimpleName();
    DeletePostTask task;
    boolean onlyImages;
    public DeletingPostDialogFragment(){}
    public DeletingPostDialogFragment(DeletePostTask task, boolean onlyImages) {
        super();
        this.task = task;
        this.onlyImages = onlyImages;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
        TextView title = (TextView)layout.findViewById(R.id.title);
        TextView message = (TextView)layout.findViewById(R.id.message);
        title.setText(R.string.delete_post_title);
        int messageId = onlyImages ? R.string.delete_post_deleting_image : R.string.delete_post_deleting;
        message.setText(messageId);
        setStyle(STYLE_NO_TITLE, 0);
        return (new AlertDialog.Builder(getActivity()))
                .setView(layout)
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                task.cancel(true);
                            }
                        })
                .create();
    }
}
