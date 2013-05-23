package com.chanapps.four.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;

import com.chanapps.four.data.*;
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
import com.android.gallery3d.app.AlbumSetPage;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.component.ToastRunnable;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;
import com.nostra13.universalimageloader.core.ImageLoader;

public class GalleryViewActivity extends AbstractGalleryActivity implements ChanIdentifiedActivity {
    public static final String TAG = "GalleryViewActivity";
    private static final boolean DEBUG = false;

    public static final String VIEW_TYPE = "viewType";

    public enum ViewType {
        PHOTO_VIEW,
        ALBUM_VIEW,
        OFFLINE_ALBUM_VIEW,
        OFFLINE_ALBUMSET_VIEW
    }

    private ViewType viewType = ViewType.PHOTO_VIEW; // default single image view

    public static final int PROGRESS_REFRESH_MSG = 0;
	public static final int START_DOWNLOAD_MSG = 1;
	public static final int FINISHED_DOWNLOAD_MSG = 2;
	public static final int DOWNLOAD_ERROR_MSG = 3;
	public static final int UPDATE_POSTNO_MSG = 4;

    private Context ctx;

	private SharedPreferences prefs = null;

    private Intent intent;

    private String boardCode = null;
    private long threadNo = 0;
    private long postNo = 0;
    private ChanPost post = null;
    private String imageUrl = null;
    private String localImageUri = null;
    private ImageLoader imageLoader;
    private LayoutInflater inflater;
    protected Handler handler;
    
    private GalleryActionBar actionBar;

    public static void startActivity(Context from, AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
        final long resto = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
        final long threadNo = resto <= 0 ? postId : resto;
        startActivity(from, boardCode, threadNo, postId, adapterView.getFirstVisiblePosition());
    }

    public static void startActivity(Context from, String boardCode, long threadNo, long postId, int position) {
        Intent intent = new Intent(from, GalleryViewActivity.class);
        intent.putExtra(VIEW_TYPE, ViewType.PHOTO_VIEW.toString());
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanPost.POST_NO, postId);
        intent.putExtra(ChanHelper.LAST_THREAD_POSITION, position);
        if (DEBUG) Log.i(TAG, "Starting full screen image viewer for: " + boardCode + "/" + threadNo + "/" + postId);
        from.startActivity(intent);
    }

    public static void startAlbumViewActivity(Context from, String boardCode, long threadNo) {
        NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.GALLERY_VIEW);
        if (DEBUG) Log.i(TAG, "Starting gallery folder viewer for: " + boardCode + "/" + threadNo);
        from.startActivity(getAlbumViewIntent(from, boardCode, threadNo));
    }
    
    public static void startOfflineAlbumViewActivity(Context from, String boardCode) {
        if (boardCode == null || boardCode.isEmpty())
            NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.ALL_CACHED_IMAGES);
        else
            NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.CACHED_BOARD_IMAGES);
        if (DEBUG) Log.i(TAG, "Starting offline gallery viewer for " + (boardCode != null ? "board " + boardCode : "whole cache"));
        from.startActivity(getOfflineAlbumViewIntent(from, boardCode));
    }

    public static Intent getAlbumViewIntent(Context from, String boardCode, long threadNo) {
        Intent intent = new Intent(from, GalleryViewActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(VIEW_TYPE, ViewType.ALBUM_VIEW.toString());
        return intent;
    }

    public static Intent getOfflineAlbumViewIntent(Context from, String boardCode) {
        Intent intent = new Intent(from, GalleryViewActivity.class);
        if (boardCode == null || boardCode.isEmpty()) {
        	intent.putExtra(VIEW_TYPE, ViewType.OFFLINE_ALBUMSET_VIEW.toString());
        } else {
        	intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        	intent.putExtra(VIEW_TYPE, ViewType.OFFLINE_ALBUM_VIEW.toString());
        }
        return intent;
    }
    
    private boolean loadChanPostData() {
		try {
			ChanThread thread = ChanFileStorage.loadThreadData(getBaseContext(), boardCode, threadNo);
            if (thread == null)
                return false;
			for (ChanPost post : thread.posts) {
				if (post.no == postNo) {
                    this.imageUrl = post.imageUrl();
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
        
        actionBar = new GalleryActionBar(this);

        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        imageLoader = ChanImageLoader.getInstance(getApplicationContext());

        setContentView(R.layout.gallery_layout);
    }

    private File ensureGalleryFolder() {
        File galleryFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File gallery4chanFolder = new File(galleryFolder, getString(com.chanapps.four.activity.R.string.app_name));
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
    }

    private void loadPrefs() {
        if (intent == null || intent != getIntent()) {
            intent = getIntent();
            if (intent.hasExtra(VIEW_TYPE)) {
                viewType = ViewType.valueOf(intent.getStringExtra(VIEW_TYPE));
            } else {
            	viewType = ViewType.OFFLINE_ALBUMSET_VIEW;
            }

            if (viewType == ViewType.PHOTO_VIEW
                    && intent.hasExtra(ChanHelper.BOARD_CODE)
                    && intent.hasExtra(ChanHelper.THREAD_NO)
                    && intent.hasExtra(ChanPost.POST_NO)) {
                boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
                threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
                postNo = intent.getLongExtra(ChanPost.POST_NO, 0);
                if (postNo == 0) {
                    postNo = threadNo; // for calls from null thread grid items used in header
                }
                imageUrl = intent.getStringExtra(ChanHelper.IMAGE_URL);
            }
            else if (viewType == ViewType.ALBUM_VIEW
                    && intent.hasExtra(ChanHelper.BOARD_CODE)
                    && intent.hasExtra(ChanHelper.THREAD_NO)) {
                boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
                threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
                postNo = 0;
                imageUrl = "";
            }
            else if (viewType == ViewType.OFFLINE_ALBUM_VIEW
                    && intent.hasExtra(ChanHelper.BOARD_CODE)) {
                boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
                threadNo = 0;
                postNo = 0;
                imageUrl = "";
            }
            else {
                viewType = ViewType.OFFLINE_ALBUMSET_VIEW;
                boardCode = "";
                threadNo = 0;
                postNo = 0;
                imageUrl = "";
            }
            if (DEBUG) Log.i(TAG, "Loaded from intent, viewType: " + viewType.toString() + " boardCode: " + boardCode + ", threadNo: " + threadNo + ", postNo: " + postNo);
        }
        /*
        if (viewType != ViewType.OFFLINE_ALBUMSET_VIEW && viewType != ViewType.OFFLINE_ALBUM_VIEW && postNo == 0) {
            viewType = ViewType.valueOf(prefs.getString(VIEW_TYPE, ViewType.PHOTO_VIEW.toString()));
            boardCode = prefs.getString(ChanHelper.BOARD_CODE, "");
            threadNo = prefs.getLong(ChanHelper.THREAD_NO, 0);
            postNo = prefs.getLong(ChanPost.POST_NO, 0);
            imageUrl = prefs.getString(ChanHelper.IMAGE_URL, "");
            if (DEBUG) Log.i(TAG, "Post no " + postNo + " laoded from preferences viewType=" + viewType);
            if (!boardCode.isEmpty() && threadNo == 0) {
                viewType = ViewType.OFFLINE_ALBUM_VIEW;
            }
            else if (boardCode.isEmpty()) {
                viewType = ViewType.OFFLINE_ALBUMSET_VIEW;
            }
        }
        */
        if (!loadChanPostData()) { // fill in the best we can
            post = new ChanPost();
            post.no = postNo;
            post.resto = threadNo;
            post.board = boardCode;
        }
        /*
        if (imageUrl == null || imageUrl.isEmpty()) {
            if (DEBUG) Log.i(TAG, "trying to load imageurl from prefs as last-ditch attempt");
            imageUrl = prefs.getString(ChanHelper.IMAGE_URL, "");
        }
        */
        if (DEBUG) Log.i(TAG, "Loaded viewType: " + viewType.toString() + " boardCode: " + boardCode + ", threadNo: " + threadNo + ", postNo: " + postNo);
        setActionBarTitle();
    }

    private void savePrefs() {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(VIEW_TYPE, viewType.toString());
        ed.putString(ChanHelper.BOARD_CODE, boardCode);
        ed.putLong(ChanHelper.THREAD_NO, threadNo);
        ed.putLong(ChanPost.POST_NO, postNo);
        ed.putString(ChanHelper.IMAGE_URL, imageUrl);
        if (DEBUG) Log.i(TAG, "Stored in prefs, post no: " + postNo);
        ed.commit();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    protected void onStop () {
    	super.onStop();
    	if (DEBUG) Log.i(TAG, "onStop");
    }

    @Override
	protected void onResume () {
		super.onResume();
		if (DEBUG) Log.i(TAG, "onResume");
		loadPrefs();
		prepareGalleryView();
        NetworkProfileManager.instance().activityChange(this);
	}
	
	public void onWindowFocusChanged (boolean hasFocus) {
		if (DEBUG) Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause");
        saveInstanceState();
    }

    protected void saveInstanceState() {
        savePrefs();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    public void onBackPressed() {
        // send the back event to the top sub-state
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
        	if (DEBUG) Log.i(TAG, "Gallery state stack: " + getStateManager().getStackDescription());
        	getStateManager().compactActivityStateStack();
        	if (getStateManager().getStateCount() > 1) {
        		getStateManager().onBackPressed();
        	} else {
        		navigateUp();
        	}
        } finally {
            root.unlockRenderThread();
        }
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
    	super.onConfigurationChanged(newConfig);
    	prepareGalleryView();
    }
    
    private void prepareGalleryView() {
    	handler = new ProgressHandler(this);
    	View contentView = inflater.inflate(R.layout.gallery_layout,
    			(ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content), false);
    	setContentView(contentView);
    	super.mGLRootView = (GLRootView) contentView.findViewById(R.id.gl_root_view);

    	Bundle data = new Bundle();
    	try {
	    	switch(viewType) {
	    	case PHOTO_VIEW:
	    		data.putString(PhotoPage.KEY_MEDIA_SET_PATH, 
	    				Path.fromString("/chan/" + boardCode + "/" + threadNo).toString());
	    		data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, 
	    				Path.fromString("/chan/" + boardCode + "/" + threadNo + "/" + postNo).toString());
	    		getStateManager().startState(PhotoPage.class, data);
	    		break;
	    	case ALBUM_VIEW:
	    		data.putString(AlbumPage.KEY_MEDIA_PATH,
	    				Path.fromString("/chan/" + boardCode + "/" + threadNo).toString());
	    		getStateManager().startState(AlbumPage.class, data);
	    		break;
	    	case OFFLINE_ALBUM_VIEW:
	    		data.putString(AlbumPage.KEY_MEDIA_PATH,
	    				Path.fromString("/chan-offline/" + boardCode).toString());
	    		getStateManager().startState(AlbumPage.class, data);
	    		break;
	    	case OFFLINE_ALBUMSET_VIEW:
	    		data.putString(AlbumSetPage.KEY_MEDIA_PATH,
	    				Path.fromString("/chan-offline").toString());
	    		getStateManager().startState(AlbumSetPage.class, data);
	    		break;
	    	}
    	} catch (Exception e) {
    		Toast.makeText(this, "Gallery not initialized properly, viewType: " + viewType, Toast.LENGTH_SHORT).show();
    		Log.e(TAG, "Gallery not initialized properly, viewType: " + viewType + ", board: " + boardCode + ", threadNo: " + threadNo, e);
    		navigateUp();
    	}
    }

    @Override
    public void setContentView(int resId) {
        super.setContentView(resId);
        super.mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
        switch (item.getItemId()) {
            case android.R.id.home:
            	//if (DEBUG) Log.i(TAG, "Gallery state stack: " + getStateManager().getStackDescription());
            	//getStateManager().compactActivityStateStack();
            	//if (getStateManager().getStateCount() > 1) {
            	//	getStateManager().onBackPressed();
            	//} else {
            		navigateUp();
            	//}
                return true;
            case R.id.download_all_images_to_gallery_menu:
                ThreadImageDownloadService.startDownloadToGalleryFolder(getBaseContext(), boardCode, threadNo, null);
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.view_image_gallery_menu:
                GalleryViewActivity.startAlbumViewActivity(this, boardCode, threadNo);
                return true;
            case R.id.offline_board_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, boardCode);
                return true;
            case R.id.offline_chan_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, null);
                return true;
            /*
            case R.id.download_image_menu:
                if (checkLocalImage() != null)
                    (new CopyImageToGalleryTask(getApplicationContext())).execute();
                else
                    Toast.makeText(this, R.string.full_screen_wait_until_downloaded, Toast.LENGTH_SHORT).show();
                return true;
            */
            case R.id.settings_menu:
                if (DEBUG) Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.about_menu:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            case R.id.exit_menu:
                ChanHelper.exitApplication(this);
                return true;
            default:
            	return getStateManager().itemSelected(item);
        }        
	    } finally {
	        root.unlockRenderThread();
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

    private void navigateUp() {
    	Intent upIntent = null;
    	switch(viewType) {
    	case PHOTO_VIEW:
    		upIntent = new Intent(this, ThreadActivity.class);
            upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            upIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
            upIntent.putExtra(ChanPost.POST_NO, postNo);
            upIntent.putExtra(ChanHelper.LAST_BOARD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));
            upIntent.putExtra(ChanHelper.LAST_THREAD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_THREAD_POSITION, 0));
    		break;
    	case ALBUM_VIEW:
    		upIntent = new Intent(this, ThreadActivity.class);
            upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            upIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
            upIntent.putExtra(ChanHelper.LAST_BOARD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));
            upIntent.putExtra(ChanHelper.LAST_THREAD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_THREAD_POSITION, 0));
    		break;
    	case OFFLINE_ALBUM_VIEW:
    		upIntent = new Intent(this, BoardActivity.class);
            upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            upIntent.putExtra(ChanHelper.LAST_BOARD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));
    		break;
    	case OFFLINE_ALBUMSET_VIEW:
    		upIntent = new Intent(this, BoardSelectorActivity.class);
    		ChanBoard board = ChanBoard.getBoardByCode(this, boardCode);
    		if (board != null) {
    			upIntent.putExtra(ChanHelper.BOARD_TYPE, board.boardType.toString());
    		}
    		upIntent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
            break;
    	}
        NavUtils.navigateUpTo(this, upIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getStateManager().createOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gallery_view_menu, menu);
        return true;
    }
    /*
    private static final int[] HIDDEN_ALBUM_MENU_ITEMS = {
            R.id.view_image_gallery_menu,
            R.id.download_image_menu,
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
    */

    private void setActionBarTitle() {
        if (DEBUG) Log.i(TAG, "setting action bar based on viewType=" + viewType);
        String title = "";
        if (boardCode != null && !boardCode.isEmpty()) {
            if (DEBUG) Log.i(TAG, "about to load board data for action bar board=" + boardCode);
            ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
            if (board == null) {
                board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
            }
            title = (board == null ? "Board" : board.name) + " /" + boardCode + "/";
            if (threadNo > 0) {
                String threadTitle = "";
                ChanThread thread = ChanFileStorage.loadThreadData(getApplicationContext(), boardCode, threadNo);
                if (thread != null)
                    threadTitle = thread.threadSubject(getApplicationContext());
                if (threadTitle.isEmpty())
                    threadTitle = "Thread " + threadNo;
                title += ChanHelper.TITLE_SEPARATOR + threadTitle;
            }
        }
        if (getActionBar() == null) {
            if (DEBUG) Log.i(TAG, "Action bar was null");
            return;
        }
        switch(viewType) {
            case OFFLINE_ALBUMSET_VIEW:
                getActionBar().setTitle(R.string.offline_chan_view_menu);
                break;
            case OFFLINE_ALBUM_VIEW:
                getActionBar().setTitle(String.format(getString(R.string.offline_board_view_title), title));
                break;
            case PHOTO_VIEW:
            case ALBUM_VIEW:
            default:
                getActionBar().setTitle(title);
        }
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        if (DEBUG) Log.i(TAG, "Set action bar");
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
	            String postNo = (String)msg.obj;
	            activity.postNo = Long.parseLong(postNo);
	            activity.savePrefs();
            	if (DEBUG) Log.w(TAG, "Updated last viewed image: " + activity.postNo);
            }
            
        }
    }

	@Override
    public GalleryActionBar getGalleryActionBar() {
    	if (actionBar == null) {
    		actionBar = new GalleryActionBar(this);
    		setActionBarTitle();
    	}
        return actionBar;
    }
    
    @Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.FULL_SCREEN_IMAGE_ACTIVITY);
	}

	@Override
	public Handler getChanHandler() {
		return handler;
	}

    @Override
    public void refresh() {}

    @Override
    public void closeSearch() {}

    @Override
    public void startProgress() {}

}
