package com.chanapps.four.data;

import java.util.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.component.AlphanumComparator;
import com.chanapps.four.component.URLFormatComponent;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;

public class ChanBoard {

	public static final String TAG = ChanBoard.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int NUM_DEFAULT_IMAGES_PER_BOARD = 3;
    private static final int NUM_RELATED_BOARDS = 3;

    public static final String BOARD_CODE = "boardCode";
    public static final String ALL_BOARDS_BOARD_CODE = BoardType.ALL_BOARDS.boardCode();
    public static final String POPULAR_BOARD_CODE = BoardType.POPULAR.boardCode();
    public static final String LATEST_BOARD_CODE = BoardType.LATEST.boardCode();
    public static final String LATEST_IMAGES_BOARD_CODE = BoardType.LATEST_IMAGES.boardCode();
    public static final String WATCHLIST_BOARD_CODE = BoardType.WATCHLIST.boardCode();
    public static final String FAVORITES_BOARD_CODE = BoardType.FAVORITES.boardCode();
    public static final String META_JAPANESE_CULTURE_BOARD_CODE = BoardType.JAPANESE_CULTURE.boardCode();
    public static final String META_INTERESTS_BOARD_CODE = BoardType.INTERESTS.boardCode();
    public static final String META_CREATIVE_BOARD_CODE = BoardType.CREATIVE.boardCode();
    public static final String META_OTHER_BOARD_CODE = BoardType.OTHER.boardCode();
    public static final String META_ADULT_BOARD_CODE = BoardType.ADULT.boardCode();
    public static final String META_MISC_BOARD_CODE = BoardType.MISC.boardCode();

    public static final String[] VIRTUAL_BOARDS = { ALL_BOARDS_BOARD_CODE, POPULAR_BOARD_CODE, LATEST_BOARD_CODE,
            LATEST_IMAGES_BOARD_CODE, WATCHLIST_BOARD_CODE, FAVORITES_BOARD_CODE,
            META_JAPANESE_CULTURE_BOARD_CODE, META_INTERESTS_BOARD_CODE,
            META_CREATIVE_BOARD_CODE, META_OTHER_BOARD_CODE,
            META_ADULT_BOARD_CODE, META_MISC_BOARD_CODE };
    public static final String[] META_BOARDS = { ALL_BOARDS_BOARD_CODE,
            META_JAPANESE_CULTURE_BOARD_CODE, META_INTERESTS_BOARD_CODE,
            META_CREATIVE_BOARD_CODE, META_OTHER_BOARD_CODE,
            META_ADULT_BOARD_CODE, META_MISC_BOARD_CODE };
    public static final String[] POPULAR_BOARDS = { POPULAR_BOARD_CODE, LATEST_BOARD_CODE, LATEST_IMAGES_BOARD_CODE };
    public static final String[] TOP_BOARDS = { ALL_BOARDS_BOARD_CODE, FAVORITES_BOARD_CODE, WATCHLIST_BOARD_CODE };

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

    protected static Map<String, int[]> boardDrawables = new HashMap<String, int[]>();

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

    public static boolean isWorksafe(Context context, String boardCode) {
        initBoards(context);
        ChanBoard board = getBoardByCode(context, boardCode);
        return safeBoards.contains(board);
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

        String[][] boardCodesByType = BoardInitializer.initBoardCodes(ctx);

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


        final AlphanumComparator comparator = new AlphanumComparator();
        //Soft all boards
        Collections.sort(boards, new Comparator<ChanBoard>() {
            @Override
            public int compare(ChanBoard lhs, ChanBoard rhs)
            {
                return comparator.compare(lhs.link, rhs.link);
            }
        });

        //Sort safe board
        Collections.sort(safeBoards, new Comparator<ChanBoard>()
        {
            @Override
            public int compare(ChanBoard lhs, ChanBoard rhs)
            {
                return comparator.compare(lhs.link, rhs.link);
            }
        });

        /* Commented out unused code
        String[][] relatedBoardCodes = BoardInitializer.initRelatedBoards();
        for (String[] relatedBoardCodeArray : relatedBoardCodes) {
            String boardCode = relatedBoardCodeArray[0];
            List<ChanBoard> relatedBoardList = new ArrayList<ChanBoard>();
            for (int i = 1; i < relatedBoardCodeArray.length; i++) {
                String relatedBoardCode = relatedBoardCodeArray[i];
                ChanBoard relatedBoard = boardByCode.get(relatedBoardCode);
                relatedBoardList.add(relatedBoard);
            }
            //Related Init
            relatedBoards.put(boardCode, relatedBoardList);
            if (DEBUG) Log.i(TAG, "Initialized /" + boardCode + "/ with " + relatedBoardList.size() + " related boards");
        }
        */

        boardDrawables = BoardInitializer.initBoardDrawables();
    }

    public static int imagelessStickyDrawableId(String boardCode, long threadNo) {
        if (boardCode.equals("s") && threadNo == 12370429)
            return R.drawable.s_2;
        else if (boardCode.equals("s") && threadNo == 9112225)
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

    static public final Map<String, Integer> spoilerImageCount = new HashMap<String, Integer>();
    static public final Random spoilerGenerator = new Random();

    static public String spoilerThumbnailUrl(Context context, String boardCode) {
        if (spoilerImageCount.isEmpty()) {
            spoilerImageCount.put("m", 4);
            spoilerImageCount.put("co", 5);
            spoilerImageCount.put("tg", 3);
            spoilerImageCount.put("tv", 5);
        }
        int spoilerImages = spoilerImageCount.containsKey(boardCode) ? spoilerImageCount.get(boardCode) : 1;
        if (spoilerImages > 1) {
            int spoilerImageNum = spoilerGenerator.nextInt(spoilerImages) + 1;
            return String.format(
                    URLFormatComponent.getUrl(context, URLFormatComponent.CHAN_SPOILER_NUMBERED_IMAGE_URL_FORMAT), boardCode, spoilerImageNum);
        }
        else {
            return String.format(
                    URLFormatComponent.getUrl(context, URLFormatComponent.CHAN_SPOILER_IMAGE_URL_FORMAT), boardCode);
        }
    }

    static public boolean isAsciiOnlyBoard(String boardCode) {
        if (boardCode.equals("q") || boardCode.equals("r9k") || boardCode.equals("news"))
            return true;
        else
            return false;
    }

    public Object[] makeRow(Context context) { // for board selector
        return makeRow(context, 0);
    }

    public Object[] makeRow(Context context, long threadNo) { // for board selector
        return ChanThread.makeBoardRow(context, link, getName(context), getRandomImageResourceId(link, threadNo), 0);
    }

    public Object[] makeHeaderRow(Context context) { // for board selector
        return ChanThread.makeHeaderRow(context, this);
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

    public void updateCountersAfterLoad(Context context) {
    	if (loadedThreads.length == 0) {
    		return;
    	}
    	Map<Long, ChanPost> currentThreads = new HashMap<Long, ChanPost>();
    	for (ChanPost thread : threads) {
    		currentThreads.put(thread.no, thread);
    	}
    	this.newThreads = 0;
    	this.updatedThreads = 0;
        ChanThread firstNewThread = null;
    	for (ChanThread newThread : loadedThreads) {
    		if (currentThreads.containsKey(newThread.no)) {
    			ChanPost currentPost = currentThreads.get(newThread.no);
    			if (currentPost.replies != newThread.replies) {
    				updatedThreads++;
    			}
    		} else {
                if (firstNewThread == null)
                    firstNewThread = newThread;
    			newThreads++;
    		}
    	}
        //if (newThreads > 0 && isFavoriteBoard(context, link))
        //    NotificationComponent.notifyNewThreads(context, link, newThreads, firstNewThread);
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

    public static boolean isTopBoard(String boardCode) {
        for (String code : TOP_BOARDS)
            if (code.equals(boardCode))
                return true;
        return false;
    }

    public boolean isTopBoard() {
        return isTopBoard(link);
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

    public static String getBestWidgetImageUrl(Context context, ChanPost thread, String backupBoardCode, int i) {
        return (thread != null && thread.tim > 0)
                ? thread.thumbnailUrl(context)
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
        boolean hasData = board != null && board.hasData();
        if (DEBUG) Log.i(TAG, "boardHasData() /" + boardCode + "/ hasData=" + hasData + " board=" + board);
        return hasData;
    }

    public boolean hasData() {
        return !defData
                && threads != null
                && threads.length > 0
                && threads[0] != null
                && !threads[0].defData;
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

    public static boolean isFavoriteBoard(final Context context, final String boardCode) {
        ChanBoard favorites = ChanFileStorage.loadBoardData(context, ChanBoard.FAVORITES_BOARD_CODE);
        if (favorites == null || !favorites.hasData())
            return false;
        for (ChanThread thread : favorites.threads) {
            if (boardCode.equals(thread.board))
                return true;
        }
        return false;
    }

    public static String boardUrl(Context context, String boardCode) {
        if (boardCode == null || boardCode.isEmpty() || isVirtualBoard(boardCode))
            return URLFormatComponent.getUrl(context, URLFormatComponent.CHAN_FRONTPAGE_URL);
        else
            return String.format(URLFormatComponent.getUrl(context, URLFormatComponent.CHAN_WEB_BOARD_URL_FORMAT), boardCode);
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


    public static boolean hasFavorites(Context context) {
        return boardHasData(context, ChanBoard.FAVORITES_BOARD_CODE);
    }

    public static boolean hasWatchlist(Context context) {
        return boardHasData(context, ChanBoard.WATCHLIST_BOARD_CODE);
    }

    public static String defaultBoardCode(final Context context) {
        //if (hasWatchlist(context))
        //    return ChanBoard.WATCHLIST_BOARD_CODE;
        //else
        if (hasFavorites(context))
            return ChanBoard.FAVORITES_BOARD_CODE;
        else
            return ChanBoard.ALL_BOARDS_BOARD_CODE;
    }

    public static boolean isPersistentBoard(final String boardCode) {
        if (WATCHLIST_BOARD_CODE.equals(boardCode))
            return true;
        else if (FAVORITES_BOARD_CODE.equals(boardCode))
            return true;
        else
            return false;
    }

}
