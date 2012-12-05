package com.chanapps.four.component;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.activity.R;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/20/12
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardSelectorAdapter extends BaseAdapter {
    Context ctx;
    LayoutInflater inflater;
    ChanBoard.Type selectedBoardType;
    int columnWidth;

    public BoardSelectorAdapter(Context ctx, ChanBoard.Type selectedBoardType, int columnWidth) {
        Log.d(BoardSelectorActivity.TAG, "BoardSelectorAdapter " + selectedBoardType + " initializing");
        this.ctx = ctx;
        this.inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.selectedBoardType = selectedBoardType;
        this.columnWidth = columnWidth;
    }

    public int getCount() {
        int size = ChanBoard.getBoardsByType(ctx, selectedBoardType).size();
        Log.d(BoardSelectorActivity.TAG, "BoardSelectorAdapter " + selectedBoardType + " returned size " + size);
        return size;
    }

    public Object getItem(int position) {
        ChanBoard board = ChanBoard.getBoardsByType(ctx, selectedBoardType).get(position);
        Log.d(BoardSelectorActivity.TAG, "BoardSelectorAdapter " + selectedBoardType + " getting item: " + position);
        //return position;
        String dataItem = position + board.link;
        Log.d(BoardSelectorActivity.TAG, "BoardSelectorAdapter " + selectedBoardType + " got dataItem: " + dataItem);
        return dataItem;
    }

    public long getItemId(int position) {
        ChanBoard board = ChanBoard.getBoardsByType(ctx, selectedBoardType).get(position);
        Log.d(BoardSelectorActivity.TAG, "BoardSelectorAdapter " + selectedBoardType + " getting item id: " + position);
        long itemId = position + 100 * board.iconId;
        Log.d(BoardSelectorActivity.TAG, "BoardSelectorAdapter " + selectedBoardType + " got itemId: " + itemId);
        return itemId;
        // return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(BoardSelectorActivity.TAG, "BoardSelectorAdapter " + selectedBoardType + " getting view at: " + position);
        View itemLayout = null;
        if (convertView == null) {
            Log.d(BoardSelectorActivity.TAG, "Creating new item view for " + position);
            itemLayout = inflater.inflate(R.layout.selector_grid_item, parent, false);
            itemLayout.setTag(selectedBoardType.toString());
        } else {
            Log.d(BoardSelectorActivity.TAG, "Using existing view for " + position);
            itemLayout = convertView;
        }

        itemLayout.setLayoutParams(new AbsListView.LayoutParams(columnWidth, columnWidth));

        ChanBoard board = ChanBoard.getBoardsByType(ctx, selectedBoardType).get(position);
        Log.d(BoardSelectorActivity.TAG, "BoardSelectorAdapter " + selectedBoardType + " board: " + board.link + " processing");

        ImageView imageView = (ImageView) itemLayout.findViewById(R.id.grid_item_image);
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(columnWidth, columnWidth));

        int imageId = 0;
        try {
            imageId = R.drawable.class.getField(board.link).getInt(null);
            Log.d(BoardSelectorActivity.TAG, "BoardSelectorAdapter " + selectedBoardType + " board " + board.link + " setting image: " + imageId);
        } catch (Exception e) {
            try {
                imageId = R.drawable.class.getField("board_" + board.link).getInt(null);
            } catch (Exception e1) {
                imageId = R.drawable.stub_image;
            }
        }
        imageView.setImageResource(imageId);

        TextView textView = (TextView) itemLayout.findViewById(R.id.grid_item_text);
        textView.setText(board.name);
        Log.d(BoardSelectorActivity.TAG, "BoardSelectorAdapter " + selectedBoardType + " board " + board.link + " setting text: " + board.name);

        return itemLayout;
    }
}
