package com.chanapps.four.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.ChanViewHelper;
import com.chanapps.four.component.ImageTextCursorAdapter;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanCursorLoader;
import com.chanapps.four.data.ChanDatabaseHelper;
import com.chanapps.four.data.ChanHelper;

public class ThreadGridActivity extends Activity
		implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>, ImageTextCursorAdapter.ViewBinder {
	public static final String TAG = ThreadGridActivity.class.getSimpleName();
	
	private SQLiteDatabase db = null;
    private ImageTextCursorAdapter adapter = null;
    private GridView gridView = null;
	private Handler handler = null;

    private ChanCursorLoader cursorLoader;
    private ChanViewHelper viewHelper;
	
    @Override
    protected void onCreate(Bundle savedInstanceState){
		Log.i(TAG, "************ onCreate");
        super.onCreate(savedInstanceState);

        viewHelper = new ChanViewHelper(this, ChanViewHelper.ViewType.GRID);

        setContentView(R.layout.board_activity_grid_layout);

        gridView = (GridView)findViewById(R.id.board_activity_grid_view);
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(gridView, display);
        cg.sizeGridToDisplay();

        adapter = new ImageTextCursorAdapter(this,
                R.layout.board_activity_grid_item,
                this,
                new String[] {"image_url", "text"},
                new int[] {R.id.board_activity_grid_item_image, R.id.board_activity_grid_item_text});
        gridView.setAdapter(adapter);

        LoaderManager.enableDebugLogging(true);

        ensureHandler();

        gridView.setClickable(true);
        gridView.setOnItemClickListener(this);
        
        //Log.i(TAG, "onCreate init loader");
        getLoaderManager().initLoader(0, null, this);
    }

    private void ensureHandler() {
        if (handler == null) {
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    Log.i(TAG, ">>>>>>>>>>> refresh message received restarting loader");
                    getLoaderManager().restartLoader(0, null, ThreadGridActivity.this);
                }

        	};
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
		Log.i(TAG, "onStart");
        viewHelper.startThreadService();
    }

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
        refreshBoard();
	}

    private void refreshBoard() {
        viewHelper.onRefresh();
        ensureHandler();
		handler.sendEmptyMessageDelayed(0, 100);
        Toast.makeText(getApplicationContext(), R.string.board_activity_refresh, Toast.LENGTH_SHORT).show();
    }

	public void onWindowFocusChanged (boolean hasFocus) {
		Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

	protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }
	
    protected void onStop () {
    	super.onStop();
    	Log.i(TAG, "onStop");
    	getLoaderManager().destroyLoader(0);
    	handler = null;
    }

	protected void onDestroy () {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
		getLoaderManager().destroyLoader(0);
		db = ChanDatabaseHelper.closeDatabase(adapter, db);
		handler = null;
	}

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        return viewHelper.setGridViewValue(view, cursor, columnIndex);
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, ">>>>>>>>>>> onCreateLoader");
		db = ChanDatabaseHelper.openDatabaseIfNecessary(this, db);
		cursorLoader = new ChanCursorLoader(getBaseContext(), db, viewHelper.getBoardCode(), viewHelper.getThreadNo());
        return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.i(TAG, ">>>>>>>>>>> onLoadFinished");
		adapter.swapCursor(data);
        ensureHandler();
		handler.sendEmptyMessageDelayed(0, 10000);
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
        viewHelper.startFullImageActivityFromGrid(adapterView, view, position, id);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(this, BoardGridActivity.class);
                upIntent.putExtra(ChanHelper.BOARD_CODE, ChanHelper.BOARD_CODE);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            case R.id.refresh_thread_menu:
                refreshBoard();
                return true;
            case R.id.view_as_list_menu:
                Intent gridIntent = new Intent(this, ThreadListActivity.class);
                gridIntent.putExtra(ChanHelper.BOARD_CODE, viewHelper.getBoardCode());
                gridIntent.putExtra(ChanHelper.THREAD_NO, viewHelper.getThreadNo());
                startActivity(gridIntent);
                return true;
            case R.id.post_reply_menu:
                Intent replyIntent = new Intent(this, PostReplyActivity.class);
                replyIntent.putExtra(ChanHelper.BOARD_CODE, viewHelper.getBoardCode());
                replyIntent.putExtra(ChanHelper.THREAD_NO, viewHelper.getThreadNo());
                startActivity(replyIntent);
                return true;
            case R.id.download_all_images_menu:
                Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT);
                return true;
            case R.id.watch_thread_menu:
                Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT);
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
        inflater.inflate(R.menu.thread_grid_menu, menu);
        return true;
    }

}
