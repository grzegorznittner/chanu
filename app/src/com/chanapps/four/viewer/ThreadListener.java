package com.chanapps.four.viewer;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.GalleryViewActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.component.ThreadExpandExifOnClickListener;
import com.chanapps.four.component.ThreadImageExpander;
import com.chanapps.four.component.ThreadViewable;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.fragment.ThreadPopupDialogFragment;

import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/21/13
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadListener {

    private static final String TAG = ThreadListener.class.getSimpleName();
    private static final boolean DEBUG = false;

    private ThreadViewable threadViewable;
    private boolean isDark;

    public ThreadListener(ThreadViewable threadViewable, boolean isDark) {
        this.threadViewable = threadViewable;
        this.isDark = isDark;
    }

    private final SpannableOnClickListener createPopupListener(final ThreadPopupDialogFragment.PopupType popupType) {
        return new SpannableOnClickListener() {
            private int pos;
            private String boardCode;
            private long threadNo;
            private long postNo;
            private void locatePost(View v) {
                pos = -1;
                if (threadViewable == null)
                    return;
                if (threadViewable.getAbsListView() == null)
                    return;
                if (threadViewable.getAdapter() == null)
                    return;
                Cursor cursor = threadViewable.getAdapter().getCursor();
                pos = threadViewable.getAbsListView().getPositionForView(v);
                if (DEBUG) Log.i(TAG, "locatePost() no cursorId, current pos=" + pos);
                if (cursor.moveToPosition(pos)) {
                    postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
                    boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
                    threadNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
                    if (threadNo <= 0)
                        threadNo = postNo;
                }
                else {
                    pos = -1;
                }
            }
            private void locatePost(View v, long cursorId) {
                if (threadViewable == null)
                    return;
                if (threadViewable.getAdapter() == null)
                    return;
                Cursor cursor = threadViewable.getAdapter().getCursor();
                postNo = cursorId;
                if (DEBUG) Log.i(TAG, "locatePost() looking for postNo=" + postNo
                        + " out of " + cursor.getCount() + " cursor items");
                // not efficient, but won't be more than a few hundred or thousand results
                final int col = cursor.getColumnIndex(ChanPost.POST_ID);
                pos = -1;
                cursor.moveToPosition(-1);
                while (cursor.moveToNext())
                    if (cursor.getLong(col) == postNo) {
                        pos = cursor.getPosition();
                        break;
                    }
                if (cursor.moveToPosition(pos)) {
                    boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
                    threadNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
                    if (threadNo <= 0)
                        threadNo = postNo;
                }
                else {
                    pos = -1;
                }
                if (DEBUG) Log.i(TAG, "locatePost() cursorId=" + cursorId + " found pos=" + pos);
            }
            private void launchThread(Activity activity, long threadNo) {
                if (threadViewable == null)
                    return;
                if (threadViewable.getAdapter() == null)
                    return;
                Cursor cursor = threadViewable.getAdapter().getCursor();
                if (cursor.moveToFirst()) {
                    boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
                    if (DEBUG) Log.i(TAG, "locatePost() launching thread activity /" + boardCode + "/" + threadNo);
                    ThreadActivity.startActivity(activity, boardCode, threadNo, "");
                }
            }
            @Override
            public void onClick(View v) {
                if (threadViewable == null)
                    return;
                locatePost(v);
                if (DEBUG) Log.i(TAG, "popupListener clicked pos=" + pos + " type=" + popupType);
                if (pos >= 0)
                    threadViewable.showDialog(boardCode, threadNo, postNo, pos, popupType);
            }
            @Override
            public void onClick(View v, long cursorId) {
                if (threadViewable == null)
                    return;
                locatePost(v, cursorId);
                if (DEBUG) Log.i(TAG, "popupListener clicked pos=" + pos + " type=" + popupType + " popup postNo=" + postNo);
                if (pos >= 0)
                    threadViewable.showDialog(boardCode, threadNo, postNo, pos, ThreadPopupDialogFragment.PopupType.SELF);
                else if (v.getContext() instanceof Activity)
                    launchThread((Activity)v.getContext(), cursorId);
            }
        };
    }

    public final SpannableOnClickListener backlinkOnClickListener = createPopupListener(ThreadPopupDialogFragment.PopupType.SELF);
    //public final View.OnClickListener backlinkOnClickListener = createPopupListener(ThreadPopupDialogFragment.PopupType.BACKLINKS);
    public final View.OnClickListener repliesOnClickListener = createPopupListener(ThreadPopupDialogFragment.PopupType.REPLIES);
    public final View.OnClickListener sameIdOnClickListener = createPopupListener(ThreadPopupDialogFragment.PopupType.SAME_ID);

    public final View.OnClickListener thumbOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (threadViewable == null)
                return;
            if (threadViewable.getAbsListView() == null)
                return;
            int pos = threadViewable.getAbsListView().getPositionForView(v);
            if (DEBUG) Log.i(TAG, "received item click pos: " + pos);

            View itemView = null;
            for (int i = 0; i < threadViewable.getAbsListView().getChildCount(); i++) {
                View child = threadViewable.getAbsListView().getChildAt(i);
                if (threadViewable.getAbsListView().getPositionForView(child) == pos) {
                    itemView = child;
                    break;
                }
            }
            if (DEBUG) Log.i(TAG, "found itemView=" + itemView);
            if (itemView == null)
                return;
            ThreadViewHolder viewHolder = (ThreadViewHolder)itemView.getTag(R.id.VIEW_HOLDER);
            if (viewHolder == null)
                return;

            if ((Boolean) itemView.getTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED)) {
                if (DEBUG) Log.i(TAG, "image already expanded, collapsing");
                if (viewHolder.list_item_image_expanded_wrapper != null)
                    viewHolder.list_item_image_expanded_wrapper.setVisibility(View.GONE);
                if (viewHolder.list_item_image_collapse != null)
                    viewHolder.list_item_image_collapse.setVisibility(View.GONE);
                if (viewHolder.list_item_image != null)
                    viewHolder.list_item_image.setVisibility(View.VISIBLE);
                if (viewHolder.list_item_image_header != null)
                    viewHolder.list_item_image_header.setVisibility(View.VISIBLE);
                if (viewHolder.list_item_image_nothumbs_expand != null)
                    viewHolder.list_item_image_nothumbs_expand.setVisibility(View.VISIBLE);
                if (viewHolder.list_item != null)
                    viewHolder.list_item.setTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED, Boolean.FALSE);
                return;
            }

            if (threadViewable == null)
                return;
            if (threadViewable.getAdapter() == null)
                return;
            Cursor cursor = threadViewable.getAdapter().getCursor();
            cursor.moveToPosition(pos);
            final int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if (DEBUG) Log.i(TAG, "clicked flags=" + flags);
            if ((flags & (ChanPost.FLAG_HAS_IMAGE)) > 0) {
                //if (viewHolder.list_item_image_expanded != null
                //        && viewHolder.list_item_image_expanded.getVisibility() != View.GONE
                //        && viewHolder.list_item_image_expanded.getHeight() > 0) {
                if (DEBUG) Log.i(TAG, "expanding pos: " + pos);
                ThreadImageExpander expander = (new ThreadImageExpander(viewHolder, cursor, expandedImageListener,
                        true,
                        isDark
                                ? R.drawable.stub_image_background_dark
                                : R.drawable.stub_image_background));
                expander.displayImage();
                itemView.setTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED, Boolean.TRUE);
            }
        }
    };

    public View.OnClickListener expandedImageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (threadViewable == null)
                return;
            if (threadViewable.getAbsListView() == null)
                return;
            int pos = -1;
            try {
                pos = threadViewable.getAbsListView().getPositionForView(v);
            }
            catch (Exception e)  {
                Log.e(TAG, "Exception getting thread viewable for view = " + v, e);
                pos = -1;
            }
            if (pos < 0)
                return;
            Cursor cursor = threadViewable.getAdapter().getCursor();
            if (!cursor.moveToPosition(pos))
                return;
            String boardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
            long postNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
            long threadNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
            if (threadNo <= 0)
                threadNo = postNo;
            //if (postNo == threadNo)
            //    postNo = 0;
            if (DEBUG) Log.i(TAG, "expandImageListener /" + boardCode + "/" + threadNo + "#p" + postNo);
            if (postNo > 0)
                GalleryViewActivity.startActivity(v.getContext(), boardCode, threadNo, postNo);
            else
                GalleryViewActivity.startAlbumViewActivity(v.getContext(), boardCode, threadNo);
        }
    };

    public final View.OnClickListener exifOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (threadViewable == null)
                return;
            if (threadViewable.getAbsListView() == null)
                return;
            int pos = threadViewable.getAbsListView().getPositionForView(v);
            if (DEBUG) Log.i(TAG, "received item click pos: " + pos);

            View itemView = null;
            for (int i = 0; i < threadViewable.getAbsListView().getChildCount(); i++) {
                View child = threadViewable.getAbsListView().getChildAt(i);
                if (threadViewable.getAbsListView().getPositionForView(child) == pos) {
                    itemView = child;
                    break;
                }
            }
            if (DEBUG) Log.i(TAG, "found itemView=" + itemView);
            if (itemView == null)
                return;
            if ((Boolean) itemView.getTag(R.id.THREAD_VIEW_IS_EXIF_EXPANDED))
                return;

            Cursor cursor = threadViewable.getAdapter() == null ? null : threadViewable.getAdapter().getCursor();
            if (cursor == null)
                return;
            if (!cursor.moveToPosition(pos))
                return;
            final int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if (DEBUG) Log.i(TAG, "clicked flags=" + flags);
            if ((flags & (ChanPost.FLAG_HAS_EXIF)) > 0) {
                (new ThreadExpandExifOnClickListener(
                        threadViewable.getAbsListView(), cursor, threadViewable.getHandler()))
                        .onClick(itemView);
                itemView.setTag(R.id.THREAD_VIEW_IS_EXIF_EXPANDED, Boolean.TRUE);
            }
        }
    };

}
