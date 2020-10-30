package com.chanapps.four.component;

import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;

public class ScrollerRunnable implements Runnable {

    private static final int SCROLL_DURATION = 1000;

    private static final int MOVE_DOWN_POS = 1;
    private static final int MOVE_UP_POS = 2;
    private final int mExtraScroll;
    private AbsListView mList;
    private int mMode;
    private int mTargetPos;
    private int mLastSeenPos;
    private int mScrollDuration;

    public ScrollerRunnable(AbsListView listView) {
        mList = listView;
        mExtraScroll = ViewConfiguration.get(mList.getContext()).getScaledFadingEdgeLength();
    }

    public void start(int position) {
        stop();

        final int firstPos = mList.getFirstVisiblePosition();
        final int lastPos = firstPos + mList.getChildCount() - 1;

        int viewTravelCount = 0;
        if (position <= firstPos) {
            viewTravelCount = firstPos - position + 1;
            mMode = MOVE_UP_POS;
        } else if (position >= lastPos) {
            viewTravelCount = position - lastPos + 1;
            mMode = MOVE_DOWN_POS;
        } else {
            // Already on screen, nothing to do
            return;
        }

        if (viewTravelCount > 0) {
            mScrollDuration = SCROLL_DURATION / viewTravelCount;
        } else {
            mScrollDuration = SCROLL_DURATION;
        }
        mTargetPos = position;
        mLastSeenPos = AbsListView.INVALID_POSITION;

        mList.post(this);
    }

    void stop() {
        mList.removeCallbacks(this);
    }

    public void run() {
        final int listHeight = mList.getHeight();
        final int firstPos = mList.getFirstVisiblePosition();

        switch (mMode) {
            case MOVE_DOWN_POS: {
                final int lastViewIndex = mList.getChildCount() - 1;
                final int lastPos = firstPos + lastViewIndex;

                if (lastViewIndex < 0) {
                    return;
                }

                if (lastPos == mLastSeenPos) {
                    // No new views, let things keep going.
                    mList.post(this);
                    return;
                }

                final View lastView = mList.getChildAt(lastViewIndex);
                final int lastViewHeight = lastView.getHeight();
                final int lastViewTop = lastView.getTop();
                final int lastViewPixelsShowing = listHeight - lastViewTop;
                final int extraScroll = lastPos < mList.getCount() - 1 ? mExtraScroll : mList.getPaddingBottom();

                mList.smoothScrollBy(lastViewHeight - lastViewPixelsShowing + extraScroll, mScrollDuration);

                mLastSeenPos = lastPos;
                if (lastPos < mTargetPos) {
                    mList.post(this);
                }
                break;
            }

            case MOVE_UP_POS: {
                if (firstPos == mLastSeenPos) {
                    // No new views, let things keep going.
                    mList.post(this);
                    return;
                }

                final View firstView = mList.getChildAt(0);
                if (firstView == null) {
                    return;
                }
                final int firstViewTop = firstView.getTop();
                final int extraScroll = firstPos > 0 ? mExtraScroll : mList.getPaddingTop();

                mList.smoothScrollBy(firstViewTop - extraScroll, mScrollDuration);

                mLastSeenPos = firstPos;

                if (firstPos > mTargetPos) {
                    mList.post(this);
                }
                break;
            }

            default:
                break;
        }
    }
}

