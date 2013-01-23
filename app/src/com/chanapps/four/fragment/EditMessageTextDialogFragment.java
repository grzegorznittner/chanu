package com.chanapps.four.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import com.chanapps.four.activity.PostReplyActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.task.PostReplyTask;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class EditMessageTextDialogFragment extends DialogFragment {

    public static final String TAG = EditMessageTextDialogFragment.class.getSimpleName();

    private PostReplyActivity activity = null;
    private EditText editMessageText = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.edit_message_text_dialog_fragment, null);
        builder.setView(view)
            .setNeutralButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditMessageTextDialogFragment.this.dismiss();
                }
            });
        editMessageText = (EditText)view.findViewById(R.id.edit_message_text);
        editMessageText.setText(activity.getMessage());
        editMessageText.requestFocus();
        return builder.create();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (PostReplyActivity)activity;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        activity.setMessage(editMessageText.getText().toString());
    }

}
