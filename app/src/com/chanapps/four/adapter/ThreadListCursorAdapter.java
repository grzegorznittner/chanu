package com.chanapps.four.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanHelper;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/4/13
 * Time: 7:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadListCursorAdapter extends AbstractThreadCursorAdapter {

    public ThreadListCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
        super(context, layout, viewBinder, from, to);
    }

    @Override
    protected View newView(Context context, ViewGroup parent, String tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        //if (ChanHelper.AD_ITEM.equals(tag)) {
        //    return mInflater.inflate(R.layout.thread_list_item_ad, parent, false);
        //}
        //else if (ChanHelper.POST_RESTO.equals(tag)) { // first item is the post which started the thread
        //    return mInflater.inflate(R.layout.thread_list_item_header, parent, false);
        //s}
        //else {
            return mInflater.inflate(R.layout.thread_list_item, parent, false);
        //}
        /*
        else if (ChanHelper.POST_IMAGE_URL.equals(tag)) {
            return mInflater.inflate(R.layout.thread_list_item, parent, false);
        } else {
            return mInflater.inflate(R.layout.thread_list_item_no_image, parent, false);
        }
        */
    }

    @Override
    protected int getThumbnailImageId() {
        return R.id.list_item_image;
    }

    @Override
    protected void setHighlightViews(View v, String tag, long postNo) {
        if (highlightPostNo == postNo || highlightIdPostNos.contains(postNo)) {
            v.setBackgroundColor(context.getResources().getColor(R.color.PaletteLightBlue));
        }
        else if (highlightPrevPostNos.contains(postNo)) {
            v.setBackgroundColor(context.getResources().getColor(R.color.PaletteLightBlue));
        }
        else if (highlightNextPostNos.contains(postNo)) {
            v.setBackgroundColor(context.getResources().getColor(R.color.PaletteLightBlue));
        }
        else if (!tag.equals(ChanHelper.AD_ITEM)){
            v.setBackgroundColor(context.getResources().getColor(R.color.PaletteTransparent));
        }
    }

}
