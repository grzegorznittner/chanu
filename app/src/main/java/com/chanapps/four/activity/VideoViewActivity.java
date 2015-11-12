/**
 * 
 */
package com.chanapps.four.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;

import com.chanapps.four.service.ImageDownloadService;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class VideoViewActivity extends Activity {
	public static final String TAG = "VideoView";
    private static final boolean DEBUG = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.video_view_layout);
    	WebView myWebView = (WebView) findViewById(R.id.video_view);
    	myWebView.getRootView().setBackgroundColor(0xffffff);
    	getWindow().getDecorView().setBackgroundColor(0xffffff);

    	setActionBarTitle();
    }
    
    @Override
	protected void onResume () {
    	super.onResume();
    	play();
    }
    
    private void setActionBarTitle() {
        if (getActionBar() != null) {
            getActionBar().setTitle(getIntent().getStringExtra(Intent.EXTRA_TITLE));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void play() {
    	try {
	    	WebView myWebView = (WebView) findViewById(R.id.video_view);
	    	myWebView.getRootView().setBackgroundColor(0xffffff);
	    	myWebView.setBackgroundColor(0xffffff);
	    	myWebView.getSettings().setJavaScriptEnabled(false);
	    	myWebView.getSettings().setBuiltInZoomControls(false);
	    	String html = "<html><body bgcolor=\"black\"><center><img src=\"" + getIntent().getStringExtra(ImageDownloadService.IMAGE_URL)
	    			+ "\"></img></body></html>";
	    	myWebView.loadDataWithBaseURL("/", html, "text/html", "UTF-8", null);
    	} catch (Throwable e) {
    		Log.e(TAG, "Web view loading error", e);
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	onBackPressed();
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

}
