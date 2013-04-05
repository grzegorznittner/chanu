package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.RefreshableActivity;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.Dismissable;
import com.chanapps.four.task.DeletePostTask;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class DeletePostDialogFragment extends DialogFragment {

    public static final String TAG = DeletePostDialogFragment.class.getSimpleName();

    private ActionMode mode = null;
    private RefreshableActivity refreshableActivity = null;
    private String boardCode = null;
    private long threadNo = 0;
    private long[] postNos = {};
    private String password = null;
    private EditText passwordText = null;
    private CheckBox imageOnlyCheckbox = null;

    public DeletePostDialogFragment(ActionMode mode, RefreshableActivity refreshableActivity,
                                    String boardCode, long threadNo, long[] postNos) {
        super();
        this.mode = mode;
        this.refreshableActivity = refreshableActivity;
        this.boardCode = boardCode;
        this.threadNo = threadNo;
        this.postNos = postNos;
        this.password = PreferenceManager
                .getDefaultSharedPreferences(refreshableActivity.getBaseContext())
                .getString(SettingsActivity.PREF_USER_PASSWORD, "");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.delete_post_dialog_fragment, null);
        builder
            .setView(view)
            .setTitle(R.string.delete_post_title)
            .setPositiveButton(R.string.dialog_delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    password = passwordText.getText().toString();
                    if ("".equals(password)) {
                        Toast.makeText(getActivity(), R.string.delete_post_enter_password, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    closeKeyboard();
                    boolean onlyImages = imageOnlyCheckbox.isChecked();
                    DeletePostTask deletePostTask = new DeletePostTask(
                            refreshableActivity, boardCode, threadNo, postNos, password, onlyImages);
                    DeletingPostDialogFragment dialogFragment = new DeletingPostDialogFragment(deletePostTask, onlyImages);
                    dialogFragment.show(getActivity().getSupportFragmentManager(), DeletingPostDialogFragment.TAG);
                    if (!deletePostTask.isCancelled())
                        deletePostTask.execute(dialogFragment);
                    mode.finish();
                }
            })
            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
        passwordText = (EditText)view.findViewById(R.id.delete_post_password);
        passwordText.setText(password);
        imageOnlyCheckbox = (CheckBox)view.findViewById(R.id.delete_post_only_image_checkbox);
        imageOnlyCheckbox.setChecked(false);
        return builder.create();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        //
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        //
    }

    private void closeKeyboard() {
        IBinder windowToken = getActivity().getCurrentFocus() != null ?
                getActivity().getCurrentFocus().getWindowToken()
                : null;
        if (windowToken != null) { // close the keyboard
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }

}
