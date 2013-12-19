package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.chanapps.four.activity.PostReplyActivity;
import com.chanapps.four.activity.R;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class PostingReplyDialogFragment extends DialogFragment {

    public static final String TAG = PostingReplyDialogFragment.class.getSimpleName();

    protected static final String THREAD_NO = "threadNo";
    protected PostReplyActivity.PostReplyTask task;
    protected long  threadNo;

    public PostingReplyDialogFragment() { // when on-create gets called
        super();
    }

    public PostingReplyDialogFragment(PostReplyActivity.PostReplyTask task, long threadNo) {
        super();
        this.task = task;
        this.threadNo = threadNo;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        if (bundle != null) { // recalled on existing task
           this.threadNo = bundle.getLong(THREAD_NO);
        }
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
        TextView title = (TextView)layout.findViewById(R.id.title);
        TextView message = (TextView)layout.findViewById(R.id.message);
        int titleId = threadNo <= 0 ? R.string.new_thread_menu : R.string.post_reply_title;
        title.setText(titleId);
        message.setText(R.string.dialog_posting_reply);
        setStyle(STYLE_NO_TITLE, 0);
        return (new AlertDialog.Builder(getActivity()))
                .setView(layout)
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (task != null)
                                    task.cancel(true);
                            }
                        })
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(THREAD_NO, threadNo);
    }

}
