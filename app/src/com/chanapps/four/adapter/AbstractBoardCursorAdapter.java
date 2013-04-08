/**
 * 
 */
package com.chanapps.four.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;

import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanHelper;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 * Copied from SDK.
 *
 * An easy adapter to map columns from a cursor to TextViews or ImageViews
 * defined in an XML file. You can specify which columns you want, which
 * views you want to display the columns, and the XML file that defines
 * the appearance of these views.
 *
 */
abstract public class AbstractBoardCursorAdapter extends ResourceCursorAdapter {
	protected static final String TAG = AbstractBoardCursorAdapter.class.getSimpleName();
	protected static final boolean DEBUG = false;
	
    /**
     * A list of columns containing the data to bind to the UI.
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected int[] mFrom;
    /**
     * A list of View ids representing the views to which the data must be bound.
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected int[] mTo;

    protected boolean isWatchlist = false;

    protected ViewBinder mViewBinder;
    protected Context context;
    protected LayoutInflater mInflater;

    protected String[] mOriginalFrom;

    /**
     * Standard constructor.
     *
     * @param context The context where the ListView associated with this
     *            SimpleListItemFactory is running
     * @param layout resource identifier of a layout file that defines the views
     *            for this list item. The layout file should include at least
     *            those named views defined in "to"
     * @param from A list of column names representing the data to bind to the UI.  Can be null
     *            if the cursor is not available yet.
     * @param to The views that should display column in the "from" parameter.
     *            These should all be TextViews. The first N views in this list
     *            are given the values of the first N columns in the from
     *            parameter.  Can be null if the cursor is not available yet.
     */
    public AbstractBoardCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
        super(context, layout, null, 0);
        this.context = context;
        mTo = to;
        mOriginalFrom = from;
        mViewBinder = viewBinder;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public AbstractBoardCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to, boolean isWatchlist) {
        this(context, layout, viewBinder, from, to);
        this.isWatchlist = isWatchlist;
    }

    /**
     * Binds all of the field names passed into the "to" parameter of the
     * constructor with their corresponding cursor columns as specified in the
     * "from" parameter.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewBinder binder = mViewBinder;
        final int count = mTo.length;
        final int[] from = mFrom;
        final int[] to = mTo;

        Object tag = view.getTag();
        if (tag != null && tag instanceof Long) {
            long viewPostId = (Long)tag;
            long cursorPostId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
            if (viewPostId == cursorPostId) {
                if (DEBUG) Log.i(TAG, "view already set, bypassing pos=" + cursor.getPosition());
                return;
            }
        }

        if (binder != null)
            binder.setViewValue(view, cursor, 0); // allow parent operations
        for (int i = 0; i < count; i++) {
            final View v = view.findViewById(to[i]);
            if (v != null) {
                boolean bound = false;
                if (binder != null) {
                    bound = binder.setViewValue(v, cursor, from[i]);
                }
            }
        }
    }
    
    /**
     * @see android.widget.ListAdapter#getView(int, View, ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	Cursor cursor = getCursor();
        if (cursor == null) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!cursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        View v = convertView != null ? convertView : newView(context, parent, "", position);
        bindView(v, context, cursor);
        return v;
    }

    abstract protected View newView(Context context, ViewGroup parent, String tag, int position);

    /**
     * Create a map from an array of strings to an array of column-id integers in mCursor.
     * If mCursor is null, the array will be discarded.
     * 
     * @param from the Strings naming the columns of interest
     */
    private void findColumns(Cursor c, String[] from) {
        if (c != null) {
            int i;
            int count = from.length;
            if (mFrom == null || mFrom.length != count) {
                mFrom = new int[count];
            }
            for (i = 0; i < count; i++) {
                mFrom[i] = c.getColumnIndexOrThrow(from[i]);
            }
        } else {
            mFrom = null;
        }
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        // super.swapCursor() will notify observers before we have
        // a valid mapping, make sure we have a mapping before this
        // happens
        if (mFrom == null) {
            findColumns(getCursor(), mOriginalFrom);
        }
        Cursor res = super.swapCursor(c);
        // rescan columns in case cursor layout is different
        findColumns(c, mOriginalFrom);
        return res;
    }
    
    /**
     * Change the cursor and change the column-to-view mappings at the same time.
     *  
     * @param c The database cursor.  Can be null if the cursor is not available yet.
     * @param from A list of column names representing the data to bind to the UI.  Can be null 
     *            if the cursor is not available yet.
     * @param to The views that should display column in the "from" parameter.
     *            These should all be TextViews. The first N views in this list
     *            are given the values of the first N columns in the from
     *            parameter.  Can be null if the cursor is not available yet.
     */
    public void changeCursorAndColumns(Cursor c, String[] from, int[] to) {
        mOriginalFrom = from;
        mTo = to;
        // super.changeCursor() will notify observers before we have
        // a valid mapping, make sure we have a mapping before this
        // happens
        if (mFrom == null) {
            findColumns(getCursor(), mOriginalFrom);
        }
        super.changeCursor(c);
        findColumns(c, mOriginalFrom);
    }

    public static interface ViewBinder {
        /**
         * Binds the Cursor column defined by the specified index to the specified view.
         *
         * When binding is handled by this ViewBinder, this method must return true.
         * If this method returns false, SimpleCursorAdapter will attempts to handle
         * the binding on its own.
         *
         * @param view the view to bind the data to
         * @param cursor the cursor to get the data from
         * @param columnIndex the column at which the data can be found in the cursor
         *
         * @return true if the data was bound to the view, false otherwise
         */
        boolean setViewValue(View view, Cursor cursor, int columnIndex);
    }
}

