package com.chanapps.four.viewer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.chanapps.four.activity.R;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 7/26/13
 * Time: 12:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class BoardGridViewHolder {

    public ViewGroup grid_item;
    public ImageView grid_item_thread_thumb;
    public ImageView grid_item_country_flag;
    public TextView grid_item_thread_subject;
    public TextView grid_item_thread_info;
    public ImageView grid_item_overflow_icon;

    public BoardGridViewHolder(View item) {
        grid_item = (ViewGroup)item;
        grid_item_thread_thumb = (ImageView)item.findViewById(R.id.grid_item_thread_thumb);
        grid_item_country_flag = (ImageView)item.findViewById(R.id.grid_item_country_flag);
        grid_item_thread_subject = (TextView)item.findViewById(R.id.grid_item_thread_subject);
        grid_item_thread_info = (TextView)item.findViewById(R.id.grid_item_thread_info);
        grid_item_overflow_icon = (ImageView)item.findViewById(R.id.grid_item_overflow_icon);
    }

}
