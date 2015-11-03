/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import java.net.URLEncoder;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.view.View.MeasureSpec;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MtpDevice;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.DetailsHelper.DetailsSource;
import com.android.gallery3d.ui.FilmStripView;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.ImportCompleteListener;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.PositionRepository;
import com.android.gallery3d.ui.PositionRepository.Position;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.ui.UserInteractionListener;
import com.android.gallery3d.util.GalleryUtils;
import com.chanapps.four.activity.*;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.URLFormatComponent;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.gallery.ChanImage;
import com.chanapps.four.gallery3d.R;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;

public class PhotoPage extends ActivityState
        implements PhotoView.PhotoTapListener, FilmStripView.Listener,
        UserInteractionListener {
    private static final String TAG = "PhotoPage";
    private static boolean DEBUG = false;

    private static final int MSG_HIDE_BARS = 1;

    private static final int HIDE_BARS_TIMEOUT = 3500;

    private static final int REQUEST_SLIDESHOW = 1;
    private static final int REQUEST_CROP = 2;
    private static final int REQUEST_CROP_PICASA = 3;

    public static final String KEY_MEDIA_SET_PATH = "media-set-path";
    public static final String KEY_MEDIA_ITEM_PATH = "media-item-path";
    public static final String KEY_INDEX_HINT = "index-hint";

    private GalleryApp mApplication;
    private SelectionManager mSelectionManager;

    private PhotoView mPhotoView;
    private PhotoPage.Model mModel;
    private FilmStripView mFilmStripView;
    private DetailsHelper mDetailsHelper;
    private boolean mShowDetails;
    private Path mPendingSharePath;

    // mMediaSet could be null if there is no KEY_MEDIA_SET_PATH supplied.
    // E.g., viewing a photo in gmail attachment
    private MediaSet mMediaSet;
    private Menu mMenu;

    private Intent mResultIntent = new Intent();
    private int mCurrentIndex = 0;
    private Handler mHandler;
    private boolean mShowBars = true;
    private ActionBar mActionBar;
    private MyMenuVisibilityListener mMenuVisibilityListener;
    private boolean mIsMenuVisible;
    private boolean mIsInteracting;
    private MediaItem mCurrentPhoto = null;
    private MenuExecutor mMenuExecutor;
    private boolean mIsActive;
    private ShareActionProvider mShareActionProvider;

    public static final String HTML_START = "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><meta http-equiv=\"cache-control\" content=\"no-cache\"/>" +
            "<meta name=\"viewport\" content=\"width=device-width, target-densitydpi=device-dpi, user-scalable=no\"/>" +
            "<style type=\"text/css\">" +
            "html, body {height: 100%;width: 100%;color: black;background: black;}" +
            "#image {position:fixed;top:0;left:0;text-align:center;}" +
            "</style></head><body><div id=\"image\"><img src=\"";
    public static final String HTML_END = "\" unselectable=\"on\" alt=\"\" title=\"\"/></div></body></html>";

    public static interface Model extends PhotoView.Model {
        public void resume();
        public void pause();
        public boolean isEmpty();
        public MediaItem getCurrentMediaItem();
        public int getCurrentIndex();
        public void setCurrentPhoto(Path path, int indexHint);
    }

    private class MyMenuVisibilityListener implements OnMenuVisibilityListener {
        public void onMenuVisibilityChanged(boolean isVisible) {
            mIsMenuVisible = isVisible;
            refreshHidingMessage();
        }
    }

    private GLView mRootPane = new GLView() {

        @Override
        protected void renderBackground(GLCanvas view) {
            view.clearBuffer();
        }

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mPhotoView.layout(0, 0, right - left, bottom - top);
            PositionRepository.getInstance(mActivity).setOffset(0, 0);
            int filmStripHeight = 0;
            if (mFilmStripView != null) {
                mFilmStripView.measure(
                        MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                        MeasureSpec.UNSPECIFIED);
                filmStripHeight = mFilmStripView.getMeasuredHeight();
                mFilmStripView.layout(0, bottom - top - filmStripHeight,
                        right - left, bottom - top);
            }
            if (mShowDetails) {
                mDetailsHelper.layout(left, GalleryActionBar.getHeight((Activity) mActivity),
                        right, bottom);
            }
        }
    };

    private void initFilmStripView() {
        Config.PhotoPage config = Config.PhotoPage.get((Context) mActivity);
        mFilmStripView = new FilmStripView(mActivity, mMediaSet,
                config.filmstripTopMargin, config.filmstripMidMargin, config.filmstripBottomMargin,
                config.filmstripContentSize, config.filmstripThumbSize, config.filmstripBarSize,
                config.filmstripGripSize, config.filmstripGripWidth);
        mRootPane.addComponent(mFilmStripView);
        mFilmStripView.setListener(this);
        mFilmStripView.setUserInteractionListener(this);
        mFilmStripView.setFocusIndex(mCurrentIndex);
        mFilmStripView.setStartIndex(mCurrentIndex);
        mRootPane.requestLayout();
        if (mIsActive) mFilmStripView.resume();
        if (!mShowBars) mFilmStripView.setVisibility(GLView.INVISIBLE);
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        mActionBar = ((Activity) mActivity).getActionBar();
        mSelectionManager = new SelectionManager(mActivity, false);
        mMenuExecutor = new MenuExecutor(mActivity, mSelectionManager);

        mPhotoView = new PhotoView(mActivity);
        mPhotoView.setPhotoTapListener(this);
        mRootPane.addComponent(mPhotoView);
        mApplication = (GalleryApp)((Activity) mActivity).getApplication();

        final String setPathString = data.getString(KEY_MEDIA_SET_PATH);
        final Path itemPath = Path.fromString(data.getString(KEY_MEDIA_ITEM_PATH));

        if (setPathString != null) {
            mMediaSet = mActivity.getDataManager().getMediaSet(setPathString);
            mMediaSet = (MediaSet)
                    mActivity.getDataManager().getMediaObject(setPathString);
            if (mMediaSet == null) {
                if (DEBUG) Log.w(TAG, "failed to restore " + setPathString);
                getDefaultMediaSet(itemPath);
            } else {
                createData(itemPath);
            }
        } else {
            getDefaultMediaSet(itemPath);
        }

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_HIDE_BARS: {
                        hideBars();
                        break;
                    }
                    default: throw new AssertionError(message.what);
                }
            }
        };

        // start the opening animation
        mPhotoView.setOpenedItem(itemPath);
    }

    private void createData(final Path itemPath) {
        mCurrentIndex = mMediaSet.getIndexOfItem(itemPath, 0);
        if (DEBUG) Log.i(TAG, "Current index from getIndexOfItem: " + mCurrentIndex + " total count: " + mMediaSet.getTotalMediaItemCount());
        if (mCurrentIndex < 0 || mCurrentIndex > mMediaSet.getTotalMediaItemCount()) {
            mCurrentIndex = 0;
        }
        PhotoDataAdapter pda = new PhotoDataAdapter(mActivity, mPhotoView, mMediaSet, itemPath, mCurrentIndex);
        mModel = pda;
        mPhotoView.setModel(mModel);

        mResultIntent.putExtra(KEY_INDEX_HINT, mCurrentIndex);
        setStateResult(Activity.RESULT_OK, mResultIntent);

        pda.setDataListener(new PhotoDataAdapter.DataListener() {

            @Override
            public void onPhotoChanged(int index, Path item) {
                if (DEBUG) Log.w(TAG, "Photo changed to " + index + ", path " + item);
                if (mFilmStripView != null) mFilmStripView.setFocusIndex(index);
                mCurrentIndex = index;
                mResultIntent.putExtra(KEY_INDEX_HINT, index);
                if (item != null) {
                    mResultIntent.putExtra(KEY_MEDIA_ITEM_PATH, item.toString());
                    MediaItem photo = mModel.getCurrentMediaItem();
                    if (photo != null) updateCurrentPhoto(photo);
                } else {
                    mResultIntent.removeExtra(KEY_MEDIA_ITEM_PATH);
                }
                setStateResult(Activity.RESULT_OK, mResultIntent);

                notifyPhotoChanged(item);
            }

            private void notifyPhotoChanged(Path item) {
                ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
                if (DEBUG) Log.i(TAG, "Current activity: " + activity.getChanActivityId());
                if (activity != null && activity.getChanActivityId().activity == LastActivity.GALLERY_ACTIVITY) {
                    Handler handler = activity.getChanHandler();
                    if (handler != null && item != null) {
                        String[] pathParts = item.split();
                        if (pathParts.length == 4) {
                            Message msg = handler.obtainMessage(GalleryViewActivity.UPDATE_POSTNO_MSG, 0, 0);
                            msg.obj = pathParts[3];
                            handler.sendMessage(msg);
                        }
                    }
                }
            }

            @Override
            public void onLoadingFinished() {
                if (DEBUG) Log.i(TAG, "Loading finished ...");
                GalleryUtils.setSpinnerVisibility((Activity) mActivity, false);
                if (!mModel.isEmpty()) {
                    MediaItem photo = mModel.getCurrentMediaItem();
                    if (photo != null) updateCurrentPhoto(photo);
                    if (DEBUG) Log.i(TAG, "photo updated " + photo.getPath());
                } else if (mIsActive) {
                    mActivity.getStateManager().finishState(PhotoPage.this);
                }
            }

            @Override
            public void onLoadingStarted() {
                if (DEBUG) Log.i(TAG, "Loading started ...");
                GalleryUtils.setSpinnerVisibility((Activity) mActivity, true);
            }

            @Override
            public void onPhotoAvailable(long version, boolean fullImage) {
                if (DEBUG) Log.w(TAG, "Photo available version: " + version + ", fullImage: " + fullImage);
                hideOrPlayAnimGif(mModel.getCurrentMediaItem(), version);
                if (mFilmStripView == null) initFilmStripView();
            }
        });
    }

    private void hideOrPlayAnimGif(MediaItem photo) {
        hideOrPlayAnimGif(photo, photo == null ? 0 : photo.getDataVersion());
    }

    private void hideOrPlayAnimGif(MediaItem photo, long version) {
        if (playableAnimGif(photo)) {
            if (DEBUG) Log.w(TAG, "Playing anim gif");
            if (!isAnimatedGifVisible())
                playAnimatedGif(photo, version);
        }
        else {
            if (DEBUG) Log.w(TAG, "Hiding anim gif view");
            hideAnimatedGif();
        }
    }

    private boolean playableAnimGif(MediaItem photo) {
        return photo != null && photo.getPlayUri() != null && (photo.getSupportedOperations() & MediaObject.SUPPORT_ANIMATED_GIF) > 0;
    }

    private void getDefaultMediaSet(final Path itemPath) {
        // Get default media set by the URI
        MediaItem mediaItem = (MediaItem)
                mActivity.getDataManager().getMediaObject(itemPath);
        mModel = new SinglePhotoDataAdapter(mActivity, mPhotoView, mediaItem);
        mPhotoView.setModel(mModel);
        updateCurrentPhoto(mediaItem);
    }

    private void setTitle(String title) {
    	/*
        if (title == null) return;
        boolean showTitle = mActivity.getAndroidContext().getResources().getBoolean(
                R.bool.show_action_bar_title);
        if (showTitle) {
            mActionBar.setTitle(title);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        } else {
        	mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setTitle("");
        }
        */
    }

    private void updateCurrentPhoto(final MediaItem photo) {
        if (DEBUG) Log.w(TAG, "updateCurrentPhoto photo=" + photo + " path=" + photo.getPath() + ", uri: " + photo.getPlayUri());
        hideOrPlayAnimGif(photo);
        if (mCurrentPhoto != photo)
            mCurrentPhoto = photo;
        if (photo == null) return;
        updateDetails();
        updateMenuOperations();
        setTitle(photo.getName());
        mPhotoView.showVideoPlayIcon(
                photo.getMediaType() == MediaObject.MEDIA_TYPE_VIDEO);
        updateSharedIntent();
        final Path itemPath = photo.getPath();
        if (itemPath != null && !itemPath.toString().isEmpty())
            mData.putString(KEY_MEDIA_ITEM_PATH, itemPath.toString());

    }

    private void updateDetails() {
        if (mCurrentPhoto == null || mDetailsHelper == null)
            return;
        mDetailsHelper.reloadDetails(mModel.getCurrentIndex());
        if (DEBUG) Log.w(TAG, "updateCurrentPhoto mDetailsHelper=" + mDetailsHelper);
        /* can't get go-directly-to-post activity working yet
        if (mCurrentPhoto instanceof ChanImage && mDetailsHelper != null) {
            ChanImage image = (ChanImage)mCurrentPhoto;
            final ChanActivityId aid = image.getChanActivityId();
            if (aid != null)
                mDetailsHelper.setClickListener(R.string.picasa_posts, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String boardCode = aid.boardCode;
                        long threadNo = aid.threadNo;
                        long postNo = aid.postNo;
                        ThreadActivity.startActivity(mActivity.getAndroidContext(), boardCode, threadNo, postNo, "");
                    }
                });
        }
        */
    }

    private void updateMenuOperations() {
        if (mCurrentPhoto == null || mMenu == null) return;
        int supportedOperations = mCurrentPhoto.getSupportedOperations();
        if (!GalleryUtils.isEditorAvailable((Context) mActivity, "image/*")) {
            supportedOperations &= ~MediaObject.SUPPORT_EDIT;
        }
        updateSlideshowMenu();
        updateShareMenu();
        MenuExecutor.updateMenuOperation(mMenu, supportedOperations);
    }

    private void updateSlideshowMenu() {
        if (mMenu == null)
            return;
        MenuItem item = mMenu.findItem(R.id.action_slideshow);
        if (item != null) item.setVisible(mMediaSet != null && !(mMediaSet instanceof MtpDevice));
    }

    /*
    private void updateShareMenu() {
        /*

        updateSharedIntent(mShareActionProvider, postPos);
        if (mMenu == null)
            return;

        MenuItem item = mMenu.findItem(R.id.action_share);
        mShareActionProvider = item == null ? null : (ShareActionProvider) item.getActionProvider();

        if (menu == null)
            return;
        MenuItem shareItem = menu.findItem(com.chanapps.four.activity.R.id.thread_share_menu);
        mShareActionProvider = shareItem == null ? null : (ShareActionProvider) shareItem.getActionProvider();
        if (DEBUG) Log.i(TAG, "setupshareActionProvider() mShareActionProvider=" + mShareActionProvider);
    Handler handler = null;
        try {
            handler = new Handler();
        }
        catch (Exception e) {
            Log.e(TAG, "onCreateActionBar exception creating handler", e);
        }
        if (mPendingSharePath != null && handler != null) {
            final Handler finalHandler = handler;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    updateShareURI(mPendingSharePath, finalHandler);
                }
            }).start();
        }
    }
     */

    private void showBars() {
        if (mShowBars) return;
        mShowBars = true;
        mActionBar.show();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        WindowManager.LayoutParams params = ((Activity) mActivity).getWindow().getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE;
        ((Activity) mActivity).getWindow().setAttributes(params);
        if (mFilmStripView != null) {
            mFilmStripView.show();
        }
    }

    private void hideBars() {
        if (!mShowBars) return;
        mShowBars = false;
        mActionBar.hide();
        WindowManager.LayoutParams params = ((Activity) mActivity).getWindow().getAttributes();
        params.systemUiVisibility = View. SYSTEM_UI_FLAG_LOW_PROFILE;
        ((Activity) mActivity).getWindow().setAttributes(params);
        if (mFilmStripView != null) {
            mFilmStripView.hide();
        }
        //FOO
    }

    private void refreshHidingMessage() {
        mHandler.removeMessages(MSG_HIDE_BARS);
        if (!mIsMenuVisible && !mIsInteracting) {
            mHandler.sendEmptyMessageDelayed(MSG_HIDE_BARS, HIDE_BARS_TIMEOUT);
        }
    }

    public void onUserInteraction() {
        showBars();
        refreshHidingMessage();
    }

    public void onUserInteractionTap() {
        if (mShowBars) {
            hideBars();
            mHandler.removeMessages(MSG_HIDE_BARS);
        } else {
            showBars();
            refreshHidingMessage();
        }
    }

    public void onUserInteractionBegin() {
        showBars();
        mIsInteracting = true;
        refreshHidingMessage();
    }

    public void onUserInteractionEnd() {
        mIsInteracting = false;
        refreshHidingMessage();
    }

    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else {
            PositionRepository repository = PositionRepository.getInstance(mActivity);
            repository.clear();
            if (mCurrentPhoto != null) {
                Position position = new Position();
                position.x = mRootPane.getWidth() / 2;
                position.y = mRootPane.getHeight() / 2;
                position.z = -1000;
                repository.putPosition(
                        Long.valueOf(System.identityHashCode(mCurrentPhoto.getPath())),
                        position);
            }
            super.onBackPressed();
        }
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        GalleryActionBar actionBar = mActivity.getGalleryActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        MenuInflater inflater = ((Activity) mActivity).getMenuInflater();
        inflater.inflate(R.menu.photo, menu);
        if (menu == null)
            return true;

        mMenu = menu;
        mShowBars = true;
        updateMenuOperations();

        return true;
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        MediaItem current = mModel.getCurrentMediaItem();

        if (current == null) {
            // item is not ready, ignore
            return true;
        }

        int currentIndex = mModel.getCurrentIndex();
        Path path = current.getPath();

        DataManager manager = mActivity.getDataManager();
        int action = item.getItemId();
        if (action == R.id.action_slideshow) {
            Bundle data = new Bundle();
            data.putString(SlideshowPage.KEY_SET_PATH, mMediaSet.getPath().toString());
            data.putInt(SlideshowPage.KEY_PHOTO_INDEX, currentIndex);
            data.putBoolean(SlideshowPage.KEY_REPEAT, true);
            mActivity.getStateManager().startStateForResult(
                    SlideshowPage.class, REQUEST_SLIDESHOW, data);
            return true;
        } else if (action == R.id.action_crop) {
            Activity activity = (Activity) mActivity;
            Intent intent = new Intent(CropImage.CROP_ACTION);
            intent.setClass(activity, CropImage.class);
            intent.setData(manager.getContentUri(path));
            activity.startActivityForResult(intent, PicasaSource.isPicasaImage(current)
                    ? REQUEST_CROP_PICASA
                    : REQUEST_CROP);
            return true;
        } else if (action == R.id.action_details) {
            if (mShowDetails) {
                hideDetails();
            } else {
                showDetails(currentIndex);
            }
            return true;
        } else if (action == R.id.action_download) {
            mSelectionManager.toggle(path);
            ArrayList<Path> ids = mSelectionManager.getSelected(true);
            ThreadImageDownloadService.startDownloadViaGalleryView(mActivity.getAndroidContext(), mMediaSet.getPath(), ids);
            mSelectionManager.toggle(path);
            Toast.makeText(mActivity.getAndroidContext(),
                    R.string.download_all_images_notice,
                    Toast.LENGTH_SHORT)
                    .show();
            return true;
        } else if (action == R.id.action_setas || action == R.id.action_confirm_delete || action == R.id.action_rotate_ccw
                || action == R.id.action_rotate_cw || action == R.id.action_show_on_map || action == R.id.action_edit) {
            //mSelectionManager.deSelectAll();
            mSelectionManager.toggle(path);
            mMenuExecutor.onMenuClicked(item, null);
            return true;
        } else if (action == R.id.action_import) {
            mSelectionManager.deSelectAll();
            mSelectionManager.toggle(path);
            mMenuExecutor.onMenuClicked(item,
                    new ImportCompleteListener(mActivity));
            return true;
        } else if (action == R.id.image_search_menu) {
            imageSearch(URLFormatComponent.getUrl(mActivity.getAndroidContext(), URLFormatComponent.TINEYE_IMAGE_SEARCH_URL_FORMAT));
            return true;
        }
        else if (action == R.id.anime_image_search_menu) {
            imageSearch(URLFormatComponent.getUrl(mActivity.getAndroidContext(), URLFormatComponent.ANIME_IMAGE_SEARCH_URL_FORMAT));
            return true;
        } else {
            return false;
        }
    }

    private void hideDetails() {
        mShowDetails = false;
        if (mDetailsHelper != null)
            mDetailsHelper.hide();
    }

    private void showDetails(int index) {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, new MyDetailsSource());
            mDetailsHelper.setCloseListener(new CloseListener() {
                public void onClose() {
                    hideDetails();
                }
            });
        }
        //mDetailsHelper.reloadDetails(index);
        updateDetails();
        mDetailsHelper.show();
    }

    public void onSingleTapUp(int x, int y) {
        MediaItem item = mModel.getCurrentMediaItem();
        if (item == null) {
            // item is not ready, ignore
            return;
        }

        boolean playGif = playableAnimGif(item);
        boolean playVideo = (item.getSupportedOperations() & MediaItem.SUPPORT_PLAY) != 0;

        /*
        if (playVideo) {
            // determine if the point is at center (1/6) of the photo view.
            // (The position of the "play" icon is at center (1/6) of the photo)
            int w = mPhotoView.getWidth();
            int h = mPhotoView.getHeight();
            playVideo = (Math.abs(x - w / 2) * 12 <= w)
                    && (Math.abs(y - h / 2) * 12 <= h);
        }
        */

        if (playGif) {
            hideOrPlayAnimGif(item);
        }
        else if (playVideo) {
            hideAnimatedGif();
            playVideo((Activity) mActivity, item.getPlayUri(), item.getPath(), item.getMimeType());
        } else {
            hideAnimatedGif();
            onUserInteractionTap();
        }
    }

    public static void playVideo(Activity activity, Uri uri, Path path, String mimeType) {
        try {
            /*
            String title = null;
            String[] parts = path.split();
            if (parts.length == 3) {
                title = "/" + parts[1] + "/" + parts[2];
            } else if (parts.length == 4) {
                title = "/" + parts[1] + "/" + parts[2] + ":" + parts[3];
            }
            intent.putExtra(Intent.EXTRA_TITLE, title);
            */
            //Toast.makeText(activity, "play video uri=" + uri + " mimeType=" + mimeType, Toast.LENGTH_SHORT).show();
            ChanImage.startViewer(activity, uri, mimeType);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err), Toast.LENGTH_SHORT).show();
        }

    }

    private void hideAnimatedGif() {
        Activity activity = (Activity)NetworkProfileManager.instance().getActivity();
        if (activity == null)
            return;
        View view = activity.findViewById(com.chanapps.four.activity.R.id.gifview);
        if (view == null)
            return;
        view.setVisibility(View.GONE);
    }

    private boolean isAnimatedGifVisible() {
        Activity activity = (Activity)NetworkProfileManager.instance().getActivity();
        if (activity == null)
            return false;
        View view = activity.findViewById(com.chanapps.four.activity.R.id.gifview);
        if (view == null)
            return false;
        return (view.getVisibility() == View.VISIBLE);
    }

    public void playAnimatedGif(MediaItem item, long version) {
        if (item == null)
            return;
        Activity activity = (Activity)NetworkProfileManager.instance().getActivity();
        if (activity == null) {
            if (DEBUG) Log.i(TAG, "Play anim gif null activity, exiting");
            return;
        }
        ViewGroup contentView = (ViewGroup)activity.findViewById(android.R.id.content);
        View rootView = contentView.getRootView();
        //ViewGroup galleryFrameLayout = (ViewGroup)activity.findViewById(com.chanapps.four.activity.R.id.gallery_frame_layout);
        View view = contentView.findViewById(com.chanapps.four.activity.R.id.gifview);
        //if (view == null || view.getVisibility() != View.GONE) {
        if (view == null) {
            if (DEBUG) Log.i(TAG, "Play anim gif null gifview, recreating activity=" + activity);
            activity.recreate();
            return;
            /*
            LayoutInflater inflater = activity.getLayoutInflater();
            view = inflater.inflate(com.chanapps.four.activity.R.layout.gifview, galleryFrameLayout, false);
            if (view == null) {
                if (DEBUG) Log.i(TAG, "Couldn't recreate gifview, exiting");
                return;
            }
            galleryFrameLayout.addView(view, 2);
            if (DEBUG) Log.i(TAG, "Recreated gifview=" + view);
		    */
        }
        WebView myWebView = (WebView) view.findViewById(com.chanapps.four.activity.R.id.video_view);
        if (myWebView == null) {
            if (DEBUG) Log.i(TAG, "Exiting play anim gif since null webview");
            return;
        }
        Uri localPlayUri = item.getPlayUri();
        if (localPlayUri == null) {
            if (DEBUG) Log.i(TAG, "Exiting play anim gif since null webview url");
            return;
        }
        if (myWebView.isFocused()) {
            if (DEBUG) Log.i(TAG, "Already focused, exiting");
        }
        /*
        if ((localPlayUri + "#" + version).equals(myWebView.getTag())) {
            if (DEBUG) Log.i(TAG, "Exiting play anim gif since already loaded url=" + localPlayUri + " version=" + version);
            return;
        }
        */
        if (DEBUG) Log.w(TAG, "Screen size w: " + rootView.getMeasuredWidth() + " h: " + rootView.getMeasuredHeight());
        if (DEBUG) Log.w(TAG, "Image  size w: " + item.getWidth() + " h: " + item.getHeight());

        int maxWidth = rootView.getMeasuredWidth() == 0 ? 200 : rootView.getMeasuredWidth();
        int maxHeight = (rootView.getMeasuredHeight() == 0 ? 200 : rootView.getMeasuredHeight());

        int itemWidth = item.getWidth() == 0 ? maxWidth : item.getWidth();
        int itemHeight = item.getHeight() == 0 ? maxHeight : item.getHeight();
        int scale = Math.min(maxWidth * 100 / itemWidth, maxHeight * 100 / itemHeight);
        if (DEBUG) Log.w(TAG, "scale: " + scale);

        myWebView.setLayoutParams(new FrameLayout.LayoutParams(itemWidth * scale / 100, itemHeight * scale / 100));
        myWebView.setInitialScale(scale);
        myWebView.getRootView().setBackgroundColor(0x000000);
        myWebView.setBackgroundColor(0x000000);
        myWebView.getSettings().setJavaScriptEnabled(false);
        myWebView.getSettings().setBuiltInZoomControls(false);

        if (DEBUG) Log.i(TAG, "Loading anim gif webview url = " + localPlayUri);
        myWebView.loadUrl(localPlayUri.toString());
        myWebView.setTag(localPlayUri + "#" + version);

        view.setVisibility(View.VISIBLE);
    }


    // Called by FileStripView.
    // Returns false if it cannot jump to the specified index at this time.
    public boolean onSlotSelected(int slotIndex) {
        return mPhotoView.jumpTo(slotIndex);
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) break;
                    Path path = mApplication
                            .getDataManager().findPathByUri(data.getData());
                    if (path != null) {
                        mModel.setCurrentPhoto(path, mCurrentIndex);
                    }
                }
                break;
            case REQUEST_CROP_PICASA: {
                int message = resultCode == Activity.RESULT_OK
                        ? R.string.crop_saved
                        : R.string.crop_not_saved;
                Toast.makeText(mActivity.getAndroidContext(),
                        message, Toast.LENGTH_SHORT).show();
                break;
            }
            case REQUEST_SLIDESHOW: {
                if (data == null) break;
                String path = data.getStringExtra(SlideshowPage.KEY_ITEM_PATH);
                int index = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                if (path != null) {
                    mModel.setCurrentPhoto(Path.fromString(path), index);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;
        if (mFilmStripView != null) {
            mFilmStripView.pause();
        }
        DetailsHelper.pause();
        if (mPhotoView != null) mPhotoView.pause();
        if (mModel != null) mModel.pause();
        if (mHandler != null) mHandler.removeMessages(MSG_HIDE_BARS);
        if (mActionBar != null) mActionBar.removeOnMenuVisibilityListener(mMenuVisibilityListener);
        if (mMenuExecutor != null) mMenuExecutor.pause();
        hideAnimatedGif();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActive = true;
        setContentPane(mRootPane);
        mModel.resume();
        mPhotoView.resume();
        if (mFilmStripView != null) {
            mFilmStripView.resume();
        }
        if (mMenuVisibilityListener == null) {
            mMenuVisibilityListener = new MyMenuVisibilityListener();
        }
        mActionBar.addOnMenuVisibilityListener(mMenuVisibilityListener);
        onUserInteraction();
    }

    private class MyDetailsSource implements DetailsSource {
        private int mIndex;

        @Override
        public MediaDetails getDetails() {
            return mModel.getCurrentMediaItem().getDetails();
        }

        @Override
        public int size() {
            return mMediaSet != null ? mMediaSet.getMediaItemCount() : 1;
        }

        @Override
        public int findIndex(int indexHint) {
            mIndex = indexHint;
            return indexHint;
        }

        @Override
        public int getIndex() {
            return mIndex;
        }
    }

    protected void updateShareMenu() {
        updateSharedIntent();
        if (mMenu == null)
            return;
        MenuItem shareItem = mMenu.findItem(R.id.action_share);
        if (shareItem == null) {
            mShareActionProvider = null;
            return;
        }
        mShareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
        shareItem.setOnMenuItemClickListener(shareActionItemListener);
        if (DEBUG) Log.i(TAG, "setupshareActionProvider() mShareActionProvider=" + mShareActionProvider);
    }

    protected MenuItem.OnMenuItemClickListener shareActionItemListener = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            onUserInteractionBegin(); // don't let menu disappear
            return false;
        }
    };

    private void setShareIntent(final Intent intent) {
        Handler handler = null;
        try {
            handler = new Handler();
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't create handler", e);
        }
        if (ActivityDispatcher.onUIThread())
            synchronized (this) {
                if (mShareActionProvider != null && intent != null)
                    mShareActionProvider.setShareIntent(intent);
            }
        else if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        if (mShareActionProvider != null && intent != null)
                            mShareActionProvider.setShareIntent(intent);
                    }
                }
            });
    }

    protected void updateSharedIntent() {
        boolean shareImageUrl = PreferenceManager
                .getDefaultSharedPreferences((Context)mActivity)
                .getBoolean(SettingsActivity.PREF_SHARE_IMAGE_URL, false);
        if (shareImageUrl)
            setURLShareIntent();
        else
            setImageShareIntent();
    }

    private void setImageShareIntent() {
        //File localImage = new File(URI.create(filePath));
        //if (localImage == null || !localImage.exists() || !localImage.canRead() || localImage.length() <= 0) {
        //    Log.e(TAG, "updateSharedIntent no image file found for path=" + filePath);
        //    return;
        //}

        // set share text
        //if (DEBUG) Log.i(TAG, "updateSharedIntent() found postNo=" + firstPost + " for threadNo=" + threadNo);
        //String linkUrl = (firstPost > 0 && firstPost != threadNo)
        //        ? ChanPost.postUrl(boardCode, threadNo, firstPost)
        //       : ChanThread.threadUrl(boardCode, threadNo);
        //String text = selectText(postPos);
        //String extraText = linkUrl + (text.isEmpty() ? "" : "\n\n" + text);

        // create intent
        //if (paths.size() == 0) {
        //intent = new Intent(Intent.ACTION_SEND);
        //    intent.putExtra(Intent.EXTRA_TEXT, extraText);
        //    intent.setType("text/html");
        //intent.putExtra(Intent.EXTRA_TEXT, linkUrl);
        //intent.setType("text/plain");
        //setShareIntent(mShareActionProvider, intent);
        /*
        } else {
            ArrayList<Uri> uris = new ArrayList<Uri>();
            ArrayList<String> missingPaths = new ArrayList<String>();
            for (String path : paths) {
                if (checkedImageUris.containsKey(path)) {
                    Uri uri = checkedImageUris.get(path);
                    uris.add(uri);
                    if (DEBUG) Log.i(TAG, "Added uri=" + uri);
                } else {
                    uris.add(Uri.fromFile(new File(path)));
                    missingPaths.add(path);
                }
            }
            */
        //intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        //Uri uri = Uri.fromFile(localImage);
        if (mCurrentPhoto == null) {
            if (DEBUG) Log.i(TAG, "updateSharedIntent no current photo, exiting");
            return;
        }
        if (DEBUG) Log.i(TAG, "updateSharedIntent mCurrentPhoto=" + mCurrentPhoto);
        DataManager manager = mActivity.getDataManager();
        int type = mCurrentPhoto.getMediaType();
        String mimeType = MenuExecutor.getMimeType(type);
        Uri uri = mCurrentPhoto.getContentUri();
        if (uri != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType(mimeType);
            setShareIntent(intent);
            if (DEBUG) Log.i(TAG, "updateSharedIntent mimeType=" + mimeType + " uri=" + uri.toString());
        }
    }

    private void setURLShareIntent() {
        if (mCurrentPhoto == null) {
            if (DEBUG) Log.i(TAG, "updateSharedIntent no current photo, exiting");
            return;
        }
        Object u = mCurrentPhoto.getDetails().getDetail(MediaDetails.INDEX_PATH);
        String url = u instanceof String ? (String)u : null;
        if (url != null && !url.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, url);
            intent.setType("text/plain");
            setShareIntent(intent);
            if (DEBUG) Log.i(TAG, "updateSharedIntent URL=" + url);
        }
    }

    private void imageSearch(String urlFormat) {
        MediaDetails details = mCurrentPhoto.getDetails();
        if (details == null) {
            Toast.makeText((Context)mActivity, com.chanapps.four.activity.R.string.full_screen_image_search_not_found, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        Object o = details.getDetail(MediaDetails.INDEX_PATH);
        if (o == null || !(o instanceof String) || ((String)o).isEmpty()) {
            Toast.makeText((Context)mActivity, com.chanapps.four.activity.R.string.full_screen_image_search_not_found, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        String imageUrl = (String)o;
        if (imageUrl.isEmpty()) {
            Toast.makeText((Context)mActivity, com.chanapps.four.activity.R.string.full_screen_image_search_not_found, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        try {
            ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
            String encodedImageUrl = URLEncoder.encode(imageUrl, "UTF-8");
            String url = String.format(urlFormat, encodedImageUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            ((Activity)activity).startActivity(intent);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't do image search imageUrl=" + imageUrl, e);
            Toast.makeText((Context)mActivity, com.chanapps.four.activity.R.string.full_screen_image_search_error, Toast.LENGTH_SHORT).show();
        }
    }

}
