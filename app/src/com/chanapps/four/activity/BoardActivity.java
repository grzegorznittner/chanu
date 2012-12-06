package com.chanapps.four.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.ChanViewHelper;
import com.chanapps.four.component.ImageTextCursorAdapter;
import com.chanapps.four.component.LoaderHandler;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanCursorLoader;
import com.chanapps.four.data.ChanHelper;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

public class BoardActivity extends Activity implements ClickableLoaderActivity {
	public static final String TAG = BoardActivity.class.getSimpleName();
	
    protected ImageTextCursorAdapter adapter;
    protected PullToRefreshGridView gridView;
    protected PullToRefreshListView listView;
    protected Handler handler;
    protected ChanCursorLoader cursorLoader;
    protected ChanViewHelper viewHelper;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "************ onCreate");
        super.onCreate(savedInstanceState);
        viewHelper = new ChanViewHelper(this, getServiceType());
        createViewFromOrientation();
        ensureHandler();
        LoaderManager.enableDebugLogging(true);
        Log.i(TAG, "onCreate init loader");
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public ChanViewHelper.ServiceType getServiceType() {
        return ChanViewHelper.ServiceType.BOARD;
    }

    protected void createViewFromOrientation() {
        if (viewHelper.getViewType() == ChanViewHelper.ViewType.GRID) {
            setContentView(R.layout.board_grid_layout);
            gridView = (PullToRefreshGridView)findViewById(R.id.board_activity_grid_view);
            Display display = getWindowManager().getDefaultDisplay();
            ChanGridSizer cg = new ChanGridSizer(gridView.getRefreshableView(), display, getServiceType());
            cg.sizeGridToDisplay();
            adapter = new ImageTextCursorAdapter(this,
                R.layout.board_grid_item,
                this,
                new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_TEXT},
                new int[] {R.id.board_activity_grid_item_image, R.id.board_activity_grid_item_text});
            gridView.getRefreshableView().setAdapter(adapter);
            gridView.setClickable(true);
            gridView.getRefreshableView().setOnItemClickListener(this);
            gridView.setLongClickable(true);
            gridView.getRefreshableView().setOnItemLongClickListener(this);
            gridView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<GridView>() {
                @Override
                public void onRefresh(PullToRefreshBase<GridView> refreshView) {
                    refresh();
                }
            });
            listView = null;
        }
        else {
            setContentView(R.layout.board_list_layout);
            listView = (PullToRefreshListView)findViewById(R.id.board_activity_list_view);
            adapter = new ImageTextCursorAdapter(this,
                    R.layout.board_list_item,
                    this,
                    new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_TEXT},
                    new int[] {R.id.list_item_image, R.id.list_item_text});
            listView.getRefreshableView().setAdapter(adapter);
            listView.setClickable(true);
            listView.getRefreshableView().setOnItemClickListener(this);
            listView.setLongClickable(true);
            listView.getRefreshableView().setOnItemLongClickListener(this);
            listView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
                @Override
                public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                    refresh();
                }
            });
            gridView = null;
        }
    }

    protected void ensureHandler() {
        if (handler == null) {
            handler = new LoaderHandler(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
		Log.i(TAG, "onStart");
        viewHelper.startService();
    }

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
        refresh();
	}

    protected void refresh() {
        createViewFromOrientation();
        viewHelper.onRefresh();
        ensureHandler();
		handler.sendEmptyMessageDelayed(0, 200);
        Toast.makeText(this, R.string.board_activity_refresh, Toast.LENGTH_SHORT).show();
    }

    @Override
	public void onWindowFocusChanged (boolean hasFocus) {
		Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onStop () {
    	super.onStop();
    	Log.i(TAG, "onStop");
    	getLoaderManager().destroyLoader(0);
    	handler = null;
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
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
		cursorLoader = new ChanCursorLoader(getBaseContext(), viewHelper.getBoardCode(), viewHelper.getThreadNo());
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.i(TAG, ">>>>>>>>>>> onLoadFinished");
		adapter.swapCursor(data);
        ensureHandler();
		handler.sendEmptyMessageDelayed(0, 5000);
        if (gridView != null) {
            gridView.onRefreshComplete();
        }
        if (listView != null) {
            listView.onRefreshComplete();
        }
		//closeDatabase();
	}

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.i(TAG, ">>>>>>>>>>> onLoaderReset");
		adapter.swapCursor(null);
		//closeDatabase();
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        viewHelper.startThreadActivity(adapterView, view, position, id);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        return viewHelper.showPopupText(adapterView, view, position, id);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, BoardSelectorActivity.class);
                NavUtils.navigateUpTo(this, intent);
                return true;
            case R.id.new_thread_menu:
                Intent replyIntent = new Intent(this, PostReplyActivity.class);
                replyIntent.putExtra(ChanHelper.BOARD_CODE, viewHelper.getBoardCode());
                startActivity(replyIntent);
                return true;
            case R.id.add_to_favorites_menu:
                ChanBoard.addBoardToFavorites(this, viewHelper.getBoardCode());
                return true;
            case R.id.settings_menu:
                Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.raw.help_header, R.raw.help_board_grid);
                rawResourceDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_menu, menu);
        return true;
    }

}
