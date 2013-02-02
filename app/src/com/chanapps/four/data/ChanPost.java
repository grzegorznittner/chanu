package com.chanapps.four.data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

import com.chanapps.four.activity.R;

public class ChanPost {

	public static final String TAG = ChanPost.class.getSimpleName();
    private static final boolean DEBUG = false;
    
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

    // settings from prefs
    public boolean hideAllText = false;
    public boolean hidePostNumbers = true;
    public boolean useFriendlyIds = true;

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
        if (hideAllText)
            return "";
        String text = sub != null && sub.trim().length() > 0
                  ? sub + (com != null && com.trim().length() > 0 ? "<br/>" + com : "")
                  : com;
        return sanitizeText(text);
    }

    private String sanitizeText(String text) {
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
        return getHeaderText(true);
    }

    public String getHeaderText(boolean useFriendlyIds) {
        return "No: " + no
                + (resto > 0 ? "\nReply To: " + resto : "")
                + (sticky > 0 ? "\nSticky" : "")
                + (closed > 0 ? "\nClosed" : "")
                + (name != null && !name.isEmpty() && !name.equalsIgnoreCase("anonymous") ? "\nName: " + name : "")
                + (trip != null && !trip.isEmpty() ? "\nTripcode: " + trip : "")
                + (id != null && !id.isEmpty() ? "\nId: " + getUserId() : "")
                + (email != null && !email.isEmpty() ? "\nEmail: " + email : "")
                + (country_name != null && !country_name.isEmpty() ? "\nCountry: " + country_name : "")
                + "\n" + (new Date(time)).toString();
    }

    public String getPostText() {
        int maxImageTextLen = fsize > 0 ? MAX_IMAGETEXT_LEN : MAX_TEXTONLY_LEN;
        int maxImageTextAbbrLen = fsize > 0 ? MAX_IMAGETEXT_ABBR_LEN : MAX_TEXTONLY_ABBR_LEN;
        String text = "";
        if (!hideAllText) {
            if (!hidePostNumbers)
                text += "<b>No: " + no + "</b>";
            if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("anonymous"))
                text += (text.isEmpty() ? "" : "<br/>\n") + "<b>Name: " + name + "</b>";
            if (id != null && !id.isEmpty())
                text += (text.isEmpty() ? "" : "<br/>\n") + "<b>Id: " + getUserId() + "</b>";
            String textLine = abbreviate(getFullText(), maxImageTextLen, maxImageTextAbbrLen);
            if (textLine != null && !textLine.isEmpty())
                text += (text.isEmpty() ? "" : "<br/>\n") + textLine;
        }
        if (fsize > 0) {
			int kbSize = (fsize / 1024) + 1;
			text += (text.isEmpty() ? "" : "<br/>\n") + kbSize + "kB " + w + "x" + h; // + " " + ext;
		}
        return text;
	}

    public String getThreadText() {
        if (fsize > 0) // has image
            return getThreadText(MAX_THREAD_IMAGETEXT_LEN, MAX_THREAD_IMAGETEXT_ABBR_LEN, false);
        else
            return getThreadText(MAX_THREAD_TEXTONLY_LEN, MAX_THREAD_TEXTONLY_ABBR_LEN, false);
    }

    public String getThreadText(int maxImageTextLen, int maxImageTextAbbrLen, boolean onBoard) {
        String text = "";
        if (!hideAllText) {
            if (!hidePostNumbers)
                text += "<b>No: " + no + "</b>";
            if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("anonymous"))
                text += (text.isEmpty() ? "" : "<br/>\n") + "<b>Name: " + name + "</b>";
            if (id != null && !id.isEmpty())
                text += (text.isEmpty() ? "" : "<br/>\n") + "<b>Id: " + getUserId() + "</b>";
            String subText = abbreviate(sanitizeText(sub), maxImageTextLen, maxImageTextAbbrLen);
            String comText = abbreviate(sanitizeText(com), maxImageTextLen, maxImageTextAbbrLen);
            if (subText != null && !subText.isEmpty())
                text += (text.isEmpty() ? "" : "<br/>\n") + subText;
            if (comText != null && !comText.isEmpty())
                text += (text.isEmpty() ? "" : "<br/>\n") + comText;
        }
        if (resto != 0) { // just a post, don't add thread stuff
            if (fsize > 0 && !onBoard) {
                int kbSize = (fsize / 1024) + 1;
                text += (text.isEmpty() ? "" : "<br/>\n") + kbSize + "kB " + w + "x" + h; // + " " + ext;
            }
        }
        else {
            text += (text.isEmpty() ? "" : "<br/>\n")
                    + replies
                    + " post" + (replies == 1 ? "" : "s")
                    + " "
                    + images
                    + " image"
                    + (images == 1 ? "" : "s");
            if (imagelimit == 1)
                text += " (IL)";
            if (bumplimit == 1)
                text += " (BL)";
            if (isDead)
                text += (!text.isEmpty() ? "" : (onBoard ? "<br/>" : " ")) + "DEAD THREAD";
            if (fsize > 0 && !onBoard) {
                int kbSize = (fsize / 1024) + 1;
                text += " " + kbSize + "kB " + w + "x" + h; // + " " + ext;
            }
        }
        return text;
	}

    public String getBoardThreadText() {
        if (fsize > 0) // has image
            return getThreadText(MAX_BOARDTHREAD_IMAGETEXT_LEN, MAX_BOARDTHREAD_IMAGETEXT_ABBR_LEN, true);
        else // text-only
            return getThreadText(MAX_TEXTONLY_LEN, MAX_TEXTONLY_ABBR_LEN, true);
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
        if (DEBUG) Log.i(TAG, "Matching postNo=" + postNo + " is " + matches + " against com=" + com);
        return matches;
    }

    private static final String SAGE_POST_ID = "Heaven";
    private static final String[] NAMES = {
            "Aries",
            "Bian",
            "Chikage",
            "Dragon",
            "Eki",
            "Fidel",
            "Goku",
            "Hotaru",
            "Ideki",
            "Judo",
            "Kendo",
            "Lima",
            "Moto",
            "Noko",
            "Oni",
            "Piku",
            "Queen",
            "Radium",
            "Sensei",
            "Totoro",
            "Usagi",
            "Virgo",
            "Waka",
            "Xi",
            "Yoto",
            "Zulu",

            "Akira",
            "Balrog",
            "Chidori",
            "Diva",
            "Endo",
            "Fap",
            "Godo",
            "Hero",
            "Ichigo",
            "Joro",
            "Kai",
            "Li",
            "Mini",
            "Naruto",
            "Opa",
            "Pei",
            "Quest",
            "Rune",
            "Shura",
            "Tetsuo",
            "Unit",
            "Victor",
            "Wiki",
            "Xenu",
            "Yolo",
            "Zolan",

            "One",
            "Two",
            "Three",
            "Four",
            "Five",
            "Six",
            "Seven",
            "Eight",
            "Nine",
            "Ten",

            "Plus",
            "Slash"

    };
    private static final String[] NAMES_2 = {
            "Arctic",
            "Brain",
            "Chimp",
            "Duck",
            "Elf",
            "Frog",
            "Gimp",
            "Hippy",
            "Imp",
            "Jumper",
            "Kitchen",
            "Lamp",
            "Mittens",
            "Night",
            "Owl",
            "Phantom",
            "Quack",
            "Rocket",
            "Storm",
            "Thunder",
            "Urchin",
            "Vampire",
            "Whale",
            "Xerxes",
            "Yuppie",
            "Zebra",

            "Ape",
            "Banana",
            "Crown",
            "Dread",
            "Eel",
            "Factor",
            "General",
            "Hound",
            "Ink",
            "Jack",
            "Killer",
            "Loader",
            "Master",
            "Nasty",
            "Onion",
            "Paste",
            "Quitter",
            "Rim",
            "Stampede",
            "Tent",
            "Unicorn",
            "Vox",
            "War",
            "Xtender",
            "Yogi",
            "Zoo",

            "Ten",
            "Twenty",
            "Thirty",
            "Fourty",
            "Fifty",
            "Sixty",
            "Seventy",
            "Eighty",
            "Ninety",
            "Hundred",

            "Minus",
            "Dot"

    };
    private static final String BASE_64_CODE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "+/";
    private static final Map<Character, String> nameMap = new HashMap<Character, String>();
    private static final Map<Character, String> nameMap2 = new HashMap<Character, String>();

    private static void initNameMap() {
        for (int i = 0; i < NAMES.length; i++) {
            String s = NAMES[i];
            char c = BASE_64_CODE.charAt(i);
            if (DEBUG) Log.i(TAG, "Putting into map " + c + ", " + s);
            nameMap.put(c, s);
        }
        for (int i = 0; i < NAMES_2.length; i++) {
            String s = NAMES_2[i];
            char c = BASE_64_CODE.charAt(i);
            if (DEBUG) Log.i(TAG, "Putting into map2 " + c + ", " + s);
            nameMap2.put(c, s);
        }
    }

    public String getUserId() {
        if (id == null)
            return "";
        else
            return getUserId(id, useFriendlyIds);
    }

    public static String getUserId(String id, boolean useFriendlyIds) {
        if (!useFriendlyIds)
            return id;
        if (id.equalsIgnoreCase(SAGE_POST_ID))
            return id;
        if (id.equalsIgnoreCase("Admin") || id.equalsIgnoreCase("Mod") || id.equalsIgnoreCase("Developer"))
            return id;
        if (DEBUG) Log.d(TAG, "Initial: " + id);

        synchronized (nameMap) {
            if (nameMap.isEmpty()) {
                initNameMap();
            }
        }

        String newId = nameMap.get(id.charAt(0)) + nameMap2.get(id.charAt(1)) + "." + id.substring(2);
        if (DEBUG) Log.i(TAG, "Final: " + newId);
        return newId;
    }

}
