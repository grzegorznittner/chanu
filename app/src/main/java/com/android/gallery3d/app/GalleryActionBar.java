/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.chanapps.four.gallery3d.R;

public class GalleryActionBar implements ActionBar.OnNavigationListener {
    private static final String TAG = "GalleryActionBar";
    private static final ActionItem[] sClusterItems = new ActionItem[]{new ActionItem(FilterUtils.CLUSTER_BY_ALBUM, true, false, R.string.albums, R.string.group_by_album), new ActionItem(FilterUtils.CLUSTER_BY_LOCATION, true, false, R.string.locations, R.string.location, R.string.group_by_location), new ActionItem(FilterUtils.CLUSTER_BY_TIME, true, false, R.string.times, R.string.time, R.string.group_by_time), new ActionItem(FilterUtils.CLUSTER_BY_FACE, true, false, R.string.people, R.string.group_by_faces), new ActionItem(FilterUtils.CLUSTER_BY_TAG, true, false, R.string.tags, R.string.group_by_tags)};
    private ClusterRunner mClusterRunner;
    //private CharSequence[] mTitles;
    //private ArrayList<Integer> mActions;
    private Context mContext;
    private LayoutInflater mInflater;
    private GalleryActivity mActivity;
    private ActionBar mActionBar;
    private int mCurrentIndex;
    private ClusterAdapter mAdapter = new ClusterAdapter();

    public GalleryActionBar(GalleryActivity activity) {
        mActionBar = ((Activity) activity).getActionBar();
        mContext = activity.getAndroidContext();
        mActivity = activity;
        mInflater = ((Activity) mActivity).getLayoutInflater();
        mCurrentIndex = 0;
    }

    public static int getHeight(Activity activity) {
        ActionBar actionBar = activity.getActionBar();
        return actionBar != null ? actionBar.getHeight() : 0;
    }

    public static String getClusterByTypeString(Context context, int type) {
        for (ActionItem item : sClusterItems) {
            if (item.action == type) {
                return context.getString(item.clusterBy);
            }
        }
        return null;
    }

    public static ShareActionProvider initializeShareActionProvider(Menu menu) {
        //todo fix also this share, like the others
        MenuItem item = menu.findItem(R.id.action_share);
        ShareActionProvider shareActionProvider = null;
        if (item != null) shareActionProvider = (ShareActionProvider) item.getActionProvider();
        return shareActionProvider;
    }

    /*
    private void createDialogData() {
        //ArrayList<CharSequence> titles = new ArrayList<CharSequence>();
        //mActions = new ArrayList<Integer>();
        //for (ActionItem item : sClusterItems) {
        //    if (item.enabled && item.visible) {
        //        titles.add(mContext.getString(item.dialogTitle));
        //        mActions.add(item.action);
        //    }
        //}
        //mTitles = new CharSequence[titles.size()];
        //titles.toArray(mTitles);
    }
    */
    public void setClusterItemEnabled(int id, boolean enabled) {
        for (ActionItem item : sClusterItems) {
            if (item.action == id) {
                item.enabled = enabled;
                return;
            }
        }
    }

    public void setClusterItemVisibility(int id, boolean visible) {
        for (ActionItem item : sClusterItems) {
            if (item.action == id) {
                item.visible = visible;
                return;
            }
        }
    }

    public int getClusterTypeAction() {
        return sClusterItems[mCurrentIndex].action;
    }

    public void hideClusterMenu() {
        //mClusterRunner = null;
        //mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }

    public String getTitle() {
        if (mActionBar != null && mActionBar.getTitle() != null)
            return mActionBar.getTitle().toString();
        return null;
    }

    public void setTitle(String title) {
        if (mActionBar != null) mActionBar.setTitle(title);
    }

    public void setTitle(int titleId) {
        if (mActionBar != null) mActionBar.setTitle(titleId);
    }

    public void setSubtitle(String title) {
        if (mActionBar != null) mActionBar.setSubtitle(title);
    }

    public int getHeight() {
        return mActionBar == null ? 0 : mActionBar.getHeight();
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (itemPosition != mCurrentIndex && mClusterRunner != null) {
            mActivity.getGLRoot().lockRenderThread();
            try {
                mClusterRunner.doCluster(sClusterItems[itemPosition].action);
            } finally {
                mActivity.getGLRoot().unlockRenderThread();
            }
        }
        return false;
    }

    public void setDisplayHomeAsUpEnabled(boolean enabled) {
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(enabled);
        }
    }

    public void setDisplayShowHomeEnabled(boolean enabled) {
        if (mActionBar != null) {
            mActionBar.setDisplayShowHomeEnabled(enabled);
        }
    }

    public interface ClusterRunner {
        void doCluster(int id);
    }

    private static class ActionItem {
        public int action;
        public boolean enabled;
        public boolean visible;
        public int spinnerTitle;
        public int dialogTitle;
        public int clusterBy;

        public ActionItem(int action, boolean applied, boolean enabled, int title, int clusterBy) {
            this(action, applied, enabled, title, title, clusterBy);
        }

        public ActionItem(int action, boolean applied, boolean enabled, int spinnerTitle, int dialogTitle, int clusterBy) {
            this.action = action;
            this.enabled = enabled;
            this.spinnerTitle = spinnerTitle;
            this.dialogTitle = dialogTitle;
            this.clusterBy = clusterBy;
            this.visible = true;
        }
    }

    private class ClusterAdapter extends BaseAdapter {

        public int getCount() {
            return sClusterItems.length;
        }

        public Object getItem(int position) {
            return sClusterItems[position];
        }

        public long getItemId(int position) {
            return sClusterItems[position].action;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.action_bar_text, parent, false);
            }
            TextView view = (TextView) convertView;
            view.setText(sClusterItems[position].spinnerTitle);
            return convertView;
        }
    }
}
