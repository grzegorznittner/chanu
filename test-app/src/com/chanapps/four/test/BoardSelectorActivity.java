package com.chanapps.four.test;

import java.util.ArrayList;

import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanBoard.Type;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AbsListView.LayoutParams;

public class BoardSelectorActivity extends FragmentActivity {
	public static final String TAG = "BoardSelectorActivity";
	
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private SharedPreferences prefs = null;
    private ChanBoard.Type selectedBoardType = ChanBoard.Type.JAPANESE_CULTURE;
    
    public static class BoardGroupFragment extends Fragment implements OnItemClickListener {
    	private ChanBoard.Type boardType;
    	private int numColumns = 1;
    	private int columnWidth = 480;
    	private ImageAdapter adapter = null;
    	
    	@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            boardType = getArguments() != null
            		? ChanBoard.Type.valueOf(getArguments().getString(ChanHelper.BOARD_TYPE)) : ChanBoard.Type.JAPANESE_CULTURE;
            columnWidth = getArguments().getInt("columnWidth");
            numColumns = getArguments().getInt("numColumns");
        }
    	
    	@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        	GridView g = (GridView) inflater.inflate(R.layout.board_grid_view, container, false);
            g.setNumColumns(numColumns);
            g.setColumnWidth(columnWidth);
        	adapter = new ImageAdapter(container.getContext(), boardType, columnWidth);
            g.setAdapter(adapter);
            g.setOnItemClickListener(this);
            return g;
    	}
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ChanBoard board = ChanBoard.getBoardsByType(boardType).get(position);
            String boardCode = board.link;
            int pageNo = 0;
            Intent intent = new Intent(view.getContext(), BoardListActivity.class);
            intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            intent.putExtra(ChanHelper.PAGE, pageNo);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.pager);
        setContentView(mViewPager);
    }
    
    @Override
	protected void onStart() {
		super.onStart();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.i(TAG, "onStart width: " + width + ", height: " + height);

        int numColumns = width / 300 == 1 ? 2 : width / 300;
        int columnWidth = (width - 15) / numColumns;

        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        mTabsAdapter = new TabsAdapter(this, getSupportFragmentManager(), mViewPager);
        for (ChanBoard.Type type : ChanBoard.Type.values()) {
        	Bundle bundle = new Bundle();
        	bundle.putString(ChanHelper.BOARD_TYPE, type.toString());
        	bundle.putInt("columnWidth", columnWidth);
        	bundle.putInt("numColumns", numColumns);
        	mTabsAdapter.addTab(bar.newTab().setText(type.toString().replaceAll("_", " ")),
            		BoardGroupFragment.class, bundle);
        }
		
        prefs = getSharedPreferences(ChanHelper.PREF_NAME, 0);
        selectedBoardType = ChanBoard.Type.valueOf(prefs.getString(ChanHelper.BOARD_TYPE, ChanBoard.Type.JAPANESE_CULTURE.toString()));
        Log.i(TAG, "onStart selectedBoardType: " + selectedBoardType);
        
        int selectedTab = 0;
        for (ChanBoard.Type type : ChanBoard.Type.values()) {
        	if (type == selectedBoardType) {
        		break;
        	}
        	selectedTab++;
        }
        getActionBar().setSelectedNavigationItem(selectedTab);
	}
    
    protected void onStop () {
    	super.onStop();
    	Log.i(TAG, "onStop");
    }

    @Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "onRestart");
	}
	
	public void onWindowFocusChanged (boolean hasFocus) {
		Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        super.onPause();        
		Log.i(TAG, "onPause");
    }
	
	protected void onDestroy () {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_grid_menu, menu);
        return true;
    }

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
    public static class TabsAdapter extends FragmentPagerAdapter
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

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            mActionBar.addTab(tab);
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
            Log.i(TAG, "Storing selected boardType: " + selectedType);
            SharedPreferences.Editor ed = mContext.prefs.edit();
            ed.putString(ChanHelper.BOARD_TYPE, selectedType);
            ed.commit();
            
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            Object tag = tab.getTag();
            for (int i=0; i<mTabs.size(); i++) {
                if (mTabs.get(i) == tag) {
                    mViewPager.setCurrentItem(i);
                }
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }
    
	public static class ImageAdapter extends BaseAdapter {
		LayoutInflater infater = null;
		Type selectedBoardType;
		int columnWidth;
		
        public ImageAdapter(Context c, Type selectedBoardType, int columnWidth) {
        	infater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	this.selectedBoardType = selectedBoardType;
        	this.columnWidth = columnWidth;
        }

        public int getCount() {
            return ChanBoard.getBoardsByType(selectedBoardType).size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	View itemLayout = null;
            if (convertView == null) {
            	Log.d(TAG, "Creating new item view for " + position);
            	itemLayout = infater.inflate(R.layout.grid_item, parent, false);
            	itemLayout.setTag(selectedBoardType.toString());
            } else {
        		Log.d(TAG, "Using existing view for " + position);
            	itemLayout = convertView;
            }
            
            itemLayout.setLayoutParams(new LayoutParams(columnWidth, columnWidth));
            
            ChanBoard board = ChanBoard.getBoardsByType(selectedBoardType).get(position);
            
            ImageView imageView = (ImageView)itemLayout.findViewById(R.id.grid_item_image);
            imageView.setLayoutParams(new RelativeLayout.LayoutParams(columnWidth, columnWidth));
            
            int imageId = 0;
            try {
				imageId = R.drawable.class.getField(board.link).getInt(null);
			} catch (Exception e) {
				try {
					imageId = R.drawable.class.getField("board_" + board.link).getInt(null);
				} catch (Exception e1) {
					imageId = R.drawable.stub_image;
				}
			}
        	imageView.setImageResource(imageId);
            
            TextView textView = (TextView)itemLayout.findViewById(R.id.grid_item_text);
            textView.setText(board.name);
            
            return itemLayout;
        }
    }	

}
