package com.chanapps.four.viewer;

import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chanapps.four.activity.R;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 7/26/13
 * Time: 12:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadViewHolder {

    public ViewGroup list_item;
    public ViewGroup list_item_num_direct_replies;
    public ImageView list_item_country_flag;
    public ImageView list_item_dead_icon;
    public ImageView list_item_closed_icon;
    public ImageView list_item_sticky_icon;
    public ImageView list_item_header_bar_chat;
    public ImageView list_item_header_bar_overflow;
    public ImageView list_item_image;
    public ImageView list_item_image_header;
    public ViewGroup list_item_image_expansion_target;
    //public ImageView list_item_image_expanded;
    public WebView list_item_image_expanded_webview;
    //public View list_item_num_comments_spinner;
    //public View list_item_num_images_spinner;
    //public ImageView list_item_image_spinner;
    public ViewGroup list_item_ago_wrapper;
    public ViewGroup list_item_header_bar_chat_wrapper;
    public ViewGroup list_item_header_bar_overflow_wrapper;
    public ViewGroup list_item_subject_icons;
    public ProgressBar list_item_expanded_progress_bar;
    public ViewGroup list_item_header_wrapper;
    public ViewGroup list_item_image_expanded_wrapper;
    public ViewGroup list_item_image_wrapper;
    public ViewGroup list_item_num_images;
    public ViewGroup list_item_num_images_top;
    public ViewGroup list_item_num_replies;
    public ViewGroup list_item_num_replies_top;
    public TextView list_item_exif_text;
    public TextView list_item_header;
    public TextView list_item_header_bar_ago;
    public TextView list_item_header_bar_no;
    public TextView list_item_num_direct_replies_text;
    public TextView list_item_num_images_label;
    public TextView list_item_num_images_text;
    public TextView list_item_num_replies_label;
    public TextView list_item_num_replies_text;
    public TextView list_item_subject;
    public TextView list_item_text;
    public View list_item_image_expanded_click_effect;
    public View list_item_num_horizontal_border;
    public View list_item_num_vertical_border;
    public View list_item_right_menu_spacer;
    public boolean isWebView;

    public ThreadViewHolder(View item) {
        list_item = (ViewGroup) item;
        list_item_num_direct_replies = item.findViewById(R.id.list_item_num_direct_replies);
        list_item_country_flag = item.findViewById(R.id.list_item_country_flag);
        list_item_dead_icon = item.findViewById(R.id.list_item_dead_icon);
        list_item_closed_icon = item.findViewById(R.id.list_item_closed_icon);
        list_item_sticky_icon = item.findViewById(R.id.list_item_sticky_icon);
        list_item_header_bar_chat = item.findViewById(R.id.list_item_header_bar_chat);
        list_item_header_bar_overflow = item.findViewById(R.id.list_item_header_bar_overflow);
        list_item_image = item.findViewById(R.id.list_item_image);
        list_item_image_header = item.findViewById(R.id.list_item_image_header);
        list_item_image_expansion_target = item.findViewById(R.id.list_item_image_expansion_target);
        //list_item_image_expanded = (ImageView)item.findViewById(R.id.list_item_image_expanded);
        list_item_image_expanded_webview = item.findViewById(R.id.list_item_image_expanded_webview);
        //list_item_num_comments_spinner = item.findViewById(R.id.list_item_num_comments_spinner);
        //list_item_num_images_spinner = item.findViewById(R.id.list_item_num_images_spinner);
        //list_item_image_spinner = (ImageView)item.findViewById(R.id.list_item_image_spinner);
        list_item_ago_wrapper = item.findViewById(R.id.list_item_ago_wrapper);
        list_item_header_bar_chat_wrapper = item.findViewById(R.id.list_item_header_bar_chat_wrapper);
        list_item_header_bar_overflow_wrapper = item.findViewById(R.id.list_item_header_bar_overflow_wrapper);
        list_item_subject_icons = item.findViewById(R.id.list_item_subject_icons);
        list_item_expanded_progress_bar = item.findViewById(R.id.list_item_expanded_progress_bar);
        list_item_header_wrapper = item.findViewById(R.id.list_item_header_wrapper);
        list_item_image_expanded_wrapper = item.findViewById(R.id.list_item_image_expanded_wrapper);
        list_item_image_wrapper = item.findViewById(R.id.list_item_image_wrapper);
        list_item_num_images = item.findViewById(R.id.list_item_num_images);
        list_item_num_images_top = item.findViewById(R.id.list_item_num_images_top);
        list_item_num_replies = item.findViewById(R.id.list_item_num_replies);
        list_item_num_replies_top = item.findViewById(R.id.list_item_num_replies_top);
        list_item_exif_text = item.findViewById(R.id.list_item_exif_text);
        list_item_header = item.findViewById(R.id.list_item_header);
        list_item_header_bar_ago = item.findViewById(R.id.list_item_header_bar_ago);
        list_item_header_bar_no = item.findViewById(R.id.list_item_header_bar_no);
        list_item_num_direct_replies_text = item.findViewById(R.id.list_item_num_direct_replies_text);
        list_item_num_images_label = item.findViewById(R.id.list_item_num_images_label);
        list_item_num_images_text = item.findViewById(R.id.list_item_num_images_text);
        list_item_num_replies_label = item.findViewById(R.id.list_item_num_replies_label);
        list_item_num_replies_text = item.findViewById(R.id.list_item_num_replies_text);
        list_item_subject = item.findViewById(R.id.list_item_subject);
        list_item_text = item.findViewById(R.id.list_item_text);
        list_item_image_expanded_click_effect = item.findViewById(R.id.list_item_image_expanded_click_effect);
        list_item_num_horizontal_border = item.findViewById(R.id.list_item_num_horizontal_border);
        list_item_num_vertical_border = item.findViewById(R.id.list_item_num_vertical_border);
        list_item_right_menu_spacer = item.findViewById(R.id.list_item_right_menu_spacer);
    }

}
