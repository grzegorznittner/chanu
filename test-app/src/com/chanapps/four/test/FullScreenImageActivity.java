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
import android.content.SharedPreferences;
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
import com.chanapps.four.data.ChanHelper;

public class FullScreenImageActivity extends Activity {

	public static final String TAG = FullScreenImageActivity.class.getSimpleName();

	private WebView webView = null;

    private Context ctx;

	private SharedPreferences prefs = null;

    private Intent intent;

    private String boardCode = null;
    private long threadNo = 0;
    private long postNo = 0;
    private String imageUrl = null;
    private int imageWidth = 0;
    private int imageHeight = 0;

    private void randomizeImage() {
        double d = Math.random();
        if (d >= 0.75) {
            setBoardCode("trv");
            postNo = 609350;
            imageUrl = "http://images.4chan.org/trv/src/1341267758351.png";
            imageWidth = 280;
            imageHeight = 280;
        }
        else if (d >= 0.5) {
            setBoardCode("diy");
            postNo = 100304;
            imageUrl = "http://images.4chan.org/diy/src/1324490988301.jpg";
            imageWidth = 324;
            imageHeight = 433;
        }
        else if (d >= 0.25) {
            setBoardCode("fit");
            postNo = 4820056;
            imageUrl = "http://images.4chan.org/fit/src/1286894765253.jpg";
            imageWidth = 368;
            imageHeight = 600;
        }
        else {
            setBoardCode("po");
            postNo = 430177;
            imageUrl = "http://images.4chan.org/po/src/1304652991998.jpg";
            imageWidth = 652;
            imageHeight = 433;
        }
    }

    private boolean loadChanPostData() {
    	ChanDatabaseHelper h = new ChanDatabaseHelper(getBaseContext());
    	Cursor c = null;
		try {
			String query = "SELECT " + ChanDatabaseHelper.POST_ID + ", "
					+ "'http://images.4chan.org/' || " + ChanDatabaseHelper.POST_BOARD_NAME
						+ " || '/src/' || " + ChanDatabaseHelper.POST_TIM
						+ " || " + ChanDatabaseHelper.POST_EXT + " 'imageurl', "
					+ ChanDatabaseHelper.POST_W + ", "
					+ ChanDatabaseHelper.POST_H
					+ " FROM " + ChanDatabaseHelper.POST_TABLE
					+ " WHERE " + ChanDatabaseHelper.POST_ID + "=" + postNo;
			c = h.getWritableDatabase().rawQuery(query, null);
			int imageurlIdx = c.getColumnIndex("imageurl");
//			int imagewidthIdx = c.getColumnIndex(ChanDatabaseHelper.POST_W);
//			int imageheightIdx = c.getColumnIndex(ChanDatabaseHelper.POST_H);
			if (c.moveToFirst()) {
				imageUrl = c.getString(imageurlIdx);
//				imageWidth = c.getInt(imagewidthIdx);
//				imageHeight = c.getInt(imageheightIdx);
				if (c.moveToNext()) {
					Log.w(TAG, "Post with id " + postNo + " for board " + boardCode + " is not unique across boards!");
				}
			} else {
				Log.w(TAG, "Post with id " + postNo + " for board " + boardCode + " has not been found!");
			}
			c.close();
			h.close();
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error querying chan DB. " + e.getMessage(), e);
		} finally {
			if (c != null) {
				c.close();
			}
			if (h != null) {
				h.close();
			}
		}
		return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        ctx = getApplicationContext();
        prefs = getSharedPreferences(ChanHelper.PREF_NAME, 0);

        webView = new WebView(this);
        setContentView(webView);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        //webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.setBackgroundColor(Color.BLACK);
        /*
        final Activity activity = this;
        webView.setWebViewClient(new WebViewClient() {
          public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(activity, "Couldn't get image, try later" + description, Toast.LENGTH_SHORT).show();
          }
        });
        */
    }

    @Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart");
        loadPrefs();
    }

    private void loadPrefs() {
        if (intent == null || intent != getIntent()) {
            intent = getIntent();
            if (intent.hasExtra(ChanHelper.POST_NO)) {
                postNo = intent.getLongExtra(ChanHelper.POST_NO, 0);
                threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
                setBoardCode(intent.getStringExtra(ChanHelper.BOARD_CODE));
                imageWidth = intent.getIntExtra(ChanHelper.IMAGE_WIDTH, 0);
                imageHeight = intent.getIntExtra(ChanHelper.IMAGE_HEIGHT, 0);
                Log.i(TAG, "Loaded from intent, boardCode: " + boardCode + ", threadNo: " + threadNo + ", postNo: " + postNo);
                savePrefs();
            } else {
                randomizeImage();
            }
        }
        else {
            boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
            threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
            postNo = prefs.getLong(ChanHelper.POST_NO, 0);
            imageWidth = prefs.getInt(ChanHelper.IMAGE_WIDTH, 0);
            imageHeight = prefs.getInt(ChanHelper.IMAGE_HEIGHT, 0);
            Log.i(TAG, "Post no " + postNo + " laoded from preferences");
        }
        loadChanPostData();
    }

    private void savePrefs() {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(ChanHelper.BOARD_CODE, boardCode);
        ed.putLong(ChanHelper.THREAD_NO, threadNo);
        ed.putLong(ChanHelper.POST_NO, postNo);
        ed.putInt(ChanHelper.IMAGE_WIDTH, imageWidth);
        ed.putInt(ChanHelper.IMAGE_HEIGHT, imageHeight);
        Log.i(TAG, "Stored in prefs, post no: " + postNo);
        ed.commit();
    }

    @Override
    protected void onStop () {
    	super.onStop();
        savePrefs();
    	Log.i(TAG, "onStop");
    }

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "onRestart");
	}

    @Override
	protected void onResume () {
		super.onResume();
		Log.i(TAG, "onResume");
        setDefaultZoom();
        loadImage();
	}
	
	public void onWindowFocusChanged (boolean hasFocus) {
		Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        savePrefs();
        super.onPause();
        savePrefs();
        Log.i(TAG, "onPause");
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setDefaultZoom();
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
                Toast.makeText(ctx, R.string.full_screen_saving_image, Toast.LENGTH_SHORT).show();
                SaveImageTask saveTask = new SaveImageTask(getApplicationContext());
                saveTask.execute(imageUrl);
                return true;
            case R.id.set_as_wallpaper_menu:
                Toast.makeText(ctx, R.string.full_screen_setting_wallpaper, Toast.LENGTH_SHORT).show();
                SetImageAsWallpaperTask wallpaperTask = new SetImageAsWallpaperTask(getApplicationContext());
                wallpaperTask.execute(imageUrl);
                return true;
            case R.id.share_image_menu:
                ShareImageTask shareImageTask = new ShareImageTask(getApplicationContext());
                shareImageTask.execute(imageUrl);
                return true;
            case R.id.settings_menu:
                Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.raw.help_header, R.raw.help_full_screen);
                rawResourceDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String saveImage(String url) {
        final int IO_BUFFER_SIZE = 4 * 1024;

        final HttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        String result = ctx.getString(R.string.full_screen_save_image_error);
        InputStream inputStream = null;
        HttpEntity entity = null;
        Bitmap bitmap = null;
        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w(TAG, "Error " + statusCode + " while retrieving image from " + url);
                return result;
            }
            entity = response.getEntity();
            if (entity == null) {
                return result;
            }
            inputStream = entity.getContent();
            bitmap = BitmapFactory.decodeStream(inputStream);
            String title = imageUrl.replaceAll(".*/", "").replaceAll("\\..*", "");
            String description = ctx.getString(R.string.full_screen_download_description) + " " + imageUrl;
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
            result = ctx.getString(R.string.full_screen_image_saved);
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
            result = ctx.getString(R.string.full_screen_wallpaper_set);
            Log.i(TAG, result);
        }
        catch (Exception e) {
            result = ctx.getString(R.string.full_screen_wallpaper_error);
            Log.e(TAG, result, e);
        }
        return result;
    }

    private String shareImage(String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        String msg;
        try {
/*
            URL imageUrl = new URL(url);
            URLConnection connection = imageUrl.openConnection();
            String contentType = connection.getContentType();
            InputStream ins = connection.getInputStream();
            intent.setType(contentType);
*/
            intent.putExtra(Intent.EXTRA_STREAM, url);
            startActivity(Intent.createChooser(intent, ctx.getString(R.string.full_screen_share_image_intent)));
            msg = ctx.getString(R.string.full_screen_image_shared);
        }
        catch (Exception e) {
            msg = ctx.getString(R.string.full_screen_sharing_error);
            Log.e(TAG, msg, e);
        }
        return msg;
    }

    private void navigateUp() {
        Intent upIntent = new Intent(this, ThreadListActivity.class);
        upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        upIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
        upIntent.putExtra(ChanHelper.POST_NO, postNo);
        NavUtils.navigateUpTo(this, upIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.full_screen_image_menu, menu);
        return true;
    }

    private void setBoardCode(String code) {
        boardCode = code;
        if (getActionBar() != null) {
            getActionBar().setTitle("/" + boardCode + " " + getString(R.string.full_screen_image_activity));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

}
