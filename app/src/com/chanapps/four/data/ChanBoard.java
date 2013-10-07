package com.chanapps.four.data;

import java.util.*;
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
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;

public class ChanBoard {

	public static final String TAG = ChanBoard.class.getSimpleName();

    private static final boolean DEBUG = false;
    private static final int NUM_DEFAULT_IMAGES_PER_BOARD = 3;
    private static final int NUM_RELATED_BOARDS = 3;
    //private static final int NUM_RELATED_THREADS = 3;

    public static final String WEB_HOME_URL = "http://www.4chan.org";
    public static final String WEB_ROOT_URL = "http://boards.4chan.org";

    public static final String BOARD_CODE = "boardCode";
    public static final String ALL_BOARDS_BOARD_CODE = BoardType.ALL_BOARDS.boardCode();
    public static final String POPULAR_BOARD_CODE = BoardType.POPULAR.boardCode();
    public static final String LATEST_BOARD_CODE = BoardType.LATEST.boardCode();
    public static final String LATEST_IMAGES_BOARD_CODE = BoardType.LATEST_IMAGES.boardCode();
    public static final String WATCHLIST_BOARD_CODE = BoardType.WATCHLIST.boardCode();
    public static final String FAVORITES_BOARD_CODE = BoardType.FAVORITES.boardCode();
    public static final String META_BOARD_CODE = BoardType.META.boardCode();
    public static final String META_JAPANESE_CULTURE_BOARD_CODE = BoardType.JAPANESE_CULTURE.boardCode();
    public static final String META_INTERESTS_BOARD_CODE = BoardType.INTERESTS.boardCode();
    public static final String META_CREATIVE_BOARD_CODE = BoardType.CREATIVE.boardCode();
    public static final String META_OTHER_BOARD_CODE = BoardType.OTHER.boardCode();
    public static final String META_ADULT_BOARD_CODE = BoardType.ADULT.boardCode();
    public static final String META_MISC_BOARD_CODE = BoardType.MISC.boardCode();

    public static final String[] VIRTUAL_BOARDS = { ALL_BOARDS_BOARD_CODE, POPULAR_BOARD_CODE, LATEST_BOARD_CODE,
            LATEST_IMAGES_BOARD_CODE, WATCHLIST_BOARD_CODE, FAVORITES_BOARD_CODE,
            META_BOARD_CODE, META_JAPANESE_CULTURE_BOARD_CODE, META_INTERESTS_BOARD_CODE,
            META_CREATIVE_BOARD_CODE, META_OTHER_BOARD_CODE,
            META_ADULT_BOARD_CODE, META_MISC_BOARD_CODE };
    public static final String[] META_BOARDS = { ALL_BOARDS_BOARD_CODE, META_BOARD_CODE,
            META_JAPANESE_CULTURE_BOARD_CODE, META_INTERESTS_BOARD_CODE,
            META_CREATIVE_BOARD_CODE, META_OTHER_BOARD_CODE,
            META_ADULT_BOARD_CODE, META_MISC_BOARD_CODE };
    public static final String[] POPULAR_BOARDS = { POPULAR_BOARD_CODE, LATEST_BOARD_CODE, LATEST_IMAGES_BOARD_CODE };

    private static final Set<String> removedBoards = new HashSet<String>();
    private static final String[] REMOVED_BOARDS = { "q" };
    static {
        removedBoards.clear();
        for (String boardCode : REMOVED_BOARDS)
            removedBoards.add(boardCode);
    }
    public static boolean isRemoved(String boardCode) {
        return removedBoards.contains(boardCode);
    }

    public static final String DEFAULT_BOARD_CODE = "a";
    public static final String PAGE = "pageNo";
    public static final String BOARD_CATALOG = "boardCatalog";

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
	public ChanThread threads[] = new ChanThread[0];
	public ChanThread loadedThreads[] = new ChanThread[0];
	public int newThreads = 0;
	public int updatedThreads = 0;
    public long lastFetched;
    public long lastSwapped;
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

    public static boolean boardNeedsRefresh(Context context, String boardCode, boolean forceRefresh) {
        ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
        if (board == null || board.defData)
            return true;
        else if (board.threads == null || board.threads.length == 0)
            return true;
        else if (board.threads[0] == null || board.threads[0].defData)
            return true;
        else if (!board.isCurrent())
            return true;
        else if (forceRefresh)
            return true;
        else
            return false;
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
		return new ArrayList<ChanBoard>(boards);
	}

    public static List<ChanBoard> getBoardsRespectingNSFW(Context context) {
        initBoards(context);
        return new ArrayList<ChanBoard>(showNSFW(context) ? boards : safeBoards);
    }

    public static List<ChanBoard> getNewThreadBoardsRespectingNSFW(Context context) {
        initBoards(context);
        List<ChanBoard> source = new ArrayList<ChanBoard>(showNSFW(context) ? boards : safeBoards);
        List<ChanBoard> filtered = new ArrayList<ChanBoard>();
        for (ChanBoard b : source)
            if (!b.isVirtualBoard())
                filtered.add(b);
        return filtered;
    }

    public static List<ChanBoard> getPickFavoritesBoardsRespectingNSFW(Context context) {
        List<ChanBoard> source = getNewThreadBoardsRespectingNSFW(context);
        List<ChanBoard> filtered = new ArrayList<ChanBoard>();
        ChanBoard board = ChanFileStorage.loadBoardData(context, FAVORITES_BOARD_CODE);
        if (board == null || board.defData || board.threads == null)
            return source;
        ChanPost[] threads = board.threads;
        Set<String> boardCodes = new HashSet<String>(threads.length);
        for (ChanPost thread : threads)
            boardCodes.add(thread.board);
        for (ChanBoard b : source)
            if (!boardCodes.contains(b.link))
                filtered.add(b);
        return filtered;
    }

    public static boolean showNSFW(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);
    }

    public static List<ChanBoard> getBoardsByType(Context context, BoardType boardType) {
        initBoards(context);
        //boolean showNSFW = showNSFW(context);
        //if (BoardType.ALL_BOARDS == boardType && showNSFW)
        //    return new ArrayList<ChanBoard>(boards);
        //else if (BoardType.ALL_BOARDS == boardType && !showNSFW)
        //    return new ArrayList<ChanBoard>(safeBoards);
        //else
            return new ArrayList<ChanBoard>(boardsByType.get(boardType));
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
                int iconId = getImageResourceId(boardCode, 0, 0);
                ChanBoard b = new ChanBoard(boardType, boardName, boardCode, iconId, workSafe, true, false);
                if (DEBUG) Log.i(TAG, "Added board /" + boardCode + "/ " + boardName);
                boardsForType.add(b);
                if (!boardByCode.containsKey(b.link)) {
                    boards.add(b);
                    if (workSafe)
                        safeBoards.add(b);
                    boardByCode.put(boardCode, b);
                }
            }
            boardsByType.put(boardType, boardsForType);
            if (DEBUG) Log.i(TAG, "Put boardsByType(" + boardType.boardCode() + ") as " + Arrays.toString(boardsForType.toArray()));
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

        initBoardDrawables();
    }

    public static int imagelessStickyDrawableId(String boardCode, long threadNo) {
        if (boardCode.equals("s") && threadNo == 9112225)
            return R.drawable.s_9112225;
        else if (boardCode.equals("gif") && threadNo == 5404329)
            return R.drawable.gif_5405329;
        else if (boardCode.equals("gif") && threadNo == 5412288)
            return R.drawable.gif_5412288;
        else
            return 0;
    }

    public static String getIndexedImageDrawableUrl(String boardCode, int index) {
        return "drawable://" + getImageResourceId(boardCode, 0, index);
    }

    public static int getRandomImageResourceId(String boardCode, long postNo) {
        return ChanBoard.getImageResourceId(boardCode, postNo, (int)(postNo % NUM_DEFAULT_IMAGES_PER_BOARD));
    }

    protected static final int STUB_IMAGE_ID = R.drawable.stub_image;

    public static int getImageResourceId(String boardCode, long postNo, int index) { // allows special-casing first (usually sticky) and multiple
        int imageId = imagelessStickyDrawableId(boardCode, postNo);
        if (imageId > 0)
            return imageId;
        int[] imageIds = boardDrawables.get(boardCode);
        if (imageIds == null || imageIds.length == 0)
            return STUB_IMAGE_ID;
        if (index >= 0 && index < 3)
            return imageIds[index];
        return imageIds[0];
    }

    public String getDescription(Context context) {
        return getDescription(context, link);
    }

    public static String getDescription(Context context, String boardCode) {
        String stringName = "board_desc_" + boardCode;
        try {
            int id = context.getResources().getIdentifier(stringName, "string", context.getPackageName());
            return context.getString(id);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't find board description for boardCode=" + boardCode);
            return "";
        }
    }

    public String getName(Context context) {
        return getName(context, link);
    }

    public static String getName(Context context, String boardCode) {
        String stringName = "board_" + boardCode;
        try {
            int id = context.getResources().getIdentifier(stringName, "string", context.getPackageName());
            return context.getString(id);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't find board description for boardCode=" + boardCode);
            return "";
        }
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
                { "lit", "tv", "p", "mu", "co", "a", "trv", "sci", "adv" },
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

    protected static Map<String, int[]> boardDrawables = new HashMap<String, int[]>();
    
    private static String[][] initBoardCodes(Context ctx) {
        String[][] boardCodesByType = {
                {
                    BoardType.ALL_BOARDS.toString(),
                        ALL_BOARDS_BOARD_CODE, ctx.getString(R.string.board_all_boards),
                        "a", ctx.getString(R.string.board_a),
                        "c", ctx.getString(R.string.board_c),
                        "w", ctx.getString(R.string.board_w),
                        "m", ctx.getString(R.string.board_m),
                        "cgl", ctx.getString(R.string.board_cgl),
                        "cm", ctx.getString(R.string.board_cm),
                        "n", ctx.getString(R.string.board_n),
                        "jp", ctx.getString(R.string.board_jp),
                        "vp", ctx.getString(R.string.board_vp),
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
                        "out", ctx.getString(R.string.board_out),
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
                        "wsg", ctx.getString(R.string.board_wsg),
                        "q", ctx.getString(R.string.board_q),
                        "trv", ctx.getString(R.string.board_trv),
                        "fit", ctx.getString(R.string.board_fit),
                        "x", ctx.getString(R.string.board_x),
                        "lit", ctx.getString(R.string.board_lit),
                        "adv", ctx.getString(R.string.board_adv),
                        "lgbt", ctx.getString(R.string.board_lgbt),
                        "mlp", ctx.getString(R.string.board_mlp),
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
                        "gif", ctx.getString(R.string.board_gif),
                        "b", ctx.getString(R.string.board_b),
                        "r", ctx.getString(R.string.board_r),
                        "r9k", ctx.getString(R.string.board_r9k),
                        "pol", ctx.getString(R.string.board_pol),
                        "soc", ctx.getString(R.string.board_soc),
                        "s4s", ctx.getString(R.string.board_s4s)
                },
                {   BoardType.WATCHLIST.toString(),
                        WATCHLIST_BOARD_CODE, ctx.getString(R.string.board_watch),
                },
                {   BoardType.FAVORITES.toString(),
                        FAVORITES_BOARD_CODE, ctx.getString(R.string.board_favorites),
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
                {
                        BoardType.META.toString(),
                        META_BOARD_CODE, ctx.getString(R.string.board_meta),
                },
                {   BoardType.JAPANESE_CULTURE.toString(),
                        META_JAPANESE_CULTURE_BOARD_CODE, ctx.getString(R.string.board_type_japanese_culture),
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
                        META_INTERESTS_BOARD_CODE, ctx.getString(R.string.board_type_interests),
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
                        META_CREATIVE_BOARD_CODE, ctx.getString(R.string.board_type_creative),
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
                        META_OTHER_BOARD_CODE, ctx.getString(R.string.board_type_other),
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
                        META_ADULT_BOARD_CODE, ctx.getString(R.string.board_type_adult),
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
                        META_MISC_BOARD_CODE, ctx.getString(R.string.board_type_misc),
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
            if (!board.isMetaBoard() && !ChanFileStorage.isBoardCachedOnDisk(context, board.link)) { // if user never visited board before
                if (DEBUG) Log.i(TAG, "Starting load service for uncached board " + board.link);
                FetchChanDataService.scheduleBoardFetch(context, board.link, false, true);
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

    public Object[] makeRow(Context context) { // for board selector
        return makeRow(context, 0);
    }

    public Object[] makeRow(Context context, long threadNo) { // for board selector
        return ChanThread.makeBoardRow(context, link, getName(context), getRandomImageResourceId(link, threadNo));
    }
    /*
    public Object[] makeThreadAdRow(Context context, int pos) {
        ChanAd ad = ChanAd.randomAd(workSafe);
        return ChanThread.makeAdRow(context, link, ad);
    }

    public Object[] makePostAdRow(Context context, int pos) {
        ChanAd ad = ChanAd.randomAd(workSafe);
        return ChanPost.makeAdRow(context, link, ad);
    }
    */
    public Object[] makePostRelatedBoardsHeaderRow(Context context) {
        return ChanPost.makeTitleRow(link, context.getString(R.string.board_related_boards_title),
                String.format(context.getString(R.string.board_related_boards_desc), link));
    }

    /*
    public Object[] makePostRelatedThreadsHeaderRow(Context context) {
        return ChanPost.makeTitleRow(link, context.getString(R.string.thread_related_threads_title),
                String.format(context.getString(R.string.thread_related_threads_desc), link));
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
            if (relatedThread.isDead)
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
    */
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
    /*
    private List<Object[]> makePostNextThreadRows(long threadNo, int numThreads) {
        List<Object[]> rows = new ArrayList<Object[]>();
        int threadPos = findThreadPos(threadNo);
        if (threadPos == -1) { // didn't find it, default to first item
            threadPos = 0;
        }
        int threadsLeft = numThreads;
        for (int i = threadPos + 1; i < threads.length && threadsLeft > 0; i++) {
            ChanPost thread = threads[i];
            if (thread.isDead || thread.defData)
                continue;
            rows.add(thread.makeThreadLinkRow());
            threadsLeft--;
        }
        return rows;
    }
     */
    public Object[] makePostBoardLinkRow(Context context, long threadNo) {
        return ChanPost.makeBoardLinkRow(context, this, threadNo);
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

    public boolean isVirtualBoard() {
        return isVirtualBoard(link);
    }

    public static boolean isVirtualBoard(String boardCode) {
        for (String virtualBoardCode : VIRTUAL_BOARDS)
            if (virtualBoardCode.equals(boardCode))
                return true;
        return false;
    }

    public boolean isMetaBoard() {
        return isMetaBoard(link);
    }

    public static boolean isMetaBoard(String boardCode) {
        for (String metaBoardCode : META_BOARDS)
            if (metaBoardCode.equals(boardCode))
                return true;
        return false;
    }

    public boolean isPopularBoard() {
        return isPopularBoard(link);
    }

    public static boolean isPopularBoard(String boardCode) {
        for (String popularBoardCode : POPULAR_BOARDS)
            if (popularBoardCode.equals(boardCode))
                return true;
        return false;
    }

    private static final String[] fastBoards = { "a", "b", "v", "vr" };
    private static final Set<String> fastBoardSet = new HashSet<String>(Arrays.asList(fastBoards));

    public boolean isFastBoard() {
        if (link == null)
            return false;
        if (fastBoardSet.contains(link))
            return true;
        return false;
    }

    public static String getBestWidgetImageUrl(ChanPost thread, String backupBoardCode, int i) {
        return (thread != null && thread.tim > 0)
                ? thread.thumbnailUrl()
                : ChanBoard.getIndexedImageDrawableUrl(
                thread != null ? thread.board : backupBoardCode,
                i);
    }
    /*
    public List<ChanBoard> relatedBoards(Context context) {
        return relatedBoards(context, 0);
    }
    */
    public List<ChanBoard> relatedBoards(Context context, long threadNo) {
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

        if (threadNo <= 0)
            Collections.shuffle(filteredBoards);
        else
            Collections.rotate(filteredBoards, (int)threadNo); // preserve order
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

    public static boolean boardHasData(Context context, String boardCode) {
        ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
        return board != null && !board.defData && board.threads != null && board.threads.length > 0;
    }

    public boolean hasData() {
        return !defData && threads != null && threads.length > 0;
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

    protected final int MAX_THREADS_BEFORE_SWAP = 20;

    public boolean shouldSwapThreads() {
        if (loadedThreads == null || loadedThreads.length == 0)
            return false;
        if (threads == null || threads.length == 0)
            return true;
        if (threads[0] == null || threads[0].defData || threads[0].no <= 0)
            return true;
        if (threads.length > MAX_THREADS_BEFORE_SWAP)
            return true;
        if (!isSwapCurrent())
            return true;
        return false;
    }

    private boolean isSwapCurrent() {
        long diff = Math.abs(new Date().getTime() - lastSwapped);
        boolean swapCurrent;
        if (lastSwapped <= 0)
            swapCurrent = false;
        else if (diff > SWAP_DELAY_MS)
            swapCurrent = false;
        else
            swapCurrent = true;
        if (DEBUG) Log.i(TAG, "isSwapCurrent /" + link + "/ lastSwapped=" + lastSwapped + " diff=" + diff + " return=" + swapCurrent);
        return swapCurrent;
    }

    public void swapLoadedThreads() {
        boolean hasNew = hasNewBoardData();
        if (DEBUG) Log.i(TAG, "swapLoadedThreads() hasNew=" + hasNew);
        if (hasNew) {
            synchronized (this) {
                threads = loadedThreads;
                loadedThreads = new ChanThread[0];
                newThreads = 0;
                updatedThreads = 0;
                lastSwapped = (new Date()).getTime();
            }
        }
    }

    public static String boardUrl(String boardCode) {
        if (boardCode == null || boardCode.isEmpty() || isVirtualBoard(boardCode))
            return WEB_HOME_URL;
        else
            return WEB_ROOT_URL + "/" + boardCode + "/";
    }

    public int getThreadIndex(String boardCode, long threadNo) {
        if (DEBUG) Log.i(TAG, "getThreadIndex /" + boardCode + "/" + threadNo);
        if (defData)
            return -1;
        if (threads == null)
            return -1;
        int index = -1;
        ChanPost thread;
        for (int i = 0; i < threads.length; i++) {
            if ((thread = threads[i]) == null)
                continue;
            if (thread.board == null)
                continue;
            if (!thread.board.equals(boardCode))
                continue;
            if (thread.no != threadNo)
                continue;
            index = i;
            break;
        }
        return index;
    }

    public boolean isCurrent() {
        FetchParams params = NetworkProfileManager.instance().getCurrentProfile().getFetchParams();
        long now = new Date().getTime();
        long interval = Math.abs(now - lastFetched);
        boolean current;
        if (lastFetched <= 0)
            current = false;
        else if (interval > params.refreshDelay)
            current = false;
        else
            current = true;
        if (DEBUG) Log.i(TAG, "isCurrent() /" + link + "/"
                + " lastFetched=" + lastFetched
                + " interval=" + interval
                + " refreshDelay=" + params.refreshDelay
                + " current=" + current
        );
        return current;
    }

    protected static final long SWAP_DELAY_MS = 300000L;

    public String refreshMessage() {
        StringBuffer msg = new StringBuffer();
        if (newThreads > 0) {
            msg.append("" + newThreads + " new thread");
            if (newThreads > 1) // + updatedThreads > 1) {
                msg.append("s");
        }
        else if (updatedThreads > 0) {
            msg.append("" + updatedThreads + " updated thread");
            if (updatedThreads > 1) // + updatedThreads > 1) {
                msg.append("s");
        }
        return msg.toString();
    }

    public static ChanThread makeFavoritesThread(Context context, String boardCode) {
        ChanBoard board = ChanBoard.getBoardByCode(context, boardCode);
        ChanThread thread = new ChanThread();
        thread.board = boardCode;
        thread.no = 0;
        thread.sub = getName(context, boardCode);
        thread.com = getDescription(context, boardCode);
        return thread;
    }

    private static void initBoardDrawables() {
        boardDrawables.clear();
        boardDrawables.put("a", new int[]{ R.drawable.a, R.drawable.a_2, R.drawable.a_3 } );
        boardDrawables.put("c", new int[]{ R.drawable.c, R.drawable.c_2, R.drawable.c_3 } );
        boardDrawables.put("w", new int[]{ R.drawable.w, R.drawable.w_2, R.drawable.w_3 } );
        boardDrawables.put("m", new int[]{ R.drawable.m, R.drawable.m_2, R.drawable.m_3 } );
        boardDrawables.put("cgl", new int[]{ R.drawable.cgl, R.drawable.cgl_2, R.drawable.cgl_3 } );
        boardDrawables.put("cm", new int[]{ R.drawable.cm, R.drawable.cm_2, R.drawable.cm_3 } );
        boardDrawables.put("n", new int[]{ R.drawable.n, R.drawable.n_2, R.drawable.n_3 } );
        boardDrawables.put("jp", new int[]{ R.drawable.jp, R.drawable.jp_2, R.drawable.jp_3 } );
        boardDrawables.put("vp", new int[]{ R.drawable.vp, R.drawable.vp_2, R.drawable.vp_3 } );
        boardDrawables.put("v", new int[]{ R.drawable.v, R.drawable.v_2, R.drawable.v_3 } );
        boardDrawables.put("vg", new int[]{ R.drawable.vg, R.drawable.vg_2, R.drawable.vg_3 } );
        boardDrawables.put("vr", new int[]{ R.drawable.vr, R.drawable.vr_2, R.drawable.vr_3 } );
        boardDrawables.put("co", new int[]{ R.drawable.co, R.drawable.co_2, R.drawable.co_3 } );
        boardDrawables.put("g", new int[]{ R.drawable.g, R.drawable.g_2, R.drawable.g_3 } );
        boardDrawables.put("tv", new int[]{ R.drawable.tv, R.drawable.tv_2, R.drawable.tv_3 } );
        boardDrawables.put("k", new int[]{ R.drawable.k, R.drawable.k_2, R.drawable.k_3 } );
        boardDrawables.put("o", new int[]{ R.drawable.o, R.drawable.o_2, R.drawable.o_3 } );
        boardDrawables.put("an", new int[]{ R.drawable.an, R.drawable.an_2, R.drawable.an_3 } );
        boardDrawables.put("tg", new int[]{ R.drawable.tg, R.drawable.tg_2, R.drawable.tg_3 } );
        boardDrawables.put("sp", new int[]{ R.drawable.sp, R.drawable.sp_2, R.drawable.sp_3 } );
        boardDrawables.put("asp", new int[]{ R.drawable.asp, R.drawable.asp_2, R.drawable.asp_3 } );
        boardDrawables.put("sci", new int[]{ R.drawable.sci, R.drawable.sci_2, R.drawable.sci_3 } );
        boardDrawables.put("int", new int[]{ R.drawable.board_int, R.drawable.board_int_2, R.drawable.board_int_3 } );
        boardDrawables.put("out", new int[]{ R.drawable.out, R.drawable.out_2, R.drawable.out_3 } );
        boardDrawables.put("i", new int[]{ R.drawable.i, R.drawable.i_2, R.drawable.i_3 } );
        boardDrawables.put("po", new int[]{ R.drawable.po, R.drawable.po_2, R.drawable.po_3 } );
        boardDrawables.put("p", new int[]{ R.drawable.p, R.drawable.p_2, R.drawable.p_3 } );
        boardDrawables.put("ck", new int[]{ R.drawable.ck, R.drawable.ck_2, R.drawable.ck_3 } );
        boardDrawables.put("ic", new int[]{ R.drawable.ic, R.drawable.ic_2, R.drawable.ic_3 } );
        boardDrawables.put("wg", new int[]{ R.drawable.wg, R.drawable.wg_2, R.drawable.wg_3 } );
        boardDrawables.put("mu", new int[]{ R.drawable.mu, R.drawable.mu_2, R.drawable.mu_3 } );
        boardDrawables.put("fa", new int[]{ R.drawable.fa, R.drawable.fa_2, R.drawable.fa_3 } );
        boardDrawables.put("toy", new int[]{ R.drawable.toy, R.drawable.toy_2, R.drawable.toy_3 } );
        boardDrawables.put("3", new int[]{ R.drawable.board_3, R.drawable.board_3_2, R.drawable.board_3_3 } );
        boardDrawables.put("gd", new int[]{ R.drawable.gd, R.drawable.gd_2, R.drawable.gd_3 } );
        boardDrawables.put("diy", new int[]{ R.drawable.diy, R.drawable.diy_2, R.drawable.diy_3 } );
        boardDrawables.put("wsg", new int[]{ R.drawable.wsg, R.drawable.wsg_2, R.drawable.wsg_3 } );
        boardDrawables.put("q", new int[]{ R.drawable.q, R.drawable.q_2, R.drawable.q_3 } );
        boardDrawables.put("trv", new int[]{ R.drawable.trv, R.drawable.trv_2, R.drawable.trv_3 } );
        boardDrawables.put("fit", new int[]{ R.drawable.fit, R.drawable.fit_2, R.drawable.fit_3 } );
        boardDrawables.put("x", new int[]{ R.drawable.x, R.drawable.x_2, R.drawable.x_3 } );
        boardDrawables.put("lit", new int[]{ R.drawable.lit, R.drawable.lit_2, R.drawable.lit_3 } );
        boardDrawables.put("adv", new int[]{ R.drawable.adv, R.drawable.adv_2, R.drawable.adv_3 } );
        boardDrawables.put("lgbt", new int[]{ R.drawable.lgbt, R.drawable.lgbt_2, R.drawable.lgbt_3 } );
        boardDrawables.put("mlp", new int[]{ R.drawable.mlp, R.drawable.mlp_2, R.drawable.mlp_3 } );
        boardDrawables.put("s", new int[]{ R.drawable.s, R.drawable.s_2, R.drawable.s_3 } );
        boardDrawables.put("hc", new int[]{ R.drawable.hc, R.drawable.hc_2, R.drawable.hc_3 } );
        boardDrawables.put("hm", new int[]{ R.drawable.hm, R.drawable.hm_2, R.drawable.hm_3 } );
        boardDrawables.put("h", new int[]{ R.drawable.h, R.drawable.h_2, R.drawable.h_3 } );
        boardDrawables.put("e", new int[]{ R.drawable.e, R.drawable.e_2, R.drawable.e_3 } );
        boardDrawables.put("u", new int[]{ R.drawable.u, R.drawable.u_2, R.drawable.u_3 } );
        boardDrawables.put("d", new int[]{ R.drawable.d, R.drawable.d_2, R.drawable.d_3 } );
        boardDrawables.put("y", new int[]{ R.drawable.y, R.drawable.y_2, R.drawable.y_3 } );
        boardDrawables.put("t", new int[]{ R.drawable.t, R.drawable.t_2, R.drawable.t_3 } );
        boardDrawables.put("hr", new int[]{ R.drawable.hr, R.drawable.hr_2, R.drawable.hr_3 } );
        boardDrawables.put("gif", new int[]{ R.drawable.gif, R.drawable.gif_2, R.drawable.gif_3 } );
        boardDrawables.put("b", new int[]{ R.drawable.b, R.drawable.b_2, R.drawable.b_3 } );
        boardDrawables.put("r", new int[]{ R.drawable.r, R.drawable.r_2, R.drawable.r_3 } );
        boardDrawables.put("r9k", new int[]{ R.drawable.r9k, R.drawable.r9k_2, R.drawable.r9k_3 } );
        boardDrawables.put("pol", new int[]{ R.drawable.pol, R.drawable.pol_2, R.drawable.pol_3 } );
        boardDrawables.put("soc", new int[]{ R.drawable.soc, R.drawable.soc_2, R.drawable.soc_3 } );
        boardDrawables.put("s4s", new int[]{ R.drawable.s4s, R.drawable.s4s_2, R.drawable.s4s_3 } );
        boardDrawables.put("popular", new int[]{ R.drawable.popular, R.drawable.popular_2, R.drawable.popular_3 } );
        boardDrawables.put("latest", new int[]{ R.drawable.latest, R.drawable.latest_2, R.drawable.latest_3 } );
        boardDrawables.put("images", new int[]{ R.drawable.images, R.drawable.images_2, R.drawable.images_3 } );
        boardDrawables.put("watchlist", new int[]{ R.drawable.watch, R.drawable.watch_2, R.drawable.watch_3 } );
        for (String boardCode : VIRTUAL_BOARDS) {
            if (isPopularBoard(boardCode))
                continue;
            if (WATCHLIST_BOARD_CODE.equals(boardCode))
                continue;
            BoardType type = BoardType.valueOfBoardCode(boardCode);
            if (type != null)
                boardDrawables.put(boardCode, new int[]{ type.drawableId(), type.drawableId(), type.drawableId() });
        }
    }

    public static boolean hasFavorites(Context context) {
        ChanBoard board = ChanFileStorage.loadBoardData(context, ChanBoard.FAVORITES_BOARD_CODE);
        return (board != null && board.threads != null && !board.defData && board.threads.length > 0
                && board.threads[0] != null && !board.threads[0].defData);
    }

    public static String defaultBoardCode(final Context context) {
        return hasFavorites(context) ? ChanBoard.FAVORITES_BOARD_CODE : ChanBoard.ALL_BOARDS_BOARD_CODE;
    }
}
