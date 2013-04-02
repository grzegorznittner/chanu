package com.chanapps.four.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.Toast;
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
public class WidgetPickBoardDialogFragment extends DialogFragment {

    public static final String TAG = WidgetPickBoardDialogFragment.class.getSimpleName();

    private static final boolean DEBUG = false;

    private int appWidgetId = 0;
    private String[] boards = null;

    private void initBoards(Context context) {
        List<ChanBoard> chanBoards = ChanBoard.getBoardsRespectingNSFW(context);
        boards = new String[chanBoards.size()];
        int i = 0;
        for (ChanBoard chanBoard : chanBoards) {
            String boardCode = chanBoard.link;
            String boardName = chanBoard.name;
            String boardLine = "/" + boardCode + " " + boardName;
            boards[i] = boardLine;
            i++;
        }
    }

    public WidgetPickBoardDialogFragment(int appWidgetId) {
        this.appWidgetId = appWidgetId;
        if (DEBUG) Log.i(TAG, "Init fragment for widget=" + appWidgetId);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        initBoards(getActivity());
        return new AlertDialog.Builder(getActivity())
        .setTitle(R.string.widget_board)
        .setItems(boards, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String boardLine = boards[which];
                String boardCode = boardLine.substring(1, boardLine.indexOf(' '));
                if (DEBUG) Log.i(TAG, "Configured widget=" + appWidgetId + " configuring for board=" + boardCode);
                boolean addedWidget = BoardWidgetProvider.initWidget(getActivity(), appWidgetId, boardCode);
                if (addedWidget) {
                    Intent intent = new Intent();
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    getActivity().setResult(Activity.RESULT_OK, intent);
                }
                else {
                    Toast.makeText(getActivity(), R.string.widget_board, Toast.LENGTH_SHORT).show();
                }
                getActivity().finish();
            }
        })
        .setNegativeButton(R.string.dialog_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (DEBUG) Log.i(TAG, "Configuration cancelled for widget=" + appWidgetId);
                        getActivity().finish();
                    }
                })
        .create();
    }

}
