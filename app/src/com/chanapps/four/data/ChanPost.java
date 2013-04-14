package com.chanapps.four.data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.MatrixCursor;
import android.text.format.DateUtils;
import android.util.Log;

import com.chanapps.four.activity.R;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

public class ChanPost {

	public static final String TAG = ChanPost.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int MAX_HEADER_NAME_LEN = 15;
    public static final int MAX_SINGLELINE_TEXT_LEN = 20;
    private static final int MIN_LINE = 30;
    private static final int MAX_LINE = 40;

    private static final int CHAN_ID = 0x01;
    private static final int CHAN_NAME = 0x02;
    private static final int CHAN_TRIP = 0x04;
    private static final int CHAN_EMAIL = 0x08;
    private static final int CHAN_HEADER_SET = 0x10;

    private int headerComponents = 0;

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
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingIntegerDeserializer.class)
    public int filedeleted = 0;

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

    public static final String quoteText(String in) {
        if (in == null || in.isEmpty())
            return "";
        String s = in.replaceAll("<br/>", "\n");
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
        return o.replaceAll("> >", ">>").replaceAll("\n", "<br/>");
    }

    private String missingHeaderLines() {
        List<String> lines = new ArrayList<String>();
        if ((headerComponents & CHAN_HEADER_SET) == 0)
            headerLine(); // side effect sets headerComponents
        if ((headerComponents & CHAN_ID) == 0 && id != null && !id.isEmpty() && !id.equalsIgnoreCase("heaven"))
            lines.add(formattedUserId());
        if ((headerComponents & CHAN_NAME) == 0 && name != null && !name.isEmpty() && !name.equalsIgnoreCase("anonymous"))
            lines.add(name);
        if ((headerComponents & CHAN_TRIP) == 0 && trip != null && !trip.isEmpty())
            lines.add(formattedUserTrip());
        if ((headerComponents & CHAN_EMAIL) == 0 && email != null && !email.isEmpty())
            lines.add(email.equalsIgnoreCase("sage") ? "sage" : email);
        if (country_name != null && !country_name.isEmpty())
            lines.add(country_name);
        return ChanHelper.join(lines, "<br/>\n");
    }

    public String fullText() {
        return fullText(false);
    }

    public String fullText(boolean showSpoiler) {
        List<String> lines = new ArrayList<String>();
        if (resto == 0)
            lines.add(threadInfoLine());
        String missingHeaderLines = missingHeaderLines();
        if (!missingHeaderLines.isEmpty())
            lines.add(missingHeaderLines);
        String subText = sanitizeText(sub, false, showSpoiler);
        if (subText != null && !subText.isEmpty())
            lines.add("<b>" + subText + "</b>");
        String comText = sanitizeText(com, false, showSpoiler);
        if (comText != null && !comText.isEmpty())
            lines.add(comText);
        return ChanHelper.join(lines, "<br/>\n");
    }


    public String spoilerText() {
        if ((sub != null && sub.indexOf("<s>") >= 0)
            || (com != null && com.indexOf("<s>") >= 0))
            return fullText(true);
        else
            return "";
    }

    private static final int MAX_THREAD_SUBJECT_LEN = 100;

    public String threadSubject(Context context) {
        String subText = sanitizeText(sub);
        if (subText != null && !subText.isEmpty())
            return subText;
        String comText = sanitizeText(com);
        if (comText != null && !comText.isEmpty())
            return comText.substring(0, Math.min(comText.length(), MAX_THREAD_SUBJECT_LEN)); // always shorter than this since only one line
        if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("anonymous"))
            return name;
        if (email != null && !email.isEmpty() && !email.equalsIgnoreCase("sage"))
            return email;
        return context.getResources().getString(R.string.thread_no_text_subject);
    }

    private String sanitizeText(String text) {
        return sanitizeText(text, false);
    }

    private String sanitizeText(String text, boolean collapseNewLines) {
        return sanitizeText(text, collapseNewLines, false);
    }

    private String sanitizeText(String text, boolean collapseNewlines, boolean showSpoiler) {
        if (text == null || text.isEmpty())
            return "";

        long start = System.currentTimeMillis();

        if (hidePostNumbers)
            text = text.replaceAll("<a[^>]*class=\"quotelink\">[^<]*</a>", "");
        else
            text = text.replaceAll("<a[^>]*class=\"quotelink\">([^<]*)</a>", "$1");

        text = text
                .replaceAll("<span[^>]*class=\"abbr\"[^>]*>.*</span>", "")    // exif reference
                .replaceAll("<table[^>]*class=\"exif\"[^>]*>.*</table>", "");  // exif info
        if (!showSpoiler)
            text = text.replaceAll("<s>[^<]*</s>", "XXXSPOILERXXX");                       // spoiler text
        text = textViewFilter(text, collapseNewlines);
        if (!showSpoiler)
            text = text.replaceAll("XXXSPOILERXXX", "<b>spoiler</b>");
        long end = System.currentTimeMillis();
        if (DEBUG) Log.v(TAG, "Regexp: " + (end - start) + "ms");

        return text;
    }

    public String exifText() {
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
		return "Post " + no + " " + com + ", thumb: " + thumbnailUrl() + " tn_w: " + tn_w + " tn_h: " + tn_h;
	}


    public String thumbnailUrl() { // thumbnail with fallback
        if (ChanBoard.isImagelessSticky(board, no))
            return "drawable://" + ChanBoard.getImageResourceId(board, no);
        else if (spoiler > 0)
            return ChanBoard.spoilerThumbnailUrl(board);
        else if (tim > 0 && filedeleted == 0 && tn_w > 2 && tn_h > 2)
            return "http://0.thumbs.4chan.org/" + board + "/thumb/" + tim + "s.jpg";
        else if (resto == 0) // thread default
            return "drawable://" + ChanBoard.getImageResourceId(board, no);
        else
            return "";
    }

    public int thumbnailId() { // for resource types
        if (ChanBoard.isImagelessSticky(board, no))
            return ChanBoard.getImageResourceId(board, no);
        else if (spoiler > 0)
            return 0;
        else if (tim > 0 && filedeleted == 0 && tn_w > 2 && tn_h > 2)
            return 0;
        else if (resto == 0) // thread default
            return ChanBoard.getImageResourceId(board, no);
        else
            return 0;
    }

    public String imageUrl() {
        return imageUrl(board, tim, ext);
   	}

    public static String imageUrl(String board, long tim, String ext) {
        if (tim != 0) {
            return "http://images.4chan.org/" + board + "/src/" + tim + ext;
        }
        return null;
    }

   	public String imageName() {
   		return no + ext;
   	}

    public String countryFlagUrl() {
        if (country != null && !country.isEmpty())
            return countryFlagUrl(board, country);
        else
            return null;
    }

    public String countryFlagUrl(String boardCode, String countryCode) {
        return "http://static.4chan.org/image/country/"
                + (boardCode.equals("pol") ? "troll/" : "")
                + countryCode.toLowerCase()
                + ".gif";
    }

    public String dateText() {
        long milliseconds = 1000 * time; // time in seconds, convert
        return (time > 0)
            ? DateUtils.getRelativeTimeSpanString(milliseconds, (new Date()).getTime(), 0, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
            : "";
    }

    public String imageDimensions() {
        if (fsize > 0) {
            int kbSize = (fsize / 1024) + 1;
            String size = (kbSize > 1000) ? (kbSize / 1000) + "MB" : kbSize + "KB";
            return w + "x" + h + " " + size;
        }
        return "";
    }

    public String headerLine() { // as side effect, set headerComponents
        List<String> items = new ArrayList<String>();
        String info = "";
        if (!hidePostNumbers)
            items.add(Long.toString(no));
        if (email != null && !email.isEmpty() && email.equals("sage")) {
            items.add("<b>sage</b>");
            headerComponents |= CHAN_ID;
            headerComponents |= CHAN_EMAIL;
        }
        else if (id != null && !id.isEmpty() && id.equalsIgnoreCase("heaven")) {
            items.add("<b>sage</b>");
            headerComponents |= CHAN_ID;
            headerComponents |= CHAN_EMAIL;
        }
        else if (id != null && !id.isEmpty()
                && (info = formattedUserId()).length() < MAX_HEADER_NAME_LEN) {
            items.add(info);
            headerComponents |= CHAN_ID;
        }
        else if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("anonymous")
                && name.length() < MAX_HEADER_NAME_LEN) {
            items.add(name);
            headerComponents |= CHAN_NAME;
        }
        else if (trip != null && !trip.isEmpty()
                && (info = formattedUserTrip()).length() < MAX_HEADER_NAME_LEN) {
            items.add(info);
            headerComponents |= CHAN_TRIP;
        }
        else if (email != null && !email.isEmpty() && !email.equalsIgnoreCase("sage")
                && email.length() < MAX_HEADER_NAME_LEN) {
            items.add(email);
            headerComponents |= CHAN_EMAIL;
        }
        return ChanHelper.join(items, " ");
    }

    public String threadInfoLine() {
        String text = "";
        if (sticky > 0 && replies == 0) {
            text += "STICKY";
        }
        else {
            if (replies > 0) {
                text += replies
                        + (replies == 1 ? "reply" : "replies")
                        + " "
                        + (images > 0 ? images : "no")
                        + " image"
                        + (images == 1 ? "" : "s");
            }
            else {
                text += "no replies";
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
        ext = from.ext;
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

    public String formattedUserId() {
        if (id == null)
            return "";
        else
            return formattedUserId(id, useFriendlyIds);
    }

    public String formattedUserTrip() {
        if (trip == null)
            return "";
        else
            return formattedUserTrip(trip, useFriendlyIds);
    }

    public static String formattedUserTrip(String trip, boolean useFriendlyIds) {
        if (trip == null)
            return "";
        if (!useFriendlyIds)
            return trip;
        if (trip.charAt(0) == '!' && trip.charAt(1) == '!')
            return "!!" + formattedUserId(trip.substring(2), useFriendlyIds);
        if (trip.charAt(0) == '!')
            return "!" + formattedUserId(trip.substring(1), useFriendlyIds);
        return trip;
    }

    public static String formattedUserId(String id, boolean useFriendlyIds) {
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

    public static MatrixCursor buildMatrixCursor() {
        return new MatrixCursor(ChanHelper.POST_COLUMNS);
    }

    public Object[] makeRow() {
        return new Object[] {
                no,
                board,
                resto,
                thumbnailUrl(),
                countryFlagUrl(),
                headerLine(),
                dateText(),
                fullText(),
                tn_w,
                tn_h,
                w,
                h,
                tim,
                spoiler,
                spoilerText(),
                exifText(),
                id,
                trip,
                name,
                email,
                imageDimensions(),
                isDead ? 1 : 0,
                closed,
                0,
                0,
                thumbnailId(),
                ext
        };
    }

    public static Object[] makeAdRow(Context context, String boardCode, String imageUrl, String clickUrl) {
        return new Object[] {
                2,
                boardCode,
                0,
                imageUrl,
                "",
                context.getString(R.string.board_advert_info),
                context.getString(R.string.advert_header),
                clickUrl,
                176,
                0,
                -1,
                -1,
                0,
                0,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                0,
                0,
                0,
                1,
                0,
                ""
        };
    }

}
