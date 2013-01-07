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
public class BoardCursorAdapter extends ResourceCursorAdapter {
	private static final String TAG = BoardCursorAdapter.class.getSimpleName();
	private static final boolean DEBUG = false;
	
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
    public BoardCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
        super(context, layout, null, 0);
        this.context = context;
        mTo = to;
        mOriginalFrom = from;
        mViewBinder = viewBinder;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        String tag = null;
        String imageUrl = null;
        int loadPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOAD_PAGE));
        int lastPage = cursor.getInt(cursor.getColumnIndex(ChanHelper.LAST_PAGE));
        long tim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
    	if (loadPage > 0) {
    		tag = ChanHelper.LOAD_PAGE;
    	}
        else if (lastPage > 0) {
            tag = ChanHelper.LAST_PAGE;
        }
        else {
    		imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
    		if (imageUrl != null && imageUrl.length() > 0) {
    			tag = ChanHelper.POST_IMAGE_URL;
    		} else {
    			tag = ChanHelper.POST_SHORT_TEXT;
    		}
    	}

        View v;
        if (convertView == null || !tag.equals(convertView.getTag())) {
            v = newView(context, parent, tag, position);
            v.setTag(tag);
            if (ChanHelper.POST_IMAGE_URL.equals(tag)) {
        		ImageView imageView = (ImageView)v.findViewById(R.id.grid_item_image);
        		imageView.setTag(imageUrl);
            }
        } else {
        	if (DEBUG) Log.d(TAG, "Reusing existing " + tag + " layout for " + position);
        	/*
            if (ChanHelper.POST_IMAGE_URL.equals(tag)) {
        		ImageView imageView = (ImageView)convertView.findViewById(R.id.board_activity_grid_item_image);
        		if (imageView != null && !imageUrl.equals(imageView.getTag())) {
        			//imageView.setImageResource(R.drawable.stub_image);
        		}
        	}
        	*/
            v = convertView;
        }
        bindView(v, context, cursor);
        return v;
    }
    
    protected View newView(Context context, ViewGroup parent, String tag, int position) {
		if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        if (ChanHelper.LOAD_PAGE.equals(tag)) {
       		return mInflater.inflate(R.layout.board_grid_item_load_page, parent, false);
       	}
        if (ChanHelper.LAST_PAGE.equals(tag)) {
       		return mInflater.inflate(R.layout.board_grid_item_last_page, parent, false);
       	}
    	if (ChanHelper.POST_IMAGE_URL.equals(tag)) {
    		return mInflater.inflate(R.layout.board_grid_item, parent, false);
    	} else {
    		return mInflater.inflate(R.layout.board_grid_item_no_image, parent, false);
    	}
    }

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

