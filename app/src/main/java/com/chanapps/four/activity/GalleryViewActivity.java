package com.chanapps.four.activity;

import android.app.ActivityManager;

import android.util.Pair;
import com.android.gallery3d.app.*;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.data.*;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;
import com.chanapps.four.service.profile.NetworkProfile;

import java.util.regex.Pattern;

public class GalleryViewActivity extends AbstractGalleryActivity implements ChanIdentifiedActivity {

    public static final String TAG = "GalleryViewActivity";

    public static final String BOARD_CODE = "boardCode";
    public static final String THREAD_NO = "threadNo";
    public static final String POST_NO = "postNo";

    private static final boolean DEBUG = false;

    public static final String VIEW_TYPE = "viewType";

    public enum ViewType {
        PHOTO_VIEW,
        ALBUM_VIEW,
        OFFLINE_ALBUM_VIEW,
        OFFLINE_ALBUMSET_VIEW
    }

    public static final int PROGRESS_REFRESH_MSG = 0;
	public static final int FINISHED_DOWNLOAD_MSG = 2;
	public static final int DOWNLOAD_ERROR_MSG = 3;
	public static final int UPDATE_POSTNO_MSG = 4;

    private ViewType viewType = ViewType.PHOTO_VIEW; // default single image view
    private String boardCode = null;
    private long threadNo = 0;
    private long postNo = 0;
    private ChanPost post = null;
    private LayoutInflater inflater;
    protected Handler handler;
    private Handler postHandler;
    private GalleryActionBar actionBar;
    private String title;
    private ChanThread thread;
    private ProgressBar progressBar;

    public static void startActivity(Context from, ChanActivityId aid) {
        startActivity(from, aid.boardCode, aid.threadNo, aid.postNo);
    }

    public static void startActivity(Context from, String boardCode, long threadNo, long postId) {
        Intent intent = createIntent(from, boardCode, threadNo, postId, ViewType.PHOTO_VIEW);
        if (DEBUG) Log.i(TAG, "Starting full screen image viewer for: " + boardCode + "/" + threadNo + "/" + postId);
        from.startActivity(intent);
    }

    public static void startActivity(Context from, AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
        final long resto = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
        final long threadNo = resto <= 0 ? postId : resto;
        startActivity(from, boardCode, threadNo, postId);
    }

    public static Intent createIntent(Context from, String boardCode, long threadNo, long postId, ViewType viewType) {
        if (DEBUG) Log.i(TAG, "createIntent() viewType=" + viewType);
        Intent intent = new Intent(from, GalleryViewActivity.class);
        intent.putExtra(VIEW_TYPE, viewType.toString());
        intent.putExtra(BOARD_CODE, boardCode);
        intent.putExtra(THREAD_NO, threadNo);
        intent.putExtra(POST_NO, postId);
        return intent;
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
        intent.putExtra(BOARD_CODE, boardCode);
        intent.putExtra(THREAD_NO, threadNo);
        intent.putExtra(VIEW_TYPE, ViewType.ALBUM_VIEW.toString());
        return intent;
    }

    public static Intent getOfflineAlbumViewIntent(Context from, String boardCode) {
        Intent intent = new Intent(from, GalleryViewActivity.class);
        if (boardCode == null || boardCode.isEmpty()) {
        	intent.putExtra(VIEW_TYPE, ViewType.OFFLINE_ALBUMSET_VIEW.toString());
        } else {
        	intent.putExtra(BOARD_CODE, boardCode);
        	intent.putExtra(VIEW_TYPE, ViewType.OFFLINE_ALBUM_VIEW.toString());
        }
        return intent;
    }

    private void loadChanPostData() {
        post = null;
        try {
            thread = ChanFileStorage.loadThreadData(getBaseContext(), boardCode, threadNo);
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
        if (DEBUG) Log.i(TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        actionBar = new GalleryActionBar(this);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(R.layout.gallery_layout);
        progressBar = (ProgressBar)findViewById(R.id.full_screen_progress_bar);
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

    protected ViewType topViewType() {
        ActivityState state = getStateManager().getTopState();
        if (DEBUG) Log.i(TAG, "onSaveInstanceState activityState=" + state);
        ViewType v;
        if (state instanceof PhotoPage)
            v = ViewType.PHOTO_VIEW;
        else if (state instanceof AlbumPage)
            v = ViewType.ALBUM_VIEW;
        else if (state instanceof AlbumSetPage)
            v = ViewType.OFFLINE_ALBUMSET_VIEW;
        else
            v = ViewType.ALBUM_VIEW;
        return v;
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        viewType = ViewType.valueOf(bundle.getString(VIEW_TYPE));
        boardCode = bundle.getString(ChanBoard.BOARD_CODE);
        threadNo = bundle.getLong(ChanThread.THREAD_NO, 0);
        postNo = bundle.getLong(ChanPost.POST_NO, 0);
        getStateManager().restoreFromState(bundle);
        if (DEBUG) Log.i(TAG, "onRestoreInstanceState() restoring from /" + boardCode + "/" + threadNo + "#p" + postNo);
    }

    protected static final String MEDIA_ITEM_PATH = "media-item-path";
    protected static final String PATH_PATTERN_STR = "/.*\\/([0-9]+)$/";
    protected static final Pattern PATH_PATTERN = Pattern.compile(PATH_PATTERN_STR);

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        postNo = topPostNo();
        viewType = topViewType();
        getStateManager().saveState(bundle);

        bundle.putString(VIEW_TYPE, viewType.toString());
        bundle.putString(ChanBoard.BOARD_CODE, boardCode);
        bundle.putLong(ChanThread.THREAD_NO, threadNo);
        bundle.putLong(ChanPost.POST_NO, postNo);
        if (DEBUG) Log.i(TAG, "onSaveInstanceState() saved to /" + boardCode + "/" + threadNo + "#p" + postNo);
        ActivityDispatcher.store(this);
    }

    protected long topPostNo() {
        ActivityState state = getStateManager().getTopState();
        if (state == null)
            return 0;
        Bundle b = state.getData();
        String path = (String)b.get(MEDIA_ITEM_PATH);
        if (path == null || path.isEmpty())
            return 0;
        int i = path.lastIndexOf("/");
        if (i == -1 || (++i + 1) >= path.length())
            return 0;
        String postNoStr = path.substring(i);
        return Long.valueOf(postNoStr);
    }

    protected void setFromIntent(Intent intent) {
        String s = intent.getStringExtra(VIEW_TYPE);
        if (DEBUG) Log.i(TAG, "setFromIntent viewType=" + s);
        if (s != null && !s.isEmpty())
            viewType = ViewType.valueOf(s);
        else
            viewType = ViewType.OFFLINE_ALBUMSET_VIEW;
        boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
        threadNo = intent.getLongExtra(ChanThread.THREAD_NO, 0);
        postNo = intent.getLongExtra(ChanPost.POST_NO, 0);
        if (DEBUG) Log.i(TAG, "setFromIntent() loaded /" + boardCode + "/" + threadNo + "#" + postNo
                + " viewType=" + viewType.toString());
    }

    @Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.i(TAG, "onStart");
        postHandler = new Handler();
        loadDataAsync();
    }

    @Override
    protected void onStop () {
    	super.onStop();
    	if (DEBUG) Log.i(TAG, "onStop");
        postHandler = null;
    }

    @Override
	protected void onResume () {
		super.onResume();
		if (DEBUG) Log.i(TAG, "onResume");
	}

    @Override
	protected void onPause() {
        try {
            super.onPause();
        }
        catch (Exception e) {
            Log.e(TAG, "onPause() gallery state exception", e);
        }
        if (DEBUG) Log.i(TAG, "onPause");
    }

    @Override
    public void onBackPressed() {
        if (DEBUG) Log.i(TAG, "onBackPressed()");
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
        }
        catch (Exception e) {
            Log.e(TAG, "onBackPressed() exception", e);
            finish();
        }
        catch (Error e) {
            Log.e(TAG, "onBackPressed() error (probably assertion error)", e);
            finish();
        }
        finally {
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

    private void loadDataAsync() {
        setProgressBar(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                delayLoop();
            }
        }).start();
    }

    private void delayLoop() {
        loadChanPostData();
        loadActionBarTitle();
        final long delayMs = calcDelayMs();
        if (DEBUG) Log.i(TAG, "loadDataAsync() delayMs=" + delayMs);
        if (delayMs <= 0) {
            displayGallery(); // load now
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delayMs);
                    delayLoop();
                }
                catch (InterruptedException e) {
                    delayLoop();
                }
            }
        }).start();
    }

    private void displayGallery() {
        if (postHandler != null)
            postHandler.post(new Runnable() {
                @Override
                public void run() {
                    setActionBarTitle();
                    setProgressBar(false);
                    prepareGalleryView();
                    activityChangeAsync();
                }
            });
    }

    protected void activityChangeAsync() {
        final ChanIdentifiedActivity activity = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (NetworkProfileManager.instance().getActivity() != activity) {
                    if (DEBUG) Log.i(TAG, "onResume() activityChange to " + activity.getChanActivityId() );
                    NetworkProfileManager.instance().activityChange(activity);
                }
            }
        }).start();
    }

    private void setProgressBar(boolean on) {
        if (progressBar != null) {
            if (DEBUG) Log.i(TAG, "setProgressBar(" + on + ")");
            if (on)
                progressBar.setVisibility(View.VISIBLE);
            else
                progressBar.setVisibility(View.GONE);
        }
    }

    private long calcDelayMs() {
        boolean loaded;
        if (thread == null || thread.defData)
            loaded = false;
        else if (thread.isDead)
            loaded = true;
        else if (thread.posts == null || thread.posts.length == 0)
            loaded = false;
        else if (thread.posts[0] == null || thread.posts[0].defData)
            loaded = false;
        else if (thread.posts.length < thread.posts[0].images)
            loaded = false;
        else
            loaded = true;

        if (DEBUG) Log.i(TAG, "calcDelayMs() loaded=" + loaded + " tryCount=" + loadTryCount + " thread=" + thread.toString());

        if (++loadTryCount > MAX_LOAD_TRIES) {
            loadTryCount = 0;
            return 0;
        }
        else if (loaded)
            return 0;
        else if (NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth() == NetworkProfile.Health.NO_CONNECTION)
            return 0;
        else
            return NetworkProfileManager.instance().getFetchParams().readTimeout / 10;
    }

    private static final int MAX_LOAD_TRIES = 5;
    private int loadTryCount = 0;

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
                if (DEBUG) Log.i(TAG, "starting photo state");
	    		getStateManager().startState(PhotoPage.class, data);
	    		break;
	    	case ALBUM_VIEW:
                data.putString(AlbumPage.KEY_MEDIA_PATH,
	    				Path.fromString("/chan/" + boardCode + "/" + threadNo).toString());
                if (DEBUG) Log.i(TAG, "starting album state");
                getStateManager().startState(AlbumPage.class, data);
	    		break;
	    	case OFFLINE_ALBUM_VIEW:
                data.putString(AlbumPage.KEY_MEDIA_PATH,
	    				Path.fromString("/chan-offline/" + boardCode).toString());
                if (DEBUG) Log.i(TAG, "starting offline album state");
                getStateManager().startState(AlbumPage.class, data);
	    		break;
	    	case OFFLINE_ALBUMSET_VIEW:
	    		data.putString(AlbumSetPage.KEY_MEDIA_PATH,
	    				Path.fromString("/chan-offline").toString());
                if (DEBUG) Log.i(TAG, "starting offline albumset state");
                getStateManager().startState(AlbumSetPage.class, data);
	    		break;
	    	}
    	} catch (Error e) {
    		Log.e(TAG, "Error initializing gallery, navagiting up, viewType: " + viewType + ", board: " + boardCode + ", threadNo: " + threadNo, e);
    		navigateUp();
    	} catch (Exception e) {
    		Log.e(TAG, "Execption initializing gallery, navigating up, viewType: " + viewType + ", board: " + boardCode + ", threadNo: " + threadNo, e);
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
                ThreadImageDownloadService.startDownloadViaThreadMenu(getBaseContext(), boardCode, threadNo, new long[]{});
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                return true;
            //case R.id.view_image_gallery_menu:
            //    GalleryViewActivity.startAlbumViewActivity(this, boardCode, threadNo);
            //    return true;
            case R.id.offline_board_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, boardCode);
                return true;
            case R.id.offline_chan_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, null);
                return true;
            case R.id.web_menu:
                String url;
                if (postNo > 0)
                    url = ChanPost.postUrl(this, boardCode, threadNo, postNo);
                else
                    url = ChanThread.threadUrl(this, boardCode, threadNo);
                ActivityDispatcher.launchUrlInBrowser(this, url);
            default:
            	return getStateManager().itemSelected(item);
        }        
	    } finally {
	        root.unlockRenderThread();
	    }
    }

    private void navigateUp() {
        Pair<Integer, ActivityManager.RunningTaskInfo> p = ActivityDispatcher.safeGetRunningTasks(this);
        int numTasks = p.first;
        ActivityManager.RunningTaskInfo task = p.second;
        if (task != null) {
            if (DEBUG) Log.i(TAG, "navigateUp() top=" + task.topActivity + " base=" + task.baseActivity);
            if (task.baseActivity != null && !this.getClass().getName().equals(task.baseActivity.getClassName())) {
                if (DEBUG) Log.i(TAG, "navigateUp() using finish instead of intents with me="
                        + this.getClass().getName() + " base=" + task.baseActivity.getClassName());
                finish();
                return;
            }
        }

    	Intent intent = null;
    	switch(viewType) {
    	case PHOTO_VIEW:
    		intent = new Intent(this, ThreadActivity.class);
            intent.putExtra(BOARD_CODE, boardCode);
            intent.putExtra(THREAD_NO, threadNo);
            intent.putExtra(ChanPost.POST_NO, postNo);
    		break;
    	case ALBUM_VIEW:
    		intent = new Intent(this, ThreadActivity.class);
            intent.putExtra(BOARD_CODE, boardCode);
            intent.putExtra(THREAD_NO, threadNo);
    		break;
    	case OFFLINE_ALBUM_VIEW:
    		intent = new Intent(this, BoardActivity.class);
            intent.putExtra(BOARD_CODE, boardCode);
    		break;
    	case OFFLINE_ALBUMSET_VIEW:
            if (boardCode == null || boardCode.isEmpty())
                boardCode = ChanBoard.defaultBoardCode(this);
    		intent = BoardActivity.createIntent(this, boardCode, "");
    		intent.putExtra(ActivityDispatcher.IGNORE_DISPATCH, true);
            break;
    	}
        startActivity(intent);
        finish();
        //NavUtils.navigateUpTo(this, upIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getStateManager().createOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gallery_view_menu, menu);
        return true;
    }

    private void loadActionBarTitle() {
        title = "";
        if (boardCode != null && !boardCode.isEmpty()) {
            if (DEBUG) Log.i(TAG, "about to load board data for action bar board=" + boardCode);
            ChanBoard board = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
            if (board == null) {
                board = ChanBoard.getBoardByCode(getApplicationContext(), boardCode);
            }
            String rawTitle = ChanBoard.getName(getApplicationContext(), boardCode);
            title = (rawTitle == null ? "Board" : rawTitle) + " /" + boardCode + "/";
            /*
            if (threadNo > 0) {
                String threadTitle = "";
                ChanThread thread = ChanFileStorage.loadThreadData(getApplicationContext(), boardCode, threadNo);
                if (thread != null)
                    threadTitle = thread.threadSubject(getApplicationContext());
                if (threadTitle.isEmpty())
                    threadTitle = "Thread " + threadNo;
                title += TITLE_SEPARATOR + threadTitle;
            }
            */
        }
    }

    private void setActionBarTitle() {
        if (DEBUG) Log.i(TAG, "setting action bar based on viewType=" + viewType);
        if (getActionBar() == null) {
            if (DEBUG) Log.i(TAG, "Action bar was null");
            return;
        }
        switch(viewType) {
            case OFFLINE_ALBUMSET_VIEW:
                getActionBar().setTitle(R.string.offline_chan_view_menu);
                break;
            case OFFLINE_ALBUM_VIEW:
            case PHOTO_VIEW:
            case ALBUM_VIEW:
            default:
                getActionBar().setTitle(title);
        }
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        if (DEBUG) Log.i(TAG, "Set action bar");
    }

    private class ProgressHandler extends Handler {
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
        ViewType type;
        try {
            type = currentViewType();
        }
        catch (AssertionError e) {
            if (DEBUG) Log.i(TAG, "getChanActivityId() /" + boardCode + "/" + threadNo + " buggered out on view type error, defaulting", e);
            type = defaultViewType();
        }
        return new ChanActivityId(LastActivity.GALLERY_ACTIVITY, boardCode, threadNo, postNo, type);
    }

    protected ViewType defaultViewType() {
        if (boardCode == null || boardCode.isEmpty())
            return ViewType.OFFLINE_ALBUMSET_VIEW;
        else if (threadNo <= 0)
            return ViewType.OFFLINE_ALBUM_VIEW;
        else if (postNo <= 0)
            return ViewType.ALBUM_VIEW;
        else
            return ViewType.PHOTO_VIEW;
    }

    protected ViewType currentViewType() {
        ActivityState activityState = getStateManager().getTopState();
        ViewType t;
        if (activityState == null)
            return ViewType.OFFLINE_ALBUMSET_VIEW;
        if (activityState instanceof PhotoPage)
            return ViewType.PHOTO_VIEW;
        if (activityState instanceof AlbumPage) {
            Bundle data = activityState.getData();
            if (data == null)
                return ViewType.OFFLINE_ALBUM_VIEW;
            String path = data.getString(AlbumPage.KEY_MEDIA_PATH);
            if (path == null || path.isEmpty())
                return ViewType.OFFLINE_ALBUM_VIEW;
            if (path.matches("/chan-offline/.*"))
                return ViewType.OFFLINE_ALBUM_VIEW;
            return ViewType.ALBUM_VIEW;
        }
        return ViewType.OFFLINE_ALBUMSET_VIEW;
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
    public void setProgress(boolean on) {}

    @Override
    public void switchBoard(String boardCode, String query) {}

    /*
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (Build.VERSION.SDK_INT >= 18) {
            if (hasFocus) {
                int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
                int newUiOptions = uiOptions;
                newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
                newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                //newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
            }
        }
    }
    */

}
