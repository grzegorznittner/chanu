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

    public static final int MAX_SINGLELINE_TEXT_LEN = 20;
    private static final int MIN_LINE = 30;
    private static final int MAX_LINE = 40;
    private static final int MAX_THREAD_SUBJECT_LEN = 100;
    private static final int MIN_SUBJECT_LEN = 2;
    private static final int MAX_SUBJECT_LEN = 100; // 4chan enforces 100

    public static final String POST_NO = "postNo";
    public static final String POST_ID = "_id";
    public static final String POST_BOARD_CODE = "boardCode";
    public static final String POST_NAME = "name";
    public static final String POST_EMAIL = "email";
    public static final String POST_TIM = "tim";
    public static final String POST_EXT = "ext";
    public static final String POST_W = "w";
    public static final String POST_H = "h";
    public static final String POST_TN_W = "tn_w";
    public static final String POST_TN_H = "tn_h";
    public static final String POST_RESTO = "resto";
    public static final String POST_HEADLINE_TEXT = "headlineText"; // we construct and filter this
    public static final String POST_SUBJECT_TEXT = "subjectText"; // we construct and filter this // NOT USED
    public static final String POST_TEXT = "text"; // we construct and filter this
    public static final String POST_IMAGE_URL = "imageUrl"; // we construct this from board and tim
    public static final String POST_FULL_IMAGE_URL = "fullImageUrlrl"; // we construct this from board and tim
    public static final String POST_COUNTRY_URL = "countryUrl"; // we construct this from the country code
    public static final String POST_SPOILER_SUBJECT = "spoilerSubject";
    public static final String POST_SPOILER_TEXT = "spoilerText";
    public static final String POST_EXIF_TEXT = "exifText";
    public static final String POST_USER_ID = "id";
    public static final String POST_TRIPCODE = "trip";
    public static final String POST_THUMBNAIL_ID = "postThumbnailId";
    public static final String POST_FLAGS = "postFlags";
    public static final int FLAG_HAS_IMAGE = 0x001;
    public static final int FLAG_HAS_SUBJECT = 0x002;
    public static final int FLAG_HAS_TEXT = 0x004;
    public static final int FLAG_HAS_SPOILER = 0x008;
    public static final int FLAG_HAS_EXIF = 0x010;
    public static final int FLAG_HAS_COUNTRY = 0x020;
    public static final int FLAG_IS_DEAD = 0x040;
    public static final int FLAG_IS_CLOSED = 0x080;
    public static final int FLAG_IS_AD = 0x100;
    public static final int FLAG_IS_TITLE = 0x200;
    public static final int FLAG_IS_THREADLINK = 0x400;
    public static final int FLAG_IS_BOARDLINK = 0x800;

    private int postFlags(boolean isAd, boolean isThreadLink, String subject, String text, String exifText) {
        int flags = 0;
        if (tim > 0)
            flags |= FLAG_HAS_IMAGE;
        if (subject != null && !subject.isEmpty())
            flags |= FLAG_HAS_SUBJECT;
        if (text != null && !text.isEmpty())
            flags |= FLAG_HAS_TEXT;
        if (!isThreadLink && hasSpoiler())
            flags |= FLAG_HAS_SPOILER;
        if (!isThreadLink && exifText != null && !exifText.isEmpty())
            flags |= FLAG_HAS_EXIF;
        if (country != null && !country.isEmpty())
            flags |= FLAG_HAS_COUNTRY;
        if (isDead)
            flags |= FLAG_IS_DEAD;
        if (closed > 0)
            flags |= FLAG_IS_CLOSED;
        if (isAd)
            flags |= FLAG_IS_AD;
        if (isThreadLink)
            flags |= FLAG_IS_THREADLINK;
        return flags;
    }

    public static final String[] POST_COLUMNS = {
            POST_ID,
            POST_BOARD_CODE,
            POST_RESTO,
            POST_IMAGE_URL,
            POST_FULL_IMAGE_URL,
            POST_COUNTRY_URL,
            POST_HEADLINE_TEXT,
            POST_SUBJECT_TEXT,
            POST_TEXT,
            POST_TN_W,
            POST_TN_H,
            POST_W,
            POST_H,
            POST_TIM,
            POST_SPOILER_SUBJECT,
            POST_SPOILER_TEXT,
            POST_EXIF_TEXT,
            POST_USER_ID,
            POST_TRIPCODE,
            POST_NAME,
            POST_EMAIL,
            POST_THUMBNAIL_ID,
            POST_EXT,
            POST_FLAGS
    };

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

    private boolean hasSpoiler() {
        if (spoiler > 0)
            return true;
        if (sub != null && sub.matches(".*<s>.*</s>.*"))
            return true;
        if (com != null && com.matches(".*<s>.*</s>.*"))
            return true;
        return false;
    }

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

    protected String[] textComponents(String query) {
        return textComponents(query, false);
    }

    private String[] spoilerComponents(String query) {
        return textComponents(query, true);
    }

    private String[] textComponents(String query, boolean showSpoiler) {
        String subText = sanitizeText(sub, false, showSpoiler);
        String comText = sanitizeText(com, false, showSpoiler);
        String subject = subText != null ? subText : "";
        String message = comText != null ? comText : "";

        if (!subject.isEmpty() || message.isEmpty()) { // we have a subject or can't extract from message
            if (DEBUG) Log.v(TAG, "Exception: provided subject=" + subject + " message=" + message);
            return highlightComponents(subject, message, query);
        }

        if (comText.length() <= MAX_SUBJECT_LEN) { // just make message the subject
            subject = message;
            message = "";
            if (DEBUG) Log.v(TAG, "made message the subject=" + subject + " message=" + message);
            return highlightComponents(subject, message, query);
        }

        // start subject extraction process
        String[] terminators = { "\r", "\n", "<br/>", "<br>", ". ", "! ", "? ", "; ", ": ", ", " };
        message = message
                .replaceAll("(<br/?>)+", "<br/>")
                .trim()
                .replaceFirst("^(<br/?>)+", "")
                .replaceFirst("(<br/?>)+$", "")
                .trim();
        for (String terminator : terminators) {
            int i = message.indexOf(terminator);
            if (i > MIN_SUBJECT_LEN && i < MAX_SUBJECT_LEN) { // extract the subject
                int len = terminator.length();
                subject = message.substring(0, i + len).trim().replaceFirst("(<br/?>)+$", "").trim();
                message = message.substring(i + len).trim().replaceFirst("^(<br/?>)+", "").trim();
                if (DEBUG) Log.v(TAG, "extracted subject=" + subject + " message=" + message);
                return highlightComponents(subject, message, query);
            }
        }

        // cutoff
        int i = MAX_SUBJECT_LEN - 1; // start cut at max len
        char c;
        while (!Character.isWhitespace(c = comText.charAt(i)) && i > 0)
            i--; // rewind until we reach a whitespace character
        if (i > MIN_SUBJECT_LEN) { // we found a suitable cutoff point
            subject = comText.substring(0, i).trim().replaceFirst("(<br/?>)+$", "").trim();
            message = comText.substring(i + 1).trim().replaceFirst("^(<br/?>)+", "").trim();
            if (DEBUG) Log.v(TAG, "cutoff subject=" + subject + " message=" + message);
        }

        // default
        if (DEBUG) Log.v(TAG, "default subject=" + subject + " message=" + message);
        return highlightComponents(subject, message, query);
    }

    private String[] highlightComponents(String subject, String message, String query) {
        return new String[] { highlightComponent(subject, query), highlightComponent(message, query) };
    }

    private static final String HIGHLIGHT_COLOR = "#aaa268";

    private String highlightComponent(String component, String query) {
        if (query.isEmpty())
            return component;
        String regex = "(?i)(" + query + ")";
        String replace = "<b><font color=\"" + HIGHLIGHT_COLOR + "\">$1</font></b>";
        return component.replaceAll(regex, replace);
    }

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
        else if (resto <= 0) // thread default
            return "drawable://" + ChanBoard.getRandomImageResourceId(board, no);
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
        else if (resto <= 0) // thread default
            return ChanBoard.getRandomImageResourceId(board, no);
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

    public String headline(String query, boolean boardLevel) {
        List<String> items = new ArrayList<String>();
        if (!boardLevel && !hidePostNumbers)
            items.add(Long.toString(no));
        if (email != null && !email.isEmpty() && email.equals("sage"))
            items.add("<b>sage</b>");
        if (id != null && !id.isEmpty() && id.equals("Heaven"))
            items.add("<b>sage</b>");
        if (id != null && !id.isEmpty())
            items.add(formattedUserId());
        if (name != null && !name.isEmpty() && !name.equals("Anonymous"))
            items.add(name);
        if (trip != null && !trip.isEmpty())
            items.add(formattedUserTrip());
        if (email != null && !email.isEmpty() && !email.equals("sage"))
            items.add(email);
        if (country_name != null && !country_name.isEmpty())
            items.add(country_name);
        if (!boardLevel && fsize > 0)
            items.add(imageDimensions());
        if (resto <= 0)
            items.add(threadInfoLine());
        items.add(dateText());
        String component = ChanHelper.join(items, " <b>&middot;</b> ");
        return highlightComponent(component, query);
    }

    public String threadInfoLine() {
        String text = "";
        if (sticky > 0 && replies == 0) {
            text += "STICKY";
        }
        else {
            if (replies > 0) {
                text += replies
                        + " "
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

    public boolean matchesQuery(String query) {
        if (query == null || query.isEmpty())
            return true;
        // should use StringUtils.containsIgnoreCase
        if (no != 0 && Long.toString(no).contains(query))
            return true;
        if (id != null && id.toLowerCase().contains(query))
            return true;
        if (name != null && name.toLowerCase().contains(query))
            return true;
        if (trip != null && trip.toLowerCase().contains(query))
            return true;
        if (email != null && email.toLowerCase().contains(query))
            return true;
        if (country_name != null && country_name.toLowerCase().contains(query))
            return true;
        if (sub != null && sub.toLowerCase().contains(query))
            return true;
        if (com != null && com.toLowerCase().contains(query))
            return true;
        if (DEBUG) Log.i(TAG, "skipping post not matching query: " + no + " " + sub + " " + com);
        return false;
    }

    public static MatrixCursor buildMatrixCursor() {
        return new MatrixCursor(POST_COLUMNS);
    }

    public Object[] makeRow(String query) {
        String[] textComponents = textComponents(query);
        String[] spoilerComponents = spoilerComponents(query);
        String exifText = exifText();
        return new Object[] {
                no,
                board,
                resto,
                thumbnailUrl(),
                imageUrl(),
                countryFlagUrl(),
                headline(query, false),
                textComponents[0],
                textComponents[1],
                tn_w,
                tn_h,
                w,
                h,
                tim,
                spoilerComponents[0],
                spoilerComponents[1],
                exifText(),
                id,
                trip,
                name,
                email,
                thumbnailId(),
                ext,
                postFlags(false, false, textComponents[0], textComponents[1], exifText)
        };
    }

    public Object[] makeThreadLinkRow() {
        String[] textComponents = textComponents("");
        return new Object[] {
                no,
                board,
                resto,
                thumbnailUrl(),
                "",
                countryFlagUrl(),
                headline("", false),
                textComponents[0],
                "",
                tn_w,
                tn_h,
                w,
                h,
                tim,
                "",
                "",
                "",
                id,
                trip,
                name,
                email,
                thumbnailId(),
                ext,
                postFlags(false, true, textComponents[0], "", "")
        };
    }

    public static Object[] makeBoardLinkRow(ChanBoard board) {
        int drawableId = board.getRandomImageResourceId();
        return new Object[] {
                board.link.hashCode(),
                board.link,
                0,
                "drawable://" + drawableId,
                "",
                "",
                "",
                board.name,
                "",
                250,
                250,
                -1,
                -1,
                0,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                drawableId,
                "",
                FLAG_HAS_IMAGE | FLAG_HAS_SUBJECT | FLAG_IS_BOARDLINK
        };
    }

    public static Object[] makeAdRow(Context context, String boardCode, ChanAd ad) {
        String subject = context.getString(R.string.advert_header);
        return new Object[] {
                2,
                boardCode,
                0,
                ad.imageUrl(),
                "",
                "",
                context.getString(R.string.board_advert_full),
                subject,
                ad.clickUrl(),
                ad.tn_w(),
                ad.tn_h(),
                -1,
                -1,
                0,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                FLAG_HAS_IMAGE | FLAG_HAS_SUBJECT | FLAG_IS_AD
        };
    }

    public static Object[] makeTitleRow(String boardCode, String title) {
        String subject = title;
        return new Object[] {
                3,
                boardCode,
                0,
                "",
                "",
                "",
                "",
                subject,
                "",
                "",
                "",
                -1,
                -1,
                0,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                FLAG_HAS_SUBJECT | FLAG_IS_TITLE
        };
    }

}
