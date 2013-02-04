package com.chanapps.four.fragment;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.adapter.BoardSelectorAdapter;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.loader.BoardSelectorCursorLoader;
import com.chanapps.four.loader.ChanWatchlistCursorLoader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

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
        AbstractBoardCursorAdapter.ViewBinder
{

    private static final String TAG = BoardSelectorActivity.class.getSimpleName();
    private static final boolean DEBUG = false;
    protected static final int IMAGE_RESOURCE_ID_KEY = R.id.grid_item_image;

    private ChanBoard.Type boardType;
    private ResourceCursorAdapter adapter;
    private GridView gridView;
    private Context context;
    public int columnWidth = 0;
    protected Handler handler;
    protected Loader<Cursor> cursorLoader;

    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;

    private byte[] decodeTempStorage = new byte[16000]; // to avoid constant reallocations on bitmap scaling

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
        ensureHandler();
        LoaderManager.enableDebugLogging(true);
        getLoaderManager().initLoader(0, null, this);
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
        gridView = (GridView) inflater.inflate(R.layout.board_selector_grid, container, false);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(gridView, display, ChanGridSizer.ServiceType.SELECTOR);
        cg.sizeGridToDisplay();
        context = container.getContext();
        columnWidth = cg.getColumnWidth();
        if (boardType == ChanBoard.Type.WATCHLIST) {
            adapter = new BoardGridCursorAdapter(
                    context,
                    R.layout.board_grid_item,
                    this,
                    new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_TEXT, ChanHelper.POST_COUNTRY_URL},
                    new int[] {R.id.grid_item_image, R.id.grid_item_text, R.id.grid_item_country_flag},
                    true
            );
        }
        else {
            adapter = new BoardSelectorAdapter(
                    context,
                    R.layout.selector_grid_item,
                    this,
                    new String[] {ChanHelper.BOARD_IMAGE_RESOURCE_ID, ChanHelper.BOARD_NAME},
                    new int[] {R.id.grid_item_image, R.id.grid_item_text}
            );
        }
        gridView.setAdapter(adapter);
        gridView.setClickable(true);
        gridView.setLongClickable(true);
        gridView.setOnItemClickListener(this);
        gridView.setOnItemLongClickListener(this);
        return gridView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getLoaderManager().getLoader(0) == null || !getLoaderManager().getLoader(0).isStarted())
            ensureHandler().sendEmptyMessageDelayed(0, BoardActivity.LOADER_RESTART_INTERVAL_MICRO_MS);
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
        if (boardType == ChanBoard.Type.WATCHLIST)
            return BoardActivity.setViewValue(view, cursor, columnIndex, getImageLoader(), getDisplayImageOptions());
        else
            return setViewValueForBoardSelector(view, cursor, columnIndex);
    }

    private boolean setViewValueForBoardSelector(View view, Cursor cursor, int columnIndex) {
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.BOARD_CODE));
        int imageResourceId = cursor.getInt(cursor.getColumnIndex(ChanHelper.BOARD_IMAGE_RESOURCE_ID));
        String boardName = cursor.getString(cursor.getColumnIndex(ChanHelper.BOARD_NAME));
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            tv.setText(boardName);
            return true;
        } else if (view instanceof ImageView && view.getId() == R.id.grid_item_image) {
            ImageView iv = (ImageView) view;
            try {
                Integer viewResourceIdInt = (Integer)iv.getTag(IMAGE_RESOURCE_ID_KEY);
                int viewResourceId = viewResourceIdInt != null ? viewResourceIdInt : 0;
                if (DEBUG) Log.i(TAG, "iv imageId=" + imageResourceId + " viewId=" + viewResourceId);
                try {
                    if (iv.getDrawable() == null || viewResourceId != imageResourceId) {
                        if (DEBUG) Log.i(TAG, "setting resource id for imageId=" + imageResourceId);
                        iv.setImageBitmap(null);
                        iv.setTag(IMAGE_RESOURCE_ID_KEY, imageResourceId);
                        final BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inTempStorage = decodeTempStorage;
                        options.inPurgeable = true;
                        options.inSampleSize = 1;
                        Bitmap b = BitmapFactory.decodeResource(getResources(), imageResourceId, options);
                        iv.setImageBitmap(b);
                    }
                }
                catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "Couldn't load board selector image for boardCode=" + boardCode + " imageId=" + imageResourceId);
                    iv.setImageBitmap(null);
                }
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "Exception setting image view with imageId=" + imageResourceId, e);
                iv.setImageBitmap(null);
            }
            return true;
        } else {
            return false;
        }
    }

    private ImageLoader getImageLoader() {
        //return ((BoardSelectorActivity)getActivity()).imageLoader;
        return imageLoader;
    }

    private DisplayImageOptions getDisplayImageOptions() {
        //return ((BoardSelectorActivity)getActivity()).displayImageOptions;
        return displayImageOptions;
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        if (adapter != null) {
            if (boardType == ChanBoard.Type.WATCHLIST)
                cursorLoader = new ChanWatchlistCursorLoader(getActivity().getBaseContext());
            else
                cursorLoader = new BoardSelectorCursorLoader(getActivity().getBaseContext(), boardType);
        }
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoadFinished boardType=" + boardType);
        if (adapter != null) {
		    adapter.swapCursor(data);
        }
        if (boardType == ChanBoard.Type.WATCHLIST)
            ensureHandler().sendEmptyMessageDelayed(0, BoardActivity.LOADER_RESTART_INTERVAL_MED_MS);
        else
            ensureHandler().sendEmptyMessageDelayed(0, BoardActivity.LOADER_RESTART_INTERVAL_SHORT_MS);
    }

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoaderReset boardType=" + boardType);
        if (adapter != null) {
		    adapter.swapCursor(null);
        }
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (boardType == ChanBoard.Type.WATCHLIST) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            final int loadItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOADING_ITEM));
            final int lastItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.LAST_ITEM));
            final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
            if (loadItem > 0 || lastItem > 0 || adItem > 0)
                return;
            ThreadActivity.startActivity(getActivity(), parent, view, position, id, true);
        }
        else {
            ChanBoard board = ChanBoard.getBoardsByType(getActivity(), boardType).get(position);
            String boardCode = board.link;
            BoardActivity.startActivity(getActivity(), boardCode);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (boardType == ChanBoard.Type.WATCHLIST) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            final int loadPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOADING_ITEM));
            final int lastPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LAST_ITEM));
            if (loadPage > 0 || lastPage > 0)
                return false;
            final long tim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
            if (tim > 0) {
                WatchlistDeleteDialogFragment d = new WatchlistDeleteDialogFragment(handler, tim);
                d.show(getFragmentManager(), d.TAG);
                return true;
            }
        }
        return false;
    }

    protected Handler ensureHandler() {
        if (handler == null) {
            handler = new FragmentLoaderHandler(this);
        }
        return handler;
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

    public static class FragmentLoaderHandler extends Handler {
        private BoardGroupFragment fragment;
        private static final String TAG = FragmentLoaderHandler.class.getSimpleName();
        public FragmentLoaderHandler() {}
        public FragmentLoaderHandler(BoardGroupFragment fragment) {
            this.fragment = fragment;
        }
        @Override
        public void handleMessage(Message msg) {
            if (DEBUG) Log.v(TAG, ">>>>>>>>>>> refresh message received restarting loader");
            if (fragment.isDetached())
                return;
            try {
                if (fragment.getLoaderManager() != null) {
                    fragment.ensureHandler();
                    fragment.getLoaderManager().restartLoader(0, null, fragment);
                }
            }
            catch (IllegalStateException e) {
                if (DEBUG) Log.d(TAG, "Datached fragment loader restart called, shouldn't be a problem, ignoring", e);
            }
            catch (Exception e) {
                if (DEBUG) Log.e(TAG, "Couldn't restart loader manager for watchlist fragment", e);
            }

        }
    }

}
