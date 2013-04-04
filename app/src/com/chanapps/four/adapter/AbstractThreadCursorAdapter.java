package com.chanapps.four.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanHelper;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 12/21/12
 * Time: 12:14 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractThreadCursorAdapter extends AbstractBoardCursorAdapter {

    protected static final String TAG = AbstractThreadCursorAdapter.class.getSimpleName();
    protected static final boolean DEBUG = false;

    public AbstractThreadCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
        super(context, layout, viewBinder, from, to);
    }

}
