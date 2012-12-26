package com.chanapps.four.data;

import android.content.Context;
import android.util.Log;
import com.chanapps.four.activity.R;
import com.chanapps.four.loader.BoardCursorLoader;

import java.util.*;

public class ChanPost {

	public static final String TAG = ChanPost.class.getSimpleName();
    public static final int MAX_BOARDTHREAD_IMAGETEXT_LEN = 78;
    public static final int MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN = 75;

    // THREE COL SIZES
    public static final int MAX_IMAGETEXT_LEN = 33;
    public static final int MAX_IMAGETEXT_ABBR_LEN = 30;
    public static final int MAX_TEXTONLY_LEN = 93;
    public static final int MAX_TEXTONLY_ABBR_LEN = 90;

    /* TWO COL SIZES
    public static final int MAX_IMAGETEXT_LEN = 78;
    public static final int MAX_IMAGETEXT_ABBR_LEN = 75;
    public static final int MAX_TEXTONLY_LEN = 303;
    public static final int MAX_TEXTONLY_ABBR_LEN = 300;
    */

    private static final int MIN_LINE = 30;
    private static final int MAX_LINE = 40;

    public String board;
    public long no = -1;
    public int sticky = 0;
    public String now;
    public Date created;
    public long time = -1;
    public String name;
    public String sub;
    public String com;
    public String tim;
    public String filename;
    public String ext;
    public int w = 0;
    public int h = 0;
    public int tn_w = 0;
    public int tn_h = 0;
    public int fsize = -1;
    public int resto = -1;
    public int replies = -1;
    public int images = -1;
    public int omitted_posts = -1;
    public int omitted_images = -1;
    public int bumplimit = 0;
    public int imagelimit = 0;
    public boolean isDead = false;

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

    public String getFullText() {
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
                .trim()
            : "";
        long end = System.currentTimeMillis();
        Log.v(TAG, "Regexp: " + (end - start) + "ms");
        return foo;
    }

    private static final String collapseNewlines(String s) {
        return s.replaceAll("(\\s*\\n)+", "\n");
    }

    private static String clickForMore = null;

    public static void initClickForMore(Context c) {
        clickForMore = c.getString(R.string.board_click_for_more);
    }

    private static String abbreviate(String s, int maxLen, int maxAbbrLen) {
        return abbreviate(s, maxLen, maxAbbrLen, false);
    }

    private static String abbreviate(String s, int maxLen, int maxAbbrLen, boolean longtext) {
        return
            (s.length() <= maxLen)
            ? s
            : s.substring(0, maxAbbrLen).replaceAll("\\s+", " ").replaceFirst("\\s+\\S+$", "")
                    + "..."
                    + (longtext && clickForMore != null ? "\n" + clickForMore : "");
    }

    public String toString() {
		return "Thread " + no + " " + com + ", thumb: " + getThumbnailUrl() + " tn_w: " + tn_w + " tn_h: " + tn_h;
	}


   	public String getThumbnailUrl() {
        return tim != null
   			? "http://0.thumbs.4chan.org/" + board + "/thumb/" + tim + "s.jpg"
            : null;
    }

   	public String getImageUrl() {
   		if (tim != null) {
   			return "http://images.4chan.org/" + board + "/src/" + tim + ext;
   		}
   		return null;
   	}

    public String getPostText(boolean hideAllText) {
        String text = hideAllText ? "" : getFullText();
		if (fsize > 0) {
            text = abbreviate(text, MAX_IMAGETEXT_LEN, MAX_IMAGETEXT_ABBR_LEN);
            if (text.length() > 0) {
				text += "\n";
			}
			int kbSize = (fsize / 1024) + 1;
			text += kbSize + "kB " + w + "x" + h; // + " " + ext;
		}
        else {
            text = abbreviate(text, MAX_TEXTONLY_LEN, MAX_TEXTONLY_ABBR_LEN, true);
        }
        return text;
	}

    public String getPostText() {
        return getPostText(false);
    }

    public String getThreadText() {
        return getThreadText(false);
    }

    public String getThreadText(boolean hideAllText) {
        return getThreadText(hideAllText, MAX_IMAGETEXT_LEN, MAX_IMAGETEXT_ABBR_LEN);
    }

    public String getThreadText(boolean hideAllText, int maxImageTextLen, int maxImageTextAbbrLen) {
        if (hideAllText) {
            return "";
        }
        String text = abbreviate(getFullText(), maxImageTextLen, maxImageTextAbbrLen);
        if (resto != 0) { // just a post, don't add thread stuff
            return text;
        }
        if (text.length() > 0) {
            text += "\n";
        }
        if (isDead) {
            text += "DEAD THREAD";
        }
        else {
            text += replies
                    + " post" + (replies == 1 ? "" : "s")
                    + " "
                    + images
                    + " image"
                    + (images == 1 ? "" : "s");
            if (imagelimit == 1) {
                text += " (IL)";
            }
            if (bumplimit == 1) {
                text += " (BL)";
            }
        }
		return text;
	}

    public String getBoardThreadText() {
        return getThreadText(false, MAX_BOARDTHREAD_IMAGETEXT_LEN, MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN);
    }

    public void mergeIntoThreadList(List<ChanPost> threads) {
        boolean exists = false;
        for (ChanPost existingThread : threads) {
            if (this.no == existingThread.no) {
                exists = true;
                existingThread.copyThreadStatusFields(this);
                break;
            }
        }
        if (!exists) {
            threads.add(this);
        }
    }

    public void copyThreadStatusFields(ChanPost from) {
        bumplimit = from.bumplimit;
        imagelimit = from.imagelimit;
        images = from.images;
        omitted_images = from.omitted_images;
        omitted_posts = from.omitted_posts;
        replies = from.replies;
    }

}
