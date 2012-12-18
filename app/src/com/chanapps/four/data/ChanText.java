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

    private static final int MIN_LINE = 30;
    private static final int MAX_LINE = 40;

    public static final String quoteText(String s) {
        if (s == null || s.isEmpty())
            return "";
        String o = "> ";
        int l = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                o += "\n> ";
                l = 2;
            }
            else if (l < MIN_LINE) {
                o += c;
                l++;
            }
            else if (l > MAX_LINE) {
                o += "\n> " + c;
                l = 3;
            }
            else if (c == ' ') {
                o += "\n> ";
                l = 2;
            }
            else {
                o+= c;
                l++;
            }
        }
        return o;
    }

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
                .replaceAll("\\s+", " ")
                .replaceAll("(\\s*\\n)+", "\n")
                .trim()
            : "";
        long end = System.currentTimeMillis();
        Log.v(TAG, "Regexp: " + (end - start) + "ms");
        return foo;
    }
}
