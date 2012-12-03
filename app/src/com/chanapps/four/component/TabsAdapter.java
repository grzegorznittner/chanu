package com.chanapps.four.component;

import android.app.ActionBar;
import android.nfc.Tag;
import android.support.v4.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.ViewGroup;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.activity.BoardSelectorActivity;

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
        if (position >= getCount()) {
            FragmentManager manager = ((Fragment) object).getFragmentManager();
            FragmentTransaction trans = manager.beginTransaction();
            trans.remove((Fragment) object);
            trans.commit();
        }
    }

    public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
        TabInfo info = new TabInfo(clss, args);
        tab.setTag(info);
        tab.setTabListener(this);
        mTabs.add(info);
        mActionBar.addTab(tab);
        notifyDataSetChanged();
    }

    public void removeTab(ChanBoard.Type type) {
        int i = type.ordinal();
        mTabs.remove(i);
        mActionBar.removeTabAt(i);
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
    }

    @Override
    public void onPageSelected(int position) {
        Log.i(BoardSelectorActivity.TAG, "onPageSelected position: " + position + ", storing boardType: " + ChanBoard.Type.values()[position]);
        if (mContext != null) {
        SharedPreferences prefs = mContext.getSharedPreferences(ChanHelper.PREF_NAME, 0);
            if (prefs != null) {
                SharedPreferences.Editor ed = prefs.edit();
                ed.putString(ChanHelper.BOARD_TYPE, ChanBoard.Type.values()[position].toString());
                ed.commit();
            }
        }
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
