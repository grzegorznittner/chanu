package com.chanapps.four.test;

import android.app.Activity;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import com.chanapps.four.data.ChanThread;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.util.Date;
import java.util.List;

public class FullScreenImageActivity extends Activity implements View.OnClickListener {

	public static final String TAG = FullScreenImageActivity.class.getSimpleName();

	ImageLoader imageLoader = null;
    DisplayImageOptions options = null;

    String boardCode = null;
    int threadNo = 0;
    String imageUrl = null;

    ImageView imageView = null;

    FullScreenImageActivity me = this;

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
        if (!intent.hasExtra("imageUrl")) {
            setBoardCode("trv");
            threadNo = 609350;
            imageUrl = "http://images.4chan.org/trv/src/1341267758351.png";
        }
        else {
            setBoardCode(intent.getStringExtra("boardCode"));
            threadNo = intent.getIntExtra("threadNo", 0);
            imageUrl = intent.getStringExtra("imageUrl");
        }

        Thread thread = new Thread()
        {
            @Override
            public void run() {
                refresh();
            }

            private void refresh() {
                runOnUiThread(new Runnable() {
                	@Override
                    public void run() {
                        imageLoader.displayImage(imageUrl, imageView);
                	}
                });
            }
        };
        thread.start();

        // We'll define a custom screen layout here (the one shown above), but
        // typically, you could just use the standard ListActivity layout.
        setContentView(R.layout.full_screen_image_activity_layout);

        imageView = (ImageView)findViewById(R.id.full_screen_image);
        imageView.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void navigateUp() {
        Intent upIntent = new Intent(this, ThreadListActivity.class);
        upIntent.putExtra("boardCode", boardCode);
        upIntent.putExtra("threadNo", threadNo);
        NavUtils.navigateUpTo(this, upIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.full_screen_image_menu, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        navigateUp();
    }

    private void setBoardCode(String code) {
        boardCode = code;
        if (getActionBar() != null) {
            getActionBar().setTitle("/" + boardCode + " image");
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


}
