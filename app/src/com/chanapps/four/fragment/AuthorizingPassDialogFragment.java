package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import com.chanapps.four.activity.R;
import com.chanapps.four.task.AuthorizePassTask;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class AuthorizingPassDialogFragment extends DialogFragment {
    public static final String TAG = AuthorizingPassDialogFragment.class.getSimpleName();
    AuthorizePassTask task;
    public AuthorizingPassDialogFragment(AuthorizePassTask task) {
        super();
        this.task = task;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return (new AlertDialog.Builder(getActivity()))
                .setMessage(R.string.dialog_authorizing_pass)
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
