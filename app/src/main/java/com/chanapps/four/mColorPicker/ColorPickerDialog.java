/*
 * Copyright (C) 2010 Daniel Nilsson
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

package com.chanapps.four.mColorPicker;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.chanapps.four.activity.R;
import com.chanapps.four.mColorPicker.views.ColorPanelView;
import com.chanapps.four.mColorPicker.views.ColorPickerView;
import com.chanapps.four.mColorPicker.views.ColorPickerView.OnColorChangedListener;

public class ColorPickerDialog extends AlertDialog implements ColorPickerView.OnColorChangedListener {

    private ColorPickerView mColorPicker;

    private ColorPanelView mOldColor;
    private ColorPanelView mNewColor;

    private OnColorChangedListener mListener;

    public ColorPickerDialog(Context context, int initialColor) {
        super(context);

        init(initialColor);
    }

    private void init(int color) {
        // To fight color branding.
        getWindow().setFormat(PixelFormat.RGBA_8888);

        setUp(color);

    }

    private void setUp(int color) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_color_picker, null);

        setView(layout);

        mColorPicker = layout.findViewById(R.id.color_picker_view);
        mOldColor = layout.findViewById(R.id.old_color_panel);
        mNewColor = layout.findViewById(R.id.new_color_panel);

        ((LinearLayout) mOldColor.getParent()).setPadding(Math.round(mColorPicker.getDrawingOffset()), 0, Math.round(mColorPicker.getDrawingOffset()), 0);

        mColorPicker.setOnColorChangedListener(this);

        mOldColor.setColor(color);
        mColorPicker.setColor(color, true);

    }

    @Override
    public void onColorChanged(int color) {

        mNewColor.setColor(color);

        if (mListener != null) {
            mListener.onColorChanged(color);
        }

    }

    public void setAlphaSliderVisible(boolean visible) {
        mColorPicker.setAlphaSliderVisible(visible);
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

}
