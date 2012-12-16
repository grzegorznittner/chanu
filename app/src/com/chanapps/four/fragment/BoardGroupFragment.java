package com.chanapps.four.fragment;

import android.content.Intent;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.adapter.BoardSelectorAdapter;
import com.chanapps.four.adapter.ImageTextCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.loader.ChanWatchlistCursorLoader;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 11/20/12
* Time: 4:16 PM
* To change this template use File | Settings | File Templates.
*/
public class BoardGroupFragment
        extends Fragment
        implements
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        ImageTextCursorAdapter.ViewBinder
{

    private static final String TAG = BoardGroupFragment.class.getSimpleName();

    private ChanBoard.Type boardType;
    private ImageTextCursorAdapter imageTextCursorAdapter;
    private BaseAdapter adapter;
    private GridView gridView;
    private Context context;
    public int columnWidth = 0;
    protected Handler handler;
    protected ChanWatchlistCursorLoader cursorLoader;
    protected ChanViewHelper viewHelper;

    public BaseAdapter getAdapter() {
        return adapter;

    }

    public ChanViewHelper.ServiceType getServiceType() {
        return ChanViewHelper.ServiceType.WATCHLIST;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boardType = getArguments() != null
                ? ChanBoard.Type.valueOf(getArguments().getString(ChanHelper.BOARD_TYPE))
                : ChanBoard.Type.JAPANESE_CULTURE;
        if (boardType == ChanBoard.Type.WATCHLIST) {
            viewHelper = new ChanViewHelper(this.getActivity(), ChanViewHelper.ServiceType.WATCHLIST);
        }
        if (boardType == ChanBoard.Type.FAVORITES || boardType == ChanBoard.Type.WATCHLIST) {
            setHasOptionsMenu(true);
        }
        ensureHandler();
        LoaderManager.enableDebugLogging(true);
        Log.v(TAG, "onCreate init loader");
        getLoaderManager().initLoader(0, null, this);
        Log.v(TAG, "BoardGroupFragment " + boardType + " onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "BoardGroupFragment " + boardType + " onCreateView");

        gridView = (GridView) inflater.inflate(R.layout.board_selector_grid, container, false);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(gridView, display, ChanViewHelper.ServiceType.SELECTOR);
        cg.sizeGridToDisplay();
        context = container.getContext();
        columnWidth = cg.getColumnWidth();
        if (boardType == ChanBoard.Type.WATCHLIST) {
            imageTextCursorAdapter = new ImageTextCursorAdapter(
                    context,
                    R.layout.board_grid_item,
                    this,
                    new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_TEXT},
                    new int[] {R.id.board_activity_grid_item_image, R.id.board_activity_grid_item_text}
            );
            adapter = imageTextCursorAdapter;
        }
        else {
            adapter = new BoardSelectorAdapter(context, boardType, columnWidth);
        }
        gridView.setAdapter(adapter);
        gridView.setClickable(true);
        gridView.setLongClickable(true);
        gridView.setOnItemClickListener(this);
        gridView.setOnItemLongClickListener(this);
        return gridView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (boardType == ChanBoard.Type.WATCHLIST) {
            viewHelper.startService();
        }
    }

    @Override
    public void onStop () {
    	super.onStop();
        if (boardType == ChanBoard.Type.WATCHLIST) {
    	    getLoaderManager().destroyLoader(0);
        }
    	handler = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (boardType == ChanBoard.Type.WATCHLIST) {
            getLoaderManager().destroyLoader(0);
        }
        handler = null;
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        return viewHelper.setViewValue(view, cursor, columnIndex);
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        if (imageTextCursorAdapter != null) {
            cursorLoader = new ChanWatchlistCursorLoader(getActivity().getBaseContext());
        }
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.v(TAG, ">>>>>>>>>>> onLoadFinished");
        if (imageTextCursorAdapter != null) {
		    imageTextCursorAdapter.swapCursor(data);
        }
        ensureHandler();
		handler.sendEmptyMessageDelayed(0, 10000);
	}

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.v(TAG, ">>>>>>>>>>> onLoaderReset");
        if (imageTextCursorAdapter != null) {
		    imageTextCursorAdapter.swapCursor(null);
        }
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (boardType == ChanBoard.Type.WATCHLIST) {
            viewHelper.startThreadActivity(parent, view, position, id);
        }
        else {
            ChanBoard board = ChanBoard.getBoardsByType(getActivity(), boardType).get(position);
            String boardCode = board.link;
            ChanViewHelper.startBoardActivity(parent, view, position, id, getActivity(), boardCode);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (boardType == ChanBoard.Type.WATCHLIST) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            final long threadno = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
            final long tim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
            WatchlistDeleteDialogFragment d = new WatchlistDeleteDialogFragment(handler, tim);
            d.show(getFragmentManager(), d.TAG);
        }
        else if (boardType == ChanBoard.Type.FAVORITES) {
            ChanBoard board = ChanBoard.getBoardsByType(getActivity(), boardType).get(position);
            String boardCode = board.link;
            FavoritesDeleteDialogFragment d = new FavoritesDeleteDialogFragment(this, boardCode);
            d.show(getFragmentManager(), d.TAG);
        }
        else {
            ChanBoard board = ChanBoard.getBoardsByType(getActivity(), boardType).get(position);
            String boardCode = board.link;
            FavoritesAddDialogFragment d = new FavoritesAddDialogFragment(this, boardCode);
            d.show(getFragmentManager(), d.TAG);
        }
        return true;
    }

    protected void ensureHandler() {
        if (handler == null) {
            handler = new FragmentLoaderHandler(this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (boardType == ChanBoard.Type.FAVORITES) {
            inflater.inflate(R.menu.favorites_menu, menu);
            if (adapter != null) { // has to be here because adapter is null when we modify favorites from another tab
                adapter.notifyDataSetInvalidated();
            }
        }
        else if (boardType == ChanBoard.Type.WATCHLIST) {
            inflater.inflate(R.menu.watchlist_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_favorites_menu:
                FavoritesClearDialogFragment clearDialogFragment = new FavoritesClearDialogFragment(this);
                clearDialogFragment.show(getFragmentManager(), clearDialogFragment.TAG);
                return true;
            case R.id.clear_watchlist_menu:
                WatchlistClearDialogFragment clearWatchlistFragment = new WatchlistClearDialogFragment(this);
                clearWatchlistFragment.show(getFragmentManager(), clearWatchlistFragment.TAG);
                return true;
            case R.id.settings_menu:
                Intent settingsIntent = new Intent(this.getActivity(), SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this.getActivity(), R.raw.help_header, R.raw.help_board_selector);
                rawResourceDialog.show();
                return true;
            case R.id.about_menu:
                RawResourceDialog aboutDialog = new RawResourceDialog(this.getActivity(), R.raw.legal, R.raw.info);
                aboutDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class FragmentLoaderHandler extends Handler {
        private BoardGroupFragment fragment;
        private static final String TAG = FragmentLoaderHandler.class.getSimpleName();
        public FragmentLoaderHandler() {}
        public FragmentLoaderHandler(BoardGroupFragment fragment) {
            this.fragment = fragment;
        }
        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                Log.v(fragment.getClass().getSimpleName(), ">>>>>>>>>>> refresh message received restarting loader");
                if (!fragment.isDetached()) {
                    if (fragment.boardType == ChanBoard.Type.WATCHLIST) {
                        fragment.getLoaderManager().restartLoader(0, null, fragment);
                    }
                    else if (fragment.boardType == ChanBoard.Type.FAVORITES) {
                        if (fragment.adapter != null) {
                            fragment.adapter.notifyDataSetChanged();
                        }
                    }
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't restart loader", e);
            }
        }
    }

}
