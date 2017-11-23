package com.chanapps.four.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.chanapps.four.activity.PostReplyShareActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBoard;

import java.util.List;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class PickShareBoardDialogFragment extends ListDialogFragment {

    public static final String TAG = PickShareBoardDialogFragment.class.getSimpleName();

    private static final boolean DEBUG = false;

    private String[] boards;
    private Handler activityHandler;

    private void initBoards(Context context) {
        List<ChanBoard> chanBoards = ChanBoard.getNewThreadBoardsRespectingNSFW(context);
        boards = new String[chanBoards.size()];
        int i = 0;
        for (ChanBoard chanBoard : chanBoards) {
            String boardCode = chanBoard.link;
            String boardName = chanBoard.getName(context);
            String boardLine = "/" + boardCode + " " + boardName;
            boards[i] = boardLine;
            i++;
        }
    }

    public PickShareBoardDialogFragment() {
        super();
        activityHandler = null;
    }

    public PickShareBoardDialogFragment(Handler handler) {
        super();
        activityHandler = handler;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        initBoards(getActivity());
        return createListDialog(R.string.new_thread_menu, R.string.new_thread_menu,
                R.string.post_reply_share_error,
                boards, new ListView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String boardLine = boards[position];
                        String boardCode = boardLine.substring(1, boardLine.indexOf(' '));
                        if (DEBUG) Log.i(TAG, "Picked board=" + boardCode);
                        Bundle b = new Bundle();
                        b.putString(ChanBoard.BOARD_CODE, boardCode);
                        Message msg = Message.obtain(activityHandler, PostReplyShareActivity.PICK_BOARD);
                        msg.setData(b);
                        msg.sendToTarget();
                        dismiss();
                    }
                }, new Dialog.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        Message.obtain(activityHandler, PostReplyShareActivity.POST_CANCELLED).sendToTarget();

                    }

                });
    }

}
