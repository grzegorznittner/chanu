package com.chanapps.four.test;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import com.chanapps.four.data.ChanThreadCursorLoader;
import com.chanapps.four.data.ChanDatabaseHelper;
import com.chanapps.four.data.ChanText;
import com.chanapps.four.data.ChanThreadService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class BoardListActivity extends ListActivity
		implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
	public static final String TAG = BoardListActivity.class.getSimpleName();
	
	public static class MyCursorAdapter extends SimpleCursorAdapter implements ViewBinder {
		ImageLoader imageLoader = null;
	    DisplayImageOptions options = null;
	    
		public MyCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
				ImageLoader imageLoader, DisplayImageOptions options) {
			super(context, layout, c, from, to, 0);
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
            } catch (NumberFormatException nfe) {
                imageView.setImageURI(Uri.parse(thumbnailImageUrl));
            }
        }
	}
	
	SQLiteDatabase db = null;
	ImageLoader imageLoader = null;
    DisplayImageOptions options = null;
    MyCursorAdapter adapter = null;
    String boardCode = null;
    int pageNo = 0;
    long lastUpdate = 0;
    
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
        
        options = new DisplayImageOptions.Builder()
//			.showImageForEmptyUri(R.drawable.stub_image)
			.cacheOnDisc()
			.imageScaleType(ImageScaleType.EXACT)
			.build();

        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(getApplicationContext()));

        Intent intent = getIntent();
        if (intent.hasExtra("boardCode")) {
            setBoardCode(intent.getStringExtra("boardCode"));
            pageNo = intent.getIntExtra("pageNo", 0);
        }
        else {
            setBoardCode("trv");
            pageNo = 0;
        }

        Log.i(TAG, "Starting ChanThreadService");
        Intent threadIntent = new Intent(this, ChanThreadService.class);
        threadIntent.putExtra("board", intent.getStringExtra("boardCode"));
        threadIntent.putExtra("page", 0);
        startService(threadIntent);
        
        db = new ChanDatabaseHelper(getApplicationContext()).getReadableDatabase();

        adapter = new MyCursorAdapter(this,
                R.layout.board_activity_list_item,
                null,
                new String[] {"image_url", "text"},
                new int[] {R.id.list_item_image, R.id.list_item_text},
                imageLoader, options);
        setListAdapter(adapter);
        setContentView(R.layout.board_activity_list_layout);
        
        getListView().setClickable(true);
        getListView().setOnItemClickListener(this);
        
        getLoaderManager().initLoader(0, null, this);
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
	public void onListItemClick(ListView l, View v, int position, long id) {
        Log.i(TAG, "onListItemClick: " + id);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
    	Log.i(TAG, "onItemClick id=" + id + ", position=" + position);
        Intent intent = new Intent(this, ThreadListActivity.class);
        intent.putExtra("boardCode", boardCode);
        intent.putExtra("threadNo", (int)id);
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
