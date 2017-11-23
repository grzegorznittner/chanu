package com.chanapps.four.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.chanapps.four.activity.*;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.service.NetworkProfileManager;

import java.util.List;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class PickFavoritesBoardDialogFragment extends ListDialogFragment {

    public static final String TAG = PickFavoritesBoardDialogFragment.class.getSimpleName();

    private static final boolean DEBUG = false;

    private String[] boards;

    private void initBoards(Context context) {
        List<ChanBoard> chanBoards = ChanBoard.getPickFavoritesBoardsRespectingNSFW(context);
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

    public PickFavoritesBoardDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        initBoards(getActivity());
        return createListDialog(R.string.favorites_pick_board, R.string.favorites_pick_board,
                R.string.post_reply_new_thread_error,
                boards, new ListView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String boardLine = boards[position];
                        String boardCode = boardLine.substring(1, boardLine.indexOf(' '));
                        if (DEBUG) Log.i(TAG, "Picked board=" + boardCode);
                        ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                        dismiss();
                        BoardActivity.addToFavorites(activity.getBaseContext(), activity.getChanHandler(), boardCode);
                    }
                }, new Dialog.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                    }
                });
    }

}
