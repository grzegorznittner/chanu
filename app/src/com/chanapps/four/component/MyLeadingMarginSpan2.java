package com.chanapps.four.component;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 10/19/12
 * Time: 5:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyLeadingMarginSpan2 implements LeadingMarginSpan.LeadingMarginSpan2 {
    private int margin;
    private int lines;

    MyLeadingMarginSpan2(int lines, int margin) {
        this.margin = margin;
        this.lines = lines;
    }

    /* Возвращает значение, на которе должен быть добавлен отступ */
    @Override
    public int getLeadingMargin(boolean first) {
        if (first) {
            /*
             * Данный отступ будет применен к количеству строк
             * возвращаемых getLeadingMarginLineCount()
             */
            return margin;
        } else {
            // Отступ для всех остальных строк
            return 0;
        }
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
            int top, int baseline, int bottom, CharSequence text,
            int start, int end, boolean first, Layout layout) {}

    /*
     * Возвращает количество строк, к которым должен быть
     * применен отступ возвращаемый методом getLeadingMargin(true)
     * Замечание:
     * Отступ применяется только к N строкам первого параграфа.
     */
    @Override
    public int getLeadingMarginLineCount() {
        return lines;
    }
}
