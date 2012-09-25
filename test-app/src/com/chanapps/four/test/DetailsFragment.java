package com.chanapps.four.test;

import com.chanapps.four.test.FragmentLayout.DetailsActivity;

import android.app.Fragment;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * This is the secondary fragment, displaying the details of a particular
 * item.
 */

public class DetailsFragment extends Fragment {
    public int getShownIndex() {
        return getArguments().getInt("index", 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (container == null) {
            // We have different layouts, and in one of them this
            // fragment's containing frame doesn't exist.  The fragment
            // may still be created from its saved state, but there is
            // no reason to try to create its view hierarchy because it
            // won't be displayed.  Note this is not needed -- we could
            // just run the code below, where we would create and return
            // the view hierarchy; it would just never be used.
            return null;
        }

        int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                4, getActivity().getResources().getDisplayMetrics());
        FrameLayout frameLayout = new FrameLayout(getActivity());

        ScrollView scroller = new ScrollView(getActivity());
        scroller.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        frameLayout.addView(scroller);
        
        ImageView image = new ImageView(getActivity());
        image.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        
        scroller.addView(image);
        
        TextView text = new TextView(getActivity());
        text.setPadding(padding, padding, padding, padding);
        text.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        text.setTextColor(ColorStateList.valueOf(0xffffffff));
        text.setBackgroundColor(0xAA000000);
        text.setTextSize(24);
        text.setPadding(20, 40, 20, 40);
        text.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        
        frameLayout.addView(text);

        FragmentLayout activity = (FragmentLayout)getActivity();
        int position = getShownIndex();
        if (position == 0) {
        	activity.getImageLoader().displayImage(activity.getChanThread().getImageUrl(), image, activity.getDisplayImageOptions());
        	text.setText(activity.getChanThread().com);
        } else {
        	activity.getImageLoader().displayImage(activity.getChanThread().posts.get(position - 1).getImageUrl(), image, activity.getDisplayImageOptions());
        	text.setText(activity.getChanThread().posts.get(position - 1).com);
        }
        
        return frameLayout;
    }
}