package com.chanapps.four.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.ChanViewHelper;
import com.chanapps.four.component.ImageTextCursorAdapter;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.*;

public class BoardGridActivity extends Activity
		implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>, ImageTextCursorAdapter.ViewBinder {
	public static final String TAG = BoardGridActivity.class.getSimpleName();
	
	private SQLiteDatabase db = null;
    private ImageTextCursorAdapter adapter = null;
    private String boardCode = null;
    private SharedPreferences prefs = null;
    private GridView gridView = null;
	private Handler handler = null;

    private ChanViewHelper viewHelper;

	private void openDatabaseIfNecessary() {
		try {
			if (db == null || !db.isOpen()) {
				Log.i(TAG, "Opening Chan database");
				db = new ChanDatabaseHelper(getApplicationContext()).getReadableDatabase();
			}
		} catch (SQLException se) {
			Log.e(TAG, "Cannot open database", se);
            Toast.makeText(this, R.string.board_activity_couldnt_open_db, Toast.LENGTH_SHORT).show();
			db = null;
		}
	}
	
	private void closeDatabse() {
		try {
			adapter.swapCursor(null);
			if (db != null) {
				db.close();
			}
		} finally {
			db = null;
		}
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState){
		Log.i(TAG, "************ onCreate");
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences(ChanHelper.PREF_NAME, 0);
        viewHelper = new ChanViewHelper(this);

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
                    getLoaderManager().restartLoader(0, null, BoardGridActivity.this);
                }

        	};
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
		Log.i(TAG, "onStart");

        Intent intent = getIntent();
        if (intent.hasExtra(ChanHelper.BOARD_CODE)) {
            boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
            Log.i(TAG, "Board code read from intent: " + boardCode);
        }
        if (!intent.hasExtra(ChanHelper.BOARD_CODE) || !ChanBoard.isValidBoardCode(boardCode)) {
            boardCode = prefs.getString(ChanHelper.BOARD_CODE, "s");
            Log.i(TAG, "Board code loaded from prefs: " + boardCode);
        }
        viewHelper.setBoardMenu(boardCode);
        
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(ChanHelper.BOARD_CODE, boardCode);
        ed.commit();

        Log.i(TAG, "Starting ChanLoadBoardService");
        Intent threadIntent = new Intent(this, ChanLoadBoardService.class);
        threadIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        startService(threadIntent);
    }

    protected void onStop () {
    	super.onStop();
    	Log.i(TAG, "onStop");
    	getLoaderManager().destroyLoader(0);
    	handler = null;
    }

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");

        refreshBoard();
	}

    private void refreshBoard() {
        viewHelper.refreshPrefs();
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
	
	protected void onDestroy () {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
		getLoaderManager().destroyLoader(0);
		closeDatabse();
		handler = null;
	}

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        return viewHelper.setGridViewValue(view, cursor, columnIndex);
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, ">>>>>>>>>>> onCreateLoader");
		openDatabaseIfNecessary();
		return new ChanBoardCursorLoader(getBaseContext(), db, boardCode);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.i(TAG, ">>>>>>>>>>> onLoadFinished");
		adapter.swapCursor(data);
        ensureHandler();
		handler.sendEmptyMessageDelayed(0, 10000);
		//closeDatabse();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.i(TAG, ">>>>>>>>>>> onLoaderReset");
		adapter.swapCursor(null);
		//closeDatabse();
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
    	Log.i(TAG, "onItemClick id=" + id + ", position=" + position);
        Intent intent = new Intent(this, ThreadListActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, id);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, BoardSelectorActivity.class);
                NavUtils.navigateUpTo(this, intent);
                return true;
            case R.id.refresh_board_menu:
                refreshBoard();
                return true;
            case R.id.view_as_list_menu:
                Intent listIntent = new Intent(getApplicationContext(), BoardListActivity.class);
                listIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                startActivity(listIntent);
                return true;
            case R.id.new_thread_menu:
                Intent replyIntent = new Intent(this, PostReplyActivity.class);
                replyIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                startActivity(replyIntent);
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
        inflater.inflate(R.menu.board_grid_menu, menu);
        return true;
    }

}
