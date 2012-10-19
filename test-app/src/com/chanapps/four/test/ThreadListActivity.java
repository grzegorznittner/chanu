package com.chanapps.four.test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.chanapps.four.data.ChanThread;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class ThreadListActivity extends ListActivity {

	public static final String TAG = ThreadListActivity.class.getSimpleName();
	
	public static class MyCursorAdapter extends SimpleCursorAdapter {
		ImageLoader imageLoader = null;
	    DisplayImageOptions options = null;
	    ThreadListActivity activity = null;

		public MyCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
				ImageLoader imageLoader, DisplayImageOptions options) {
			super(context, layout, c, from, to);
			this.imageLoader = imageLoader;
			this.options = options;
		}

        @Override
        public void setViewText(TextView textView, String text) {
            Cursor cursor = getCursor();
            if (cursor == null ||
                    activity == null ||
                    activity.chanThread == null ||
                    activity.chanThread.thumbnailToPointMap == null) {
                return;
            }
            String thumbnailImageUrl = cursor.getString(cursor.getColumnIndex("image_url"));
            final Point imageDimensions = thumbnailImageUrl != null
                ? activity.chanThread.thumbnailToPointMap.get(thumbnailImageUrl)
                    : null;
            if (imageDimensions != null && imageDimensions.x > 0 && imageDimensions.y > 0) {
                FlowTextHelper.tryFlowText(text, imageDimensions, textView);
            }
            else {
                textView.setText(text);
            }
            //textView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        @Override
        public void setViewImage(ImageView imageView, final String thumbnailImageUrl) {
            try {
                this.imageLoader.displayImage(thumbnailImageUrl, imageView, options);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (activity == null || activity.chanThread == null || activity.chanThread.thumbnailToImageMap == null) {
                            return;
                        }
                        String fullImageUrl = activity.chanThread.thumbnailToImageMap.get(thumbnailImageUrl);
                        if (fullImageUrl == null || fullImageUrl.trim().length() == 0) {
                            return;
                        }
                        Point fullImageDimensions = activity.chanThread.thumbnailToFullPointMap.get(thumbnailImageUrl);
                        int imageWidth = fullImageDimensions != null ? fullImageDimensions.x : 0;
                        int imageHeight = fullImageDimensions != null ? fullImageDimensions.y : 0;
                        //Toast.makeText(view.getContext(), "Loading image " + fullImageUrl, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(activity, FullScreenImageActivity.class);
                        intent.putExtra("boardCode", activity.chanThread.board);
                        intent.putExtra("threadNo", activity.chanThread.no);
                        intent.putExtra("imageUrl", fullImageUrl);
                        intent.putExtra("imageWidth", imageWidth);
                        intent.putExtra("imageHeight", imageHeight);
                        activity.startActivity(intent);
                    }
                });
            } catch (NumberFormatException nfe) {
                imageView.setImageURI(Uri.parse(thumbnailImageUrl));
            }
        }
    }

    ChanThread chanThread = null;
	ImageLoader imageLoader = null;
    DisplayImageOptions options = null;
    MyCursorAdapter adapter = null;
    ThreadLoader threadLoader = null;
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
        adapter.activity = this;

        setListAdapter(adapter);
        startManagingCursor(cursor);

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

        lastUpdate = new Date().getTime();

        threadLoader = new ThreadLoader();
        threadLoader.start();

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
            case R.id.refresh_thread_menu:
                if (threadLoader != null) {
                    Toast.makeText(getApplicationContext(), "Refreshing...", Toast.LENGTH_SHORT).show();
                    threadLoader.refresh();
                }
                return true;
            case R.id.post_new_picture_menu:
                Toast.makeText(getApplicationContext(), "New Picture", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.post_new_message_menu:
                Toast.makeText(getApplicationContext(), "New Message", Toast.LENGTH_SHORT).show();
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
            getActionBar().setTitle("/" + boardCode + " thread");
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private class ThreadLoader extends Thread {
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
    }
}
