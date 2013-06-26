package com.chanapps.four.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.text.format.DateUtils;
import android.util.Log;

import com.chanapps.four.activity.R;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

public class ChanPost {

	public static final String TAG = ChanPost.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final String HEADLINE_BOARDLEVEL_DELIMITER = "<br/>";
    public static final String HEADLINE_THREADLEVEL_DELIMITER = "<br/>";
    //public static final String HEADLINE_THREADLEVEL_DELIMITER = " &middot; ";
    private static final int MIN_LINE = 30;
    private static final int MAX_LINE = 40;
    private static final int MAX_THREAD_SUBJECT_LEN = 100;
    private static final int MIN_SUBJECT_LEN = 2;
    private static final int MAX_SUBJECT_LEN = 50; // 4chan enforces 100

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
    public static final String POST_NUM_REPLIES = "numReplies";
    public static final String POST_NUM_IMAGES = "numImages";
    public static final String POST_SUBJECT_TEXT = "subjectText"; // we construct and filter this // NOT USED
    public static final String POST_TEXT = "text"; // we construct and filter this
    public static final String POST_DATE_TEXT = "dateText"; // we construct and filter this
    public static final String POST_IMAGE_URL = "imageUrl"; // we construct this from board and tim
    public static final String POST_FULL_IMAGE_URL = "fullImageUrlrl"; // we construct this from board and tim
    public static final String POST_COUNTRY_URL = "countryUrl"; // we construct this from the country code
    public static final String POST_SPOILER_SUBJECT = "spoilerSubject";
    public static final String POST_SPOILER_TEXT = "spoilerText";
    public static final String POST_EXIF_TEXT = "exifText";
    public static final String POST_USER_ID = "id";
    public static final String POST_TRIPCODE = "trip";
    public static final String POST_THUMBNAIL_ID = "postThumbnailId";
    public static final String POST_BACKLINKS_BLOB = "backlinksBlob";
    public static final String POST_REPLIES_BLOB = "repliesBlob";
    public static final String POST_SAME_IDS_BLOB = "sameIdsBlob";
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
    public static final int FLAG_IS_BUTTON = 0x400;
    public static final int FLAG_IS_THREADLINK = 0x800;
    public static final int FLAG_IS_BOARDLINK = 0x1000;
    public static final int FLAG_IS_HEADER = 0x2000;
    public static final int FLAG_IS_URLLINK = 0x4000;
    public static final int FLAG_NO_EXPAND = 0x8000;
    public static final int FLAG_HAS_HEAD = 0x10000;

    public static String planifyText(String text) {
        return text.replaceAll("<br/?>", "\n").replaceAll("<[^>]*>", "");
    }

    public static String join(List<String> list, String delimiter) {
        String text = "";
        boolean first = true;
        for (String item : list) {
            if (first) {
                text += item;
                first = false;
                continue;
            }
            text += delimiter + item;
        }
        return text;
    }

    public static int countLines(String s) {
        if (s == null || s.isEmpty())
            return 0;
        int i = 1;
        int idx = -1;
        while ((idx = s.indexOf('\n', idx + 1)) != -1) {
            i++;
        }
        return i;
    }

    private int postFlags(boolean isAd, boolean isThreadLink, String subject, String text, String exifText, String headline) {
        int flags = 0;
        if (tim > 0)
            flags |= FLAG_HAS_IMAGE;
        if (subject != null && !subject.isEmpty())
            flags |= FLAG_HAS_SUBJECT;
        if (text != null && !text.isEmpty())
            flags |= FLAG_HAS_TEXT;
        if (!isThreadLink && spoiler > 0)
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
        if (headline != null && !headline.isEmpty())
            flags |= FLAG_HAS_HEAD;
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
            POST_NUM_REPLIES,
            POST_NUM_IMAGES,
            POST_SUBJECT_TEXT,
            POST_TEXT,
            POST_DATE_TEXT,
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
            POST_BACKLINKS_BLOB,
            POST_REPLIES_BLOB,
            POST_SAME_IDS_BLOB,
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

    // to support latest posts and recent images direct jump to post number
    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingLongDeserializer.class)
    public long jumpToPostNo = 0;

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

    private String cleanSubject(String subject) {
        return subject
                .trim()
                .replaceFirst("^(<br/?>)+", "")
                .replaceAll("(<br/?>)(<br/?>)+", "$1")
                .replaceFirst("(<br/?>)+$", "")
                .trim();
    }

    private String cleanMessage(String message) {
        return message
                .trim()
                .replaceFirst("^(<br/?>)+", "")
                .replaceFirst("(<br/?>)+$", "")
                .trim();
    }

    private String[] textComponents(String query, boolean showSpoiler) {
        String subText = sanitizeText(sub, false, showSpoiler);
        String comText = sanitizeText(com, false, showSpoiler);
        String subject = subText != null ? subText : "";
        String message = comText != null ? comText : "";

        if (resto > 0) {
            if (DEBUG) Log.v(TAG, "default subject=" + subject + " message=" + message);
            return highlightComponents(cleanSubject(subject), cleanMessage(message), query);
        }

        if (!subject.isEmpty() || message.isEmpty()) { // we have a subject or can't extract from message
            if (DEBUG) Log.v(TAG, "provided subject=" + subject + " message=" + message);
            return highlightComponents(cleanSubject(subject), cleanMessage(message), query);
        }
        if (comText.length() <= MAX_SUBJECT_LEN) { // just make message the subject
            subject = cleanSubject(message);
            message = "";
            if (DEBUG) Log.v(TAG, "made message the subject=" + subject + " message=" + message);
            return highlightComponents(subject, message, query);
        }

        // start subject extraction process
        String[] terminators = { "\r", "\n", "<br/>", "<br>", ". ", "! ", "? ", "; ", ": ", ", " };
        message = cleanMessage(message);
        for (String terminator : terminators) {
            int i = message.indexOf(terminator);
            if (i > MIN_SUBJECT_LEN && i < MAX_SUBJECT_LEN) { // extract the subject
                int len = terminator.length();
                subject = cleanSubject(message.substring(0, i + len));
                message = cleanMessage(message.substring(i + len));
                if (DEBUG) Log.v(TAG, "extracted subject=" + subject + " message=" + message);
                return highlightComponents(subject, message, query);
            }
        }

        // cutoff
        int i = MAX_SUBJECT_LEN - 1; // start cut at max len
        while (!Character.isWhitespace(comText.charAt(i)) && i > 0)
            i--; // rewind until we reach a whitespace character
        if (i > MIN_SUBJECT_LEN) { // we found a suitable cutoff point
            subject = cleanSubject(comText.substring(0, i));
            message = cleanMessage(comText.substring(i + 1));
            if (DEBUG) Log.v(TAG, "cutoff subject=" + subject + " message=" + message);
            return highlightComponents(subject, message, query);
        }

        // default
        if (DEBUG) Log.v(TAG, "default subject=" + subject + " message=" + message);
        return highlightComponents(cleanSubject(subject), cleanMessage(message), query);
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
        //if (!showSpoiler)
        //    text = text.replaceAll("<s>[^<]*</s>", "XXXSPOILERXXX");                       // spoiler text
        text = textViewFilter(text, collapseNewlines);
        //if (!showSpoiler)
        //    text = text.replaceAll("XXXSPOILERXXX", "<b>spoiler</b>");
        long end = System.currentTimeMillis();
        if (DEBUG) Log.v(TAG, "Regexp: " + (end - start) + "ms");

        return text;
    }

    public String exifText() {
        return exifText(com);
    }

    protected static final Pattern EXIF_PATTERN = Pattern.compile(".*<table[^>]*class=\"exif\"[^>]*>(.*)</table>.*");

    private static final String exifText(String text) {
        if (text == null || text.isEmpty())
            return null;
        Matcher m = EXIF_PATTERN.matcher(text);
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
                .replaceAll("<[^s/][^>]+>", "") // preserve <s> tags
                .replaceAll("<s[^>]+>", "")
                .replaceAll("</[^s][^>]*>", "")
                .replaceAll("</s[^>]+>", "")
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
        else if (tim > 0 && filedeleted == 0) // && tn_w > 2 && tn_h > 2)
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
        long timeMs = time > 0 ? 1000 * time : tim;
        return (timeMs > 0)
            ? DateUtils.getRelativeTimeSpanString(timeMs, (new Date()).getTime(), 0, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
            : "";
    }

    public String imageDimensions() {
        if (fsize > 0) {
            int kbSize = (fsize / 1024) + 1;
            String size = (kbSize > 1000) ? (kbSize / 1000) + "MB" : kbSize + "KB";
            return w + "x" + h + " - " + size;
        }
        return "";
    }

    public String headline(String query, boolean boardLevel, byte[] repliesBlob, boolean showNumReplies) {
        List<String> items = new ArrayList<String>();
        if (!boardLevel) {
            if (email != null && !email.isEmpty() && email.equals("sage"))
                items.add("<b>sage</b>");
            if (id != null && !id.isEmpty() && id.equals(SAGE_POST_ID))
                items.add("<b>sage</b>");
            if (id != null && !id.isEmpty())
                items.add("Id: " + formattedUserId());
            if (name != null && !name.isEmpty() && !name.equals("Anonymous"))
                items.add(name);
            if (trip != null && !trip.isEmpty())
                items.add(formattedUserTrip());
            if (email != null && !email.isEmpty() && !email.equals("sage"))
                items.add(email);
            if (country_name != null && !country_name.isEmpty())
                items.add(country_name);
            if (fsize > 0)
                items.add(imageDimensions());
        }
        //if (boardLevel && resto <= 0) {
            String s = threadInfoLine(boardLevel, showNumReplies);
            if (!s.isEmpty())
                items.add(s);
        //}
        /*
        if (repliesBlob != null && repliesBlob.length > 0) { // don't show text for threads
            HashSet<Long> hashSet = (HashSet<Long>)parseBlob(repliesBlob);
            int n = hashSet != null ? hashSet.size() : 0;
            if (n > 0) {
                String s = hashSet.size() + (n == 1 ? " Reply" : " Replies");
                items.add(s);
            }
        }
        */
        String delim = boardLevel ? HEADLINE_BOARDLEVEL_DELIMITER : HEADLINE_THREADLEVEL_DELIMITER;
        String component = join(items, delim);
        return highlightComponent(component, query);
    }

    public String threadInfoLine(boolean boardLevel, boolean showNumReplies) {
        if (sticky > 0 && replies == 0)
            return "STICKY";
        String text = "";
        if (showNumReplies) {
            if (replies > 0)
                text += replies
                        + " "
                        + (replies == 1 ? "post" : "posts")
                        + (boardLevel ? HEADLINE_BOARDLEVEL_DELIMITER : HEADLINE_THREADLEVEL_DELIMITER)
                        + (images > 0 ? images : "no")
                        + " "
                        + (images == 1 ? "image" : "images");
            else
                text += "no posts";
        }
        if (!boardLevel && resto == 0) {
            if (imagelimit == 1)
                text += " (IL)";
            if (bumplimit == 1)
                text += " (BL)";
            if (sticky > 0)
                text += " STICKY";
            if (closed > 0)
                text += " CLOSED";
        }
        return text;
    }

    public void mergeIntoThreadList(List<ChanPost> threads) {
        boolean exists = false;
        for (ChanPost existingThread : threads) {
            if (this.no == existingThread.no) {
                exists = true;
                existingThread.copyUpdatedInfoFields(this);
                break;
            }
        }
        if (!exists) {
            threads.add(this);
        }
    }

    public void copyUpdatedInfoFields(ChanPost from) {
        bumplimit = from.bumplimit;
        imagelimit = from.imagelimit;
        images = from.images;
        omitted_images = from.omitted_images;
        omitted_posts = from.omitted_posts;
        replies = from.replies;
        /*
        tn_w = from.tn_w;
        tn_h = from.tn_h;
        sub = from.sub;
        com = from.com;
        sticky = from.sticky;
        */
        closed = from.closed;
        /*
        spoiler = from.spoiler;
        now = from.now;
        trip = from.trip;
        id = from.id;
        capcode = from.capcode;
        country = from.country;
        country_name = from.country_name;
        email = from.email;
        created = from.created;
        time = from.time;
        tim = from.tim;
        filename = from.filename;
        ext = from.ext;
        w = from.w;
        h = from.h;
        fsize = from.fsize;
        */
        filedeleted = from.filedeleted;
    }

    public boolean refersTo(long postNo) {
        if (postNo <= 0 || com == null || com.isEmpty())
            return false;
        boolean matches = com.indexOf("#p" + postNo + "\"") >= 0;
        if (DEBUG) Log.i(TAG, "Matching postNo=" + postNo + " is " + matches + " against com=" + com);
        return matches;
    }

    protected static final Pattern BACKLINK_PATTERN = Pattern.compile("#p(\\d+)\"");

    protected HashSet<Long> backlinks() {
        HashSet<Long> backlinks = null;
        if (com != null && !com.isEmpty()) {
            Matcher m = BACKLINK_PATTERN.matcher(com);
            while (m.find()) {
                if (backlinks == null)
                    backlinks = new HashSet<Long>();
                backlinks.add(new Long(m.group(1)));
            }
        }
        return backlinks;
    }

    public static byte[] blobify(HashSet<?> hashSet) {
        if (hashSet == null || hashSet.isEmpty())
            return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(hashSet);
            return baos.toByteArray();
        }
        catch (IOException e) {
            Log.e(TAG, "Couldn't serialize set=" + hashSet, e);
        }
        return null;
    }

    public static HashSet<?> parseBlob(final byte[] b) {
        if (b == null || b.length == 0)
            return null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(b);
            ObjectInputStream ois = new ObjectInputStream(bais);
            HashSet<?> hashSet = (HashSet<?>)ois.readObject();
            return hashSet;
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't deserialize blob=" + b);
        }
        return null;
    }

    public static final String SAGE_POST_ID = "Heaven";
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

    public Object[] makeRow(String query, int i, byte[] backlinksBlob, byte[] repliesBlob, byte[] sameIdsBlob) {
        String[] textComponents = textComponents(query);
        String[] spoilerComponents = spoilerComponents(query);
        String exifText = exifText();
        String headline = headline(query, false, repliesBlob, false);
        int flags = postFlags(false, false, textComponents[0], textComponents[1], exifText, headline);
        if (i == 0)
            flags |= FLAG_IS_HEADER;
        return new Object[] {
                no,
                board,
                resto,
                thumbnailUrl(),
                imageUrl(),
                countryFlagUrl(),
                headline,
                replies,
                images,
                textComponents[0],
                textComponents[1],
                dateText(),
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
                backlinksBlob,
                repliesBlob,
                sameIdsBlob,
                flags
        };
    }

    public Object[] makeThreadLinkRow() {
        String[] textComponents = textComponents("");
        String headline = headline("", true, null, false);
        return new Object[] {
                no,
                board,
                resto,
                thumbnailUrl(),
                "",
                countryFlagUrl(),
                headline,
                replies,
                images,
                textComponents[0],
                "",
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
                null,
                null,
                null,
                postFlags(false, true, textComponents[0], "", "", headline)
        };
    }

    public static Object[] makeBoardLinkRow(Context context, ChanBoard board, long threadNo) {
        int drawableId = board.getRandomImageResourceId(threadNo);
        return new Object[] {
                board.link.hashCode(),
                board.link,
                0,
                "drawable://" + drawableId,
                "",
                "",
                board.getDescription(context),
                0,
                0,
                board.name,
                "",
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
                null,
                null,
                null,
                FLAG_HAS_IMAGE | FLAG_HAS_SUBJECT | FLAG_IS_BOARDLINK
        };
    }

    public static Object[] makeAdRow(Context context, String boardCode, ChanAd ad) {
        return new Object[] {
                ad.hashCode(),
                boardCode,
                0,
                ad.bannerImageUrl(),
                "",
                "",
                "",
                0,
                0,
                "",
                ad.bannerClickUrl(),
                "",
                ad.tn_w_banner(),
                ad.tn_h_banner(),
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
                null,
                null,
                null,
                FLAG_HAS_IMAGE | FLAG_IS_AD
        };
    }

    public static Object[] makeTitleRow(String boardCode, String title) {
        return makeTitleRow(boardCode, title, "");
    }

    public static Object[] makeTitleRow(String boardCode, String title, String desc) {
        String subject = title;
        return new Object[] {
                title.hashCode(),
                boardCode,
                0,
                "",
                "",
                "",
                "",
                0,
                0,
                subject,
                desc,
                "",
                0,
                0,
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
                null,
                null,
                null,
                FLAG_HAS_SUBJECT | FLAG_IS_TITLE
        };
    }

    public static final int MIN_TOKEN_LENGTH = 5;

    public Set<String> keywords() {
        String text = (sub == null ? "" : sub) + (com == null ? "" : com) + headline("", false, null, false);
        String stripped = text.replaceAll("<[^>]*>|\\W+", " ");
        String[] tokens = stripped.split("\\s+");
        if (DEBUG) Log.v(TAG, "threadNo=" + no + " tokens=" + Arrays.toString(tokens));
        Set<String> tokenSet = new HashSet<String>();
        for (String token : tokens) {
            if (token.length() > MIN_TOKEN_LENGTH || token.matches("[A-Z]+")) // all uppercase abbreviations
                tokenSet.add(token.toLowerCase());
        }
        if (DEBUG) Log.v(TAG, "threadNo=" + no + " keywords=" + tokenSet);
        return tokenSet;
    }

    public int keywordRelevance(Set<String> keywords) {
        Set<String> tokenSet = keywords();
        tokenSet.retainAll(keywords);
        int relevancy = tokenSet.size();
        if (DEBUG && relevancy > 0) Log.v(TAG, "relevancy=" + relevancy + " matching keywords=" + tokenSet);
        return relevancy;
    }

    public static Object[] extractPostRow(Cursor cursor) {
        int flagIdx = cursor.getColumnIndex(POST_FLAGS);
        int postNoIdx = cursor.getColumnIndex(POST_ID);
        int restoIdx = cursor.getColumnIndex(POST_RESTO);
        int timIdx = cursor.getColumnIndex(POST_TIM);
        int c = cursor.getColumnCount();
        Object[] o = new Object[c];
        for (int i = 0; i < c; i++) {
            if (i == flagIdx) {
                int flags = cursor.getInt(flagIdx);
                flags |= FLAG_NO_EXPAND;
                o[i] = flags;
                continue;
            }
            if (i == postNoIdx || i == restoIdx || i == timIdx) {
                o[i] = cursor.getLong(i);
                continue;
            }
            int type = cursor.getType(i);
            switch (type) {
                case Cursor.FIELD_TYPE_BLOB: o[i] = cursor.getBlob(i); break;
                case Cursor.FIELD_TYPE_FLOAT: o[i] = cursor.getFloat(i); break;
                case Cursor.FIELD_TYPE_INTEGER: o[i] = cursor.getInt(i); break;
                case Cursor.FIELD_TYPE_STRING: o[i] = cursor.getString(i); break;
                case Cursor.FIELD_TYPE_NULL:
                default: o[i] = null; break;
            }
        }
        return o;
    }
    
    public void updateThreadData(ChanThread t) {
    	closed = t.closed;
    	omitted_images = t.omitted_images;
    	omitted_posts = t.omitted_posts;
    	
    	if (t.posts.length > 0 && t.posts[0] != null) {
        	replies = t.posts[0].replies;
        	images = t.posts[0].images;
	    	bumplimit = t.posts[0].bumplimit;
	    	capcode = t.posts[0].capcode;
	    	com = t.posts[0].com;
	    	country = t.posts[0].country;
	    	country_name = t.posts[0].country_name;
	    	email = t.posts[0].email;
	    	ext = t.posts[0].ext;
	    	filedeleted = t.posts[0].filedeleted;
	    	filename = t.posts[0].filename;
	    	fsize = t.posts[0].fsize;
	    	h = t.posts[0].h;
	    	hideAllText = t.posts[0].hideAllText;
	    	hidePostNumbers = t.posts[0].hidePostNumbers;
	    	id = t.posts[0].id;
	    	now = t.posts[0].now;
	    	spoiler = t.posts[0].spoiler;
	    	sticky = t.posts[0].sticky;
	    	sub = t.posts[0].sub;
	    	tim = t.posts[0].tim;
	    	tn_h = t.posts[0].tn_h;
	    	tn_w = t.posts[0].tn_w;
	    	trip = t.posts[0].trip;
	    	useFriendlyIds = t.posts[0].useFriendlyIds;
	    	w = t.posts[0].w;
    	}    	
    }

    public void updateThreadDataWithPost(ChanPost t) {
    	closed = t.closed;
    	omitted_images = t.omitted_images;
    	omitted_posts = t.omitted_posts;
    	
    	replies = t.replies;
    	images = t.images;
    	bumplimit = t.bumplimit;
    	capcode = t.capcode;
    	com = t.com;
    	country = t.country;
    	country_name = t.country_name;
    	email = t.email;
    	ext = t.ext;
    	filedeleted = t.filedeleted;
    	filename = t.filename;
    	fsize = t.fsize;
    	h = t.h;
    	hideAllText = t.hideAllText;
    	hidePostNumbers = t.hidePostNumbers;
    	id = t.id;
    	now = t.now;
    	spoiler = t.spoiler;
    	sticky = t.sticky;
    	sub = t.sub;
    	tim = t.tim;
    	tn_h = t.tn_h;
    	tn_w = t.tn_w;
    	trip = t.trip;
    	useFriendlyIds = t.useFriendlyIds;
    	w = t.w;
    }

    public static String postUrl(String boardCode, long threadNo, long postNo) {
        return ChanThread.threadUrl(boardCode, threadNo) + "#p" + postNo;
    }

}
