package com.chanapps.four.test;

import java.io.InputStream;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import com.chanapps.four.data.ChanDatabaseHelper;

public class FullScreenImageActivity extends Activity {

	public static final String TAG = FullScreenImageActivity.class.getSimpleName();

    WebView webView = null;

    String boardCode = null;
    int threadNo = 0;
    String imageUrl = null;
    int imageWidth = 0;
    int imageHeight = 0;

    private void randomizeImage() {
        double d = Math.random();
        if (d >= 0.75) {
            setBoardCode("trv");
            threadNo = 609350;
            imageUrl = "http://images.4chan.org/trv/src/1341267758351.png";
            imageWidth = 280;
            imageHeight = 280;
        }
        else if (d >= 0.5) {
            setBoardCode("diy");
            threadNo = 100304;
            imageUrl = "http://images.4chan.org/diy/src/1324490988301.jpg";
            imageWidth = 324;
            imageHeight = 433;
        }
        else if (d >= 0.25) {
            setBoardCode("fit");
            threadNo = 4820056;
            imageUrl = "http://images.4chan.org/fit/src/1286894765253.jpg";
            imageWidth = 368;
            imageHeight = 600;
        }
        else {
            setBoardCode("po");
            threadNo = 430177;
            imageUrl = "http://images.4chan.org/po/src/1304652991998.jpg";
            imageWidth = 652;
            imageHeight = 433;
        }
    }
    
    private boolean loadPostData() {
    	ChanDatabaseHelper h = new ChanDatabaseHelper(getBaseContext());
		try {
			// "http://images.4chan.org/" + board + "/src/" + tim + ext;
			String query = "SELECT " + ChanDatabaseHelper.POST_ID + ", "
					+ "'http://images.4chan.org/' || " + ChanDatabaseHelper.POST_BOARD_NAME
						+ " || '/src/' || " + ChanDatabaseHelper.POST_TIM
						+ " || " + ChanDatabaseHelper.POST_EXT + " 'imageurl', "
					+ ChanDatabaseHelper.POST_W + " 'imagewidth', "
					+ ChanDatabaseHelper.POST_H + " 'imageheight'"
					+ " FROM " + ChanDatabaseHelper.POST_TABLE
					+ " WHERE " + ChanDatabaseHelper.POST_ID + "=" + threadNo;
			Cursor c = h.getWritableDatabase().rawQuery(query, null);
			int imageurlIdx = c.getColumnIndex("imageurl");
			int imagewidthIdx = c.getColumnIndex("imagewidth");
			int imageheightIdx = c.getColumnIndex("imageheight");
			if (c.moveToFirst()) {
				imageUrl = c.getString(imageurlIdx);
				imageWidth = c.getInt(imagewidthIdx);
				imageHeight = c.getInt(imageheightIdx);
				if (c.moveToNext()) {
					Log.w(TAG, "Post with id " + threadNo + " for board " + boardCode + " is not unique across boards!");
				}
			} else {
				Log.w(TAG, "Post with id " + threadNo + " for board " + boardCode + " has not been found!");
			}
			c.close();
			h.close();
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error querying chan DB. " + e.getMessage(), e);
		}
		return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.hasExtra("threadNo")) {
            setBoardCode(intent.getStringExtra("boardCode"));
            threadNo = intent.getIntExtra("threadNo", 0);
            loadPostData();
        } else {
            randomizeImage();
        }
        
//        else {
//            setBoardCode(intent.getStringExtra("boardCode"));
//            threadNo = intent.getIntExtra("threadNo", 0);
//            imageUrl = intent.getStringExtra("imageUrl");
//            imageWidth = intent.getIntExtra("imageWidth", 280);
//            imageHeight = intent.getIntExtra("imageHeight", 280);
//        }

        webView = new WebView(this);
        setContentView(webView);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        //webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        setDefaultZoom();
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

    private void setDefaultZoom() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        double screenWidth = getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT
            ? displayMetrics.widthPixels
            : displayMetrics.heightPixels;
        double screenHeight  = getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT
            ? displayMetrics.heightPixels
            : displayMetrics.widthPixels;
        double trialWidth = imageWidth;
        double trialHeight = imageHeight;
        Log.e(TAG, "screenWidth,screenHeight = " + screenWidth + ", " + screenHeight);
        Log.e(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialWidth > screenWidth) { // need to scale width down
            double scale = screenWidth / trialWidth;
            trialWidth = screenWidth;
            trialHeight = (int)(Math.floor(scale * trialHeight));
        }
        Log.e(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialHeight > screenHeight) { // need to scale height down
            double scale = screenHeight / trialHeight;
            trialWidth = (int)(Math.floor(scale * trialWidth));
            trialHeight = screenHeight;
        }
        Log.e(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialWidth < screenWidth) { // try and scale up to width
            double scale = screenWidth / trialWidth;
            Log.e(TAG, "scale = " + scale);
            int testHeight = (int)(Math.floor(scale * trialHeight));
            Log.e(TAG, "testHeight = " + testHeight);
            if (testHeight <= screenHeight) {
                trialWidth = screenWidth;
                trialHeight = testHeight;
            }
        }
        Log.e(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialHeight < screenHeight) { // try and scale up to height
            double scale = screenHeight / trialHeight;
            int testWidth = (int)(Math.floor(scale * trialWidth));
            if (testWidth <= screenWidth) {
                trialWidth = testWidth;
                trialHeight = screenHeight;
            }
        }
        Log.e(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        int initialScalePct = (int)Math.floor(100 * screenWidth / imageWidth);
        webView.setInitialScale(initialScalePct);
        Log.e(TAG, "initial Scale = " + initialScalePct);
    }

    private void loadImage() {
        //String html = "<html style=\"" +
        //        "background: #000 url(" + imageUrl + ") no-repeat center center;" +
        //        "\" />" +
        //        "</html>";
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
                SaveImageTask saveTask = new SaveImageTask(getApplicationContext());
                saveTask.execute(imageUrl);
                return true;
            case R.id.set_as_wallpaper_menu:
                Toast.makeText(getApplicationContext(), "Setting wallpaper...", Toast.LENGTH_SHORT).show();
                SetImageAsWallpaperTask wallpaperTask = new SetImageAsWallpaperTask(getApplicationContext());
                wallpaperTask.execute(imageUrl);
                return true;
            case R.id.share_image_menu:
                ShareImageTask shareImageTask = new ShareImageTask(getApplicationContext());
                shareImageTask.execute(imageUrl);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String saveImage(String url) {
        final int IO_BUFFER_SIZE = 4 * 1024;

        final HttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        String result = "Couldn't save image, try later";
        InputStream inputStream = null;
        HttpEntity entity = null;
        Bitmap bitmap = null;
        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w(TAG, "Error " + statusCode +
                        " while retrieving image from " + url);
                return result;
            }
            entity = response.getEntity();
            if (entity == null) {
                return result;
            }
            inputStream = entity.getContent();
            bitmap = BitmapFactory.decodeStream(inputStream);
            String title = imageUrl.replaceAll(".*/", "").replaceAll("\\..*", "");
            String description = "Downloaded from 4chan URL: " + imageUrl;
            /*
            String sdPath = "/4channer/images/" + boardCode + "/" + threadNo;
            String storageDir = Environment.getExternalStorageDirectory() + sdPath;
            File storagePath = new File(storageDir);
            storagePath.mkdirs();
            File file = new File(storagePath, url);
            file.createNewFile();
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            FileOutputStream fileOutput = new FileOutputStream(file);
            while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                fileOutput.write(buffer, 0, bufferLength);
            }
            fileOutput.close();
            MediaStore.Images.Media.insertImage(getContentResolver(), file.getCanonicalPath(), title, description);
            */
            MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, title, description);
            result = "Image saved";
        }
        catch (Exception e) {
            getRequest.abort();
            Log.e(TAG, "Error while retrieving bitmap from " + url, e);
        }
        finally {
            try {
                if ((client instanceof AndroidHttpClient)) {
                    ((AndroidHttpClient) client).close();
                }
                if (bitmap != null) {
                    bitmap.recycle();
                }
                 /*
                if (inputStream != null) {
                    inputStream.close();
                } */
                //entity.consumeContent();
            }
            catch (Exception e) {
                Log.e(TAG, "Exception during finally block of image download", e);
            }
        }
        return result;
    }

    private class SaveImageTask extends AsyncTask<String, Void, String> {
        private String url;
        private Context context;

        public SaveImageTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            url = params[0];
            return saveImage(url);
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }

    private class SetImageAsWallpaperTask extends AsyncTask<String, Void, String> {
        private String url;
        private Context context;

        public SetImageAsWallpaperTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            url = params[0];
            return setImageAsWallpaper(url);
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }

    private class ShareImageTask extends AsyncTask<String, Void, String> {
        private String url;
        private Context context;

        public ShareImageTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            url = params[0];
            return shareImage(url);
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }

    private String setImageAsWallpaper(String url) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
        String result = "";
        try {
            InputStream ins = new URL(url).openStream();
            wallpaperManager.setStream(ins);
            result = "Wallpaper set";
            Log.i(TAG, result);
        }
        catch (Exception e) {
            result = "Couldn't set wallpaper, try later";
            Log.e(TAG, "Couldn't set wallpaper", e);
        }
        return result;
    }

    private String shareImage(String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        String result = "Couldn't share image, try later";
        try {
/*
            URL imageUrl = new URL(url);
            URLConnection connection = imageUrl.openConnection();
            String contentType = connection.getContentType();
            InputStream ins = connection.getInputStream();
            intent.setType(contentType);
*/
            intent.putExtra(Intent.EXTRA_STREAM, url);
            startActivity(Intent.createChooser(intent, "Share Image"));
            result = "Image shared";
            Log.i(TAG, result);
        }
        catch (Exception e) {
            result = "Sharing error, try later";
            Log.e(TAG, "Couldn't share image", e);
        }
        return result;
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
