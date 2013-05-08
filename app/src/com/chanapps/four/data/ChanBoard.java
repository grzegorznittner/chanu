package com.chanapps.four.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.service.FetchChanDataService;

public class ChanBoard {

	public static final String TAG = ChanBoard.class.getSimpleName();

    private static final boolean DEBUG = false;
    private static final int NUM_DEFAULT_IMAGES_PER_BOARD = 3;
    private static final int NUM_RELATED_BOARDS = 3;
    private static final int NUM_RELATED_THREADS = 3;

    public static final String WATCH_BOARD_CODE = "watch";
    public static final String POPULAR_BOARD_CODE = "popular";
    public static final String LATEST_BOARD_CODE = "latest";
    public static final String LATEST_IMAGES_BOARD_CODE = "images";
    public static final String WATCHLIST_BOARD_CODE = "watchlist";

    public static final String DEFAULT_BOARD_CODE = "a";

    public String board;
    public String name;
    public String link;
    public int iconId;
    public int no;
	public BoardType boardType;
    public boolean workSafe;
    public boolean classic;
    public boolean textOnly;
	public ChanPost stickyPosts[] = new ChanPost[0];
	public ChanPost threads[] = new ChanThread[0];
	public ChanPost loadedThreads[] = new ChanThread[0];
	public int newThreads = 0;
	public int updatedThreads = 0;
    public long lastFetched;
    public boolean defData = false;

    private static List<ChanBoard> boards = new ArrayList<ChanBoard>();
    private static List<ChanBoard> safeBoards = new ArrayList<ChanBoard>();
    private static Map<BoardType, List<ChanBoard>> boardsByType = new HashMap<BoardType, List<ChanBoard>>();
    private static Map<String, ChanBoard> boardByCode = new HashMap<String, ChanBoard>();
    private static Map<String, List<ChanBoard>> relatedBoards = new HashMap<String, List<ChanBoard>>();

    public ChanBoard() {
        // public default constructor for Jackson
    }

    private ChanBoard(BoardType boardType, String name, String link, int iconId,
                      boolean workSafe, boolean classic, boolean textOnly) {
        this.boardType = boardType;
        this.name = name;
        this.link = link;
        this.iconId = iconId;
        this.workSafe = workSafe;
        this.classic = classic;
        this.textOnly = textOnly;
    }

    public ChanBoard copy() {
        ChanBoard copy = new ChanBoard(this.boardType, this.name, this.link, this.iconId,
                this.workSafe, this.classic, this.textOnly);
        return copy;
    }

    public String toString() {
        return "Board " + link + " page: " + no + ", stickyPosts: " + stickyPosts.length
                + ", threads: " + threads.length + ", newThreads: " + loadedThreads.length;
    }

    public static List<ChanBoard> getBoards(Context context) {
        initBoards(context);
		return boards;
	}

    public static List<ChanBoard> getBoardsRespectingNSFW(Context context) {
        initBoards(context);
        return showNSFW(context) ? boards : safeBoards;
    }

    public static boolean showNSFW(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);
    }

    public static List<ChanBoard> getBoardsByType(Context context, BoardType boardType) {
        initBoards(context);
        return boardsByType.get(boardType);
	}

	public static ChanBoard getBoardByCode(Context context, String boardCode) {
        initBoards(context);
        return boardByCode.get(boardCode);
	}
	
	public static synchronized void initBoards(Context ctx) {
        if (boards != null && boards.size() > 0) {
            return;
        }

        if (DEBUG) Log.i(TAG, "Initializing boards");
        boards = new ArrayList<ChanBoard>();
        safeBoards = new ArrayList<ChanBoard>();
        boardsByType = new HashMap<BoardType, List<ChanBoard>>();
        boardByCode = new HashMap<String, ChanBoard>();
        relatedBoards = new HashMap<String, List<ChanBoard>>();

        String[][] boardCodesByType = initBoardCodes(ctx);

        for (String[] boardCodesForType : boardCodesByType) {
            BoardType boardType = BoardType.valueOf(boardCodesForType[0]);
            List<ChanBoard> boardsForType = new ArrayList<ChanBoard>();
            for (int i = 1; i < boardCodesForType.length; i+=2) {
                String boardCode = boardCodesForType[i];
                String boardName = boardCodesForType[i+1];
                boolean workSafe = !(boardType == BoardType.ADULT || boardType == BoardType.MISC);
                int iconId = getImageResourceId(boardCode);
                ChanBoard b = new ChanBoard(boardType, boardName, boardCode, iconId, workSafe, true, false);
                boardsForType.add(b);
                boards.add(b);
                if (workSafe)
                    safeBoards.add(b);
                boardByCode.put(boardCode, b);
            }
            boardsByType.put(boardType, boardsForType);
        }

        Collections.sort(boards, new Comparator<ChanBoard>() {
            @Override
            public int compare(ChanBoard lhs, ChanBoard rhs) {
                return lhs.link.compareToIgnoreCase(rhs.link);
            }
        });
        Collections.sort(safeBoards, new Comparator<ChanBoard>() {
            @Override
            public int compare(ChanBoard lhs, ChanBoard rhs) {
                return lhs.link.compareToIgnoreCase(rhs.link);
            }
        });

        String[][] relatedBoardCodes = initRelatedBoards();
        for (String[] relatedBoardCodeArray : relatedBoardCodes) {
            String boardCode = relatedBoardCodeArray[0];
            List<ChanBoard> relatedBoardList = new ArrayList<ChanBoard>();
            for (int i = 1; i < relatedBoardCodeArray.length; i++) {
                String relatedBoardCode = relatedBoardCodeArray[i];
                ChanBoard relatedBoard = boardByCode.get(relatedBoardCode);
                relatedBoardList.add(relatedBoard);
            }
            relatedBoards.put(boardCode, relatedBoardList);
            if (DEBUG) Log.i(TAG, "Initialized /" + boardCode + "/ with " + relatedBoardList.size() + " related boards");
        }

    }

    public static boolean isImagelessSticky(String boardCode, long postNo) {
        if (boardCode.equals("s") && postNo == 9112225)
            return true;
        if (boardCode.equals("gif") && (postNo == 5405329 || postNo == 5412288))
            return true;
        return false;
    }

    public int getImageResourceId() {
        return getImageResourceId(link);
    }

    public static int getImageResourceId(String boardCode) {
        return getImageResourceId(boardCode, 0);
    }

    public static int getIndexedImageResourceId(String boardCode, int index) {
        return getImageResourceId(boardCode, 0, index);
    }

    public static String getIndexedImageDrawableUrl(String boardCode, int index) {
        return "drawable://" + getIndexedImageResourceId(boardCode, index);
    }
    public static int getImageResourceId(String boardCode, long postNo) {
        return getImageResourceId(boardCode, postNo, -1);
    }

    public static int getRandomImageResourceId(String boardCode, long postNo) {
        return ChanBoard.getImageResourceId(boardCode, postNo, (int)(Math.random() * NUM_DEFAULT_IMAGES_PER_BOARD));
    }

    public int getRandomImageResourceId() {
        return ChanBoard.getRandomImageResourceId(link, 0);
    }

    public static int getImageResourceId(String boardCode, long postNo, int index) { // allows special-casing first (usually sticky) and multiple
        int imageId;
        String fileRoot;
        if (index == 0)
            fileRoot = boardCode;
        else if (index > 0)
            fileRoot = boardCode + "_" + (index+1);
        else if (isImagelessSticky(boardCode, postNo))
            fileRoot = boardCode + "_" + postNo;
        else
            fileRoot = boardCode;
        if (boardCode.equals("3") || boardCode.equals("int")) // avoid collisions
            fileRoot = "board_" + fileRoot;
        try {
            imageId = R.drawable.class.getField(fileRoot).getInt(null);
        } catch (Exception e) {
            try {
                fileRoot = boardCode;
                if (boardCode.equals("3") || boardCode.equals("int")) // avoid collisions
                    fileRoot = "board_" + fileRoot;
                imageId = R.drawable.class.getField(fileRoot).getInt(null);
            } catch (Exception e1) {
                imageId = R.drawable.stub_image;
            }
        }
        return imageId;
    }

    private static String[][] initRelatedBoards() {
        String[][] relatedBoardCodes = {
                { "a", "c", "w", "m", "cgl", "cm", "jp", "vg", "vp", "co", "tv", "h", "d", "e", "y", "u", "d", "t" },
                { "c", "a", "w", "cm", "vp", "mlp", "e", "u" },
                { "w", "a", "wg", "p" },
                { "m", "a", "n", "o", "k", "tg", "toy" },
                { "cgl", "fa", "jp", "tv", "co" },
                { "cm", "a", "c", "fit", "hm", "y" },
                { "n", "o", "trv", "g" },
                { "jp", "cgl", "a", "co", "i", "h" },
                { "vp", "mlp", "co", "tv", "toy" },
                { "v", "vg", "vr", "g", "o", "k" },
                { "vg", "v", "vr", "g", "o", "k", "sp", "asp" },
                { "vr", "vg", "v", "g", "diy", "tg" },
                { "co", "a", "cgl", "vp", "tv", "t" },
                { "g", "sci", "o", "k", "diy", "v", "n" },
                { "tv", "co", "a", "lit", "wsg", "t" },
                { "k", "m", "o", "toy", "g", "out" },
                { "o", "g", "k", "out", "n" },
                { "an", "toy", "p", "vp" },
                { "tg", "vr", "toy", "diy", "po" },
                { "sp", "asp", "out", "vg", "fit", "k" },
                { "asp", "sp", "out", "fit", "p", "n", "vg" },
                { "int", "trv", "jp", "adv", "pol", "q", "b" },
                { "out", "o", "fit", "k", "n", "p", "trv" },
                { "i", "po", "p", "ic", "3", "gd", "jp", "e" },
                { "po", "jp", "tg", "diy", "i", "ic", "3", "gd" },
                { "p", "out", "tv", "an", "ic", "wg", "s", "hr" },
                { "ck", "jp", "adv", "fit", "int", "trv" },
                { "ic", "i", "po", "adv", "gd", "3", "diy" },
                { "wg", "w", "p", "ic", "gd", "3" },
                { "mu", "lit", "tv", "p", "ic" },
                { "fa", "cgl", "p", "ic", "adv", "diy" },
                { "toy", "an", "jp", "vp", "co" },
                { "3", "i", "ic", "po", "gd" },
                { "gd", "i", "ic", "p", "po" },
                { "diy", "n", "o", "k", "po", "gd", "toy" },
                { "wsg", "tv", "co", "wg", "gif", "b" },
                { "q", "adv", "lgbt", "wsg", "pol", "r9k", "soc", "r", "b" },
                { "trv", "int", "pol", "p", "wg", "soc", "x" },
                { "fit", "cm", "c", "ck", "out", "sp", "asp", "hm", "s", "hc" },
                { "x", "p", "int", "trv", "lit", "adv" },
                { "adv", "q", "trv", "x", "ic", "soc", "r9k", "s4s", "pol", "b" },
                { "lgbt", "c", "cm", "adv", "s", "hm", "y", "u", "q", "soc", "s4s", "pol" },
                { "mlp", "vp", "co", "tv", "toy", "d", "b" },
                { "s", "hc", "e", "hr", "h", "t", "u", "gif", "fa", "fit", "r", "b" },
                { "hc", "s", "h", "d", "gif", "r", "b", "t", "b" },
                { "hm", "cm", "y", "fit", "lgbt", "fa", "b" },
                { "h", "d", "e", "y", "u", "a", "c", "t" },
                { "e", "h", "c", "s", "u", "d" },
                { "u", "e", "c", "s", "lgbt", "h", "d" },
                { "d", "h", "hc", "t", "mlp" },
                { "y", "hm", "cm", "h", "hd", "lgbt", "fit", "fa" },
                { "t", "tv", "co", "a", "hc", "h", "hd", "r" },
                { "hr", "p", "gif", "s", "hc" },
                { "gif", "wsg", "hc", "s", "hr", "tv", "b" },
                { "b", "pol", "int", "hc", "s", "q", "soc", "s4s", "r9k", "adv", "a", "v", "tg" },
                { "r", "r9k", "t", "hc", "soc", "b", "s" },
                { "r9k", "b", "s4s", "q", "soc", "adv" },
                { "pol", "int", "b", "s4s", "q" },
                { "soc", "adv", "r9k", "b", "trv" },
                { "s4s", "pol", "b", "q" }
        };
        return relatedBoardCodes;
    }

    private static String[][] initBoardCodes(Context ctx) {
        String[][] boardCodesByType = {

                {   BoardType.WATCHLIST.toString(),
                        WATCH_BOARD_CODE, ctx.getString(R.string.board_watch),
                        WATCHLIST_BOARD_CODE, ctx.getString(R.string.board_watch)
                },
                {   BoardType.POPULAR.toString(),
                        POPULAR_BOARD_CODE, ctx.getString(R.string.board_popular)
                },
                {   BoardType.LATEST.toString(),
                        LATEST_BOARD_CODE, ctx.getString(R.string.board_latest)
                },
                {   BoardType.LATEST_IMAGES.toString(),
                        LATEST_IMAGES_BOARD_CODE, ctx.getString(R.string.board_latest_images)
                },
                {   BoardType.JAPANESE_CULTURE.toString(),
                        "a", ctx.getString(R.string.board_a),
                        "c", ctx.getString(R.string.board_c),
                        "w", ctx.getString(R.string.board_w),
                        "m", ctx.getString(R.string.board_m),
                        "cgl", ctx.getString(R.string.board_cgl),
                        "cm", ctx.getString(R.string.board_cm),
                        "n", ctx.getString(R.string.board_n),
                        "jp", ctx.getString(R.string.board_jp),
                        "vp", ctx.getString(R.string.board_vp)
                },
                {   BoardType.INTERESTS.toString(),
                        "v", ctx.getString(R.string.board_v),
                        "vg", ctx.getString(R.string.board_vg),
                        "vr", ctx.getString(R.string.board_vr),
                        "co", ctx.getString(R.string.board_co),
                        "g", ctx.getString(R.string.board_g),
                        "tv", ctx.getString(R.string.board_tv),
                        "k", ctx.getString(R.string.board_k),
                        "o", ctx.getString(R.string.board_o),
                        "an", ctx.getString(R.string.board_an),
                        "tg", ctx.getString(R.string.board_tg),
                        "sp", ctx.getString(R.string.board_sp),
                        "asp", ctx.getString(R.string.board_asp),
                        "sci", ctx.getString(R.string.board_sci),
                        "int", ctx.getString(R.string.board_int),
                        "out", ctx.getString(R.string.board_out)
                },
                {   BoardType.CREATIVE.toString(),
                        "i", ctx.getString(R.string.board_i),
                        "po", ctx.getString(R.string.board_po),
                        "p", ctx.getString(R.string.board_p),
                        "ck", ctx.getString(R.string.board_ck),
                        "ic", ctx.getString(R.string.board_ic),
                        "wg", ctx.getString(R.string.board_wg),
                        "mu", ctx.getString(R.string.board_mu),
                        "fa", ctx.getString(R.string.board_fa),
                        "toy", ctx.getString(R.string.board_toy),
                        "3", ctx.getString(R.string.board_3),
                        "gd", ctx.getString(R.string.board_gd),
                        "diy", ctx.getString(R.string.board_diy),
                        "wsg", ctx.getString(R.string.board_wsg)
                },
                {   BoardType.OTHER.toString(),
                        "q", ctx.getString(R.string.board_q),
                        "trv", ctx.getString(R.string.board_trv),
                        "fit", ctx.getString(R.string.board_fit),
                        "x", ctx.getString(R.string.board_x),
                        "lit", ctx.getString(R.string.board_lit),
                        "adv", ctx.getString(R.string.board_adv),
                        "lgbt", ctx.getString(R.string.board_lgbt),
                        "mlp", ctx.getString(R.string.board_mlp)
                },
                {   BoardType.ADULT.toString(),
                        "s", ctx.getString(R.string.board_s),
                        "hc", ctx.getString(R.string.board_hc),
                        "hm", ctx.getString(R.string.board_hm),
                        "h", ctx.getString(R.string.board_h),
                        "e", ctx.getString(R.string.board_e),
                        "u", ctx.getString(R.string.board_u),
                        "d", ctx.getString(R.string.board_d),
                        "y", ctx.getString(R.string.board_y),
                        "t", ctx.getString(R.string.board_t),
                        "hr", ctx.getString(R.string.board_hr),
                        "gif", ctx.getString(R.string.board_gif)
                },
                {   BoardType.MISC.toString(),
                        "b", ctx.getString(R.string.board_b),
                        "r", ctx.getString(R.string.board_r),
                        "r9k", ctx.getString(R.string.board_r9k),
                        "pol", ctx.getString(R.string.board_pol),
                        "soc", ctx.getString(R.string.board_soc),
                        "s4s", ctx.getString(R.string.board_s4s)
                }

        };
        return boardCodesByType;
    }

    public static void preloadUncachedBoards(Context context) {
        List<ChanBoard> boards = ChanBoard.getBoards(context);
        for (ChanBoard board : boards) {
            if (!ChanFileStorage.isBoardCachedOnDisk(context, board.link)) { // if user never visited board before
                if (DEBUG) Log.i(TAG, "Starting load service for uncached board " + board.link);
                FetchChanDataService.scheduleBackgroundBoardFetch(context, board.link);
                break; // don't schedule more than one per call to avoid overloading
            }
        }
    }

    static private Set<String> spoilerBoardCodes = new HashSet<String>();
    static public boolean hasSpoiler(String boardCode) {
        if (spoilerBoardCodes.isEmpty()) {
            synchronized (spoilerBoardCodes) {
                String[] spoilers = { "a", "m", "u", "v", "vg", "r9k", "co", "jp", "lit", "mlp", "tg", "tv", "vp" };
                for (int i = 0; i < spoilers.length; i++)
                    spoilerBoardCodes.add(spoilers[i]);
            }
        }
        return spoilerBoardCodes.contains(boardCode);
    }

    static public boolean hasName(String boardCode) {
        if (boardCode.equals("b") || boardCode.equals("soc") || boardCode.equals("q"))
            return false;
        else
            return true;
    }

    static public boolean hasSubject(String boardCode) {
        if (boardCode.equals("b") || boardCode.equals("soc"))
            return false;
        else
            return true;
    }

    static public boolean requiresThreadSubject(String boardCode) {
        if (boardCode.equals("q"))
            return true;
        else
            return false;
    }

    static public boolean requiresThreadImage(String boardCode) {
        if (boardCode.equals("q"))
            return false;
        else
            return true;
    }

    static public boolean allowsBump(String boardCode) {
        if (boardCode.equals("q"))
            return false;
        else
            return true;
    }

    /*
    /i - lots of stuff
    */

    static public final String SPOILER_THUMBNAIL_IMAGE_ROOT = "http://static.4chan.org/image/spoiler-";
    static public final String SPOILER_THUMBNAIL_IMAGE_EXTENSION = ".png";
    static public final Map<String, Integer> spoilerImageCount = new HashMap<String, Integer>();
    static public final Random spoilerGenerator = new Random();
    static public String spoilerThumbnailUrl(String boardCode) {
        if (spoilerImageCount.isEmpty()) {
            spoilerImageCount.put("m", 4);
            spoilerImageCount.put("co", 5);
            spoilerImageCount.put("tg", 3);
            spoilerImageCount.put("tv", 5);
        }
        int spoilerImages = spoilerImageCount.containsKey(boardCode) ? spoilerImageCount.get(boardCode) : 1;
        if (spoilerImages > 1) {
            int spoilerImageNum = spoilerGenerator.nextInt(spoilerImages) + 1;
            return SPOILER_THUMBNAIL_IMAGE_ROOT + boardCode + spoilerImageNum + SPOILER_THUMBNAIL_IMAGE_EXTENSION;
        }
        else {
            return SPOILER_THUMBNAIL_IMAGE_ROOT + boardCode + SPOILER_THUMBNAIL_IMAGE_EXTENSION;
        }
    }

    static public boolean isAsciiOnlyBoard(String boardCode) {
        if (boardCode.equals("q") || boardCode.equals("r9k"))
            return true;
        else
            return false;
    }

    public Object[] makeRow() { // for board selector
        return ChanThread.makeBoardRow(link, name, getRandomImageResourceId());
    }

    /*
    public Object[] makeThreadAdRow(Context context, int pos) {
        ChanAd ad = ChanAd.randomAd(workSafe, pos);
        return ChanThread.makeAdRow(context, ad);
    }
    */

    public Object[] makePostAdRow(Context context, int pos) {
        ChanAd ad = ChanAd.randomAd(workSafe, pos);
        return ChanPost.makeAdRow(context, link, ad);
    }

    public Object[] makePostRelatedThreadsHeaderRow(Context context) {
        return ChanPost.makeTitleRow(link, context.getString(R.string.thread_related_threads_title).toUpperCase());
    }

    public Object[] makePostRelatedBoardsHeaderRow(Context context) {
        return ChanPost.makeTitleRow(link, context.getString(R.string.board_related_boards_title).toUpperCase());
    }

    public List<Object[]> makePostRelatedThreadsRows(long threadNo) {
        List<Object[]> rows = new ArrayList<Object[]>();
        if (threadNo == 0 || threads == null)
            return rows;
        synchronized (threads) {
            if (threads.length == 0)
                return rows;
            rows = makePostRelatedThreadRows(threadNo, NUM_RELATED_THREADS);
            if (rows.size() == 0)
                rows = makePostNextThreadRows(threadNo, NUM_RELATED_THREADS);
            return rows;
        }
    }

    private List<Object[]> makePostRelatedThreadRows(long threadNo, int numThreads) {
        final List<Object[]> rows = new ArrayList<Object[]>();

        // first find the thread
        int threadPos = findThreadPos(threadNo);
        if (threadPos == -1)
            return rows;
        final ChanPost thread = threads[threadPos];

        // count thread relevance, relevance = number of keywords in common
        final List<ChanPost> relatedList = new ArrayList<ChanPost>();
        final Map<ChanPost, Integer> relatedMap = new HashMap<ChanPost, Integer>();
        final Set<String> keywords = thread.keywords();
        for (int i = 0; i < threads.length; i++) {
            ChanPost relatedThread = threads[i];
            if (thread.no == relatedThread.no)
                continue; // it's me!
            if (relatedThread.sticky > 0)
                continue;
            int relatedCount = relatedThread.keywordRelevance(keywords);
            if (relatedCount > 0) {
                relatedList.add(relatedThread);
                relatedMap.put(relatedThread, relatedCount);
            }
        }

        // order by relevance, most relevant first
        Collections.sort(relatedList, new Comparator<ChanPost>() {
            @Override
            public int compare(ChanPost lhs, ChanPost rhs) {
                return relatedMap.get(rhs) - relatedMap.get(lhs);
            }
        });

        // finally make the thread-cursor-usable list
        for (int i = 0; i < relatedList.size() && i < numThreads; i++) {
            rows.add(relatedList.get(i).makeThreadLinkRow());
        }
        return rows;
    }

    private int findThreadPos(long threadNo) {
        // find position of thread in list
        int threadPos = -1;
        for (int i = 0; i < threads.length; i++) {
            ChanPost thread = threads[i];
            if (thread != null && thread.no == threadNo) {
                threadPos = i;
                break;
            }
        }
        return threadPos;
    }

    private List<Object[]> makePostNextThreadRows(long threadNo, int numThreads) {
        List<Object[]> rows = new ArrayList<Object[]>();
        int threadPos = findThreadPos(threadNo);
        if (threadPos == -1) { // didn't find it, default to first item
            threadPos = 0;
        }

        // find prev and next if any
        for (int i = threadPos + 1; i < threads.length && i < threadPos + numThreads + 1; i++) {
            ChanPost nextThread = threads[i];
            rows.add(nextThread.makeThreadLinkRow());
        }
        return rows;
    }

    public Object[] makePostBoardLinkRow() {
        return ChanPost.makeBoardLinkRow(this);
    }

    public void updateCountersAfterLoad() {
    	if (loadedThreads.length == 0) {
    		return;
    	}
    	Map<Long, ChanPost> currentThreads = new HashMap<Long, ChanPost>();
    	for (ChanPost thread : threads) {
    		currentThreads.put(thread.no, thread);
    	}
    	this.newThreads = 0;
    	this.updatedThreads = 0;
    	for (ChanPost newPost : loadedThreads) {
    		if (currentThreads.containsKey(newPost.no)) {
    			ChanPost currentPost = currentThreads.get(newPost.no);
    			if (currentPost.replies != newPost.replies) {
    				updatedThreads++;
    			}
    		} else {
    			newThreads++;
    		}
    	}
    	if (DEBUG) Log.i(TAG, "Updated board " + name + ", " + newThreads + " new threads, " + updatedThreads + " updated threads.");
    }

    public static void setupActionBarBoardSpinner(final Activity activity, final Menu menu, final String currentBoardCode) {
        if (DEBUG) Log.i(BoardSelectorActivity.TAG, "setupActionBarSpinner " + activity + " " + menu + " boardCode=" + currentBoardCode);
        MenuItem item = menu.findItem(R.id.board_jump_spinner_menu);
        if (item == null)
            return;
        Spinner spinner = (Spinner)item.getActionView();
        spinner.setOnItemSelectedListener(null);
        int position = 0;
        if (currentBoardCode == null || currentBoardCode.isEmpty() || activity instanceof BoardSelectorActivity) {
            position = 0;
        }
        else {
            SpinnerAdapter spinnerAdapter = spinner.getAdapter();
            for (int i = 0; i < spinnerAdapter.getCount(); i++) {
                String boardText = (String)spinnerAdapter.getItem(i);
                if (boardText.matches("/" + currentBoardCode + "/.*")) {
                    position = i;
                    break;
                }
            }
        }
        spinner.setSelection(position, false);
        spinner.setOnItemSelectedListener(new ActionBarSpinnerHandler(activity, currentBoardCode));
    }

    public static void resetActionBarSpinner(Menu menu) {
        MenuItem boardJump = menu.findItem(R.id.board_jump_spinner_menu);
        if (boardJump != null && boardJump.getActionView() != null)
            ((Spinner)boardJump.getActionView()).setSelection(0, false);
    }

    private static class ActionBarSpinnerHandler implements AdapterView.OnItemSelectedListener {

        private Activity activity;
        private String createdWithBoardCode = null;

        public ActionBarSpinnerHandler(final Activity activity, final String createdWithBoardCode) {
            this.activity = activity;
            this.createdWithBoardCode = createdWithBoardCode;
        }

        protected void dispatchToBoardSelector(AdapterView<?> parent, BoardSelectorTab tab) {
            if (activity instanceof BoardSelectorActivity)
            { // special case change tab
                /*
                if (parent instanceof Spinner) {
                    Spinner spinner = (Spinner)parent;
                    spinner.setSelection(0, false);
                }
                */
                ((BoardSelectorActivity)activity).selectTab(tab);
            }
            else {
                BoardSelectorActivity.startActivity(activity, tab);
            }
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { // for action bar spinner
            if (position < 0)
                return;
            String boardAsMenu = (String) parent.getItemAtPosition(position);
            if (DEBUG) Log.i(BoardSelectorActivity.TAG, "onItemSelected boardSelected=" + boardAsMenu + " created with board=" + createdWithBoardCode);
            if (boardAsMenu == null
                    || boardAsMenu.isEmpty()
                    || boardAsMenu.equals(activity.getString(R.string.board_select))
                    || boardAsMenu.equals(activity.getString(R.string.board_select_abbrev)))
                return;
            else if (boardAsMenu.equals(activity.getString(R.string.board_watch))
                    || boardAsMenu.equals(activity.getString(R.string.board_watch_abbrev)))
                dispatchToBoardSelector(parent, BoardSelectorTab.WATCHLIST);
            else if (boardAsMenu.equals(activity.getString(R.string.board_type_recent))
                    || boardAsMenu.equals(activity.getString(R.string.board_type_recent_abbrev)))
                dispatchToBoardSelector(parent, BoardSelectorTab.RECENT);
            else {
                Pattern p = Pattern.compile("/([^/]*)/.*");
                Matcher m = p.matcher(boardAsMenu);
                if (!m.matches())
                    return;
                String boardCodeForJump = m.group(1);
                if (boardCodeForJump == null || boardCodeForJump.isEmpty() || boardCodeForJump.equals(createdWithBoardCode))
                    return;
                BoardActivity.startActivity(activity, boardCodeForJump);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { // for action bar spinner
        }

    }

    public boolean isVirtualBoard() {
        return isVirtualBoard(link);
    }

    public static boolean isVirtualBoard(String boardCode) {
        return WATCH_BOARD_CODE.equals(boardCode)
                || POPULAR_BOARD_CODE.equals(boardCode)
                || LATEST_BOARD_CODE.equals(boardCode)
                || LATEST_IMAGES_BOARD_CODE.equals(boardCode);
    }

    public static String getBestWidgetImageUrl(ChanPost thread, String backupBoardCode, int i) {
        return (thread != null && thread.tim > 0)
                ? thread.thumbnailUrl()
                : ChanBoard.getIndexedImageDrawableUrl(
                thread != null ? thread.board : backupBoardCode,
                i);
    }

    public List<ChanBoard> relatedBoards(Context context) {
        initBoards(context);
        if (isVirtualBoard())
            return new ArrayList<ChanBoard>();

        List<ChanBoard> boards = relatedBoards.get(link);
        if (DEBUG) Log.i(TAG, "Found " + (boards == null ? 0 : boards.size()) + " related boards for /" + link + "/");
        if (boards == null)
            return new ArrayList<ChanBoard>();

        boolean showAdult = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);
        List<ChanBoard> filteredBoards = new ArrayList<ChanBoard>();
        for (ChanBoard board : boards) {
            if (board != null && (board.workSafe || showAdult))
                filteredBoards.add(board);
        }

        Collections.shuffle(filteredBoards);
        List<ChanBoard> boardList = new ArrayList<ChanBoard>(NUM_RELATED_BOARDS);
        int j = 0;
        for (ChanBoard relatedBoard : filteredBoards) {
            if (j >= NUM_RELATED_BOARDS)
                break;
            if (!link.equals(relatedBoard.link)) {
                boardList.add(relatedBoard);
                j++;
            }
        }
        return boardList;
    }

    public boolean hasNewBoardData() {
        if (defData)
            return false;
        if (newThreads > 0)
            return true;
        if (updatedThreads > 0)
            return true;
        if (loadedThreads != null && loadedThreads.length > 0)
            return true;
        return false;
    }

    public boolean shouldSwapThreads() {
        if (loadedThreads == null || loadedThreads.length == 0)
            return false;
        if (threads == null || threads.length == 0)
            return true;
        if (threads[0] == null || threads[0].no <= 0)
            return true;
        return false;
    }

    public void swapLoadedThreads() {
        if (hasNewBoardData()) {
            synchronized (this) {
                threads = loadedThreads;
                loadedThreads = new ChanThread[0];
                newThreads = 0;
                updatedThreads = 0;
            }
        }
    }


}
