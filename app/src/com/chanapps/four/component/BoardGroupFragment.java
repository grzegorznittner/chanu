package com.chanapps.four.component;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.activity.R;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 11/20/12
* Time: 4:16 PM
* To change this template use File | Settings | File Templates.
*/
public class BoardGroupFragment extends Fragment implements AdapterView.OnItemClickListener {
    private ChanBoard.Type boardType;
    private BoardSelectorAdapter adapter;
    private GridView gridView;
    private Context context;
    private int columnWidth = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boardType = getArguments() != null
                ? ChanBoard.Type.valueOf(getArguments().getString(ChanHelper.BOARD_TYPE)) : ChanBoard.Type.JAPANESE_CULTURE;
        Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + " onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + " onCreateView");

        gridView = (GridView) inflater.inflate(R.layout.board_selector_grid, container, false);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(gridView, display);
        cg.sizeGridToDisplay();
        context = container.getContext();
        columnWidth = cg.getColumnWidth();
        adapter = new BoardSelectorAdapter(context, boardType, columnWidth);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
        return gridView;
    }
               /*
    public void clearFavorites() {
        Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + " clearFavorites called at fragment level");
        if (gridView != null) {
            gridView.invalidate();
            Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + " invalidated grid view");
        }
        else {
            Log.e(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + " null grid view, couldn't invalidate");
        }

        if (adapter != null) {
            adapter.notifyDataSetInvalidated();
            Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + " invalidated adapter data");
        }
        else {
            Log.e(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + " null adapter, couldn't invalidate");
        }

    }
                  */
    @Override
    public void onPause() {
        super.onPause();
        Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + "onPause");
    }
    @Override
    public void onStop() {
        super.onStop();
        Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + "onStop");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + "onResume");
        if (gridView != null) {
            gridView.invalidate();
            Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + " invalidated grid view");
        }
        else {

            Log.e(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + " null grid view, couldn't invalidate");
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof BoardSelectorActivity) {
            BoardSelectorActivity a = (BoardSelectorActivity)activity;
            if (boardType == null) {
                boardType = a.selectedBoardType;
            }
        }

        Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + (boardType == null ? "null" : boardType) + " onAttach");
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(BoardSelectorActivity.TAG, "BoardGroupFragment " + boardType + " onDetach");

        //gridView.invalidate();
        //gridView = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ChanBoard board = ChanBoard.getBoardsByType(getActivity(), boardType).get(position);
        String boardCode = board.link;
        ChanViewHelper.startBoardActivity(parent, view, position, id, getActivity(), boardCode);
    }
}
