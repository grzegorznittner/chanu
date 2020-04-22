/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.graphics.RectF;

import javax.microedition.khronos.opengles.GL11;

//
// GLCanvas gives a convenient interface to draw using OpenGL.
//
// When a rectangle is specified in this interface, it means the region
// [x, x+width) * [y, y+height)
//
public interface GLCanvas {
    int SAVE_FLAG_ALL = 0xFFFFFFFF;
    int SAVE_FLAG_CLIP = 0x01;
    int SAVE_FLAG_ALPHA = 0x02;
    int SAVE_FLAG_MATRIX = 0x04;

    // Tells GLCanvas the size of the underlying GL surface. This should be
    // called before first drawing and when the size of GL surface is changed.
    // This is called by GLRoot and should not be called by the clients
    // who only want to draw on the GLCanvas. Both width and height must be
    // nonnegative.
    void setSize(int width, int height);

    // Clear the drawing buffers. This should only be used by GLRoot.
    void clearBuffer();

    // This is the time value used to calculate the animation in the current
    // frame. The "set" function should only called by GLRoot, and the
    // "time" parameter must be nonnegative.
    void setCurrentAnimationTimeMillis(long time);

    long currentAnimationTimeMillis();

    void setBlendEnabled(boolean enabled);

    float getAlpha();

    // Sets and gets the current alpha, alpha must be in [0, 1].
    void setAlpha(float alpha);

    // (current alpha) = (current alpha) * alpha
    void multiplyAlpha(float alpha);

    // Change the current transform matrix.
    void translate(float x, float y, float z);

    void scale(float sx, float sy, float sz);

    void rotate(float angle, float x, float y, float z);

    void multiplyMatrix(float[] mMatrix, int offset);

    // Modifies the current clip with the specified rectangle.
    // (current clip) = (current clip) intersect (specified rectangle).
    // Returns true if the result clip is non-empty.
    boolean clipRect(int left, int top, int right, int bottom);

    // Pushes the configuration state (matrix, alpha, and clip) onto
    // a private stack.
    int save();

    // Same as save(), but only save those specified in saveFlags.
    int save(int saveFlags);

    // Pops from the top of the stack as current configuration state (matrix,
    // alpha, and clip). This call balances a previous call to save(), and is
    // used to remove all modifications to the configuration state since the
    // last save call.
    void restore();

    // Draws a line using the specified paint from (x1, y1) to (x2, y2).
    // (Both end points are included).
    void drawLine(float x1, float y1, float x2, float y2, GLPaint paint);

    // Draws a rectangle using the specified paint from (x1, y1) to (x2, y2).
    // (Both end points are included).
    void drawRect(float x1, float y1, float x2, float y2, GLPaint paint);

    // Fills the specified rectangle with the specified color.
    void fillRect(float x, float y, float width, float height, int color);

    // Draws a texture to the specified rectangle.
    void drawTexture(BasicTexture texture, int x, int y, int width, int height);

    void drawMesh(BasicTexture tex, int x, int y, int xyBuffer, int uvBuffer, int indexBuffer, int indexCount);

    // Draws a texture to the specified rectangle. The "alpha" parameter
    // overrides the current drawing alpha value.
    void drawTexture(BasicTexture texture, int x, int y, int width, int height, float alpha);

    // Draws a the source rectangle part of the texture to the target rectangle.
    void drawTexture(BasicTexture texture, RectF source, RectF target);

    // Draw two textures to the specified rectangle. The actual texture used is
    // from * (1 - ratio) + to * ratio
    // The two textures must have the same size.
    void drawMixed(BasicTexture from, BasicTexture to, float ratio, int x, int y, int w, int h);

    void drawMixed(BasicTexture from, int toColor, float ratio, int x, int y, int w, int h);

    // Return a texture copied from the specified rectangle.
    BasicTexture copyTexture(int x, int y, int width, int height);

    // Gets the underlying GL instance. This is used only when direct access to
    // GL is needed.
    GL11 getGLInstance();

    // Unloads the specified texture from the canvas. The resource allocated
    // to draw the texture will be released. The specified texture will return
    // to the unloaded state. This function should be called only from
    // BasicTexture or its descendant
    boolean unloadTexture(BasicTexture texture);

    // Delete the specified buffer object, similar to unloadTexture.
    void deleteBuffer(int bufferId);

    // Delete the textures and buffers in GL side. This function should only be
    // called in the GL thread.
    void deleteRecycledResources();

}
