package com.chanapps.four.data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import com.chanapps.four.activity.R;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

public class ChanPost {

	public static final String TAG = ChanPost.class.getSimpleName();
    private static final boolean DEBUG = false;
    
    public static final int MAX_SINGLELINE_TEXT_LEN = 20;
    public static final int MAX_DOUBLELINE_TEXT_LEN = 40;
    private static final int MIN_LINE = 30;
    private static final int MAX_LINE = 40;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String board;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingLongDeserializer.class)
    public long no = -1;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int sticky = 0;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int closed = 0;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String now;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String trip;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String id;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String capcode;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String country;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String country_name;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String email;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingDateDeserializer.class)
    public Date created;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingLongDeserializer.class)
    public long time = -1;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String name;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String sub;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String com;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingLongDeserializer.class)
    public long tim = 0;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String filename;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingStringDeserializer.class)
    public String ext;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int w = 0;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int h = 0;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int tn_w = 0;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int tn_h = 0;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int fsize = -1;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingLongDeserializer.class)
    public long resto = -1;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int replies = -1;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int images = -1;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int omitted_posts = -1;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int omitted_images = -1;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int bumplimit = 0;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int imagelimit = 0;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int spoiler = 0;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingBooleanDeserializer.class)
    public boolean isDead = false;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingBooleanDeserializer.class)
    public boolean defData = false;

    // settings from prefs
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingBooleanDeserializer.class)
    public boolean hideAllText = false; // no longer used
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingBooleanDeserializer.class)
    public boolean hidePostNumbers = true;
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingBooleanDeserializer.class)
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
        String text = "";
        String subText = sanitizeText(sub);
        if (subText != null && !subText.isEmpty())
            text += "<b>Subject: " + subText + "</b>";
        String comText = com != null && com.trim().length() > 0 ? sanitizeText(com) : "";
        if (comText != null && !comText.isEmpty())
            text += (text.isEmpty() ? "" : "<br/>\n") + comText;
        return text;
    }

    private String sanitizeText(String text) {
        return sanitizeText(text, false);
    }

    private String sanitizeText(String text, boolean collapseNewlines) {
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
                .replaceAll("<s>[^<]*</s>", "SPOILER");                       // spoiler text
        text = textViewFilter(text, collapseNewlines);

        long end = System.currentTimeMillis();
        if (DEBUG) Log.v(TAG, "Regexp: " + (end - start) + "ms");

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
        return textViewFilter(s, false);
    }

    private static final String textViewFilter(String s, boolean collapseNewlines) {
        String t = s
                .replaceAll("<br */?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#0*39;", "'")
                .replaceAll("&#0*44;", ",")
                .replaceAll("&#[0-9abcdef]*;", "")
                .replaceFirst("^\n+", "")
                .replaceFirst("\n+$", "");
        if (collapseNewlines)
            t = t.replaceAll("\n+", " ");
        else
            t = t.replaceAll("\n", "<br/>");
        return t.trim();
    }

    private static final String collapseNewlines(String s) {
        return s.replaceAll("(\\s*\\n)+", "\n");
    }

    private static String clickForMore = null;

    public static void initClickForMore(Context c) {
        clickForMore = c.getString(R.string.board_click_for_more);
    }

    public static String abbreviate(String s, int maxLen) {
        return abbreviate(s, maxLen, maxLen - 3, false);
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
            : s.substring(0, maxLen)
                    .replaceAll("\\s+", " ")
                    .replaceFirst("\\s+\\S+$", "")
                    .replaceFirst("\\s+$", "")
                    + (longtext && clickForMore != null ? "\n" + clickForMore : "");
/*
            : s.substring(0, maxAbbrLen)
                    .replaceAll("\\s+", " ")
                    .replaceFirst("\\s+\\S+$", "")
                    .replaceFirst("\\s+$", "")
                    + "..."
                    + (longtext && clickForMore != null ? "\n" + clickForMore : "");
*/
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

    public String getTimeString(long seconds) {
        long milliseconds = 1000 * seconds;
        Date d = new Date(milliseconds);
        return d.toString();
    }

    public String getDateText() {
        long milliseconds = 1000 * time; // time in seconds, convert
        return (time > 0)
            ? DateUtils.getRelativeTimeSpanString(milliseconds, (new Date()).getTime(), 0, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
            : "";
    }

    public String getImageDimensions() {
        if (fsize > 0) {
            int kbSize = (fsize / 1024) + 1;
            String size = (kbSize > 1000) ? (kbSize / 1000) + "MB" : kbSize + "KB";
            return w + "x" + h + " " + size;
        }
        return "";
    }

    public String getUserHeaderText() {
        String text = "";
        if (!hidePostNumbers)
            text += "No: " + no + "";
        if (id != null && !id.isEmpty()) {
            text += (text.isEmpty() ? "" : "\n") + "Id: " + getUserId() + "";
            if (id.equalsIgnoreCase("admin") && name != null && !name.isEmpty())
                text += " - " + name;
        }
        if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("anonymous"))
            text += (text.isEmpty() ? "" : "\n") + "Name: " + name + "";
        if (trip != null && !trip.isEmpty())
            text += (text.isEmpty() ? "" : "\n") + "Tripcode: " + trip + "";
        if (email != null && !email.isEmpty())
            text += (text.isEmpty() ? "" : "\n") + "Email: " + email + "";
        if (country_name != null && !country_name.isEmpty())
            text += (text.isEmpty() ? "" : "\n") + "Country: " + country_name + "";
        return text;
	}

    public String getThreadNotificationText() {
        String text = "";
        text += getUserHeaderText();
        if (sticky > 0 && replies == 0) {
            text += (text.isEmpty() ? "" : "<br/>\n") + "STICKY";
        }
        else {
            if (replies > 0) {
                text += (text.isEmpty() ? "" : "<br/>\n")
                        + replies
                        + " post" + (replies == 1 ? "" : "s")
                        + " "
                        + (images > 0 ? images : "no")
                        + " img"
                        + (images == 1 ? "" : "s");
            }
            else {
                text += (text.isEmpty() ? "" : "<br/>\n") + "no replies";
            }
            if (imagelimit == 1)
                text += " (IL)";
            if (bumplimit == 1)
                text += " (BL)";
            if (isDead)
                text += (text.isEmpty() ? "" : " ") + "DEAD";
            if (sticky > 0)
                text += (text.isEmpty() ? "" : " ") + "STICKY";
            if (closed > 0)
                text += (text.isEmpty() ? "" : " ") + "CLOSED";
        }
        return text;
    }

    public String getBoardText() {
        if (resto != 0)
            return ""; // just a post
        String text = "";

        String subText = abbreviate(sanitizeText(sub, true), MAX_DOUBLELINE_TEXT_LEN);
        if (subText != null && !subText.isEmpty()) {
            text += "<b>" + subText + "</b>";
        }
        else {
            String comText = abbreviate(sanitizeText(com, true), MAX_DOUBLELINE_TEXT_LEN);
            if (comText != null && !comText.isEmpty())
                text += "<b>" + comText + "</b>";
        }

        if (sticky > 0 && replies == 0) { // special formatting
            text += "<br/>\nSTICKY";
        }
        else {
            if (replies > 0) {
                text += (text.isEmpty() ? "" : "<br/>\n")
                        + replies
                        + " post" + (replies == 1 ? "" : "s")
                        + " "
                        + (images > 0 ? images : "0")
                        + "i";
                        //+ (images == 1 ? "" : "s");
            }
            else {
                text += (text.isEmpty() ? "" : "<br/>\n") + "no replies";
            }
            if (imagelimit == 1)
                text += " IL";
            if (bumplimit == 1)
                text += " BL";
            if (sticky > 0)
                text += " S";
            if (closed > 0)
                text += " C";
            if (isDead)
                text += " D";
        }
        return text;
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
        closed = from.closed;
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

    public void clearImageInfo() {
        w = 0;
        h = 0;
        tn_w = 0;
        tn_h = 0;
        tim = 0;
        fsize = -1;
        filename = null;
        ext = null;
    }

}
