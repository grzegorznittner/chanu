package com.chanapps.four.activity;

import com.chanapps.four.data.*;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;

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

    private Intent intent;
    private String boardCode = null;
    private long threadNo = 0;
    private long postNo = 0;
    private ChanPost post = null;
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
        if (DEBUG) Log.i(TAG, "Starting full screen image viewer for: " + boardCode + "/" + threadNo + "/" + postId);
        from.startActivity(intent);
    }

    public static void startAlbumViewActivity(Context from, String boardCode, long threadNo) {
        //NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.GALLERY_VIEW);
        if (DEBUG) Log.i(TAG, "Starting gallery folder viewer for: " + boardCode + "/" + threadNo);
        from.startActivity(getAlbumViewIntent(from, boardCode, threadNo));
    }
    
    public static void startOfflineAlbumViewActivity(Context from, String boardCode) {
        //if (boardCode == null || boardCode.isEmpty())
        //    NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.ALL_CACHED_IMAGES);
        //else
        //    NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.CACHED_BOARD_IMAGES);
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

    private void loadChanPostData() {
        post = null;
        try {
            ChanThread thread = ChanFileStorage.loadThreadData(getBaseContext(), boardCode, threadNo);
            if (thread != null) {
                for (ChanPost post : thread.posts) {
                    if (post.no == postNo) {
                        this.post = post;
                        break;
                    }
                }
            }
        } catch (Exception e) {
			Log.e(TAG, "Error load post data. " + e.getMessage(), e);
		}
        if (post == null) {
            post = new ChanPost();
            post.no = postNo;
            post.resto = threadNo;
            post.board = boardCode;
        }
    }

    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        actionBar = new GalleryActionBar(this);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(R.layout.gallery_layout);

        if (bundle != null)
            onRestoreInstanceState(bundle);
        else
            setFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        setFromIntent(intent);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        viewType = ViewType.valueOf(bundle.getString(VIEW_TYPE));
        boardCode = bundle.getString(ChanBoard.BOARD_CODE);
        threadNo = bundle.getLong(ChanThread.THREAD_NO, 0);
        postNo = bundle.getLong(ChanPost.POST_NO, 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putString(VIEW_TYPE, viewType.toString());
        bundle.putString(ChanBoard.BOARD_CODE, boardCode);
        bundle.putLong(ChanThread.THREAD_NO, threadNo);
        bundle.putLong(ChanPost.POST_NO, postNo);
    }

    protected void setFromIntent(Intent intent) {
        if (intent.hasExtra(VIEW_TYPE))
            viewType = ViewType.valueOf(intent.getStringExtra(VIEW_TYPE));
        else
            viewType = ViewType.OFFLINE_ALBUMSET_VIEW;
        boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
        threadNo = intent.getLongExtra(ChanThread.THREAD_NO, 0);
        postNo = intent.getLongExtra(ChanPost.POST_NO, 0);
        if (DEBUG) Log.i(TAG, "Loaded from intent, viewType: " + viewType.toString() + " boardCode: " + boardCode + ", threadNo: " + threadNo + ", postNo: " + postNo);
    }

    @Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.i(TAG, "onStart");
        loadChanPostData();
        setActionBarTitle();
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
                finish();
        		//navigateUp();
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

    private void navigateUp() {
    	Intent upIntent = null;
    	switch(viewType) {
    	case PHOTO_VIEW:
    		upIntent = new Intent(this, ThreadActivity.class);
            upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            upIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
            upIntent.putExtra(ChanPost.POST_NO, postNo);
    		break;
    	case ALBUM_VIEW:
    		upIntent = new Intent(this, ThreadActivity.class);
            upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            upIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
    		break;
    	case OFFLINE_ALBUM_VIEW:
    		upIntent = new Intent(this, BoardActivity.class);
            upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
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
