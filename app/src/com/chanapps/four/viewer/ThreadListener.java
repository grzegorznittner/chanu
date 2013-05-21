package com.chanapps.four.viewer;

import android.database.Cursor;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.component.ThreadExpandExifOnClickListener;
import com.chanapps.four.component.ThreadExpandImageOnClickListener;
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
    private static final boolean DEBUG = false;

    private FragmentManager fragmentManager;
    private AbsListView absListView;
    private AbstractBoardCursorAdapter adapter;
    private Handler handler;

    public ThreadListener(FragmentManager fragmentManager, AbsListView absListView,
                          AbstractBoardCursorAdapter adapter, Handler handler) {
        this.fragmentManager = fragmentManager;
        this.absListView = absListView;
        this.adapter = adapter;
        this.handler = handler;
    }

    private final View.OnClickListener createPopupListener(final ThreadPopupDialogFragment.PopupType popupType) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = absListView.getPositionForView(v);
                Cursor cursor = adapter.getCursor();
                cursor.moveToPosition(pos);
                String linkedBoardCode = cursor.getString(cursor.getColumnIndex(ChanPost.POST_BOARD_CODE));
                long linkedThreadNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_RESTO));
                long linkedPostNo = cursor.getLong(cursor.getColumnIndex(ChanPost.POST_ID));
                if (linkedThreadNo <= 0)
                    linkedThreadNo = linkedPostNo;
                (new ThreadPopupDialogFragment(linkedBoardCode, linkedThreadNo, linkedPostNo, pos, popupType))
                        .show(fragmentManager, ThreadPopupDialogFragment.TAG);
            }
        };
    }

    public final View.OnClickListener backlinkOnClickListener = createPopupListener(ThreadPopupDialogFragment.PopupType.BACKLINKS);
    public final View.OnClickListener repliesOnClickListener = createPopupListener(ThreadPopupDialogFragment.PopupType.REPLIES);
    public final View.OnClickListener sameIdOnClickListener = createPopupListener(ThreadPopupDialogFragment.PopupType.SAME_ID);

    public final View.OnClickListener imageOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            if (DEBUG) Log.i(TAG, "received item click pos: " + pos);

            View itemView = null;
            for (int i = 0; i < absListView.getChildCount(); i++) {
                View child = absListView.getChildAt(i);
                if (absListView.getPositionForView(child) == pos) {
                    itemView = child;
                    break;
                }
            }
            if (DEBUG) Log.i(TAG, "found itemView=" + itemView);
            if (itemView == null)
                return;
            if ((Boolean) itemView.getTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED))
                return;

            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            final int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if (DEBUG) Log.i(TAG, "clicked flags=" + flags);
            if ((flags & (ChanPost.FLAG_HAS_IMAGE)) > 0) {
                (new ThreadExpandImageOnClickListener(v.getContext(), cursor, itemView)).onClick(itemView);
                itemView.setTag(R.id.THREAD_VIEW_IS_IMAGE_EXPANDED, Boolean.TRUE);
            }
        }
    };

    public final View.OnClickListener exifOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = absListView.getPositionForView(v);
            if (DEBUG) Log.i(TAG, "received item click pos: " + pos);

            View itemView = null;
            for (int i = 0; i < absListView.getChildCount(); i++) {
                View child = absListView.getChildAt(i);
                if (absListView.getPositionForView(child) == pos) {
                    itemView = child;
                    break;
                }
            }
            if (DEBUG) Log.i(TAG, "found itemView=" + itemView);
            if (itemView == null)
                return;
            if ((Boolean) itemView.getTag(R.id.THREAD_VIEW_IS_EXIF_EXPANDED))
                return;

            Cursor cursor = adapter.getCursor();
            cursor.moveToPosition(pos);
            final int flags = cursor.getInt(cursor.getColumnIndex(ChanPost.POST_FLAGS));
            if (DEBUG) Log.i(TAG, "clicked flags=" + flags);
            if ((flags & (ChanPost.FLAG_HAS_EXIF)) > 0) {
                (new ThreadExpandExifOnClickListener(absListView, cursor, handler)).onClick(itemView);
                itemView.setTag(R.id.THREAD_VIEW_IS_EXIF_EXPANDED, Boolean.TRUE);
            }
        }
    };

}
