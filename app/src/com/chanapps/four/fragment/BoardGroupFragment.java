package com.chanapps.four.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.chanapps.four.adapter.BoardSelectorAdapter;
import com.chanapps.four.adapter.BoardCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
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

    private static final String TAG = BoardGroupFragment.class.getSimpleName();

    private ChanBoard.Type boardType;
    private BoardCursorAdapter boardCursorAdapter;
    private BaseAdapter adapter;
    private GridView gridView;
    private Context context;
    public int columnWidth = 0;
    protected Handler handler;
    protected ChanWatchlistCursorLoader cursorLoader;
    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;

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
            setHasOptionsMenu(true);
        }
        else if (boardType == ChanBoard.Type.FAVORITES) {
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
        ChanGridSizer cg = new ChanGridSizer(gridView, display, ChanGridSizer.ServiceType.SELECTOR);
        cg.sizeGridToDisplay();
        context = container.getContext();
        columnWidth = cg.getColumnWidth();
        if (boardType == ChanBoard.Type.WATCHLIST) {
            boardCursorAdapter = new BoardCursorAdapter(
                    context,
                    R.layout.board_grid_item,
                    this,
                    new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_TEXT},
                    new int[] {R.id.board_activity_grid_item_image, R.id.board_activity_grid_item_text}
            );
            adapter = boardCursorAdapter;
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
    public void onPause() {
        super.onPause();
        ((BoardSelectorActivity)getActivity()).saveInstanceState();
    }

    @Override
    public void onStart() {
        super.onStart();
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
        String shortText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT));
        String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        if (view instanceof TextView) {
            //todo - @john - if the text is hidden then the image should take the full available space.
            TextView tv = (TextView) view;
            if (shortText == null || shortText.isEmpty()) {
                tv.setText("");
                tv.setVisibility(View.INVISIBLE);
            }
            else {
                tv.setText(shortText);
            }
            return true;
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            try {
                this.imageLoader.displayImage(imageUrl, iv, displayImageOptions);
            } catch (NumberFormatException nfe) {
                iv.setImageURI(Uri.parse(imageUrl));
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        if (boardCursorAdapter != null) {
            cursorLoader = new ChanWatchlistCursorLoader(getActivity().getBaseContext());
        }
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.v(TAG, ">>>>>>>>>>> onLoadFinished");
        if (boardCursorAdapter != null) {
		    boardCursorAdapter.swapCursor(data);
        }
        ensureHandler();
		handler.sendEmptyMessageDelayed(0, 10000);
	}

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.v(TAG, ">>>>>>>>>>> onLoaderReset");
        if (boardCursorAdapter != null) {
		    boardCursorAdapter.swapCursor(null);
        }
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (boardType == ChanBoard.Type.WATCHLIST) {
            ThreadActivity.startActivity(getActivity(), parent, view, position, id);
        }
        else {
            ChanBoard board = ChanBoard.getBoardsByType(getActivity(), boardType).get(position);
            String boardCode = board.link;
            Intent intent = new Intent(this.getActivity(), BoardActivity.class);
            intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            intent.putExtra(ChanHelper.PAGE, 0);
            intent.putExtra(ChanHelper.LAST_BOARD_POSITION, 0);
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
