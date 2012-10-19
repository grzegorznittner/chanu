package com.chanapps.four.test;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FullScreenImageActivity extends Activity {

	public static final String TAG = FullScreenImageActivity.class.getSimpleName();

    WebView webView = null;

    String boardCode = null;
    int threadNo = 0;
    String imageUrl = null;
    int imageWidth = 0;
    int imageHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (!intent.hasExtra("imageUrl")) {
            setBoardCode("trv");
            threadNo = 609350;
            imageUrl = "http://images.4chan.org/trv/src/1341267758351.png";
            imageWidth = 280;
            imageHeight = 280;
        }
        else {
            setBoardCode(intent.getStringExtra("boardCode"));
            threadNo = intent.getIntExtra("threadNo", 0);
            imageUrl = intent.getStringExtra("imageUrl");
            imageWidth = intent.getIntExtra("imageWidth", 280);
            imageHeight = intent.getIntExtra("imageHeight", 280);
        }

        webView = new WebView(this);
        setContentView(webView);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        //webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        //webView.setBackgroundColor(0);
        /*
        final Activity activity = this;
        webView.setWebViewClient(new WebViewClient() {
          public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(activity, "Couldn't get image, try later" + description, Toast.LENGTH_SHORT).show();
          }
        });
        */
        loadImage();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        loadImage();
    }

    public void loadImage() {
        //DisplayMetrics displayMetrics = new DisplayMetrics();
        //getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //int screenHeight  = getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT
        //        ? displayMetrics.heightPixels
        //        : displayMetrics.widthPixels;
        //int vSpace = screenHeight / 2 - imageHeight / 2;
        // String html = "<html><body><center><img src=\"" + imageUrl +  "\" vspace=\"" + vSpace + "\"/></center></body></html>";

        int halfWidth = imageWidth / 2;
        int halfHeight = imageHeight / 2;
        String html = "<html style=\"" +
                //"width: 100%; height: 100%;" +
                "background: #000 url(" + imageUrl + ") no-repeat center center;" +
                "\" />" +
                //"<body>" +
                //"<div style=\"" +
                //"position: absolute; " +
                //"top: 50%; " +
                //"left: 50%; " +
                //"margin-top: -" + halfHeight +"px; " +
                //"margin-left: -" + halfWidth + "px; " +
                //"width:" + imageWidth + "px; " +
                //"height: " + imageHeight + "px; " +
                //"background: url(" + imageUrl + ") center center no-repeat;" +
                //"\" />" +
                //"<img " +
                //"style=\"left: 50%; top: 50%; margin-left:-" + halfWidth + "px; margin-top:-" + halfHeight + "px;\" " +
                //"style=\"display: block; margin: auto;\" " +
                //"src=\"" + imageUrl + "\"/>" +
                //"</body>" +
                "</html>";

        //webView.loadData(html, "text/html", "UTF-8");
        webView.setBackgroundColor(Color.BLACK);
        webView.loadUrl(imageUrl);

    }

    public int getScreenOrientation()
    {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int orientation;
        if (displayMetrics.widthPixels == displayMetrics.heightPixels) {
            orientation = Configuration.ORIENTATION_SQUARE;
        }
        else {
            if(displayMetrics.widthPixels < displayMetrics.heightPixels) {
                orientation = Configuration.ORIENTATION_PORTRAIT;
            }
            else {
                 orientation = Configuration.ORIENTATION_LANDSCAPE;
            }
        }
        return orientation;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUp();
                return true;
            case R.id.download_image_menu:
                Toast.makeText(getApplicationContext(), "Saving image...", Toast.LENGTH_SHORT).show();
                downloadImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void downloadImage() {

        try {
            String storageDir = Environment.getExternalStorageDirectory() + "/com.chanapps.4channer/images/";
            File storagePath = new File(storageDir);
            storagePath.mkdirs();
            File file = new File(storagePath, imageUrl);

            URL url = new URL(imageUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.connect();
            FileOutputStream fileOutput = new FileOutputStream(file);
            InputStream inputStream = urlConnection.getInputStream();
            int downloadedSize = 0;

            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutput.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
            }
            fileOutput.close();
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Download error, try later", Toast.LENGTH_SHORT).show();
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

    /*
    @Override
    public void onClick(View view) {
        navigateUp();
    }
    */

    private void setBoardCode(String code) {
        boardCode = code;
        if (getActionBar() != null) {
            getActionBar().setTitle("/" + boardCode + " image");
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


}
