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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * A grid that displays a set of framed photos.
 *
 */
public class PhotoGrid extends Activity implements OnItemClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.photo_grid);

        GridView g = (GridView) findViewById(R.id.myGrid);
        g.setAdapter(new ImageAdapter(this));
        g.setOnItemClickListener(this);
    }

    
    @Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    	Intent intent = new Intent();
        intent.setClass(getApplicationContext(), FragmentLayout.class);
        //intent.putExtra("index", index);
        startActivity(intent);
	}

	public class ImageAdapter extends BaseAdapter {
        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            return mThumbIds.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
                //int parentWidth = parent.get;
                //int imageWidth = (parentWidth - 4 * 10) / 2;
                imageView.setLayoutParams(new GridView.LayoutParams(250, 250));
                imageView.setAdjustViewBounds(false);
                imageView.setBackgroundColor(0x99999999);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                //imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageResource(mThumbIds[position]);
            return imageView;
        }

        private Context mContext;

        private Integer[] mThumbIds = {
                R.drawable.heidi1, R.drawable.heidi2,
                R.drawable.heidi3,
                R.drawable.heidi1, R.drawable.heidi2,
                R.drawable.heidi3,
                R.drawable.heidi1, R.drawable.heidi2,
                R.drawable.heidi3
        };
    }

}
