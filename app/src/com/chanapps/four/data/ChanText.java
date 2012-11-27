package com.chanapps.four.data;

import android.nfc.Tag;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 10/13/12
 * Time: 2:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanText {
    private static final String TAG = ChanText.class.getSimpleName();

    public static final String getText(String sub, String com) {
        String text = sub != null && sub.trim().length() > 0
                  ? sub + (com != null && com.trim().length() > 0 ? "<br/>" + com : "")
                  : com;
        return sanitizeText(text);
    }

    private static final String sanitizeText(String text) {
        long start = System.currentTimeMillis();
        String foo =
            (text != null && text.length() > 0)
            ? text
                .replaceAll("<a[^>]*class=\"quotelink\">[^<]*</a>", "")
                .replaceAll("<br */?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#44;", ",")
                .replaceAll("&#[0-9abcdef]*;", "")
                .replaceAll(" +", " ")
                .trim()
            : "";
        long end = System.currentTimeMillis();
        Log.v(TAG, "Regexp: " + (end - start) + "ms");
        return foo;
    }
}
