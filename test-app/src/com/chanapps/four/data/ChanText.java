package com.chanapps.four.data;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 10/13/12
 * Time: 2:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanText {
    public static final String sanitizeText(String text) {
        return
            (text != null && text.trim().length() > 0)
            ? text
                .replaceAll("<a[^>]*class=\"quotelink\">[^<]*</a>", "")
                .replaceAll("<br */?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll(" +", " ")
                .trim()
            : "";
    }
}
