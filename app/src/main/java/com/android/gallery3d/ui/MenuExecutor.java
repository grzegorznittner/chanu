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

package com.android.gallery3d.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.gallery3d.app.CropImage;
import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.chanapps.four.gallery3d.R;
import com.chanapps.four.service.ThreadImageDownloadService;

import java.util.ArrayList;

public class MenuExecutor {
    public static final int EXECUTION_RESULT_SUCCESS = 1;
    public static final int EXECUTION_RESULT_FAIL = 2;
    public static final int EXECUTION_RESULT_CANCEL = 3;
    @SuppressWarnings("unused")
    private static final String TAG = "MenuExecutor";
    private static final int MSG_TASK_COMPLETE = 1;
    private static final int MSG_TASK_UPDATE = 2;
    private static final int MSG_DO_SHARE = 3;
    private final GalleryActivity mActivity;
    private final SelectionManager mSelectionManager;
    private final Handler mHandler;
    private ProgressDialog mDialog;
    private Future<?> mTask;

    public MenuExecutor(GalleryActivity activity, SelectionManager selectionManager) {
        mActivity = Utils.checkNotNull(activity);
        mSelectionManager = Utils.checkNotNull(selectionManager);
        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_TASK_COMPLETE: {
                        stopTaskAndDismissDialog();
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressComplete(message.arg1);
                        }
                        mSelectionManager.leaveSelectionMode();
                        break;
                    }
                    case MSG_TASK_UPDATE: {
                        if (mDialog != null) mDialog.setProgress(message.arg1);
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressUpdate(message.arg1);
                        }
                        break;
                    }
                    case MSG_DO_SHARE: {
                        ((Activity) mActivity).startActivity((Intent) message.obj);
                        break;
                    }
                }
            }
        };
    }

    private static ProgressDialog showProgressDialog(Context context, int titleId, int progressMax) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(titleId);
        dialog.setMax(progressMax);
        dialog.setCancelable(false);
        dialog.setIndeterminate(false);
        if (progressMax > 1) {
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        }
        dialog.show();
        return dialog;
    }

    private static void setMenuItemVisibility(Menu menu, int id, boolean visibility) {
        MenuItem item = menu.findItem(id);
        if (item != null) item.setVisible(visibility);
    }

    public static void updateMenuOperation(Menu menu, int supported) {
        boolean supportCrop = (supported & MediaObject.SUPPORT_CROP) != 0;
        boolean supportShare = (supported & MediaObject.SUPPORT_SHARE) != 0;
        boolean supportSetAs = (supported & MediaObject.SUPPORT_SETAS) != 0;
        boolean supportEdit = (supported & MediaObject.SUPPORT_EDIT) != 0;
        boolean supportInfo = (supported & MediaObject.SUPPORT_INFO) != 0;

        setMenuItemVisibility(menu, R.id.action_download, true);
        setMenuItemVisibility(menu, R.id.action_crop, supportCrop);
        setMenuItemVisibility(menu, R.id.action_share, supportShare);
        setMenuItemVisibility(menu, R.id.action_setas, supportSetAs);
        setMenuItemVisibility(menu, R.id.action_edit, supportEdit);
        setMenuItemVisibility(menu, R.id.action_details, supportInfo);
    }

    public static String getMimeType(int type) {
        switch (type) {
            case MediaObject.MEDIA_TYPE_IMAGE:
                return "image/*";
            case MediaObject.MEDIA_TYPE_VIDEO:
                return "video/*";
            default:
                return "*/*";
        }
    }

    private void stopTaskAndDismissDialog() {
        if (mTask != null) {
            mTask.cancel();
            mTask.waitDone();
            mDialog.dismiss();
            mDialog = null;
            mTask = null;
        }
    }

    public void pause() {
        stopTaskAndDismissDialog();
    }

    private void onProgressUpdate(int index, ProgressListener listener) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TASK_UPDATE, index, 0, listener));
    }

    private void onProgressComplete(int result, ProgressListener listener) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TASK_COMPLETE, result, 0, listener));
    }

    private Path getSingleSelectedPath() {
        ArrayList<Path> ids = mSelectionManager.getSelected(true);
        Utils.assertTrue(ids.size() == 1);
        return ids.get(0);
    }

    public boolean onMenuClicked(MenuItem menuItem, ProgressListener listener) {
        DataManager manager = mActivity.getDataManager();
        int action = menuItem.getItemId();
        if (action == R.id.action_select_all) {
            if (mSelectionManager.inSelectAllMode()) {
                mSelectionManager.deSelectAll();
            } else {
                mSelectionManager.selectAll();
            }
            return true;
        } else if (action == R.id.action_download) {
            ArrayList<Path> ids = mSelectionManager.getSelected(true);
            if (ids != null && ids.size() > 0) {
                ThreadImageDownloadService.startDownloadViaGalleryView(mActivity.getAndroidContext(), ids.get(0), ids);
            }
            return true;
        } else if (action == R.id.action_crop) {
            Path path = getSingleSelectedPath();
            String mimeType = getMimeType(manager.getMediaType(path));
            Intent intent = new Intent(CropImage.ACTION_CROP).setDataAndType(manager.getContentUri(path), mimeType);
            ((Activity) mActivity).startActivity(intent);
            return true;
        } else if (action == R.id.action_setas) {
            Path path = getSingleSelectedPath();
            int type = manager.getMediaType(path);
            Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
            String mimeType = getMimeType(type);
            intent.setDataAndType(manager.getContentUri(path), mimeType);
            intent.putExtra("mimeType", mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Activity activity = (Activity) mActivity;
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.set_as)));
            return true;
        }  else {
            return false;
        }
    }

    public void startAction(int action, int title, ProgressListener listener) {
        ArrayList<Path> ids = mSelectionManager.getSelected(false);
        stopTaskAndDismissDialog();

        Activity activity = (Activity) mActivity;
        mDialog = showProgressDialog(activity, title, ids.size());
        MediaOperation operation = new MediaOperation(action, ids, listener);
        mTask = mActivity.getThreadPool().submit(operation, null);
    }

    private boolean execute(DataManager manager, JobContext jc, int cmd, Path path) {
        boolean result = true;
        Log.v(TAG, "Execute cmd: " + cmd + " for " + path);
        long startTime = System.currentTimeMillis();

        if (cmd == R.id.action_toggle_full_caching) {
            MediaObject obj = manager.getMediaObject(path);
            int cacheFlag = obj.getCacheFlag();
            if (cacheFlag == MediaObject.CACHE_FLAG_FULL) {
                cacheFlag = MediaObject.CACHE_FLAG_SCREENNAIL;
            } else {
                cacheFlag = MediaObject.CACHE_FLAG_FULL;
            }
            obj.cache(cacheFlag);
        } else if (cmd == R.id.action_edit) {
            Activity activity = (Activity) mActivity;
            MediaItem item = (MediaItem) manager.getMediaObject(path);
            try {
                activity.startActivity(Intent.createChooser(new Intent(Intent.ACTION_EDIT).setDataAndType(item.getContentUri(), item.getMimeType()).setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null));
            } catch (Throwable t) {
                Log.w(TAG, "failed to start edit activity: ", t);
                Toast.makeText(activity, activity.getString(R.string.activity_not_found), Toast.LENGTH_SHORT).show();
            }
        } else {
            throw new AssertionError();
        }
        Log.v(TAG, "It takes " + (System.currentTimeMillis() - startTime) + " ms to execute cmd for " + path);
        return result;
    }

    public interface ProgressListener {
        void onProgressUpdate(int index);

        void onProgressComplete(int result);
    }

    private class MediaOperation implements Job<Void> {
        private final ArrayList<Path> mItems;
        private final int mOperation;
        private final ProgressListener mListener;

        public MediaOperation(int operation, ArrayList<Path> items, ProgressListener listener) {
            mOperation = operation;
            mItems = items;
            mListener = listener;
        }

        public Void run(JobContext jc) {
            int index = 0;
            DataManager manager = mActivity.getDataManager();
            int result = EXECUTION_RESULT_SUCCESS;
            try {
                for (Path id : mItems) {
                    if (jc.isCancelled()) {
                        result = EXECUTION_RESULT_CANCEL;
                        break;
                    }
                    if (!execute(manager, jc, mOperation, id)) {
                        result = EXECUTION_RESULT_FAIL;
                    }
                    onProgressUpdate(index++, mListener);
                }
            } catch (Throwable th) {
                Log.e(TAG, "failed to execute operation " + mOperation + " : " + th);
            } finally {
                onProgressComplete(result, mListener);
            }
            return null;
        }
    }
}

