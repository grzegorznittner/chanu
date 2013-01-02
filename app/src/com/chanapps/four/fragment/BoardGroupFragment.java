package com.chanapps.four.fragment;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.chanapps.four.activity.*;
import com.chanapps.four.adapter.BoardCursorAdapter;
import com.chanapps.four.adapter.BoardSelectorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
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
        BoardCursorAdapter.ViewBinder
{

    private static final String TAG = BoardSelectorActivity.class.getSimpleName();
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
        //if (boardType == ChanBoard.Type.WATCHLIST || boardType == ChanBoard.Type.FAVORITES) {
        //    setHasOptionsMenu(true);
        //}
        ensureHandler();
        LoaderManager.enableDebugLogging(true);
        getLoaderManager().initLoader(0, null, this);
        Log.v(TAG, "BoardGroupFragment " + boardType + " onCreate");
        //setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "BoardGroupFragment " + boardType + " onCreateView");
        gridView = (GridView) inflater.inflate(R.layout.board_selector_grid, container, false);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(gridView, display, ChanGridSizer.ServiceType.SELECTOR);
        cg.sizeGridToDisplay();
        context = container.getContext();
        columnWidth = cg.getColumnWidth();
        if (boardType == ChanBoard.Type.WATCHLIST) {
            adapter = new BoardCursorAdapter(
                    context,
                    R.layout.board_grid_item,
                    this,
                    new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_TEXT, ChanHelper.POST_COUNTRY_URL},
                    new int[] {R.id.grid_item_image, R.id.grid_item_text, R.id.grid_item_country_flag}
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
            return ThreadActivity.setViewValue(view, cursor, columnIndex, getImageLoader(), getDisplayImageOptions(), false);
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
                Log.i(TAG, "iv imageId=" + imageResourceId + " viewId=" + viewResourceId);
                try {
                    if (iv.getDrawable() == null || viewResourceId != imageResourceId) {
                        Log.i(TAG, "setting resource id for imageId=" + imageResourceId);
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
                    Log.e(TAG, "Couldn't load board selector image for boardCode=" + boardCode + " imageId=" + imageResourceId);
                    iv.setImageBitmap(null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception setting image view with imageId=" + imageResourceId, e);
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
		Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
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
		Log.v(TAG, ">>>>>>>>>>> onLoadFinished boardType=" + boardType);
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
		Log.v(TAG, ">>>>>>>>>>> onLoaderReset boardType=" + boardType);
        if (adapter != null) {
		    adapter.swapCursor(null);
        }
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (boardType == ChanBoard.Type.WATCHLIST) {
            ThreadActivity.startActivity(getActivity(), parent, view, position, id, true);
        }
        else {
            ChanBoard board = ChanBoard.getBoardsByType(getActivity(), boardType).get(position);
            String boardCode = board.link;
            Intent intent = new Intent(this.getActivity(), BoardActivity.class);
            intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            intent.putExtra(ChanHelper.PAGE, 0);
            intent.putExtra(ChanHelper.LAST_BOARD_POSITION, 0);
            intent.putExtra(ChanHelper.FROM_PARENT, true);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
            editor.putInt(ChanHelper.LAST_BOARD_POSITION, 0); // reset it
            editor.commit();
            startActivity(intent);
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

    protected Handler ensureHandler() {
        if (handler == null) {
            handler = new FragmentLoaderHandler(this);
        }
        return handler;
    }

    /*
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Log.i(TAG, "Called onPrepareOptionsMenu fragment boardType=" + boardType);
    }
    */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.i(TAG, "Called onCreateOptionsMenu fragment boardType=" + boardType);
        //MenuItem test = menu.add("Test");
        //test.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        //getActivity().getActionBar().show();
        if (inflater == null) {
            Log.e(TAG, "Can't inflate, null menu inflater");
        }
        if (boardType == ChanBoard.Type.FAVORITES) {
            inflater.inflate(R.menu.favorites_menu, menu);
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
            case R.id.clean_watchlist_menu:
                WatchlistCleanDialogFragment cleanWatchlistFragment = new WatchlistCleanDialogFragment(this);
                cleanWatchlistFragment.show(getFragmentManager(), cleanWatchlistFragment.TAG);
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
            Log.v(TAG, ">>>>>>>>>>> refresh message received restarting loader");
            if (fragment.isDetached())
                return;

            try {
                if (fragment.boardType == ChanBoard.Type.FAVORITES && fragment.adapter != null) {
                    fragment.adapter.notifyDataSetChanged();
                }
            }
            catch (IllegalStateException e) {
                Log.d(TAG, "Detached favorites fragment loader called, shouldn't be a problem, ignoring", e);
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't notify adapter of favorites board type data changed");
            }

            try {
                if (fragment.getLoaderManager() != null) {
                    fragment.ensureHandler();
                    fragment.getLoaderManager().restartLoader(0, null, fragment);
                }
            }
            catch (IllegalStateException e) {
                Log.d(TAG, "Datached fragment loader restart called, shouldn't be a problem, ignoring", e);
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't restart loader manager for watchlist fragment", e);
            }

        }
    }

}
