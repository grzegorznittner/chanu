package com.chanapps.four.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.utils.StorageUtils;

public class FullScreenImageActivity extends Activity {

	public static final String TAG = FullScreenImageActivity.class.getSimpleName();

	private WebView webView = null;

    private Context ctx;

	private SharedPreferences prefs = null;

    private Intent intent;

    private String boardCode = null;
    private long threadNo = 0;
    private long postNo = 0;
    private ChanPost post = null;
    private String imageUrl = null;
    private String localImageUri = null;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private DisplayImageOptions options;
    private ImageLoader imageLoader;
    private LayoutInflater inflater;
    private View loadingView;
    protected Handler handler;
    private long downloadEnqueueId;
    private DownloadManager dm;
    private BroadcastReceiver receiver;

    private boolean loadChanPostData() {
		try {
			ChanThread thread = ChanFileStorage.loadThreadData(getBaseContext(), boardCode, threadNo);
			for (ChanPost post : thread.posts) {
				if (post.no == postNo) {
					imageUrl = "http://images.4chan.org/" + thread.board + "/src/" + post.tim + post.ext;
					this.post = post;
					return true;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error load post data. " + e.getMessage(), e);
		}
		return false;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        ctx = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        //webView.setBackgroundColor(Color.BLACK);
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));
        options = new DisplayImageOptions.Builder()
			.cacheOnDisc()
			.imageScaleType(ImageScaleType.EXACT)
			.build();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                	Cursor c = null;
                	try {
	                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
	                    Query query = new Query();
	                    query.setFilterById(downloadEnqueueId);
	                    c = dm.query(query);
	                    if (c.moveToFirst()) {
	                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
	                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
	                        	localImageUri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
	                        	
	                        	ParcelFileDescriptor parcel = dm.openDownloadedFile(downloadEnqueueId);
	                        	copyImageFileToBoardFolder(parcel);
	                        	showImage();
	                        }
	                    }
                	} catch (Exception e) {
                		Log.e(TAG, "Error storing downloaded file", e);
                	} finally {
                		c.close();
                	}
                }
                unregisterReceiver(this);
            }

        };
    }

	private void copyImageFileToBoardFolder(ParcelFileDescriptor parcel) throws FileNotFoundException, IOException {
		InputStream downloadedFile = null;
		OutputStream newFile = null;
		try {
			downloadedFile = new FileInputStream(parcel.getFileDescriptor());
			String imageFile = getLocalImagePath(getCacheFolder());
			Log.i(TAG, "Image downloaded, copying to " + imageFile);
			newFile = new FileOutputStream(new File(URI.create(imageFile)), false);

		    byte[] buffer = new byte[1024];
		    int length;
		    while((length = downloadedFile.read(buffer)) > 0) {
		        newFile.write(buffer, 0, length);
		    }
		    localImageUri = imageFile;
		    Log.i(TAG, "Image file copied to " + imageFile);
		    dm.remove(downloadEnqueueId);
		} finally {
			if (downloadedFile != null) {
				downloadedFile.close();
			}
			if (newFile != null) {
				newFile.close();
			}
		}
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
                Log.e(TAG, "Intent received without postno");
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
		loadOrShowImage();
	}
	
	public void onWindowFocusChanged (boolean hasFocus) {
		Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        savePrefs();
        super.onPause();
        savePrefs();
        dm.remove(downloadEnqueueId);
        Log.i(TAG, "onPause - removing download id " + downloadEnqueueId);
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
   		// checking filesystem if image file is available
    	loadOrShowImage();
    }
    
    private void loadOrShowImage() {
    	localImageUri = checkLocalImage();
    	
    	if (localImageUri != null) {
    		showImage();
    	} else {
    		loadImage();
    	}
    }

	private String checkLocalImage() {
		String cacheFolder = getCacheFolder();
    	String localImageUri = getLocalImagePath(cacheFolder);
    	try {
	    	File localImage = new File(URI.create(localImageUri));
	    	if (localImage.exists()) {
//	    		if (localImage.length() == post.fsize) {
//	    			Log.i(TAG, "Image " + localImageUri + " already exists and has correct size");
	    			return localImageUri;
//	    		} else {
//	    			Log.i(TAG, "Image " + localImageUri + " available but size is not correct, should be: " + post.fsize
//	    					+ ", current file size: " + localImage.length());
//	    			//localImage.delete();
//	    		}
	    	} else {
	    		Log.i(TAG, "Image " + localImageUri + " doesn't exist");
	    	}
    	} catch (Exception e) {
    		Log.e(TAG, "Image file checking error", e);
    	}
    	return null;
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
        Log.v(TAG, "screenWidth,screenHeight = " + screenWidth + ", " + screenHeight);
        Log.v(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialWidth > screenWidth) { // need to scale width down
            double scale = screenWidth / trialWidth;
            trialWidth = screenWidth;
            trialHeight = (int)(Math.floor(scale * trialHeight));
        }
        Log.v(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialHeight > screenHeight) { // need to scale height down
            double scale = screenHeight / trialHeight;
            trialWidth = (int)(Math.floor(scale * trialWidth));
            trialHeight = screenHeight;
        }
        Log.v(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialWidth < screenWidth) { // try and scale up to width
            double scale = screenWidth / trialWidth;
            Log.v(TAG, "scale = " + scale);
            int testHeight = (int)(Math.floor(scale * trialHeight));
            Log.v(TAG, "testHeight = " + testHeight);
            if (testHeight <= screenHeight) {
                trialWidth = screenWidth;
                trialHeight = testHeight;
            }
        }
        Log.v(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialHeight < screenHeight) { // try and scale up to height
            double scale = screenHeight / trialHeight;
            int testWidth = (int)(Math.floor(scale * trialWidth));
            if (testWidth <= screenWidth) {
                trialWidth = testWidth;
                trialHeight = screenHeight;
            }
        }
        Log.v(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        int initialScalePct = (int)Math.floor(100 * screenWidth / imageWidth);
        webView.setInitialScale(initialScalePct);
        Log.v(TAG, "initial Scale = " + initialScalePct);
    }
    
    private void showImage() {
    	Log.i(TAG, "Displaing image " + localImageUri);
    	webView = new WebView(this);
        setContentView(webView);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        webView.getSettings().setBuiltInZoomControls(true);
        
        setDefaultZoom();
        
        webView.loadUrl(localImageUri);
    }

    private void loadImage() {
    	this.localImageUri = null;
    	Log.i(TAG, "Loading image from URL: " + imageUrl);
    	registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    	
        Request request = new Request(Uri.parse(imageUrl));
        downloadEnqueueId = dm.enqueue(request);
        Log.i(TAG, "Equeuing image " + imageUrl + " in DM, id " + downloadEnqueueId);

		String thumbUrl = "http://0.thumbs.4chan.org/" + post.board + "/thumb/" + post.tim + "s.jpg";

		loadingView = inflater.inflate(R.layout.fullscreen_image_loading, (ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content), false);
		ImageView imageView = (ImageView)loadingView.findViewById(R.id.fullscreen_image_image);
		imageLoader.displayImage(thumbUrl, imageView, options);
		
		TextView textView = (TextView)loadingView.findViewById(R.id.fullscreen_image_text);
		textView.setText("Loading image\n0 / " + post.fsize);
		
		ProgressBar progressBar = (ProgressBar)loadingView.findViewById(R.id.fullscreen_image_progressbar);
		progressBar.setProgress(0);
		progressBar.setMax(post.fsize);
		
		setContentView(loadingView);
		
        handler = new ProgressHandler(this);
        handler.sendEmptyMessageDelayed(0, 100);
    }

    private String getLocalImagePath(String cacheFolder) {
    	return cacheFolder + "/" + post.board + "/" + post.no + post.ext;
    }
    
	private String getCacheFolder() {
		String cacheDir = "Android/data/" + getBaseContext().getPackageName() + "/cache/";
		File picCacheDir = StorageUtils.getOwnCacheDirectory(getBaseContext(), cacheDir);
		String baseDir = "";
		if (picCacheDir != null && (picCacheDir.exists() || picCacheDir.mkdirs())) {
			baseDir = "file://" + picCacheDir.getAbsolutePath();
		}
		return baseDir;
	}

    public int getScreenOrientation() {
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
                ShareImageTask shareImageTask = new ShareImageTask(this);
                shareImageTask.execute(webView);
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
            return setImageAsWallpaper();
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }

    private class ShareImageTask extends AsyncTask<WebView, Void, String> {
        private Context context;

        public ShareImageTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(WebView... params) {
            return shareImage();
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }
    
    private String setImageAsWallpaper() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
        String result = "";
        try {
            Log.i(TAG, "Setting wallpaper from file " + this.localImageUri);
            wallpaperManager.setStream(new FileInputStream(new File(URI.create(this.localImageUri))));
            result = ctx.getString(R.string.full_screen_wallpaper_set);
            Log.i(TAG, result);
        }
        catch (Exception e) {
            result = ctx.getString(R.string.full_screen_wallpaper_error);
            Log.e(TAG, result, e);
        }
        return result;
    }

    private String shareImage() {
        String msg;
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            if (localImageUri.endsWith("jpeg") || localImageUri.endsWith("jpg")) {
            	intent.setType("image/jpeg");
            } else if (localImageUri.endsWith("gif")) {
            	intent.setType("image/gif");
            } else if (localImageUri.endsWith("png")) {
            	intent.setType("image/png");
            } else if (localImageUri.endsWith("bmp")) {
            	intent.setType("image/bmp");
            }
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(URI.create(localImageUri))));
            startActivity(Intent.createChooser(intent, ctx.getString(R.string.full_screen_share_image_intent)));
            return null;
        }
        catch (Exception e) {
            msg = ctx.getString(R.string.full_screen_sharing_error);
            Log.e(TAG, msg, e);
        }
        return msg;
    }

    private void navigateUp() {
        Intent upIntent = new Intent(this, ThreadActivity.class);
        upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        upIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
        upIntent.putExtra(ChanHelper.POST_NO, postNo);
        upIntent.putExtra(ChanHelper.LAST_BOARD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));
        upIntent.putExtra(ChanHelper.LAST_THREAD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_THREAD_POSITION, 0));
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

    private static class ProgressHandler extends Handler {
    	FullScreenImageActivity activity;
    	ProgressHandler(FullScreenImageActivity activity) {
    		super();
    		this.activity = activity;
    	}
    	
    	@Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            
            int localFileSize = 0;
            Query query = new Query();
            query.setFilterById(activity.downloadEnqueueId);
            Cursor c = activity.dm.query(query);
            if (c.moveToFirst()) {
                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                localFileSize = c.getInt(columnIndex);
            }
            c.close();
            Log.i(TAG, "handle message: updating progress bar " + localFileSize);
            
            ProgressBar progressBar = (ProgressBar)activity.loadingView.findViewById(R.id.fullscreen_image_progressbar);
    		progressBar.setProgress(localFileSize);
    		TextView textView = (TextView)activity.loadingView.findViewById(R.id.fullscreen_image_text);
    		textView.setText("Loading image\n" + localFileSize + " / " + activity.post.fsize);
    		
            if (activity.hasWindowFocus() && activity.localImageUri == null) {
            	this.sendEmptyMessageDelayed(0, 100);
            }
        }
    };
}
