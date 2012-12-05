package com.chanapps.four.component;

import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import com.chanapps.four.data.*;
import com.chanapps.four.activity.R;

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
        LoaderManager.LoaderCallbacks<Cursor>,
        ImageTextCursorAdapter.ViewBinder
{

    private static final String TAG = BoardGroupFragment.class.getSimpleName();

    private ChanBoard.Type boardType;
    private ImageTextCursorAdapter imageTextCursorAdapter;
    private BaseAdapter adapter;
    private GridView gridView;
    private Context context;
    private int columnWidth = 0;
    protected Handler handler;
    protected ChanWatchlistCursorLoader cursorLoader;
    protected ChanViewHelper viewHelper;

    public ChanViewHelper.ServiceType getServiceType() {
        return ChanViewHelper.ServiceType.WATCHLIST;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boardType = getArguments() != null
                ? ChanBoard.Type.valueOf(getArguments().getString(ChanHelper.BOARD_TYPE)) : ChanBoard.Type.JAPANESE_CULTURE;
        if (boardType == ChanBoard.Type.WATCHING) {
            viewHelper = new ChanViewHelper(this.getActivity(), ChanViewHelper.ServiceType.WATCHLIST);
        }
        ensureHandler();
        LoaderManager.enableDebugLogging(true);
        Log.i(TAG, "onCreate init loader");
        getLoaderManager().initLoader(0, null, this);
        Log.d(TAG, "BoardGroupFragment " + boardType + " onCreate");
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
        if (boardType == ChanBoard.Type.WATCHING) {
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
        gridView.setOnItemClickListener(this);
        return gridView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (boardType == ChanBoard.Type.WATCHING) {
            viewHelper.startService();
        }
    }

    @Override
    public void onStop () {
    	super.onStop();
    	getLoaderManager().destroyLoader(0);
    	handler = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getLoaderManager().destroyLoader(0);
        handler = null;
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        return viewHelper.setViewValue(view, cursor, columnIndex);
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, ">>>>>>>>>>> onCreateLoader");
        if (imageTextCursorAdapter != null) {
            cursorLoader = new ChanWatchlistCursorLoader(getActivity().getBaseContext());
        }
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.i(TAG, ">>>>>>>>>>> onLoadFinished");
        if (imageTextCursorAdapter != null) {
		    imageTextCursorAdapter.swapCursor(data);
        }
        ensureHandler();
		handler.sendEmptyMessageDelayed(0, 10000);
	}

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.i(TAG, ">>>>>>>>>>> onLoaderReset");
        if (imageTextCursorAdapter != null) {
		    imageTextCursorAdapter.swapCursor(null);
        }
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (boardType == ChanBoard.Type.WATCHING) {
            viewHelper.startThreadActivity(parent, view, position, id);
        }
        else {
            ChanBoard board = ChanBoard.getBoardsByType(getActivity(), boardType).get(position);
            String boardCode = board.link;
            ChanViewHelper.startBoardActivity(parent, view, position, id, getActivity(), boardCode);
        }
    }

    protected void ensureHandler() {
        if (handler == null) {
            handler = new FragmentLoaderHandler(this);
        }
    }

    private class FragmentLoaderHandler extends Handler {
        private BoardGroupFragment fragment;
        public FragmentLoaderHandler() {}
        public FragmentLoaderHandler(BoardGroupFragment fragment) {
            this.fragment = fragment;
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(fragment.getClass().getSimpleName(), ">>>>>>>>>>> refresh message received restarting loader");
            if (!fragment.isDetached()) {
                fragment.getLoaderManager().restartLoader(0, null, fragment);
            }
        }
    }
}
