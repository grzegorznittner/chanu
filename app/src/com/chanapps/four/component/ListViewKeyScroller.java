package com.chanapps.four.component;

import android.view.KeyEvent;
import android.widget.AbsListView;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 9/13/13
* Time: 8:42 PM
* To change this template use File | Settings | File Templates.
*/
public class ListViewKeyScroller {

    protected static final int PAGE_SCROLL_OFFSET_PX = 200;
    protected static final int PAGE_SCROLL_DURATION_MS = 1000;

    public static boolean dispatchKeyEvent(KeyEvent event, AbsListView absListView) {
        if (absListView == null)
            return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    scrollToPrevious(absListView);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    scrollToNext(absListView);
                    return true;
            }
        }
        if (event.getAction() == KeyEvent.ACTION_UP
                && (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }
        return false;
    }

    private static void scrollToPrevious(AbsListView absListView) {
        if (absListView == null)
            return;
        int height = absListView.getHeight() - PAGE_SCROLL_OFFSET_PX;
        if (height <= 0)
            height = absListView.getHeight();
        absListView.smoothScrollBy(-1 * height, PAGE_SCROLL_DURATION_MS);
    }

    private static void scrollToNext(AbsListView absListView) {
        if (absListView == null)
            return;
        int height = absListView.getHeight() - PAGE_SCROLL_OFFSET_PX;
        if (height <= 0)
            height = absListView.getHeight();
        absListView.smoothScrollBy(height, PAGE_SCROLL_DURATION_MS);
    }
}
