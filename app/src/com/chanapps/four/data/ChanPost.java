package com.chanapps.four.data;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.chanapps.four.activity.R;

public class ChanPost {

	public static final String TAG = ChanPost.class.getSimpleName();
    public static final int MAX_BOARDTHREAD_IMAGETEXT_LEN = 75;
    public static final int MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN = MAX_BOARDTHREAD_IMAGETEXT_LEN - 3;

    /* THREE COL SIZES
    public static final int MAX_IMAGETEXT_LEN = 33;
    public static final int MAX_IMAGETEXT_ABBR_LEN = 30;
    public static final int MAX_TEXTONLY_LEN = 93;
    public static final int MAX_TEXTONLY_ABBR_LEN = 90;
    */

    /* TWO COL SIZES */
    public static final int MAX_IMAGETEXT_LEN = MAX_BOARDTHREAD_IMAGETEXT_LEN;
    public static final int MAX_IMAGETEXT_ABBR_LEN = MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN;
    public static final int MAX_TEXTONLY_LEN = (int)(MAX_BOARDTHREAD_IMAGETEXT_LEN * 3.5);
    public static final int MAX_TEXTONLY_ABBR_LEN = MAX_TEXTONLY_LEN - 3;


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

    public String getFullText() {
        return getFullText(true);
    }

    public String getFullText(boolean withNumbers) {
        String text = sub != null && sub.trim().length() > 0
                  ? sub + (com != null && com.trim().length() > 0 ? "<br/>" + com : "")
                  : com;
        return sanitizeText(text, withNumbers);
    }

    private static final String sanitizeText(String text) {
        return sanitizeText(text, true);
    }

    private static final String sanitizeText(String text, boolean withNumbers) {
        if (text == null || text.isEmpty())
            return "";

        long start = System.currentTimeMillis();

        if (withNumbers)
            text = text.replaceAll("<a[^>]*class=\"quotelink\">([^<]*)</a>", "$1");
        else
            text = text.replaceAll("<a[^>]*class=\"quotelink\">[^<]*</a>", "");

        text = text
                .replaceAll("<br */?>", "\n")
                .replaceAll("\n\n\n+", "\n\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#44;", ",")
                .replaceAll("&#[0-9abcdef]*;", "")
                .trim();

        long end = System.currentTimeMillis();
        Log.v(TAG, "Regexp: " + (end - start) + "ms");

        return text;
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
        return "No: " + no
                + (resto > 0 ? "\nReply To: " + resto : "")
                + (sticky > 0 ? "\nSticky" : "")
                + (closed > 0 ? "\nClosed" : "")
                + (name != null && !name.isEmpty() && !name.equalsIgnoreCase("anonymous") ? "\nName: " + name : "")
                + (trip != null && !trip.isEmpty() ? "\nTripcode: " + trip : "")
                + (id != null && !id.isEmpty() ? "\nId: " + id : "")
                + (email != null && !email.isEmpty() ? "\nEmail: " + email : "")
                + (country_name != null && !country_name.isEmpty() ? "\nCountry: " + country_name : "")
                + "\n" + (new Date(time)).toString();
    }

    public String getPostText(boolean hideAllText) {
    	if (defData) {
    		return "We're preparing images for you.\n"
    				+ "Please wait.";
    	}

        int maxImageTextLen = fsize > 0 ? MAX_IMAGETEXT_LEN : MAX_TEXTONLY_LEN;
        int maxImageTextAbbrLen = fsize > 0 ? MAX_IMAGETEXT_ABBR_LEN : MAX_TEXTONLY_ABBR_LEN;
        String text = "";
        if (!hideAllText) {
            text += no + "\n" + abbreviate(getFullText(), maxImageTextLen, maxImageTextAbbrLen);
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

    public String getPostText() {
        return getPostText(false);
    }

    public String getThreadText() {
        return getThreadText(false);
    }

    public String getThreadText(boolean hideAllText) {
        return getThreadText(hideAllText, MAX_IMAGETEXT_LEN, MAX_IMAGETEXT_ABBR_LEN, false);
    }

    public String getThreadText(boolean hideAllText, int maxImageTextLen, int maxImageTextAbbrLen, boolean onBoard) {
    	if (defData) {
    		return "We're preparing images for you.\n"
    				+ "Please wait.";
    	}
    	
        String text = "";
        if (!hideAllText) {
            if (!onBoard)
                text += no + "\n";
            text += abbreviate(getFullText(!onBoard), maxImageTextLen, maxImageTextAbbrLen);
        }
        if (fsize > 0 && !onBoard) {
            if (text.length() > 0) {
                text += "\n";
            }
            int kbSize = (fsize / 1024) + 1;
            text += kbSize + "kB " + w + "x" + h; // + " " + ext;
        }
        if (resto != 0) { // just a post, don't add thread stuff
            return text;
        }
        if (text.length() > 0) {
            text += "\n";
        }
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
        if (isDead) {
            text += "\nDEAD THREAD";
        }
        return text;
	}

    public String getBoardThreadText() {
        return getThreadText(false, MAX_BOARDTHREAD_IMAGETEXT_LEN, MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN, true);
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
        //Pattern p = Pattern.compile("#p" + postNo + "\\");
        //Matcher m = p.matcher(com);
        Log.i(TAG, "Matching postNo=" + postNo + " is " + matches + " against com=" + com);
        //return m.matches();
        return matches;
    }
    
}
