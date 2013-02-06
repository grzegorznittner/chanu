package com.chanapps.four.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.component.ToastRunnable;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.fragment.GoToBoardDialogFragment;
import com.chanapps.four.fragment.SetWallpaperDialogFragment;
import com.chanapps.four.service.ImageDownloadService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile.Type;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class FullScreenImageActivity extends FragmentActivity implements ChanIdentifiedActivity {

	public static final String TAG = "FullScreenImageActivity";
	public static final int PROGRESS_REFRESH_MSG = 0;
	public static final int START_DOWNLOAD_MSG = 1;
	public static final int FINISHED_DOWNLOAD_MSG = 2;
	public static final int DOWNLOAD_ERROR_MSG = 3;

    private static final boolean DEBUG = false;

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
    private ChanPost prevPost = null;
    private ChanPost nextPost = null;

    public static void startActivity(Context from, AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
        final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_RESTO));
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        final int w = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_W));
        final int h = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_H));
        Intent intent = new Intent(from, FullScreenImageActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.POST_NO, postId);
        intent.putExtra(ChanHelper.IMAGE_WIDTH, w);
        intent.putExtra(ChanHelper.IMAGE_HEIGHT, h);
        intent.putExtra(ChanHelper.LAST_THREAD_POSITION, adapterView.getFirstVisiblePosition());
        if (DEBUG) Log.i(TAG, "Starting full screen image viewer for: " + boardCode + "/" + threadNo + "/" + postId);
        from.startActivity(intent);
    }

    private boolean loadChanPostData() {
		try {
			ChanThread thread = ChanFileStorage.loadThreadData(getBaseContext(), boardCode, threadNo);
            if (thread == null)
                return false;
			for (ChanPost post : thread.posts) {
				if (post.no == postNo) {
                    this.imageUrl = post.getImageUrl();
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

        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        //webView.setBackgroundColor(Color.BLACK);
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));
        options = new DisplayImageOptions.Builder()
			.cacheOnDisc()
			.imageScaleType(ImageScaleType.EXACT)
			.build();
    }

    private void viewImageGallery() {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(localImageUri), "image/*");
            startActivity(intent);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't start image gallery for url=" + localImageUri, e);
            runOnUiThread(new ToastRunnable(this, R.string.full_screen_view_gallery_error));
        }
    }

    private File ensureGalleryFolder() {
        File galleryFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File gallery4chanFolder = new File(galleryFolder, "4channer");
        if (!gallery4chanFolder.exists())
            gallery4chanFolder.mkdirs();
        return gallery4chanFolder;
    }

    private Integer copyImageFileToGallery() {
        InputStream boardFile = null;
        OutputStream newFile = null;
        try {
            String galleryFilename = ChanFileStorage.getLocalGalleryImageFilename(post);
            File gallery4chanFolder = ensureGalleryFolder();
            File galleryFile = new File(gallery4chanFolder, galleryFilename);

            boardFile = new FileInputStream(new File(URI.create(this.localImageUri)));
            newFile = new FileOutputStream(galleryFile);

            IOUtils.copy(boardFile, newFile);

            MediaScannerConnection.scanFile(this,
                    new String[] { galleryFile.toString()},
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            if (DEBUG) Log.i(TAG, "Scanned file for gallery: " + path + " " + uri);
                            runOnUiThread(new ToastRunnable(FullScreenImageActivity.this, R.string.full_screen_saved_to_gallery));
                        }
                    });
        }
        catch (Exception e) {
            Log.e(TAG, "Error saving image file to gallery", e);
            return R.string.full_screen_save_image_error;
        }
        finally {
            IOUtils.closeQuietly(boardFile);
            IOUtils.closeQuietly(newFile);
        }
        return null;
    }

    @Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.i(TAG, "onStart");
        loadPrefs();
        loadPrevNext();
    }

    private void loadPrefs() {
        if (intent == null || intent != getIntent()) {
            intent = getIntent();
            if (intent.hasExtra(ChanHelper.POST_NO)) {
                boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
                threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
                postNo = intent.getLongExtra(ChanHelper.POST_NO, 0);
                if (postNo == 0)
                    postNo = threadNo; // for calls from null thread grid items used in header
                imageUrl = intent.getStringExtra(ChanHelper.IMAGE_URL);
                imageWidth = intent.getIntExtra(ChanHelper.IMAGE_WIDTH, 0);
                imageHeight = intent.getIntExtra(ChanHelper.IMAGE_HEIGHT, 0);
                if (DEBUG) Log.i(TAG, "Loaded from intent, boardCode: " + boardCode + ", threadNo: " + threadNo + ", postNo: " + postNo);
            } else {
                Log.e(TAG, "Intent received without postno");
            }
        }
        if (postNo == 0) {
            boardCode = prefs.getString(ChanHelper.BOARD_CODE, "");
            threadNo = prefs.getLong(ChanHelper.THREAD_NO, 0);
            postNo = prefs.getLong(ChanHelper.POST_NO, 0);
            imageUrl = prefs.getString(ChanHelper.IMAGE_URL, "");
            imageWidth = prefs.getInt(ChanHelper.IMAGE_WIDTH, 0);
            imageHeight = prefs.getInt(ChanHelper.IMAGE_HEIGHT, 0);
            if (DEBUG) Log.i(TAG, "Post no " + postNo + " laoded from preferences");
        }
        if (!loadChanPostData()) { // fill in the best we can
            post = new ChanPost();
            post.no = postNo;
            post.resto = threadNo;
            post.board = boardCode;
            post.w = imageWidth;
            post.h = imageHeight;
        }
        if (imageUrl == null || imageUrl.isEmpty()) {
            if (DEBUG) Log.i(TAG, "trying to load imageurl from prefs as last-ditch attempt");
            imageUrl = prefs.getString(ChanHelper.IMAGE_URL, "");
        }
        if (DEBUG) Log.i(TAG, "loaded image from prefs/intent url=" + imageUrl);
        setActionBarTitle();
    }

    private void loadPrevNext() {
        ChanThread thread = ChanFileStorage.loadThreadData(this, boardCode, threadNo);
        if (thread != null) {
            ChanPost[] prevNext = thread.getPrevNextPosts(postNo);
            prevPost = prevNext[0];
            nextPost = prevNext[1];
        }
        else {
            prevPost = null;
            nextPost = null;
        }
    }

    private void savePrefs() {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(ChanHelper.BOARD_CODE, boardCode);
        ed.putLong(ChanHelper.THREAD_NO, threadNo);
        ed.putLong(ChanHelper.POST_NO, postNo);
        ed.putString(ChanHelper.IMAGE_URL, imageUrl);
        ed.putInt(ChanHelper.IMAGE_WIDTH, imageWidth);
        ed.putInt(ChanHelper.IMAGE_HEIGHT, imageHeight);
        if (DEBUG) Log.i(TAG, "Stored in prefs, post no: " + postNo);
        ed.commit();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    protected void onStop () {
    	super.onStop();
        savePrefs();
    	if (DEBUG) Log.i(TAG, "onStop");
    }

	@Override
	protected void onRestart() {
		super.onRestart();
        if (DEBUG) Log.i(TAG, "onRestart");
	}

    @Override
	protected void onResume () {
		super.onResume();
		if (DEBUG) Log.i(TAG, "onResume");
        loadOrShowImage();
        
        NetworkProfileManager.instance().activityChange(this);
	}
	
	public void onWindowFocusChanged (boolean hasFocus) {
		if (DEBUG) Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        super.onPause();
        saveInstanceState();
        if (DEBUG) Log.i(TAG, "onPause - removing download id " + imageUrl);
        ImageDownloadService.cancelService(getBaseContext(), imageUrl);
    }

    protected void saveInstanceState() {
        savePrefs();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
		if (DEBUG) Log.i(TAG, "onDestroy");
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
    	try {
            String localImageUri = ChanFileStorage.getLocalImageUrl(getBaseContext(), post);
            File localImage = new File(URI.create(localImageUri));
	    	if (localImage.exists()) {
    			return localImageUri;
	    	} else {
	    		if (DEBUG) Log.i(TAG, "Image " + localImageUri + " doesn't exist");
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
        if (DEBUG) Log.v(TAG, "screenWidth,screenHeight = " + screenWidth + ", " + screenHeight);
        if (DEBUG) Log.v(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialWidth > screenWidth) { // need to scale width down
            double scale = screenWidth / trialWidth;
            trialWidth = screenWidth;
            trialHeight = (int)(Math.floor(scale * trialHeight));
        }
        if (DEBUG) Log.v(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialHeight > screenHeight) { // need to scale height down
            double scale = screenHeight / trialHeight;
            trialWidth = (int)(Math.floor(scale * trialWidth));
            trialHeight = screenHeight;
        }
        if (DEBUG) Log.v(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialWidth < screenWidth) { // try and scale up to width
            double scale = screenWidth / trialWidth;
            if (DEBUG) Log.v(TAG, "scale = " + scale);
            int testHeight = (int)(Math.floor(scale * trialHeight));
            if (DEBUG) Log.v(TAG, "testHeight = " + testHeight);
            if (testHeight <= screenHeight) {
                trialWidth = screenWidth;
                trialHeight = testHeight;
            }
        }
        if (DEBUG) Log.v(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        if (trialHeight < screenHeight) { // try and scale up to height
            double scale = screenHeight / trialHeight;
            int testWidth = (int)(Math.floor(scale * trialWidth));
            if (testWidth <= screenWidth) {
                trialWidth = testWidth;
                trialHeight = screenHeight;
            }
        }
        if (DEBUG) Log.v(TAG, "trialWidth,trialHeight = " + trialWidth + ", " + trialHeight);
        int initialScalePct = (int)Math.floor(100 * screenWidth / imageWidth);
        webView.setInitialScale(initialScalePct);
        if (DEBUG) Log.v(TAG, "initial Scale = " + initialScalePct);
    }
    
    private void showImage() {
    	localImageUri = checkLocalImage();
    	if (DEBUG) Log.i(TAG, "Displaying image " + localImageUri);
    	webView = new WebView(this);
        setContentView(webView);
        registerForContextMenu(webView);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        webView.getSettings().setBuiltInZoomControls(true);
        
        setDefaultZoom();
        
        webView.loadUrl(localImageUri);
    }

    private void loadImage() {
    	if (NetworkProfileManager.instance().getCurrentProfile().getConnectionType() == Type.NO_CONNECTION) {
    		if (DEBUG) Log.i(TAG, "Off-line mode, download not started");    		
            showOfflineScreen();
    	} else {
	    	showDownloadScreen();
    	}
    }

	private void showOfflineScreen() {
		try {
			loadingView = inflater.inflate(R.layout.fullscreen_image_offline, (ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content), false);
			ImageView imageView = (ImageView)loadingView.findViewById(R.id.fullscreen_image_image);
			imageLoader.displayImage(post.getThumbnailUrl(), imageView, options);
			
			final Button loginButton = (Button) loadingView.findViewById(R.id.fullscreen_image_close_button);
				loginButton.setOnClickListener(new OnClickListener() {
			        @Override
			        public void onClick(final View v) {
			            if (DEBUG) Log.w(TAG, "Download page closed");
			            FullScreenImageActivity.this.finish();
			        }
			});
	
			TextView fileSizeTextView = (TextView)loadingView.findViewById(R.id.fullscreen_image_sizetext);
			fileSizeTextView.setText("" + post.w + "px x " + post.h + "px");
	
			setContentView(loadingView);
	
			handler = new ProgressHandler(this);
		} catch (Exception e) {
		    Log.e(TAG, "Couldn't open offline view for image with url=" + imageUrl, e);
		    runOnUiThread(new ToastRunnable(this, R.string.full_screen_view_image_error));
		}
	}

	private void showDownloadScreen() {
		try {
		    this.localImageUri = null;
		    if (DEBUG) Log.i(TAG, "Loading image from URL: " + imageUrl);

		    String imageFile = ChanFileStorage.getLocalImageUrl(getBaseContext(), post);
		    ImageDownloadService.startService(getBaseContext(), boardCode, threadNo, postNo, imageUrl, imageFile);
		    
		    loadingView = inflater.inflate(R.layout.fullscreen_image_loading, (ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content), false);
		    ImageView imageView = (ImageView)loadingView.findViewById(R.id.fullscreen_image_image);
            imageLoader.displayImage(post.getThumbnailUrl(), imageView, options);
		    
		    final Button loginButton = (Button) loadingView.findViewById(R.id.fullscreen_image_cancel_button);
		    	loginButton.setOnClickListener(new OnClickListener() {
		            @Override
		            public void onClick(final View v) {
		            	ImageDownloadService.cancelService(getBaseContext(), imageUrl);
		                if (DEBUG) Log.w(TAG, "Download cancelled for " + imageUrl);
		                FullScreenImageActivity.this.finish();
		            }
		    });

		    TextView fileSizeTextView = (TextView)loadingView.findViewById(R.id.fullscreen_image_sizetext);
		    fileSizeTextView.setText("" + post.w + "px x " + post.h + "px");
		    TextView textView = (TextView)loadingView.findViewById(R.id.fullscreen_image_text);
		    textView.setText("0kB / " + ((post.fsize / 1024) + 1));

		    ProgressBar progressBar = (ProgressBar)loadingView.findViewById(R.id.fullscreen_image_progressbar);
		    progressBar.setProgress(0);
		    progressBar.setMax(post.fsize);

		    setContentView(loadingView);

		    handler = new ProgressHandler(this);
		    handler.sendEmptyMessageDelayed(PROGRESS_REFRESH_MSG, 100);
		} catch (Exception e) {
		    Log.e(TAG, "Couldn't load image for full screen view with url=" + imageUrl, e);
		    runOnUiThread(new ToastRunnable(this, R.string.full_screen_view_image_error));
		}
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

    private void navigateToPost(ChanPost post, Direction direction) {
        if (post == null || post.no == 0)
            return;
        ImageDownloadService.cancelService(getBaseContext(), imageUrl);
        
        Intent intent = new Intent(this, FullScreenImageActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.POST_NO, post.no);
        intent.putExtra(ChanHelper.IMAGE_WIDTH, post.w);
        intent.putExtra(ChanHelper.IMAGE_HEIGHT, post.h);
        if (DEBUG) Log.i(TAG, "Starting navigate to prev/next image: " + boardCode + "/" + threadNo + ":" + postNo);
        startActivity(intent);
        switch (direction) {
            case PREV:
                overridePendingTransition(R.animator.push_right_in, R.animator.push_right_out);
                break;
            case NEXT:
                overridePendingTransition(R.animator.push_left_in, R.animator.push_left_out);
                break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.full_screen_image_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.download_image_menu:
                if (checkLocalImage() != null)
                    (new CopyImageToGalleryTask(getApplicationContext())).execute();
                else
                    Toast.makeText(this, R.string.full_screen_wait_until_downloaded, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.share_image_menu:
                if (checkLocalImage() != null)
                    shareImage();
                else
                    Toast.makeText(this, R.string.full_screen_wait_until_downloaded, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.set_as_wallpaper_menu:
                if (checkLocalImage() != null)
                    (new SetWallpaperDialogFragment(localImageUri))
                            .show(getSupportFragmentManager(), SetWallpaperDialogFragment.TAG);
                else
                    Toast.makeText(this, R.string.full_screen_wait_until_downloaded, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.view_image_gallery_menu:
                if (checkLocalImage() != null)
                    viewImageGallery();
                else
                    Toast.makeText(this, R.string.full_screen_wait_until_downloaded, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.image_search_menu:
                if (checkLocalImage() != null)
                    imageSearch();
                else
                    Toast.makeText(this, R.string.full_screen_wait_until_downloaded, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.anime_image_search_menu:
                if (checkLocalImage() != null)
                    animeImageSearch();
                else
                    Toast.makeText(this, R.string.full_screen_wait_until_downloaded, Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private enum Direction {
        PREV,
        NEXT
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUp();
                return true;
            case R.id.prev_image_menu:
                navigateToPost(prevPost, Direction.PREV);
                return true;
            case R.id.next_image_menu:
                navigateToPost(nextPost, Direction.NEXT);
                return true;
            case R.id.go_to_board_menu:
                new GoToBoardDialogFragment().show(getSupportFragmentManager(), GoToBoardDialogFragment.TAG);
                return true;
            case R.id.settings_menu:
                if (DEBUG) Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.layout.about_dialog, R.raw.help_header, R.raw.help_full_screen);
                rawResourceDialog.show();
                return true;
            case R.id.about_menu:
                RawResourceDialog aboutDialog = new RawResourceDialog(this, R.layout.about_dialog, R.raw.about_header, R.raw.about_detail);
                aboutDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static final String IMAGE_SEARCH_ROOT = "http://tineye.com/search?url=";
    private static final String IMAGE_SEARCH_ROOT_ANIME = "http://iqdb.org/?url=";

    private void imageSearch() {
        imageSearch(IMAGE_SEARCH_ROOT);
    }

    private void animeImageSearch() {
        imageSearch(IMAGE_SEARCH_ROOT_ANIME);
    }

    private void imageSearch(String rootUrl) {
        try {
            String encodedImageUrl = URLEncoder.encode(imageUrl, "UTF-8");
            String url =  rootUrl + encodedImageUrl;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't do image search imageUrl=" + imageUrl, e);
            Toast.makeText(this, R.string.full_screen_image_search_error, Toast.LENGTH_SHORT).show();
        }
    }

    private class CopyImageToGalleryTask extends AsyncTask<String, Void, Integer> {
        private Context context;

        public CopyImageToGalleryTask(Context context) {
            this.context = context;
        }

        @Override
        protected Integer doInBackground(String... params) {
            return copyImageFileToGallery();
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != null && !(result.equals(0)))
                Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }


    private void shareImage() {
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
        }
        catch (Exception e) {
            Log.e(TAG, "Error sharing image", e);
            Toast.makeText(this, R.string.full_screen_sharing_error, Toast.LENGTH_SHORT).show();
        }
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


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (prevPost != null) {
            menu.getItem(0).setVisible(true);
            menu.getItem(0).setEnabled(true);
        }
        else {
            menu.getItem(0).setVisible(false);
            menu.getItem(0).setEnabled(false);
        }
        if (nextPost != null) {
            menu.getItem(1).setVisible(true);
            menu.getItem(1).setEnabled(true);
        }
        else {
            menu.getItem(1).setVisible(false);
            menu.getItem(1).setEnabled(false);
        }
        return true;
    }

    private void setActionBarTitle() {
        if (getActionBar() != null) {
            getActionBar().setTitle("/" + boardCode + "/" + threadNo + (threadNo == postNo ? "" : ":" + postNo));
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
            
            if (msg.what == PROGRESS_REFRESH_MSG) {
	            int localFileSize = msg.arg1;
	            if (DEBUG) Log.i(TAG, "handle message: updating progress bar " + localFileSize);
	            
	            int totalSize = (activity.post.fsize / 1024) + 1;
	            int downloadedSize = (localFileSize / 1024);
	            
	            ProgressBar progressBar = (ProgressBar)activity.loadingView.findViewById(R.id.fullscreen_image_progressbar);
	    		progressBar.setProgress(localFileSize);
	    		TextView textView = (TextView)activity.loadingView.findViewById(R.id.fullscreen_image_text);
	    		textView.setText("" + downloadedSize + "kB / " + totalSize + "kB");
            } else if (msg.what == START_DOWNLOAD_MSG) {
            	activity.loadImage();
            } else if (msg.what == FINISHED_DOWNLOAD_MSG) {
            	activity.showImage();
            } else if (msg.what == DOWNLOAD_ERROR_MSG) {
            	activity.finish();
            }
        }
    }
    
    @Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.FULL_SCREEN_IMAGE_ACTIVITY);
	}

	@Override
	public Handler getChanHandler() {
		return handler;
	}
}
