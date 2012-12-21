package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.BaseAdapter;
import android.widget.GridView;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.adapter.BoardSelectorAdapter;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.component.ToastRunnable;

import java.util.List;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class FavoritesAddDialogFragment extends DialogFragment {
    public static final String TAG = FavoritesAddDialogFragment.class.getSimpleName();
    private BoardGroupFragment fragment;
    private String boardCode;
    public FavoritesAddDialogFragment(BoardGroupFragment fragment, String boardCode) {
        super();
        this.fragment = fragment;
        this.boardCode = boardCode;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return (new AlertDialog.Builder(getActivity()))
                .setMessage(R.string.dialog_add_favorites_board)
                .setPositiveButton(R.string.dialog_add,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Context ctx = getActivity();
                                List<ChanBoard> favorites = ChanBoard.getBoardsByType(ctx, ChanBoard.Type.FAVORITES);
                                if (favorites.isEmpty()) {
                                    ChanBoard.addBoardToFavorites(ctx, boardCode);
                                    BoardSelectorActivity activity = (BoardSelectorActivity) fragment.getActivity();
                                    BoardGroupFragment favoritesFragment = activity.getFavoritesFragment();
                                    if (favoritesFragment != null) {
                                        BaseAdapter adapter = favoritesFragment.getAdapter();
                                        if (adapter != null) {
                                            adapter.notifyDataSetChanged();
                                        }
                                        GridView view = (GridView)favoritesFragment.getView();
                                        if (view != null) {
                                            view.setAdapter(null);
                                            adapter = new BoardSelectorAdapter(ctx, ChanBoard.Type.FAVORITES, favoritesFragment.columnWidth);
                                            view.setAdapter(adapter);
                                            adapter.notifyDataSetInvalidated();
                                        }
                                    }
                                }
                                else {
                                    ChanBoard.addBoardToFavorites(ctx, boardCode);
                                    BoardSelectorActivity activity = (BoardSelectorActivity) fragment.getActivity();
                                    BoardGroupFragment favoritesFragment = activity.getFavoritesFragment();
                                    if (favoritesFragment != null) {
                                        BaseAdapter adapter = favoritesFragment.getAdapter();
                                        if (adapter != null) {
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                }
                                (new ToastRunnable(getActivity(),getString(R.string.dialog_added_to_favorites))).run();
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // ignore
                            }
                        })
                .create();
    }
}
