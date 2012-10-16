package com.chanapps.four.test;

import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

import com.chanapps.four.data.ChanThread;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class ThreadListActivity extends ListActivity implements AdapterView.OnItemClickListener {

	public static final String TAG = ThreadListActivity.class.getSimpleName();
	
	public static class MyCursorAdapter extends SimpleCursorAdapter {
		ImageLoader imageLoader = null;
	    DisplayImageOptions options = null;
	    
		public MyCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
				ImageLoader imageLoader, DisplayImageOptions options) {
			super(context, layout, c, from, to);
			this.imageLoader = imageLoader;
			this.options = options;
		}
		
		@Override
		public void setViewImage(ImageView v, String value) {
			try {
				this.imageLoader.displayImage(value, v, options);
	        } catch (NumberFormatException nfe) {
	            v.setImageURI(Uri.parse(value));
	        }
		}
	}
	
    ChanThread chanThread = null;
	ImageLoader imageLoader = null;
    DisplayImageOptions options = null;
    MyCursorAdapter adapter = null;
    MatrixCursor cursor = new MatrixCursor(new String[] {"_id", "image_url", "text"});
    long lastUpdate = 0;
    String boardCode = null;
    int threadNo = 0;

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
        	Log.i(TAG, "Notifying adapter change for " + msg.arg1);
    		adapter.notifyDataSetChanged();
    		if (new Date().getTime() - lastUpdate > 500) {
    			Log.i(TAG, "######## Updating list view for " + msg.arg1);
    			getListView().requestLayout();
    		}
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

        adapter = new MyCursorAdapter(getApplicationContext(),
                R.layout.thread_activity_list_item,
                cursor,
                new String[] {"image_url", "text"},
                new int[] {R.id.list_item_image, R.id.list_item_text},
        		imageLoader, options);
        
        setListAdapter(adapter);
        startManagingCursor(cursor);

        Intent intent = getIntent();
        if (intent.hasExtra("threadNo")) {
            setBoardCode(intent.getStringExtra("boardCode"));
            threadNo = intent.getIntExtra("threadNo", 0);
        }
        else {
            boardCode = "sp";
            threadNo = 26837084;
        }

        lastUpdate = new Date().getTime();
        
        Thread thread = new Thread()
        {
            @Override
            public void run() {
                chanThread = new ChanThread();
            	chanThread.cursor = cursor;
                chanThread.loadChanThread(handler, boardCode, threadNo);
                refresh();
            }
            
            private void refresh() {
                runOnUiThread(new Runnable() {
                	@Override
                    public void run() {
                        // Bind to our new adapter.
                		Log.i(TAG, "Data loaded ...");
                		adapter.notifyDataSetChanged();
                		getListView().requestLayout();
                	}
                });
            }
        };
        thread.start();
        
        // We'll define a custom screen layout here (the one shown above), but
        // typically, you could just use the standard ListActivity layout.
        setContentView(R.layout.thread_activity_list_layout);

        // Query for all people contacts using the Contacts.People convenience class.
        // Put a managed wrapper around the retrieved cursor so we don't have to worry about
        // requerying or closing it as the activity changes state.
        startManagingCursor(cursor);
        

        // Now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        adapter = new MyCursorAdapter(this,
                R.layout.thread_activity_list_item,
                cursor,
                new String[] {"image_url", "text"},
                new int[] {R.id.list_item_image, R.id.list_item_text},
                imageLoader, options);

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

    @Override
    public void onItemClick(android.widget.AdapterView<?> adapterView, android.view.View view, int position, long id) {
        if (chanThread == null || chanThread.posts == null) {
            return;
        }

        int postNo = (position == 0)
                ? chanThread.no
                : chanThread.posts.get(position - 1).no;

        String imageUrl = (position == 0)
                ? chanThread.getImageUrl()
                : chanThread.posts.get(position - 1).getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        Intent intent = new Intent(this, FullScreenImageActivity.class);
        intent.putExtra("boardCode", chanThread.board);
        intent.putExtra("threadNo", chanThread.no);
        intent.putExtra("postNo",postNo);
        intent.putExtra("imageUrl", imageUrl);
        startActivity(intent);
    }

    private void setBoardCode(String code) {
        boardCode = code;
        if (getActionBar() != null) {
            getActionBar().setTitle("/" + boardCode + " thread");
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


}
