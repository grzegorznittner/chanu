package com.chanapps.four.viewer;

import android.app.Fragment;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import com.chanapps.four.activity.GalleryViewActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ThreadExpandExifOnClickListener;
import com.chanapps.four.component.ThreadExpandImageOnClickListener;
import com.chanapps.four.component.ThreadViewable;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.fragment.ThreadPopupDialogFragment;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/21/13
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadListener {

    private static final String TAG = ThreadListener.class.getSimpleName();
    private static final boolean DEBUG = true;

    private ThreadViewable threadViewable;

    public ThreadListener(ThreadViewable threadViewable) {
        this.threadViewable = threadViewable;
    }

    private final View.OnClickListener createPopupListener(final ThreadPopupDialogFragment.PopupType popupType) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = threadViewable.getAbsListView().getPositionForView(v);
                Cursor cursor = threadViewable.getAdapter().getCursor();
                cursor.moveToPosition(pos);
                String linkedBoardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
                long linkedThreadNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
                long linkedPostNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
                if (linkedThreadNo <= 0)
                    linkedThreadNo = linkedPostNo;
                if (DEBUG) Log.i(TAG, "popupListener clicked pos=" + pos + " type=" + popupType);
                threadViewable.showDialog(linkedBoardCode, linkedThreadNo, linkedPostNo, pos, popupType);
            }
        };
    }

    public final View.OnClickListener backlinkOnClickListener = createPopupListener(ThreadPopupDialogFragment.PopupType.BACKLINKS);
    public final View.OnClickListener repliesOnClickListener = createPopupListener(ThreadPopupDialogFragment.PopupType.REPLIES);
    public final View.OnClickListener sameIdOnClickListener = createPopupListener(ThreadPopupDialogFragment.PopupType.SAME_ID);

    public final View.OnClickListener imageOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
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
            if ((Boolean) itemView.getTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED))
                return;

            Cursor cursor = threadViewable.getAdapter().getCursor();
            cursor.moveToPosition(pos);
            final int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if (DEBUG) Log.i(TAG, "clicked flags=" + flags);
            if ((flags & (ChanPost.FLAG_HAS_IMAGE)) > 0) {
                (new ThreadExpandImageOnClickListener(v.getContext(), cursor, itemView, expandedImageListener))
                        .onClick(itemView);
                itemView.setTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED, Boolean.TRUE);
            }
        }
    };

    public View.OnClickListener expandedImageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = threadViewable.getAbsListView().getPositionForView(v);
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
