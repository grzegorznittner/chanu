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

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class PasswordInfoDialogFragment extends DialogFragment {
    public static final String TAG = PasswordInfoDialogFragment.class.getSimpleName();
    public PasswordInfoDialogFragment() {
        super();
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
        TextView title = (TextView)layout.findViewById(R.id.title);
        TextView message = (TextView)layout.findViewById(R.id.message);
        title.setText(R.string.pref_user_password);
        message.setText(R.string.post_reply_password_info_detail);
        setStyle(STYLE_NO_TITLE, 0);
        Dialog dialog = (new AlertDialog.Builder(getActivity()))
                .setView(layout)
                .setNeutralButton(R.string.thread_context_select,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }
}
