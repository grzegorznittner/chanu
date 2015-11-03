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
public class BoardViewHolder {

    public ViewGroup grid_item;
    public ViewGroup grid_item_bottom_frame;
    public ViewGroup grid_item_thumb_frame;
    public ImageView grid_item_thread_thumb;
    public ImageView grid_item_country_flag;
    public TextView grid_item_board_code;
    public TextView grid_item_thread_subject;
    public TextView grid_item_thread_subject_header;
    public TextView grid_item_thread_subject_header_abbr;
    public TextView grid_item_thread_info;
    public TextView grid_item_thread_info_header;
    public TextView grid_item_num_replies_text;
    public TextView grid_item_num_images_text;
    public TextView grid_item_num_replies_label;
    public TextView grid_item_num_images_label;
    public TextView grid_item_num_replies_label_abbr;
    public TextView grid_item_num_images_label_abbr;
    public ImageView grid_item_num_replies_img;
    public ImageView grid_item_num_images_img;
    public View grid_item_overflow_icon;
    public ImageView grid_item_dead_icon;
    public ImageView grid_item_closed_icon;
    public ImageView grid_item_sticky_icon;

    public ImageView grid_item_thread_thumb_1;
    public TextView grid_item_thread_subject_1;
    public ImageView grid_item_country_flag_1;
    public ImageView grid_item_thread_thumb_2;
    public TextView grid_item_thread_subject_2;
    public ImageView grid_item_country_flag_2;
    public ImageView grid_item_thread_thumb_3;
    public TextView grid_item_thread_subject_3;
    public ImageView grid_item_country_flag_3;
    public ImageView grid_item_thread_thumb_4;
    public TextView grid_item_thread_subject_4;
    public ImageView grid_item_country_flag_4;
    public ImageView grid_item_thread_thumb_5;
    public TextView grid_item_thread_subject_5;
    public ImageView grid_item_country_flag_5;


    public BoardViewHolder(View item) {
        grid_item = (ViewGroup)item;
        grid_item_bottom_frame = (ViewGroup)item.findViewById(R.id.grid_item_bottom_frame);
        grid_item_thumb_frame = (ViewGroup)item.findViewById(R.id.grid_item_thumb_frame);
        grid_item_thread_thumb = (ImageView)item.findViewById(R.id.grid_item_thread_thumb);
        grid_item_country_flag = (ImageView)item.findViewById(R.id.grid_item_country_flag);
        grid_item_board_code = (TextView)item.findViewById(R.id.grid_item_board_code);
        grid_item_thread_subject = (TextView)item.findViewById(R.id.grid_item_thread_subject);
        grid_item_thread_subject_header = (TextView)item.findViewById(R.id.grid_item_thread_subject_header);
        grid_item_thread_subject_header_abbr = (TextView)item.findViewById(R.id.grid_item_thread_subject_header_abbr);
        grid_item_thread_info = (TextView)item.findViewById(R.id.grid_item_thread_info);
        grid_item_thread_info_header = (TextView)item.findViewById(R.id.grid_item_thread_info_header);
        grid_item_num_replies_text = (TextView)item.findViewById(R.id.grid_item_num_replies_text);
        grid_item_num_images_text = (TextView)item.findViewById(R.id.grid_item_num_images_text);
        grid_item_num_replies_label = (TextView)item.findViewById(R.id.grid_item_num_replies_label);
        grid_item_num_images_label = (TextView)item.findViewById(R.id.grid_item_num_images_label);
        grid_item_num_replies_label_abbr = (TextView)item.findViewById(R.id.grid_item_num_replies_label_abbr);
        grid_item_num_images_label_abbr = (TextView)item.findViewById(R.id.grid_item_num_images_label_abbr);
        grid_item_num_replies_img = (ImageView)item.findViewById(R.id.grid_item_num_replies_img);
        grid_item_num_images_img = (ImageView)item.findViewById(R.id.grid_item_num_images_img);
        grid_item_overflow_icon = item.findViewById(R.id.grid_item_overflow_icon);
        grid_item_dead_icon = (ImageView)item.findViewById(R.id.grid_item_dead_icon);
        grid_item_closed_icon = (ImageView)item.findViewById(R.id.grid_item_closed_icon);
        grid_item_sticky_icon = (ImageView)item.findViewById(R.id.grid_item_sticky_icon);

        grid_item_thread_thumb_1 = (ImageView)item.findViewById(R.id.grid_item_thread_thumb_1);
        grid_item_thread_subject_1 = (TextView)item.findViewById(R.id.grid_item_thread_subject_1);
        grid_item_country_flag_1 = (ImageView)item.findViewById(R.id.grid_item_country_flag_1);
        grid_item_thread_thumb_2 = (ImageView)item.findViewById(R.id.grid_item_thread_thumb_2);
        grid_item_thread_subject_2 = (TextView)item.findViewById(R.id.grid_item_thread_subject_2);
        grid_item_country_flag_2 = (ImageView)item.findViewById(R.id.grid_item_country_flag_2);
        grid_item_thread_thumb_3 = (ImageView)item.findViewById(R.id.grid_item_thread_thumb_3);
        grid_item_thread_subject_3 = (TextView)item.findViewById(R.id.grid_item_thread_subject_3);
        grid_item_country_flag_3 = (ImageView)item.findViewById(R.id.grid_item_country_flag_3);
        grid_item_thread_thumb_4 = (ImageView)item.findViewById(R.id.grid_item_thread_thumb_4);
        grid_item_thread_subject_4 = (TextView)item.findViewById(R.id.grid_item_thread_subject_4);
        grid_item_country_flag_4 = (ImageView)item.findViewById(R.id.grid_item_country_flag_4);
        grid_item_thread_thumb_5 = (ImageView)item.findViewById(R.id.grid_item_thread_thumb_5);
        grid_item_thread_subject_5 = (TextView)item.findViewById(R.id.grid_item_thread_subject_5);
        grid_item_country_flag_5 = (ImageView)item.findViewById(R.id.grid_item_country_flag_5);

    }

}
