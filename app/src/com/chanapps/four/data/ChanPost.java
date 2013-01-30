package com.chanapps.four.data;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

import com.chanapps.four.activity.R;

public class ChanPost {

	public static final String TAG = ChanPost.class.getSimpleName();
    public static final int MAX_BOARDTHREAD_IMAGETEXT_LEN = 75;
    public static final int MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN = MAX_BOARDTHREAD_IMAGETEXT_LEN - 3;
    public static final int MAX_THREAD_IMAGETEXT_LEN = (int)(MAX_BOARDTHREAD_IMAGETEXT_LEN * 1.5);
    public static final int MAX_THREAD_IMAGETEXT_ABBR_LEN = MAX_THREAD_IMAGETEXT_LEN - 3;

    /* TWO COL SIZES */
    public static final int MAX_IMAGETEXT_LEN = MAX_BOARDTHREAD_IMAGETEXT_LEN;
    public static final int MAX_IMAGETEXT_ABBR_LEN = MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN;
    public static final int MAX_TEXTONLY_LEN = (int)(MAX_BOARDTHREAD_IMAGETEXT_LEN * 3.5);
    public static final int MAX_TEXTONLY_ABBR_LEN = MAX_TEXTONLY_LEN - 3;
    public static final int MAX_THREAD_TEXTONLY_LEN = (int)(MAX_TEXTONLY_LEN * 1.5);
    public static final int MAX_THREAD_TEXTONLY_ABBR_LEN = MAX_THREAD_TEXTONLY_LEN - 3;

    private static final int MIN_LINE = 30;
    private static final int MAX_LINE = 40;

    public String board;
    public long no = -1;
    public int sticky = 0;
    public int closed = 0;
    public String now;
    public String trip;
    public String id;
    public String capcode;
    public String country;
    public String country_name;
    public String email;
    public Date created;
    public long time = -1;
    public String name;
    public String sub;
    public String com;
    public long tim = 0;
    public String filename;
    public String ext;
    public int w = 0;
    public int h = 0;
    public int tn_w = 0;
    public int tn_h = 0;
    public int fsize = -1;
    public long resto = -1;
    public int replies = -1;
    public int images = -1;
    public int omitted_posts = -1;
    public int omitted_images = -1;
    public int bumplimit = 0;
    public int imagelimit = 0;
    public int spoiler = 0;
    public boolean isDead = false;
    public boolean defData = false;

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

    public String getFullText(boolean hideAllText, boolean hidePostNumbers) {
        if (hideAllText)
            return "";
        String text = sub != null && sub.trim().length() > 0
                  ? sub + (com != null && com.trim().length() > 0 ? "<br/>" + com : "")
                  : com;
        return sanitizeText(text, hidePostNumbers);
    }

    private static final String sanitizeText(String text, boolean hidePostNumbers) {
        if (text == null || text.isEmpty())
            return "";

        long start = System.currentTimeMillis();

        if (hidePostNumbers)
            text = text.replaceAll("<a[^>]*class=\"quotelink\">[^<]*</a>", "");
        else
            text = text.replaceAll("<a[^>]*class=\"quotelink\">([^<]*)</a>", "$1");

        text = text
                .replaceAll("<span[^>]*class=\"abbr\"[^>]*>.*</span>", "")    // exif reference
                .replaceAll("<table[^>]*class=\"exif\"[^>]*>.*</table>", "")  // exif info
                .replaceAll("<s>[^<]*</s>", "");                         // spoiler text
        text = textViewFilter(text);

        long end = System.currentTimeMillis();
        Log.v(TAG, "Regexp: " + (end - start) + "ms");

        return text;
    }

    public String getExifText() {
        return exifText(com);
    }

    private static final String exifText(String text) {
        if (text == null || text.isEmpty())
            return null;
        Pattern p = Pattern.compile(".*<table[^>]*class=\"exif\"[^>]*>(.*)</table>.*");
        Matcher m = p.matcher(text);
        if (!m.matches())
            return null;
        String g = m.group(1);
        if (g == null || g.isEmpty())
            return null;
        String s = g.replaceAll("<tr[^>]*><td colspan=\"2\"[^>]*><b>([^<]*)</b></td></tr>", "$1\n");
        String t = s.replaceAll("<tr[^>]*><td[^>]*>([^<]*)</td><td[^>]*>([^<]*)</td></tr>", "$1: $2\n");
        return textViewFilter(t);
    }

    public String getSpoilerText() {
        return spoilerText(com);
    }

    private static final String spoilerText(String text) {
        if (text == null || text.isEmpty())
            return null;
        Pattern p = Pattern.compile("<s>([^<]*)</s>");
        Matcher m = p.matcher(text);
        int start = 0;
        String s = "";
        while (m.find(start)) {
            String g = m.group(1);
            if (g != null && !g.isEmpty())
                s += (s.isEmpty() ? "" : "\n") + g;
            start = m.end();
        }
        if (s.isEmpty())
            return null;
        return "SPOILER:\n" + textViewFilter(s);
    }

    private static final String textViewFilter(String s) {
        return s.replaceAll("<br */?>", "\n")
                .replaceAll("\n\n\n+", "\n\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#0*39;", "'")
                .replaceAll("&#0*44;", ",")
                .replaceAll("&#[0-9abcdef]*;", "")
                .trim();
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
        if (s == null || s.isEmpty())
            return "";
        return
            (s.length() <= maxLen)
            ? s
            : s.substring(0, maxAbbrLen).replaceAll("\\s+", " ").replaceFirst("\\s+\\S+$", "")
                    + "..."
                    + (longtext && clickForMore != null ? "\n" + clickForMore : "");
    }

    public String toString() {
		return "Post " + no + " " + com + ", thumb: " + getThumbnailUrl() + " tn_w: " + tn_w + " tn_h: " + tn_h;
	}


   	public String getThumbnailUrl() {
   		if (tim > 0) {
   			return "http://0.thumbs.4chan.org/" + board + "/thumb/" + tim + "s.jpg";
   		} else if (tim == 0) {
   			return null;
   		} else {
   			// default board image should be returned
   			return null;
   		}
    }

   	public String getImageUrl() {
   		if (tim != 0) {
   			return "http://images.4chan.org/" + board + "/src/" + tim + ext;
   		}
   		return null;
   	}
   	
   	public String getImageName() {
   		return no + ext;
   	}

    public String getCountryFlagUrl() {
        if (country != null && !country.isEmpty())
            return getCountryFlagUrl(board, country);
        else
            return null;
    }

    public String getCountryFlagUrl(String boardCode, String countryCode) {
        return "http://static.4chan.org/image/country/"
                + (boardCode.equals("pol") ? "troll/" : "")
                + countryCode.toLowerCase()
                + ".gif";
    }

    public String getHeaderText() {
        return getHeaderText(false);
    }

    public String getHeaderText(boolean useFriendlyIds) {
        return "No: " + no
                + (resto > 0 ? "\nReply To: " + resto : "")
                + (sticky > 0 ? "\nSticky" : "")
                + (closed > 0 ? "\nClosed" : "")
                + (name != null && !name.isEmpty() && !name.equalsIgnoreCase("anonymous") ? "\nName: " + name : "")
                + (trip != null && !trip.isEmpty() ? "\nTripcode: " + trip : "")
                + (id != null && !id.isEmpty() ? "\nId: " + formatId(id, useFriendlyIds) : "")
                + (email != null && !email.isEmpty() ? "\nEmail: " + email : "")
                + (country_name != null && !country_name.isEmpty() ? "\nCountry: " + country_name : "")
                + "\n" + (new Date(time)).toString();
    }

    private static final String SAGE_POST_ID = "Heaven";
    private static final String PATTERN_POST_ID = "(\\d)";
    public String formatId(String id, boolean useFriendlyIds) {
        if (!useFriendlyIds)
            return id;
        if (id.equalsIgnoreCase(SAGE_POST_ID))
            return id;
        Pattern pattern = Pattern.compile(PATTERN_POST_ID);
        return id;

    }

    public String getPostText(boolean hideAllText, boolean hidePostNumbers) {
    	if (defData) {
            return "Loading images..."; // FIXME: should be loading graphic or localized text
    	}

        int maxImageTextLen = fsize > 0 ? MAX_IMAGETEXT_LEN : MAX_TEXTONLY_LEN;
        int maxImageTextAbbrLen = fsize > 0 ? MAX_IMAGETEXT_ABBR_LEN : MAX_TEXTONLY_ABBR_LEN;
        String text = "";
        if (!hideAllText) {
            if (!hidePostNumbers)
                text += no;
            String textLine = abbreviate(getFullText(hideAllText, hidePostNumbers), maxImageTextLen, maxImageTextAbbrLen);
            if (textLine != null && !textLine.isEmpty()) {
                if (text != null && !text.isEmpty())
                    text += "\n";
                text += textLine;
            }
        }
        if (fsize > 0) {
            if (text.length() > 0) {
				text += "\n";
			}
			int kbSize = (fsize / 1024) + 1;
			text += kbSize + "kB " + w + "x" + h; // + " " + ext;
		}
        return text;
	}

    public String getThreadText(boolean hideAllText, boolean hidePostNumbers) {
        if (fsize > 0) // has image
            return getThreadText(hideAllText, hidePostNumbers, MAX_THREAD_IMAGETEXT_LEN, MAX_THREAD_IMAGETEXT_ABBR_LEN, false);
        else
            return getThreadText(hideAllText, hidePostNumbers, MAX_THREAD_TEXTONLY_LEN, MAX_THREAD_TEXTONLY_ABBR_LEN, false);
    }

    public String getThreadText(boolean hideAllText, boolean hidePostNumbers, int maxImageTextLen, int maxImageTextAbbrLen, boolean onBoard) {
    	if (defData)
    		return "Loading..."; // FIXME should be localized string

        String text = "";
        if (!hideAllText) {
            if (onBoard) {
                text += abbreviate(getFullText(hideAllText, hidePostNumbers), maxImageTextLen, maxImageTextAbbrLen);
            }
            else {
                if (!hidePostNumbers)
                    text += no;
                String subText = abbreviate(sanitizeText(sub, hidePostNumbers), maxImageTextLen, maxImageTextAbbrLen);
                String comText = abbreviate(sanitizeText(com, hidePostNumbers), maxImageTextLen, maxImageTextAbbrLen);
                if (subText != null && !subText.isEmpty()) {
                    if (text != null && !text.isEmpty())
                        text += " ";
                    text += subText;
                }
                if (comText != null && !comText.isEmpty()) {
                    if (text != null && !text.isEmpty())
                        text += "\n";
                    text += comText;
                }
            }
        }
        if (resto != 0) { // just a post, don't add thread stuff
            if (fsize > 0 && !onBoard) {
                int kbSize = (fsize / 1024) + 1;
                text += "\n" + kbSize + "kB " + w + "x" + h; // + " " + ext;
            }
        }
        else {
            if (text.length() > 0)
                text += "\n";
            text += replies
                    + " post" + (replies == 1 ? "" : "s")
                    + " "
                    + images
                    + " image"
                    + (images == 1 ? "" : "s");
            if (imagelimit == 1)
                text += " (IL)";
            if (bumplimit == 1)
                text += " (BL)";
            if (isDead) {
                if (onBoard)
                    text += "\n";
                else
                    text += " ";
                text += "DEAD THREAD";
            }
            if (fsize > 0 && !onBoard) {
                int kbSize = (fsize / 1024) + 1;
                text += " " + kbSize + "kB " + w + "x" + h; // + " " + ext;
            }
        }
        return text;
	}

    public String getBoardThreadText(boolean hideAllText, boolean hidePostNumbers) {
        if (fsize > 0) // has image
            return getThreadText(hideAllText, hidePostNumbers, MAX_BOARDTHREAD_IMAGETEXT_LEN, MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN, true);
        else // text-only
            return getThreadText(hideAllText, hidePostNumbers, MAX_TEXTONLY_LEN, MAX_TEXTONLY_ABBR_LEN, true);
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

    public boolean refersTo(long postNo) {
        if (postNo <= 0 || com == null || com.isEmpty())
            return false;
        boolean matches = com.indexOf("#p" + postNo + "\"") >= 0;
        Log.i(TAG, "Matching postNo=" + postNo + " is " + matches + " against com=" + com);
        return matches;
    }
    
}
