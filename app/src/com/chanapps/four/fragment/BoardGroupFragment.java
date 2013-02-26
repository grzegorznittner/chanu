package com.chanapps.four.fragment;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ResourceCursorAdapter;
import android.widget.ScrollView;

import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.component.BoardTypeView;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanBoard.Type;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanThread;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

/**
* User: arley
*/
public class BoardGroupFragment extends Fragment
{

    private static final String TAG = BoardSelectorActivity.class.getSimpleName();
    private static final boolean DEBUG = false;
    protected static final int IMAGE_RESOURCE_ID_KEY = R.id.grid_item_image;

    private ChanBoard.Type boardType;
    private ResourceCursorAdapter adapter;
    
    private ScrollView scrollView;
    
    public int columnWidth = 0;
    protected Handler handler;
    protected Loader<Cursor> cursorLoader;

    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;

    private int numCols = 2;
    
    public BaseAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boardType = getArguments() != null
                ? ChanBoard.Type.valueOf(getArguments().getString(ChanHelper.BOARD_TYPE))
                : ChanBoard.Type.JAPANESE_CULTURE;
        if (boardType == ChanBoard.Type.WATCHLIST) {
            imageLoader = ImageLoader.getInstance();
            imageLoader.init(ImageLoaderConfiguration.createDefault(getActivity()));
            displayImageOptions = new DisplayImageOptions.Builder()
                    .showImageForEmptyUri(R.drawable.stub_image)
                    .cacheOnDisc()
                    .imageScaleType(ImageScaleType.EXACT)
                    .build();
        }
        if (DEBUG) Log.v(TAG, "BoardGroupFragment " + boardType + " onCreate");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (DEBUG) Log.d(TAG, "BoardGroupFragment " + boardType + " onCreateView");
        
        scrollView = (ScrollView) inflater.inflate(R.layout.board_selector_grid, container, false);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(scrollView, display, ChanGridSizer.ServiceType.SELECTOR);
        cg.sizeGridToDisplay();

        numCols = cg.getNumColumns();
        columnWidth = cg.getColumnWidth();
        
        ensureHandler();
        BoardTypeView boardTypeView = (BoardTypeView)scrollView.findViewById(R.id.board_selector_type_view);
        boardTypeView.setBoardData(handler, boardType, numCols, columnWidth);
        boardTypeView.setBoardGroupFragment(this);
        
        return scrollView;
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop () {
    	super.onStop();
        handler = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        handler = null;
    }

    public void onItemClick(View view, String boardCode, long threadNo) {
        //ChanHelper.fadeout(getActivity(), view);
        if (threadNo > 0) {
        	ChanThread thread = ChanFileStorage.loadThreadData(getActivity(), boardCode, threadNo);
            if (thread != null) {
                ThreadActivity.startActivity(getActivity(), thread, view, threadNo, true);
            }
            else {
                ThreadActivity.startActivity(getActivity(), boardCode, threadNo);
            }
        } else {
            BoardActivity.startActivity(getActivity(), boardCode);
        }
    }

    public boolean onItemLongClick(View view, Type boardType, String boardCode, long threadNo) {
    	Log.i(TAG, "Long click " + boardType + " /" + boardCode + "/" + threadNo);
        if (boardType == ChanBoard.Type.WATCHLIST && threadNo >= 0) {
        	ChanThread thread = ChanFileStorage.loadThreadData(getActivity(), boardCode, threadNo);
            if (thread != null && thread.posts[0].tim > 0) {
                WatchlistDeleteDialogFragment d = new WatchlistDeleteDialogFragment(view, handler, thread.posts[0].tim);
                d.show(getFragmentManager(), WatchlistDeleteDialogFragment.TAG);
                return true;
            }
        }
        return false;
    }

    protected Handler ensureHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }
    
    public void invalidate() {
    	if (scrollView != null) {
    		handler.postDelayed(new Runnable () {
                public void run() {
            		BoardTypeView boardTypeView = (BoardTypeView)scrollView.findViewById(R.id.board_selector_type_view);
            		boardTypeView.postInvalidate();
            		boardTypeView.refreshDrawableState();
            		scrollView.postInvalidate();
                }
            }, 300);
    	}
    }


    public void onPrepareOptionsMenu(Menu menu, Context menuContext, ChanBoard.Type selectedBoardType) {
        super.onPrepareOptionsMenu(menu);
        if (DEBUG) Log.i(TAG, "Called onPrepareOptionsMenu fragment selectedBoardType=" + selectedBoardType + " menuContext=" + menuContext);
        if (menuContext == null)
            return;
        menu.removeItem(R.id.clear_watchlist_menu);
        menu.removeItem(R.id.clean_watchlist_menu);
        MenuInflater inflater = new MenuInflater(menuContext);
        if (selectedBoardType == ChanBoard.Type.WATCHLIST) {
            inflater.inflate(R.menu.watchlist_menu, menu);
        }
    }
}
