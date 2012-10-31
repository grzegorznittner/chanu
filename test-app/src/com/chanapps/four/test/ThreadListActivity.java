package com.chanapps.four.test;

import java.util.Date;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
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
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

import com.chanapps.four.data.ChanDatabaseHelper;
import com.chanapps.four.data.ChanPostCursorLoader;
import com.chanapps.four.data.ChanPostService;
import com.chanapps.four.data.ChanText;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class ThreadListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

	public static final String TAG = ThreadListActivity.class.getSimpleName();
	
	public static class ThreadCursorAdapter extends SimpleCursorAdapter implements ViewBinder {
		ImageLoader imageLoader = null;
	    DisplayImageOptions options = null;
	    ThreadListActivity activity = null;
	    
		public ThreadCursorAdapter(ThreadListActivity activity, int layout, Cursor c, String[] from, int[] to,
				ImageLoader imageLoader, DisplayImageOptions options) {
			super(activity, layout, c, from, to, 0);
			this.activity = activity;
			this.imageLoader = imageLoader;
			this.options = options;
			setViewBinder(this);
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

		@Override
        public void setViewText(TextView textView, String text) {
			Log.w(TAG, "setViewText - This should not be called");
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
        
        @Override
        public void setViewImage(ImageView imageView, final String thumbnailImageUrl) {
        	Log.w(TAG, "setViewImage - This should not be called");
        }

        public void setViewImage(ImageView imageView, final String thumbnailImageUrl, Cursor cursor) {
            try {
                this.imageLoader.displayImage(thumbnailImageUrl, imageView, options);
                final int postId = cursor.getInt(cursor.getColumnIndex("_id"));
                
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(activity, FullScreenImageActivity.class);
                        intent.putExtra("boardCode", activity.boardCode);
                        intent.putExtra("threadNo", postId);
                        activity.startActivity(intent);
                    }
                });
            } catch (NumberFormatException nfe) {
                imageView.setImageURI(Uri.parse(thumbnailImageUrl));
            }
        }
	}

	SQLiteDatabase db = null;
	ImageLoader imageLoader = null;
    DisplayImageOptions options = null;
    ThreadCursorAdapter adapter = null;
    String boardCode = null;
    int threadNo = 0;
    long lastUpdate = 0;

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
        
        options = new DisplayImageOptions.Builder()
//			.showImageForEmptyUri(R.drawable.stub_image)
			.cacheOnDisc()
			.imageScaleType(ImageScaleType.EXACT)
			.build();

        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(getApplicationContext()));

        Intent intent = getIntent();
        if (intent.hasExtra("threadNo")) {
            setBoardCode(intent.getStringExtra("boardCode"));
            threadNo = intent.getIntExtra("threadNo", 0);
        }
        else {
            boardCode = "trv";
            threadNo = 609350;
        }
        Log.e(TAG, "Threadno: " + threadNo);
        
        Log.i(TAG, "Starting ChanPostService");
        Intent postIntent = new Intent(this, ChanPostService.class);
        postIntent.putExtra("board", intent.getStringExtra("boardCode"));
        postIntent.putExtra("thread", threadNo);
        startService(postIntent);
        
        db = new ChanDatabaseHelper(getApplicationContext()).getReadableDatabase();

        adapter = new ThreadCursorAdapter(this,
                R.layout.thread_activity_list_item,
                null,
                new String[] {"image_url", "text"},
                new int[] {R.id.list_item_image, R.id.list_item_text},
                imageLoader, options);
        setListAdapter(adapter);
        setContentView(R.layout.board_activity_list_layout);
        
        getListView().setClickable(true);
        //getListView().setOnItemClickListener(this);
        
        getLoaderManager().initLoader(0, null, this);

        lastUpdate = new Date().getTime();
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
                int pageNo = 0;
                Intent upIntent = new Intent(this, BoardListActivity.class);
                upIntent.putExtra("boardCode", boardCode);
                upIntent.putExtra("pageNo", pageNo);
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
                replyIntent.putExtra("boardCode", boardCode);
                replyIntent.putExtra("threadNo", threadNo);
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
