package com.chanapps.four.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.chanapps.four.activity.GalleryViewActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.fragment.ThreadFragment;
import com.chanapps.four.fragment.ThreadPopupDialogFragment;
import com.chanapps.four.viewer.ThreadViewHolder;
import com.chanapps.four.viewer.ThreadViewer;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/4/13
 * Time: 7:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadListCursorAdapter extends AbstractThreadCursorAdapter {

    protected static final String TAG = ThreadListCursorAdapter.class.getSimpleName();
    protected static final boolean DEBUG = false;

    protected static final int TYPE_MAX_COUNT = 5;
    protected static final int TYPE_HEADER = 0;
    protected static final int TYPE_IMAGE_ITEM = 1;
    protected static final int TYPE_TEXT_ITEM = 2;
    protected static final int TYPE_TITLE = 3;
    protected static final int TYPE_LINK = 4;

    protected boolean showContextMenu;
    protected Runnable onDismissCallback;

    protected ThreadListCursorAdapter(Context context, int layout, ViewBinder viewBinder, String[] from, int[] to) {
        super(context, layout, viewBinder, from, to);
    }

    public ThreadListCursorAdapter(Context context, ViewBinder viewBinder, boolean showContextMenu, Runnable onDismissCallback) {
        this(context,
                R.layout.thread_list_image_item,
                viewBinder,
                new String[]{
                        ChanPost.POST_IMAGE_URL,
                        ChanPost.POST_IMAGE_URL,
                        ChanPost.POST_HEADLINE_TEXT,
                        ChanPost.POST_SUBJECT_TEXT,
                        ChanPost.POST_SUBJECT_TEXT,
                        ChanPost.POST_SUBJECT_TEXT,
                        ChanPost.POST_TEXT,
                        ChanPost.POST_COUNTRY_URL,
                        ChanPost.POST_FLAGS
                },
                new int[]{
                        R.id.list_item_image_wrapper,
                        R.id.list_item_image,
                        R.id.list_item_header,
                        R.id.list_item_subject,
                        R.id.list_item_subject_icons,
                        R.id.list_item_title,
                        R.id.list_item_text,
                        R.id.list_item_country_flag,
                        R.id.list_item_exif_text
                });
        this.showContextMenu = showContextMenu;
        this.onDismissCallback = onDismissCallback;
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = getCursor();
        if (cursor == null)
            throw new IllegalStateException("this should only be called when the cursor is valid");
        if (!cursor.moveToPosition(position))
            throw new IllegalStateException("couldn't move cursor to position " + position);
        int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
        int tag;
        if ((flags & ChanPost.FLAG_IS_HEADER) > 0)
            tag = TYPE_HEADER;
        else if ((flags & ChanPost.FLAG_IS_TITLE) > 0)
            tag = TYPE_TITLE;
        else if ((flags & ChanPost.FLAG_HAS_IMAGE) > 0)
            tag = TYPE_IMAGE_ITEM;
        else
            tag = TYPE_TEXT_ITEM;
        return tag;
    }

    protected int getItemViewLayout(int tag) {
        int id;
        switch(tag) {
            case TYPE_HEADER:
                id = R.layout.thread_list_header;
                break;
            case TYPE_TITLE:
                id = R.layout.thread_list_title;
                break;
            case TYPE_LINK:
                id = R.layout.thread_list_link;
                break;
            case TYPE_IMAGE_ITEM:
                id = R.layout.thread_list_image_item;
                break;
            case TYPE_TEXT_ITEM:
            default:
                id = R.layout.thread_list_text_item;
        }
        return id;
    }

    @Override
    protected View newView(ViewGroup parent, int tag, int position) {
        if (DEBUG) Log.d(TAG, "Creating " + tag + " layout for " + position);
        int id = getItemViewLayout(tag);
        View v = mInflater.inflate(id, parent, false);
        ThreadViewHolder viewHolder = new ThreadViewHolder(v);
        v.setTag(R.id.VIEW_TAG_TYPE, tag);
        v.setTag(R.id.VIEW_HOLDER, viewHolder);
        return v;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_MAX_COUNT;
    }

    @Override
    protected void updateView(final View view, final Cursor cursor, final int pos) {
        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID)); // id of header is the threadNo
        final long resto = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO)); // resto of item is the threadNo
        final long threadNo = resto > 0 ? resto : postId;
        final long postNo = resto > 0 ? postId : 0;
        ThreadViewHolder viewHolder = (ThreadViewHolder)view.getTag(R.id.VIEW_HOLDER);
        final ThreadFragment fragment = context != null && context instanceof ThreadActivity
                ? ((ThreadActivity)context).getCurrentFragment()
                : null;
        final String query = fragment == null ? "" : fragment.getQuery();
        if (resto == 0) { // it's a header
            if (DEBUG) Log.i(TAG, "view already set for thread header, only adjusting status icons and num comments/images/replies");
            int flagIdx = cursor.getColumnIndex(ChanPost.POST_FLAGS);
            int flags = flagIdx >= 0 ? cursor.getInt(flagIdx) : -1;
            ThreadViewer.setSubjectIcons(viewHolder, flags);
            View.OnClickListener listener = fragment != null
                    ? ThreadViewer.createCommentsOnClickListener(fragment.getAbsListView(), fragment.getHandler())
                    : null;
            ThreadViewer.setHeaderNumRepliesImages(viewHolder, cursor,
                    listener,
                    ThreadViewer.createImagesOnClickListener(context, boardCode, threadNo));
            ThreadViewer.displayNumDirectReplies(viewHolder, cursor, showContextMenu, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (context instanceof FragmentActivity) {
                        if (DEBUG) Log.i(TAG, "should dismiss parent here fragment=" + fragment);
                        if (onDismissCallback != null)
                            onDismissCallback.run();
                        //(new ThreadPopupDialogFragment(fragment, boardCode, threadNo, threadNo, pos, ThreadPopupDialogFragment.PopupType.REPLIES, query))
                        (new ThreadPopupDialogFragment(fragment, boardCode, threadNo, threadNo, ThreadPopupDialogFragment.PopupType.REPLIES, query))
                                .show(((FragmentActivity)context).getSupportFragmentManager(), ThreadPopupDialogFragment.TAG);
                    }
                }
            });
            return;
        }
        else {
            if (DEBUG) Log.i(TAG, "view already set for thread item, only adjusting num replies");
            if (DEBUG) Log.i(TAG, "displayNumDirectReplies showContextMenu=" + showContextMenu + " cursor count" + cursor.getCount());
            ThreadViewer.displayNumDirectReplies(viewHolder, cursor, showContextMenu, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (context instanceof FragmentActivity) {
                        if (DEBUG) Log.i(TAG, "should dismiss parent here fragment=" + fragment);
                        if (onDismissCallback != null)
                            onDismissCallback.run();
                        //(new ThreadPopupDialogFragment(fragment, boardCode, threadNo, postNo, pos, ThreadPopupDialogFragment.PopupType.REPLIES, query))
                        (new ThreadPopupDialogFragment(fragment, boardCode, threadNo, postNo, ThreadPopupDialogFragment.PopupType.REPLIES, query))
                                .show(((FragmentActivity)context).getSupportFragmentManager(), ThreadPopupDialogFragment.TAG);
                    }
                }
            });
            return;
        }
    }

}
