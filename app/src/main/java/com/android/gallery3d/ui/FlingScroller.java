/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.ui;

import com.android.gallery3d.common.Utils;

// This is a customized version of Scroller, with a interface similar to
// android.widget.Scroller. It does fling only, not scroll.
//
// The differences between the this Scroller and the system one are:
//
// (1) The velocity does not change because of min/max limit.
// (2) The duration is different.
// (3) The deceleration curve is different.
class FlingScroller {
    private static final String TAG = "FlingController";

    // The fling duration (in milliseconds) when velocity is 1 pixel/second
    private static final float FLING_DURATION_PARAM = 50f;
    private static final int DECELERATED_FACTOR = 4;

    private int mStartX, mStartY;
    private int mMinX, mMinY, mMaxX, mMaxY;
    private double mSinAngle;
    private double mCosAngle;
    private int mDuration;
    private int mDistance;
    private int mFinalX, mFinalY;

    private int mCurrX, mCurrY;
    private double mCurrV;

    public int getFinalX() {
        return mFinalX;
    }

    public int getFinalY() {
        return mFinalY;
    }

    public int getDuration() {
        return mDuration;
    }

    public int getCurrX() {
        return mCurrX;

    }

    public int getCurrY() {
        return mCurrY;
    }

    public int getCurrVelocityX() {
        return (int) Math.round(mCurrV * mCosAngle);
    }

    public int getCurrVelocityY() {
        return (int) Math.round(mCurrV * mSinAngle);
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
        mStartX = startX;
        mStartY = startY;
        mMinX = minX;
        mMinY = minY;
        mMaxX = maxX;
        mMaxY = maxY;

        double velocity = Math.hypot(velocityX, velocityY);
        mSinAngle = velocityY / velocity;
        mCosAngle = velocityX / velocity;
        //
        // The position formula: x(t) = s + (e - s) * (1 - (1 - t / T) ^ d)
        //     velocity formula: v(t) = d * (e - s) * (1 - t / T) ^ (d - 1) / T
        // Thus,
        //     v0 = d * (e - s) / T => (e - s) = v0 * T / d
        //

        // Ta = T_ref * (Va / V_ref) ^ (1 / (d - 1)); V_ref = 1 pixel/second;
        mDuration = (int) Math.round(FLING_DURATION_PARAM * Math.pow(Math.abs(velocity), 1.0 / (DECELERATED_FACTOR - 1)));

        // (e - s) = v0 * T / d
        mDistance = (int) Math.round(velocity * mDuration / DECELERATED_FACTOR / 1000);

        mFinalX = getX(1.0f);
        mFinalY = getY(1.0f);
    }

    public void computeScrollOffset(float progress) {
        progress = Math.min(progress, 1);
        float f = 1 - progress;
        f = 1 - (float) Math.pow(f, DECELERATED_FACTOR);
        mCurrX = getX(f);
        mCurrY = getY(f);
        mCurrV = getV(progress);
    }

    private int getX(float f) {
        return (int) Utils.clamp(Math.round(mStartX + f * mDistance * mCosAngle), mMinX, mMaxX);
    }

    private int getY(float f) {
        return (int) Utils.clamp(Math.round(mStartY + f * mDistance * mSinAngle), mMinY, mMaxY);
    }

    private double getV(float progress) {
        // velocity formula: v(t) = d * (e - s) * (1 - t / T) ^ (d - 1) / T
        return DECELERATED_FACTOR * mDistance * 1000 * Math.pow(1 - progress, DECELERATED_FACTOR - 1) / mDuration;
    }
}
