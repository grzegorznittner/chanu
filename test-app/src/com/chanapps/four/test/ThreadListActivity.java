package com.chanapps.four.test;

import java.util.Date;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.chanapps.four.component.ImageTextCursorAdapter;
import com.chanapps.four.data.ChanDatabaseHelper;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPostCursorLoader;
import com.chanapps.four.data.ChanPostService;
import com.chanapps.four.data.ChanText;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class ThreadListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>, ImageTextCursorAdapter.ViewBinder {

	public static final String TAG = ThreadListActivity.class.getSimpleName();
	
	private SQLiteDatabase db = null;
	private ImageLoader imageLoader = null;
	private DisplayImageOptions options = null;
	private ImageTextCursorAdapter adapter = null;
	private String boardCode = null;
	private long threadNo = 0;
	private long lastUpdate = 0;
    private SharedPreferences prefs = null;

    private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Log.d(TAG, ">>>>>>>>>>> refresh message received");
			getLoaderManager().restartLoader(0, null, ThreadListActivity.this);
		}

	};
    
    @Override
    protected void onCreate(Bundle savedInstanceState){
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
                R.layout.thread_activity_list_item,
                this,
                new String[] {"image_url", "text"},
                new int[] {R.id.list_item_image, R.id.list_item_text});
        setListAdapter(adapter);
        setContentView(R.layout.board_activity_list_layout);
        
        getListView().setClickable(true);
        //getListView().setOnItemClickListener(this);
        
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent.hasExtra(ChanHelper.THREAD_NO)) {
            setBoardCode(intent.getStringExtra(ChanHelper.BOARD_CODE));
            threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
            Log.i(TAG, "Loaded from intent, boardCode: " + boardCode + ", threadNo: " + threadNo);
        } else {
            boardCode = prefs.getString(ChanHelper.BOARD_CODE, "not-set");
            threadNo = prefs.getLong(ChanHelper.THREAD_NO, 0);
            Log.i(TAG, "Loaded from prefs, boardCode: " + boardCode + ", threadNo: " + threadNo);
        }
        Log.e(TAG, "Threadno: " + threadNo);

        Log.i(TAG, "Starting ChanPostService");
        Intent postIntent = new Intent(this, ChanPostService.class);
        postIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        postIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
        startService(postIntent);

        //getLoaderManager().initLoader(0, null, this);
        getLoaderManager().restartLoader(0, null, this);

        lastUpdate = new Date().getTime();
    }

    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor ed = prefs.edit();
        ed.putLong(ChanHelper.THREAD_NO, threadNo);
        Log.i(TAG, "Stored in prefs, thread no: " + threadNo);
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
            final long postId = cursor.getLong(cursor.getColumnIndex("_id"));
            
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ThreadListActivity.this, FullScreenImageActivity.class);
                    intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                    intent.putExtra(ChanHelper.THREAD_NO, threadNo);
                    intent.putExtra(ChanHelper.POST_NO, postId);
                    startActivity(intent);
                }
            });
        } catch (NumberFormatException nfe) {
            imageView.setImageURI(Uri.parse(thumbnailImageUrl));
        }
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, ">>>>>>>>>>> onCreateLoader");

		return new ChanPostCursorLoader(getBaseContext(), db, boardCode, threadNo);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(this, BoardListActivity.class);
                upIntent.putExtra(ChanHelper.BOARD_CODE, ChanHelper.BOARD_CODE);
                upIntent.putExtra(ChanHelper.PAGE, 0);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            case R.id.refresh_thread_menu:
            	handler.sendEmptyMessageDelayed(0, 100);
                return true;
            case R.id.view_as_grid_menu:
                Toast.makeText(getApplicationContext(), "View as Grid", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.view_as_list_menu:
                Toast.makeText(getApplicationContext(), "View as List", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.post_reply_menu:
                Intent replyIntent = new Intent(this, PostReplyActivity.class);
                replyIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                replyIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
                startActivity(replyIntent);
                return true;
            case R.id.download_all_images_menu:
                Toast.makeText(getApplicationContext(), "Starting download...", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.watch_thread_menu:
                Toast.makeText(getApplicationContext(), "Watch this thread", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.settings_menu:
                Toast.makeText(getApplicationContext(), "Settings", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.help_menu:
                Toast.makeText(getApplicationContext(), "Help", Toast.LENGTH_SHORT).show();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thread_list_menu, menu);
        return true;
    }

    private void setBoardCode(String code) {
        boardCode = code;
        if (getActionBar() != null) {
            getActionBar().setTitle("/" + boardCode + " " + getString(R.string.thread_list_activity));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
}
