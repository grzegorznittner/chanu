package com.chanapps.four.activity;

import android.app.ListActivity;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import android.widget.Toast;
import com.chanapps.four.component.ChanViewHelper;
import com.chanapps.four.component.ImageTextCursorAdapter;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.*;

public class ThreadListActivity
        extends ListActivity
        implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>, ImageTextCursorAdapter.ViewBinder {

	public static final String TAG = ThreadListActivity.class.getSimpleName();
	
	private SQLiteDatabase db = null;
	private ImageTextCursorAdapter adapter = null;

    private ChanViewHelper viewHelper;
    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        viewHelper = new ChanViewHelper(this, ChanViewHelper.ViewType.LIST);

        adapter = new ImageTextCursorAdapter(this,
                R.layout.thread_activity_list_item,
                this,
                new String[] {"image_url", "text"},
                new int[] {R.id.list_item_image, R.id.list_item_text});
        setListAdapter(adapter);
        setContentView(R.layout.board_activity_list_layout);

        LoaderManager.enableDebugLogging(true);

        ensureHandler();

        getListView().setClickable(true);
        getListView().setOnItemClickListener(this);
        
        getLoaderManager().initLoader(0, null, this);
    }

    private void ensureHandler() {
        if (handler == null) {
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    Log.i(TAG, ">>>>>>>>>>> refresh message received restarting loader");
                    getLoaderManager().restartLoader(0, null, ThreadListActivity.this);
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
        return viewHelper.setListViewValue(view, cursor, columnIndex);
	}

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, ">>>>>>>>>>> onCreateLoader");
        db = ChanDatabaseHelper.openDatabaseIfNecessary(this, db);
		return new ChanCursorLoader(getBaseContext(), db, viewHelper.getBoardCode(), viewHelper.getThreadNo());
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.d(TAG, ">>>>>>>>>>> onLoadFinished");
		adapter.swapCursor(data);
        ensureHandler();
		handler.sendEmptyMessageDelayed(0, 10000);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(TAG, ">>>>>>>>>>> onLoaderReset");
		adapter.swapCursor(null);
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        viewHelper.startFullImageActivityFromList(adapterView, view, position, id);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(this, BoardListActivity.class);
                upIntent.putExtra(ChanHelper.BOARD_CODE, ChanHelper.BOARD_CODE);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            case R.id.refresh_thread_menu:
                refreshBoard();
                return true;
            case R.id.view_as_grid_menu:
                Intent gridIntent = new Intent(this, ThreadGridActivity.class);
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
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.raw.help_header, R.raw.help_thread_list);
                rawResourceDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thread_list_menu, menu);
        return true;
    }

}
