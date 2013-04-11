package com.chanapps.four.component;

import android.database.Cursor;
import android.text.Html;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanHelper;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 4/9/13
* Time: 10:59 AM
* To change this template use File | Settings | File Templates.
*/
public class ThreadSpoilerOnClickListener implements View.OnClickListener {

    String spoilerText = "";

    public ThreadSpoilerOnClickListener(Cursor cursor) {
        spoilerText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SPOILER_TEXT));
    }

    @Override
    public void onClick(View v) {
        ViewParent parent = v.getParent();
        if (parent == null || !(parent instanceof CheckableLinearLayout))
            return;
        CheckableLinearLayout layout = (CheckableLinearLayout)parent;
        TextView listItemSnippet = (TextView)layout.findViewById(R.id.list_item_snippet);
        TextView listItemText = (TextView)layout.findViewById(R.id.list_item_text);
        if (listItemSnippet == null || listItemText == null)
            return;
        listItemSnippet.setVisibility(View.INVISIBLE);
        listItemText.setText(Html.fromHtml(spoilerText));
        listItemText.setVisibility(View.VISIBLE);
        v.setVisibility(View.GONE);
    }
}
