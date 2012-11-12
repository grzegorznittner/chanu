/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.chanapps.four.test;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanBoard.Type;

/**
 * A grid that displays a set of framed photos.
 *
 */
public class BoardGridActivity
        extends TabActivity
        implements OnItemClickListener, TabHost.TabContentFactory, TabHost.OnTabChangeListener
{
	public static final String TAG = "BoardGridActivity";
	
	private int width, height;
	private int numColumns = 2;
	private int columnWidth = 300;
	
	private ChanBoard.Type selectedBoardType = ChanBoard.Type.JAPANESE_CULTURE;
	private ImageAdapter adapter;
	private SharedPreferences prefs = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.photo_grid);
    }
    
    @Override
	protected void onStart() {
		super.onStart();
		
        prefs = getSharedPreferences(ChanHelper.PREF_NAME, 0);
        selectedBoardType = ChanBoard.Type.valueOf(prefs.getString(ChanHelper.BOARD_TYPE, ChanBoard.Type.JAPANESE_CULTURE.toString()));
        Log.i(TAG, "onStart selectedBoardType: " + selectedBoardType);
        
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        Log.i(TAG, "onStart width: " + width + ", height: " + height);

        numColumns = width / 300 == 1 ? 2 : width / 300;
        columnWidth = (width - 15) / numColumns;

	    TabHost tabHost = getTabHost();
        for (ChanBoard.Type type : ChanBoard.Type.values()) {
            tabHost.addTab(tabHost.newTabSpec(type.toString())
                    .setIndicator(type.toString().replaceAll("_", " "))
                    .setContent(this));
        }
        tabHost.setOnTabChangedListener(this);
        
        int selectedTab = 0;
        for (ChanBoard.Type type : ChanBoard.Type.values()) {
        	if (type == selectedBoardType) {
        		break;
        	}
        	selectedTab++;
        }
        tabHost.setCurrentTab(selectedTab);
        setDefaultTab(selectedTab);
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
	
	protected void onResume () {
		super.onResume();
		Log.i(TAG, "onResume");
	}
	
	public void onWindowFocusChanged (boolean hasFocus) {
		Log.i(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

	protected void onPause() {
        super.onPause();
        
		Log.i(TAG, "onPause");
    }
	
	protected void onDestroy () {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
	}
	
    /** {@inheritDoc} */
    public View createTabContent(String tag) {
    	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	GridView g = (GridView) inflater.inflate(R.layout.board_grid_view, null, false);
        g.setNumColumns(numColumns);
        g.setColumnWidth(columnWidth);
    	adapter = new ImageAdapter(getApplicationContext(), ChanBoard.Type.valueOf(tag), columnWidth);
        g.setAdapter(adapter);
        g.setOnItemClickListener(this);
        return g;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_grid_menu, menu);
        return true;
    }
    
    @Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ChanBoard board = ChanBoard.getBoardsByType(selectedBoardType).get(position);
        String boardCode = board.link;
        int pageNo = 0;
        Intent intent = new Intent(this, BoardListActivity.class);
        intent.putExtra("boardCode", boardCode);
        intent.putExtra("pageNo", pageNo);
        startActivity(intent);
    }
    
	@Override
	public void onTabChanged(String tabId) {
		Log.i(TAG, "onTabChanged tabId: " + tabId);
		selectedBoardType = ChanBoard.Type.valueOf(tabId);
    	Log.i(TAG, "Storing tab selection: " + selectedBoardType);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(ChanHelper.BOARD_TYPE, selectedBoardType.toString());
        ed.commit();
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
