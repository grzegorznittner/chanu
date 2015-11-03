package com.chanapps.four.component;

import android.database.Cursor;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanPost;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 4/9/13
* Time: 10:59 AM
* To change this template use File | Settings | File Templates.
*/
public class ThreadExifOnClickListener implements View.OnClickListener {

    String exifText = "";

    public ThreadExifOnClickListener(Cursor cursor) {
        exifText = cursor.getString(cursor.getColumnIndex(ChanPost.POST_EXIF_TEXT));
    }

    @Override
    public void onClick(View v) {
        ViewParent parent = v.getParent();
        if (parent == null || !(parent instanceof ViewGroup))
            return;
        ViewGroup layout = (ViewGroup)parent;
        TextView listItemText = (TextView)layout.findViewById(R.id.list_item_exif_text);
        listItemText.setText(Html.fromHtml(exifText));
        listItemText.setVisibility(View.VISIBLE);
        v.setVisibility(View.GONE);
    }
}
