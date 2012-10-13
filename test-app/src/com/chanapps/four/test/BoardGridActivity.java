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
import com.chanapps.four.data.ChanBoard.Type;

/**
 * A grid that displays a set of framed photos.
 *
 */
public class BoardGridActivity extends TabActivity implements OnItemClickListener, TabHost.TabContentFactory {
	public static final String TAG = "BoardGridActivity";
	
	private int width, height;
	private int numColumns = 2;
	private int columnWidth = 300;
	
	private ChanBoard.Type selectedBoardType = ChanBoard.Type.JAPANESE_CULTURE;
	private ImageAdapter adapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        Log.i(TAG, "width: " + width + ", height: " + height);
                
	    setContentView(R.layout.photo_grid);
	    
        numColumns = width / 300 == 1 ? 2 : width / 300;
        columnWidth = (width - 15) / numColumns;
        
	    TabHost tabHost = getTabHost();
        for (ChanBoard.Type type : ChanBoard.Type.values()) {
            tabHost.addTab(tabHost.newTabSpec(type.toString())
                    .setIndicator(type.toString())
                    .setContent(this));
        }
        setDefaultTab(0);
    }
    
    /** {@inheritDoc} */
    public View createTabContent(String tag) {
    	selectedBoardType = ChanBoard.Type.valueOf(tag);
    	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	GridView g = (GridView) inflater.inflate(R.layout.board_grid_view, null, false);
        g.setNumColumns(numColumns);
        g.setColumnWidth(columnWidth);
    	adapter = new ImageAdapter(getApplicationContext(), selectedBoardType, columnWidth);
        g.setAdapter(adapter);
        g.setOnItemClickListener(this);
        return g;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_menu, menu);
        return true;
    }
    
    @Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    	Intent intent = new Intent();
        intent.setClass(getApplicationContext(), FragmentLayout.class);
        intent.putExtra("board", "diy");
        intent.putExtra("thread", 287252);
        startActivity(intent);
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
