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
    public static final boolean REFRESH_THREADS_ON_REQUEST = true;
    public static final long MAX_DELAY_FOR_REFRESH_THREADS_ON_REQUEST = 60 * 60 * 1000;

    // AD STUFF
    private static final String JLIST_AD_AFFILIATE_CODE = "4539";
    private static final double AD_ADULT_PROBABILITY_ON_ADULT_BOARD = 1.0;
    private static final int[] JLIST_AD_CODES = { 118, 113, 68 };
    private static final int[] JLIST_AD_SMALL_CODES = { 21, 97, 104, 121, 120 };
    private static final int[] JLIST_AD_ADULT_CODES = { 122, 70 };
    private static final String JLIST_AD_ROOT_URL = "http://anime.jlist.com";
    private static final String JLIST_AD_IMAGE_ROOT_URL = JLIST_AD_ROOT_URL + "/media/" + JLIST_AD_AFFILIATE_CODE;
    private static final String JLIST_AD_CLICK_ROOT_URL = JLIST_AD_ROOT_URL + "/click/" + JLIST_AD_AFFILIATE_CODE;

    public ChanBoard() {
		// public default constructor for Jackson
	}

	private ChanBoard(Type type, String name, String link, int iconId,
			boolean workSafe, boolean classic, boolean textOnly) {
		this.type = type;
		this.name = name;
		this.link = link;
		this.iconId = iconId;
		this.workSafe = workSafe;
		this.classic = classic;
		this.textOnly = textOnly;
	}
	
	public enum Type { WATCHLIST, JAPANESE_CULTURE, INTERESTS, CREATIVE, OTHER, ADULT, MISC };

    public static final String WATCH_BOARD_CODE = "watch";

    public static final String DEFAULT_BOARD_CODE = "a";

    public String board;
    public String name;
    public String link;
    public int iconId;
    public int no;
	public Type type;
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

    private Random generator = new Random();
	
	public ChanBoard copy() {
		ChanBoard copy = new ChanBoard(this.type, this.name, this.link, this.iconId,
				this.workSafe, this.classic, this.textOnly);
		return copy;
	}

	public String toString() {
        return "Board " + link + " page: " + no + ", stickyPosts: " + stickyPosts.length
        		+ ", threads: " + threads.length + ", newThreads: " + loadedThreads.length;
    }

    private static List<ChanBoard> boards;
    private static List<ChanBoard> safeBoards;
    private static Map<Type, List<ChanBoard>> boardsByType;
    private static Map<String, ChanBoard> boardByCode;

    public static String getBoardTypeName(Context ctx, Type boardType) {
        switch (boardType) {
            case JAPANESE_CULTURE: return ctx.getString(R.string.board_type_japanese_culture);
            case INTERESTS: return ctx.getString(R.string.board_type_interests);
            case CREATIVE: return ctx.getString(R.string.board_type_creative);
            case ADULT: return ctx.getString(R.string.board_type_adult);
            case MISC: return ctx.getString(R.string.board_type_misc);
            case OTHER: return ctx.getString(R.string.board_type_other);
            case WATCHLIST: return ctx.getString(R.string.board_watch);
            default:
                return ctx.getString(R.string.board_type_japanese_culture);
        }
    }

    public static boolean isNSFWBoardType(Type boardType) {
        if (boardType == Type.ADULT || boardType == Type.MISC) {
            return true;
        }
        else {
            return false;
        }
    }

	public static List<ChanBoard> getBoards(Context context) {
		if (boards == null) {
			initBoards(context);
		}
		return boards;
	}

    public static List<ChanBoard> getBoardsRespectingNSFW(Context context) {
        if (boards == null) {
            initBoards(context);
        }
        return showNSFW(context) ? boards : safeBoards;
    }

    public static boolean showNSFW(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);
    }

	public static List<ChanBoard> getBoardsByType(Context context, Type type) {
		if (boards == null) {
			initBoards(context);
		}
        else if (type == Type.WATCHLIST) { // handled at thread level
            return null;
        }
		return boardsByType.get(type);
	}

	public static ChanBoard getBoardByCode(Context context, String boardCode) {
        if (boards == null) {
   			initBoards(context);
   		}
        return boardByCode.get(boardCode);
	}
	
	private static void initBoards(Context ctx) {
        boards = new ArrayList<ChanBoard>();
        safeBoards = new ArrayList<ChanBoard>();
        boardsByType = new HashMap<Type, List<ChanBoard>>();
        boardByCode = new HashMap<String, ChanBoard>();

        String[][] boardCodesByType = initBoardCodes(ctx);

        for (String[] boardCodesForType : boardCodesByType) {
            Type boardType = Type.valueOf(boardCodesForType[0]);
            List<ChanBoard> boardsForType = new ArrayList<ChanBoard>();
            for (int i = 1; i < boardCodesForType.length; i+=2) {
                String boardCode = boardCodesForType[i];
                String boardName = boardCodesForType[i+1];
                boolean workSafe = !(boardType == Type.ADULT || boardType == Type.MISC);
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

    public static int getImageResourceId(String boardCode, long postNo) {
        return getImageResourceId(boardCode, postNo, -1);
    }

    public static int getImageResourceId(String boardCode, long postNo, int index) { // allows special-casing first (usually sticky) and multiple
        int imageId = 0;
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

    private static String[][] initBoardCodes(Context ctx) {
        String[][] boardCodesByType = {

                {   Type.WATCHLIST.toString()
                },
                {   Type.JAPANESE_CULTURE.toString(),
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
                {   Type.INTERESTS.toString(),
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
                {   Type.CREATIVE.toString(),
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
                {   Type.OTHER.toString(),
                        "q", ctx.getString(R.string.board_q),
                        "trv", ctx.getString(R.string.board_trv),
                        "fit", ctx.getString(R.string.board_fit),
                        "x", ctx.getString(R.string.board_x),
                        "lit", ctx.getString(R.string.board_lit),
                        "adv", ctx.getString(R.string.board_adv),
                        "lgbt", ctx.getString(R.string.board_lgbt),
                        "mlp", ctx.getString(R.string.board_mlp)
                },
                {   Type.ADULT.toString(),
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
                {   Type.MISC.toString(),
                        "b", ctx.getString(R.string.board_b),
                        "r", ctx.getString(R.string.board_r),
                        "r9k", ctx.getString(R.string.board_r9k),
                        "pol", ctx.getString(R.string.board_pol),
                        "soc", ctx.getString(R.string.board_soc)
                }

        };
        return boardCodesByType;
    }

    public static void preloadUncachedBoards(Context context) {
        List<ChanBoard> boards = ChanBoard.getBoards(context);
        for (ChanBoard board : boards) {
            if (!ChanFileStorage.isBoardCachedOnDisk(context, board.link)) { // if user never visited board before
                if (DEBUG) Log.i(TAG, "Starting load service for uncached board " + board.link);
                FetchChanDataService.scheduleBoardFetch(context, board.link);
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
        return ChanThread.makeBoardRow(link, name, getImageResourceId());
    }

    public Object[] makeThreadAdRow(Context context) {
        int adCode =
                (!workSafe && generator.nextDouble() < AD_ADULT_PROBABILITY_ON_ADULT_BOARD)
                        ? JLIST_AD_ADULT_CODES[generator.nextInt(JLIST_AD_ADULT_CODES.length)]
                        : JLIST_AD_CODES[generator.nextInt(JLIST_AD_CODES.length)];
        String imageUrl = JLIST_AD_IMAGE_ROOT_URL + "/" + adCode;
        String clickUrl = JLIST_AD_CLICK_ROOT_URL + "/" + adCode;
        return ChanThread.makeAdRow(context, imageUrl, clickUrl);
    }

    public Object[] makePostAdRow(Context context) {
        int adCode =
                (!workSafe && generator.nextDouble() < AD_ADULT_PROBABILITY_ON_ADULT_BOARD)
                        ? JLIST_AD_ADULT_CODES[generator.nextInt(JLIST_AD_ADULT_CODES.length)]
                        : JLIST_AD_CODES[generator.nextInt(JLIST_AD_CODES.length)];
        String imageUrl = JLIST_AD_IMAGE_ROOT_URL + "/" + adCode;
        String clickUrl = JLIST_AD_CLICK_ROOT_URL + "/" + adCode;
        return ChanPost.makeAdRow(context, link, imageUrl, clickUrl);
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
    	Log.i(TAG, "Updated board " + name + ", " + newThreads + " new threads, " + updatedThreads + " updated threads.");
    }

    public static void setupActionBarBoardSpinner(final Activity activity, final Menu menu, final String currentBoardCode) {
        if (DEBUG) Log.i(BoardSelectorActivity.TAG, "setupActionBarSpinner " + activity + " " + menu + " boardCode=" + currentBoardCode);
        MenuItem item = menu.findItem(R.id.board_jump_spinner_menu);
        Spinner spinner = (Spinner)item.getActionView();
        spinner.setOnItemSelectedListener(null);
        int position = 0;
        if (currentBoardCode == null || currentBoardCode.isEmpty()) {
            position = 0;
        }
        else if (currentBoardCode.equals(ChanBoard.WATCH_BOARD_CODE)) {
            position = 0; // always move it to "SelectBoard" to overcome view pager action bar bug
        }
        else if (activity instanceof BoardSelectorActivity) {
            position = 0; // always select from board selector
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

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { // for action bar spinner
            if (position < 0)
                return;
            String boardAsMenu = (String) parent.getItemAtPosition(position);
            if (DEBUG) Log.i(BoardSelectorActivity.TAG, "onItemSelected boardSelected=" + boardAsMenu + " created with board=" + createdWithBoardCode);
            if (boardAsMenu == null || boardAsMenu.isEmpty())
                return;
            if (boardAsMenu.equals(activity.getString(R.string.board_select_abbrev)))
                return;
            if (boardAsMenu.equals(activity.getString(R.string.board_watch_abbrev))) {
                if (activity instanceof BoardSelectorActivity
                        && ChanBoard.WATCH_BOARD_CODE.equals(createdWithBoardCode))
                { // special case change tab
                    BoardSelectorActivity bsa = (BoardSelectorActivity)activity;
                    bsa.ensureTabsAdapter();
                    if (parent instanceof Spinner) {
                        Spinner spinner = (Spinner)parent;
                        spinner.setSelection(0, false);
                    }
                    if (bsa.selectedBoardType == Type.WATCHLIST)
                        return;
                }
                BoardSelectorActivity.startActivity(activity, ChanBoard.Type.WATCHLIST);
                return;
            }
            Pattern p = Pattern.compile("/([^/]*)/.*");
            Matcher m = p.matcher(boardAsMenu);
            if (!m.matches())
                return;
            String boardCodeForJump = m.group(1);
            if (boardCodeForJump == null || boardCodeForJump.isEmpty() || boardCodeForJump.equals(createdWithBoardCode))
                return;
            BoardActivity.startActivity(activity, boardCodeForJump);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { // for action bar spinner
        }

    }

}
