package com.chanapps.four.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;

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
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumPage;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.component.ToastRunnable;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.fragment.SetWallpaperDialogFragment;
import com.chanapps.four.service.ImageDownloadService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class GalleryViewActivity extends AbstractGalleryActivity implements ChanIdentifiedActivity {

    public static final String TAG = "GalleryViewActivity";

    public static final String VIEW_TYPE = "viewType";

    public enum ViewType {
        PHOTO_VIEW,
        ALBUM_VIEW
    }

    private ViewType viewType = ViewType.PHOTO_VIEW; // default single image view

    public static final int PROGRESS_REFRESH_MSG = 0;
	public static final int START_DOWNLOAD_MSG = 1;
	public static final int FINISHED_DOWNLOAD_MSG = 2;
	public static final int DOWNLOAD_ERROR_MSG = 3;
	public static final int UPDATE_POSTNO_MSG = 4;

    private static final boolean DEBUG = true;

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
    private ImageLoader imageLoader;
    private LayoutInflater inflater;
    protected Handler handler;

    public static void startActivity(Context from, AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
        final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_RESTO));
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        final int w = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_W));
        final int h = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_H));
        startActivity(from, boardCode, threadNo, postId, w, h, adapterView.getFirstVisiblePosition());
    }

    public static void startActivity(Context from, String boardCode, long threadNo, long postId, int w, int h, int position) {
        Intent intent = new Intent(from, GalleryViewActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.POST_NO, postId);
        intent.putExtra(ChanHelper.IMAGE_WIDTH, w);
        intent.putExtra(ChanHelper.IMAGE_HEIGHT, h);
        intent.putExtra(ChanHelper.LAST_THREAD_POSITION, position);
        if (DEBUG) Log.i(TAG, "Starting full screen image viewer for: " + boardCode + "/" + threadNo + "/" + postId);
        from.startActivity(intent);
    }

    public static void startAlbumViewActivity(Context from, String boardCode, long threadNo) {
        if (DEBUG) Log.i(TAG, "Starting gallery folder viewer for: " + boardCode + "/" + threadNo);
        from.startActivity(getAlbumViewIntent(from, boardCode, threadNo));
    }

    public static Intent getAlbumViewIntent(Context from, String boardCode, long threadNo) {
        Intent intent = new Intent(from, GalleryViewActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(VIEW_TYPE, ViewType.ALBUM_VIEW.toString());
        return intent;
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
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        ctx = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        //webView.setBackgroundColor(Color.BLACK);
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));
        
        setContentView(R.layout.gallery_layout);
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
        if (!gallery4chanFolder.exists()) {
            if (!gallery4chanFolder.mkdirs()) {
                return null;
            }
        }
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
                            runOnUiThread(new ToastRunnable(GalleryViewActivity.this, R.string.full_screen_saved_to_gallery));
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
    }

    private void loadPrefs() {
        if (intent == null || intent != getIntent()) {
            intent = getIntent();
            if (intent.hasExtra(ChanHelper.BOARD_CODE) && intent.hasExtra(ChanHelper.THREAD_NO)) {
                if (intent.hasExtra(VIEW_TYPE))
                    viewType = ViewType.valueOf(intent.getStringExtra(VIEW_TYPE));
                boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
                threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
                postNo = intent.getLongExtra(ChanHelper.POST_NO, 0);
                if (postNo == 0)
                    postNo = threadNo; // for calls from null thread grid items used in header
                imageUrl = intent.getStringExtra(ChanHelper.IMAGE_URL);
                imageWidth = intent.getIntExtra(ChanHelper.IMAGE_WIDTH, 0);
                imageHeight = intent.getIntExtra(ChanHelper.IMAGE_HEIGHT, 0);
                if (DEBUG) Log.i(TAG, "Loaded from intent, viewType: " + viewType.toString() + " boardCode: " + boardCode + ", threadNo: " + threadNo + ", postNo: " + postNo);
            } else {
                Log.e(TAG, "Intent received without postno");
            }
        }
        if (postNo == 0) {
            viewType = ViewType.valueOf(prefs.getString(VIEW_TYPE, ViewType.PHOTO_VIEW.toString()));
            boardCode = prefs.getString(ChanHelper.BOARD_CODE, "");
            threadNo = prefs.getLong(ChanHelper.THREAD_NO, 0);
            postNo = prefs.getLong(ChanHelper.POST_NO, 0);
            imageUrl = prefs.getString(ChanHelper.IMAGE_URL, "");
            imageWidth = prefs.getInt(ChanHelper.IMAGE_WIDTH, 0);
            imageHeight = prefs.getInt(ChanHelper.IMAGE_HEIGHT, 0);
            if (DEBUG) Log.i(TAG, "Post no " + postNo + " laoded from preferences viewType=" + viewType);
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
        GLRoot root = getGLRoot();
        if (root != null) {
	        root.lockRenderThread();
	        try {
	            getStateManager().destroy();
	        } finally {
	            root.unlockRenderThread();
	        }
        }
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
   		// checking filesystem if image file is available
    	loadOrShowImage();
    }
    
    private void loadOrShowImage() {
    	/*
    	localImageUri = checkLocalImage();
    	
    	if (localImageUri != null) {
    		showImage();
    	} else {
    		loadImage();
    	}
    	*/
    	showImage();
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

    private void showImage() {
    	handler = new ProgressHandler(this);
    	View contentView = inflater.inflate(R.layout.gallery_layout,
    			(ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content), false);
    	setContentView(contentView);
    	super.mGLRootView = (GLRootView) contentView.findViewById(R.id.gl_root_view);

    	Bundle data = new Bundle();

    	Path setPath = Path.fromString("/chan/" + boardCode + "/" + threadNo);
		data.putString(PhotoPage.KEY_MEDIA_SET_PATH, setPath.toString());
		data.putString(AlbumPage.KEY_MEDIA_PATH, setPath.toString());
    	
		Path itemPath = Path.fromString("/chan/" + boardCode + "/" + threadNo + "/" + postNo);
		data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, itemPath.toString());

        Class viewClass = viewType == ViewType.ALBUM_VIEW ? AlbumPage.class : PhotoPage.class;
        getStateManager().startState(viewClass, data);
    }

    @Override
    public void setContentView(int resId) {
        super.setContentView(resId);
        super.mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUp();
                return true;
            case R.id.download_all_images_to_gallery_menu:
                ThreadImageDownloadService.startDownloadToGalleryFolder(getBaseContext(), boardCode, threadNo, null);
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.view_image_gallery_menu:
                GalleryViewActivity.startAlbumViewActivity(this, boardCode, threadNo);
                return true;
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
                            .show(getFragmentManager(), SetWallpaperDialogFragment.TAG);
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
            case R.id.exit_menu:
                ChanHelper.exitApplication(this);
                return true;
            default:
                return onContextItemSelected(item);
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
        inflater.inflate(R.menu.gallery_view_menu, menu);
        return true;
    }

    private static final int[] HIDDEN_ALBUM_MENU_ITEMS = {
            R.id.view_image_gallery_menu,
            R.id.download_image_menu,
            R.id.share_image_menu,
            R.id.set_as_wallpaper_menu,
            R.id.image_search_menu,
            R.id.anime_image_search_menu
    };

    private static final int[] HIDDEN_PHOTO_MENU_ITEMS = {
            R.id.download_all_images_to_gallery_menu
    };

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        for (int id : HIDDEN_ALBUM_MENU_ITEMS) {
            MenuItem item = menu.findItem(id);
            if (item != null) {
                boolean visible = (viewType != ViewType.ALBUM_VIEW);
                item.setVisible(visible);
                item.setEnabled(visible);
            }
        }
        for (int id : HIDDEN_PHOTO_MENU_ITEMS) {
            MenuItem item = menu.findItem(id);
            if (item != null) {
                boolean visible = (viewType != ViewType.PHOTO_VIEW);
                item.setVisible(visible);
                item.setEnabled(visible);
            }
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
    	GalleryViewActivity activity;
    	ProgressHandler(GalleryViewActivity activity) {
    		super();
    		this.activity = activity;
    	}
    	
    	@Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            
            if (msg.what == PROGRESS_REFRESH_MSG) {
	            int localFileSize = msg.arg1 + 1;
	            int totalFileSize = msg.arg2;
	            if (DEBUG) Log.i(TAG, "handle message: updating progress bar " + localFileSize);
	            
	            ProgressBar progressBar = (ProgressBar)activity.findViewById(R.id.full_screen_progress_bar);
	            if (progressBar != null) {
		            if (localFileSize != totalFileSize) {
			            progressBar.setVisibility(ProgressBar.VISIBLE);
			    		progressBar.setProgress(localFileSize);
			    		progressBar.setMax(totalFileSize);
		            } else {
		            	progressBar.setVisibility(ProgressBar.INVISIBLE);
		            }
	            }
            } else if (msg.what == UPDATE_POSTNO_MSG) {
	            int selectedPostNo = msg.arg1;
	            //String url = (String)msg.obj;
	            
	            activity.postNo = selectedPostNo;
	            //activity.imageUrl = url;
	            activity.savePrefs();
            	Log.i(TAG, "Updated last viewed image: " + activity.postNo);
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
