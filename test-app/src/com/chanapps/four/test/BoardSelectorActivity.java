package com.chanapps.four.test;

import com.chanapps.four.component.ImageAdapter;
import com.chanapps.four.component.TabsAdapter;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
import android.widget.GridView;

public class BoardSelectorActivity extends FragmentActivity {
	public static final String TAG = "BoardSelectorActivity";
	
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private SharedPreferences prefs = null;
    private ChanBoard.Type selectedBoardType = ChanBoard.Type.JAPANESE_CULTURE;
    
    public static class BoardGroupFragment extends Fragment implements OnItemClickListener {
    	private ChanBoard.Type boardType;
    	private ImageAdapter adapter = null;
    	
    	@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            boardType = getArguments() != null
            		? ChanBoard.Type.valueOf(getArguments().getString(ChanHelper.BOARD_TYPE)) : ChanBoard.Type.JAPANESE_CULTURE;
        }
    	
    	@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            int numColumns = width / 300 == 1 ? 2 : width / 300;
            int columnWidth = (width - 15) / numColumns;
            Log.i(TAG, "BoardGroupFragment onCreateView width: " + width + ", height: " + height + ", numCols: " + numColumns);
            
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
            
            Log.i(TAG, "onItemClick boardType: " + boardType);

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

        final ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setTitle(getString(R.string.app_name));
            bar.setDisplayHomeAsUpEnabled(false);
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
        }

        if (mTabsAdapter == null) {
            mTabsAdapter = new TabsAdapter(this, getSupportFragmentManager(), mViewPager);
            for (ChanBoard.Type type : ChanBoard.Type.values()) {
                Bundle bundle = new Bundle();
                bundle.putString(ChanHelper.BOARD_TYPE, type.toString());
                mTabsAdapter.addTab(bar.newTab().setText(type.toString().replaceAll("_", " ")),
                        BoardGroupFragment.class, bundle);
            }
        }
	}

    private void setTabFromPrefs() {
        prefs = getSharedPreferences(ChanHelper.PREF_NAME, 0);
        selectedBoardType = ChanBoard.Type.valueOf(prefs.getString(ChanHelper.BOARD_TYPE, ChanBoard.Type.JAPANESE_CULTURE.toString()));
        Log.e(TAG, "onStart selectedBoardType: " + selectedBoardType);

        int selectedTab = 0;
        for (ChanBoard.Type type : ChanBoard.Type.values()) {
        	if (type == selectedBoardType) {
        		break;
        	}
        	selectedTab++;
        }
        getActionBar().setSelectedNavigationItem(selectedTab);
        if (mViewPager.getCurrentItem() != selectedTab) {
            mViewPager.setCurrentItem(selectedTab, true);
        }
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
    protected void onResume() {
        super.onResume();
        setTabFromPrefs();
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

}
