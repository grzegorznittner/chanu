package com.chanapps.four.adapter;

import android.app.ActionBar;
import android.content.Context;
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
import com.chanapps.four.activity.R;
import com.chanapps.four.data.BoardSelectorTab;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.fragment.BoardGroupFragment;

import javax.security.auth.login.LoginException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    private final static boolean DEBUG = true;

    private final Context mContext;
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

    public TabsAdapter(Context context, ActionBar actionBar, FragmentManager fm, ViewPager pager) {
        super(fm);
        mContext = context;
        mActionBar = actionBar;
        mViewPager = pager;
        mViewPager.setAdapter(this);
        mViewPager.setOnPageChangeListener(this);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        //if (position >= getCount()) {
        if (object != null && object instanceof Fragment) {
            FragmentManager manager = ((Fragment) object).getFragmentManager();
            if (manager == null)
                return;
            FragmentTransaction trans = manager.beginTransaction();
            if (trans == null)
                return;
            trans.remove((Fragment) object);
            trans.commit();
        }
        //}
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

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    // DANGER WILL ROBINSON!!! DANGER!!!
    // This method doesn't do what you think it does.  It does *not* get the item at the position.
    // What it does is *create* a new unattached fragment.  You never want to call this manually.
    // Instead, call getFragmentAtPosition(int position)
    public Fragment getItem(int position) {
        TabInfo info = mTabs.get(position);
        String selectedTab = info.args.getString(BoardSelectorActivity.BOARD_SELECTOR_TAB);
        if (DEBUG) Log.i(BoardSelectorActivity.TAG, "TabsAdapter tab=" + selectedTab + " instantiating Fragment");
        Fragment fragment = Fragment.instantiate(mContext, info.clss.getName(), info.args);
        if (DEBUG) Log.i(BoardSelectorActivity.TAG, "returning fragment tag=" + fragment.getTag());
        return fragment;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//        mContext.invalidateOptionsMenu();
        if (DEBUG) Log.d(BoardSelectorActivity.TAG, "on page scrolled pos=" + position);
    }

    @Override
    public void onPageSelected(int position) {
        if (mActionBar != null) {
            mActionBar.setSelectedNavigationItem(position);
            selectInSpinnerIfPresent(position, true);
        }
        // following jazz is for watchlist clean/clear menus
        //Fragment fragment = getItem(position);
        //mContext.selectedBoardTab = BoardSelectorTab.values()[position];
        //if (DEBUG) Log.d(BoardSelectorActivity.TAG, "TabsAdapter pager set to " + position
        //        + " with fragment=" + (fragment != null ? fragment.getTag() : null)
        //        + " selectedBoardType=" + mContext.selectedBoardTab);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (DEBUG) Log.d(BoardSelectorActivity.TAG, "page scroll state changed state=" + state);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
        Object tag = tab.getTag();
        for (int i=0; i<mTabs.size(); i++) {
            if (mTabs.get(i) == tag) {
            	if (DEBUG) Log.d(BoardSelectorActivity.TAG, "TabAdapter setting pager to " + i);
                mViewPager.setCurrentItem(i);
                return;
            }
            /*
                Fragment fragment = getItem(i);
                Menu menu = mContext.menu;
                ChanBoard.Type selectedBoardType = mContext.activeBoardTypes.get(i);
                mContext.selectedBoardType = selectedBoardType;
                mContext.invalidateOptionsMenu();
                if (DEBUG) Log.d(BoardSelectorActivity.TAG, "TabsAdapter pager set to " + i
                        + " with fragment=" + (fragment != null ? fragment.getTag() : null)
                        + " menu=" + menu
                        + " selectedBoardType=" + selectedBoardType);
                if (fragment != null && menu != null) {
                    ((BoardGroupFragment)fragment).onPrepareOptionsMenu(menu, mContext, selectedBoardType);
                }
                //if (mActionBar != null) {
                //    mActionBar.setSelectedNavigationItem(i);
                //}
                //tab.setText(mContext.selectedBoardType.toString());
                if (mActionBar != null) {
                    //mActionBar.selectTab(tab);
                    //mContext.setTabToSelectedType(true);
                }
                tab.setText(selectedBoardType.toString());
                notifyDataSetChanged();
                //tab.select();
                return;
                */
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction transaction) {
        if (DEBUG) Log.d(BoardSelectorActivity.TAG, "Unselected tab=" + tab.getText());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction transaction) {
        if (DEBUG) Log.d(BoardSelectorActivity.TAG, "Reselected tab=" + tab.getText());
    }

    private void selectInSpinnerIfPresent(int position, boolean animate) {
        try {
            //View actionBarView = mContext.findViewById(R.id.abs__action_bar);
            View actionBarView = null;
            if (actionBarView == null) {
                int id = mContext.getResources().getIdentifier("action_bar", "id", "android");
                actionBarView = mViewPager.findViewById(id);
            }
            if (actionBarView == null)
                return;

            Class<?> actionBarViewClass = actionBarView.getClass();
            Field mTabScrollViewField = actionBarViewClass.getDeclaredField("mTabScrollView");
            mTabScrollViewField.setAccessible(true);

            Object mTabScrollView = mTabScrollViewField.get(actionBarView);
            if (mTabScrollView == null) {
                return;
            }

            Field mTabSpinnerField = mTabScrollView.getClass().getDeclaredField("mTabSpinner");
            mTabSpinnerField.setAccessible(true);

            Object mTabSpinner = mTabSpinnerField.get(mTabScrollView);
            if (mTabSpinner == null) {
                return;
            }

            Method setSelectionMethod = mTabSpinner.getClass().getSuperclass().getDeclaredMethod("setSelection", Integer.TYPE, Boolean.TYPE);
            setSelectionMethod.invoke(mTabSpinner, position, animate);

            Method requestLayoutMethod = mTabSpinner.getClass().getSuperclass().getDeclaredMethod("requestLayout");
            requestLayoutMethod.invoke(mTabSpinner);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public Fragment getFragmentAtPosition(int pos) {
        Object fragment = instantiateItem(mViewPager, pos);
        if (fragment instanceof Fragment)
            return (Fragment)fragment;
        else
            return null;
    }

}
