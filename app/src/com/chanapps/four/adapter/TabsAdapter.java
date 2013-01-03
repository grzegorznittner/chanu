package com.chanapps.four.adapter;

import android.app.ActionBar;
import android.nfc.Tag;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.fragment.BoardGroupFragment;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;

/**
 * This is a helper class that implements the management of tabs and all
 * details of connecting a ViewPager with associated TabHost.  It relies on a
 * trick.  Normally a tab host has a simple API for supplying a View or
 * Intent that each tab will show.  This is not sufficient for switching
 * between pages.  So instead we make the content part of the tab host
 * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
 * view to show as the tab content.  It listens to changes in tabs, and takes
 * care of switch to the correct paged in the ViewPager whenever the selected
 * tab changes.
 */
public class TabsAdapter extends FragmentPagerAdapter
        implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
    private final BoardSelectorActivity mContext;
    private final ActionBar mActionBar;
    private final ViewPager mViewPager;
    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

    static final class TabInfo {
        private final Class<?> clss;
        private final Bundle args;

        TabInfo(Class<?> _class, Bundle _args) {
            clss = _class;
            args = _args;
        }
    }

    public TabsAdapter(BoardSelectorActivity activity, FragmentManager fm, ViewPager pager) {
        super(fm);
        mContext = activity;
        mActionBar = activity.getActionBar();
        mViewPager = pager;
        mViewPager.setAdapter(this);
        mViewPager.setOnPageChangeListener(this);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        //if (position >= getCount()) {
        if (object != null && object instanceof Fragment) {
            FragmentManager manager = ((Fragment) object).getFragmentManager();
            FragmentTransaction trans = manager.beginTransaction();
            trans.remove((Fragment) object);
            trans.commit();
        }
        //}
    }

    public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
        addTab(tab, clss, args, -1);
    }

    public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args, int position) {
        TabInfo info = new TabInfo(clss, args);
        tab.setTag(info);
        tab.setTabListener(this);
        if (position >= 0) {
            mTabs.add(position, info);
            mActionBar.addTab(tab, position);
        }
        else {
            mTabs.add(info);
            mActionBar.addTab(tab);
        }
        notifyDataSetChanged();
    }

    public void removeTab(int position) {
        mTabs.remove(position);
        mActionBar.removeTabAt(position);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public Fragment getItem(int position) {
        TabInfo info = mTabs.get(position);
        String selectedType = info.args.getString(ChanHelper.BOARD_TYPE);
        Log.d(BoardSelectorActivity.TAG, "TabsAdapter " + selectedType + " instantiating Fragment");
        return Fragment.instantiate(mContext, info.clss.getName(), info.args);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//        mContext.invalidateOptionsMenu();
    }

    @Override
    public void onPageSelected(int position) {
        mContext.selectedBoardType = mContext.activeBoardTypes.get(position);
        //mContext.invalidateOptionsMenu();
        if (mActionBar != null) {
            mActionBar.setSelectedNavigationItem(position);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
        Object tag = tab.getTag();
        for (int i=0; i<mTabs.size(); i++) {
            if (mTabs.get(i) == tag) {
                Log.d(BoardSelectorActivity.TAG, "TabAdapter setting pager to " + i);
                mViewPager.setCurrentItem(i);
                Fragment fragment = getItem(i);
                Menu menu = mContext.menu;
                if (fragment != null && menu != null) {
                    ((BoardGroupFragment)fragment).onPrepareOptionsMenu(menu, mContext, mContext.selectedBoardType);
                }
            }
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction transaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction transaction) {
    }

}
