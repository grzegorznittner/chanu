package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.chanapps.four.activity.R;
import com.chanapps.four.task.ReportPostTask;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 12/14/12
 * Time: 12:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReportingPostDialogFragment extends DialogFragment {
    public static final String TAG = ReportingPostDialogFragment.class.getSimpleName();
    ReportPostTask task;

    public ReportingPostDialogFragment() {
    }

    public ReportingPostDialogFragment(ReportPostTask task) {
        super();
        this.task = task;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
        TextView title = layout.findViewById(R.id.title);
        TextView message = layout.findViewById(R.id.message);
        title.setText(R.string.dialog_report_post);
        message.setText(R.string.report_post_reporting);
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
