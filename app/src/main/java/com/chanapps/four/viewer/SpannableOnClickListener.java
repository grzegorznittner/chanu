package com.chanapps.four.viewer;

import android.view.View;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 9/7/13
* Time: 1:29 PM
* To change this template use File | Settings | File Templates.
*/
public interface SpannableOnClickListener extends View.OnClickListener {
    void onClick(View v, long cursorId);
}
