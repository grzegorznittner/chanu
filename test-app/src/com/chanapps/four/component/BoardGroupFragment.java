package com.chanapps.four.component;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.test.BoardListActivity;
import com.chanapps.four.test.BoardSelectorActivity;
import com.chanapps.four.test.R;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 11/20/12
* Time: 4:16 PM
* To change this template use File | Settings | File Templates.
*/
public class BoardGroupFragment extends Fragment implements AdapterView.OnItemClickListener {
    private ChanBoard.Type boardType;
    private ImageAdapter adapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boardType = getArguments() != null
                ? ChanBoard.Type.valueOf(getArguments().getString(ChanHelper.BOARD_TYPE)) : ChanBoard.Type.JAPANESE_CULTURE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        GridView g = (GridView) inflater.inflate(R.layout.board_selector_grid_view, container, false);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(g, display);
        cg.sizeGridToDisplay();
        adapter = new ImageAdapter(container.getContext(), boardType, cg.getColumnWidth());
        g.setAdapter(adapter);
        g.setOnItemClickListener(this);
        return g;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ChanBoard board = ChanBoard.getBoardsByType(boardType).get(position);
        String boardCode = board.link;

        Log.i(BoardSelectorActivity.TAG, "onItemClick boardType: " + boardType);

        Intent intent = new Intent(view.getContext(), BoardListActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        startActivity(intent);
    }
}
