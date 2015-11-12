package com.chanapps.four.gallery;

import com.chanapps.four.activity.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

public class GalleryFrameLayout extends FrameLayout {
    public GalleryFrameLayout(Context context) {
        super(context);
    }
    
    public GalleryFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GalleryFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
    	WebView myWebView = (WebView) findViewById(com.chanapps.four.activity.R.id.video_view);
    	String html = "<html><body bgcolor=\"black\"></body></html>";
    	myWebView.loadDataWithBaseURL("/", html, "text/html", "UTF-8", null);
    	
    	View view = findViewById(R.id.gifview);
    	if (view.getVisibility() != View.GONE) {
    		view.setVisibility(View.GONE);
    	}
		return false;
    }

}
