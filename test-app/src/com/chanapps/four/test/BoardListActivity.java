package com.chanapps.four.test;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.chanapps.four.component.ImageTextCursorAdapter;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanDatabaseHelper;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanText;
import com.chanapps.four.data.ChanThreadCursorLoader;
import com.chanapps.four.data.ChanThreadService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class BoardListActivity extends ListActivity
		implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>, ImageTextCursorAdapter.ViewBinder {
	public static final String TAG = BoardListActivity.class.getSimpleName();
	
	private SQLiteDatabase db = null;
	private ImageLoader imageLoader = null;
    private DisplayImageOptions options = null;
    private ImageTextCursorAdapter adapter = null;
    private String boardCode = null;
    private long lastUpdate = 0;
    private SharedPreferences prefs = null;
    
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Log.d(TAG, ">>>>>>>>>>> refresh message received");
			getLoaderManager().restartLoader(0, null, BoardListActivity.this);
		}

	};
	
    @Override
    protected void onCreate(Bundle savedInstanceState){
		Log.d(TAG, "************ onCreate");
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences(ChanHelper.PREF_NAME, 0);
        
        options = new DisplayImageOptions.Builder()
//			.showImageForEmptyUri(R.drawable.stub_image)
			.cacheOnDisc()
			.imageScaleType(ImageScaleType.EXACT)
			.build();

        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(getApplicationContext()));


        db = new ChanDatabaseHelper(getApplicationContext()).getReadableDatabase();

        adapter = new ImageTextCursorAdapter(this,
                R.layout.board_activity_list_item,
                this,
                new String[] {"image_url", "text"},
                new int[] {R.id.list_item_image, R.id.list_item_text});
        setListAdapter(adapter);
        setContentView(R.layout.board_activity_list_layout);
        
        getListView().setClickable(true);
        getListView().setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent.hasExtra(ChanHelper.BOARD_CODE)) {
            setBoardCode(intent.getStringExtra(ChanHelper.BOARD_CODE));
            Log.i(TAG, "Board code read from intent: " + boardCode);
        }
        if (!intent.hasExtra(ChanHelper.BOARD_CODE) || !ChanBoard.isValidBoardCode(boardCode)) {
            setBoardCode(prefs.getString(ChanHelper.BOARD_CODE, "s"));
            Log.i(TAG, "Board code loaded from prefs: " + boardCode);
        }

        Log.i(TAG, "Starting ChanThreadService");
        Intent threadIntent = new Intent(this, ChanThreadService.class);
        threadIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        threadIntent.putExtra(ChanHelper.PAGE, 0);
        startService(threadIntent);

        //getLoaderManager().initLoader(0, null, this);
        getLoaderManager().restartLoader(0, null, this);
    }

    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(ChanHelper.BOARD_CODE, boardCode);
        Log.i(TAG, "Stored in prefs, board code: " + boardCode);
        ed.commit();
    }

	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		if (view instanceof TextView) {
			String text = cursor.getString(columnIndex);
			text = ChanText.sanitizeText(text);
            setViewText((TextView) view, text, cursor);
            return true;
        } else if (view instanceof ImageView) {
        	String text = cursor.getString(columnIndex);
            setViewImage((ImageView) view, text, cursor);
            return true;
        } else {
        	return false;
        }
	}

    public void setViewText(TextView textView, String text, Cursor cursor) {
        if (cursor == null) {
        	Log.w(TAG, "setViewText - Why is cursor null?");
            return;
        }
        int tn_w = cursor.getInt(cursor.getColumnIndex("tn_w"));
        int tn_h = cursor.getInt(cursor.getColumnIndex("tn_h"));
        //Log.i(TAG, "tn_w=" + tn_w + ", tn_h=" + tn_h);
        Point imageDimensions = new Point(tn_w, tn_h);
        if (imageDimensions != null && imageDimensions.x > 0 && imageDimensions.y > 0) {
        	text = text == null ? "" : text;
            FlowTextHelper.tryFlowText(text, imageDimensions, textView);
        } else {
            textView.setText(text);
        }
    }
    
    public void setViewImage(ImageView imageView, final String thumbnailImageUrl, Cursor cursor) {
        try {
            this.imageLoader.displayImage(thumbnailImageUrl, imageView, options);
        } catch (NumberFormatException nfe) {
            imageView.setImageURI(Uri.parse(thumbnailImageUrl));
        }
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, ">>>>>>>>>>> onCreateLoader");

		return new ChanThreadCursorLoader(getBaseContext(), db, boardCode);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Log.d(TAG, ">>>>>>>>>>> onLoadFinished");
		adapter.swapCursor(data);
		handler.sendEmptyMessageDelayed(0, 2000);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(TAG, ">>>>>>>>>>> onLoaderReset");
		adapter.swapCursor(null);
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "************ onDestroy");
		adapter.swapCursor(null);
		super.onDestroy();
		if (db != null) {
			db.close();
			db = null;
		}
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
                Intent intent = new Intent(this, BoardGridActivity.class);
                NavUtils.navigateUpTo(this, intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_list_menu, menu);
        return true;
    }

    private void setBoardCode(String code) {
        boardCode = code;
        if (getActionBar() != null) {
            getActionBar().setTitle("/" + boardCode + " " + getString(R.string.board_list_activity));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

}
