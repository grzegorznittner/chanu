package com.chanapps.four.component;

import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.widget.AbsListView;
import android.widget.ResourceCursorAdapter;
import com.chanapps.four.fragment.ThreadPopupDialogFragment;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 6/27/13
 * Time: 7:30 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ThreadViewable {

    AbsListView getAbsListView();

    ResourceCursorAdapter getAdapter();

    Handler getHandler();

    FragmentManager getFragmentManager();

    void showDialog(String boardCode, long threadNo, long postNo, int pos, ThreadPopupDialogFragment.PopupType popupType);
}

