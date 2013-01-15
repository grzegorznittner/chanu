package com.chanapps.four.data;

import java.util.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chanapps.four.activity.BoardSelectorActivity;
import com.chanapps.four.activity.R;

public class ChanBoard {
	public static final String TAG = ChanBoard.class.getSimpleName();
	
	public ChanBoard() {
		// public default constructor for Jackson
	}

	private ChanBoard(Type type, String name, String link, int iconId,
			boolean workSafe, boolean classic, boolean textOnly, boolean lastPage) {
		this.type = type;
		this.name = name;
		this.link = link;
		this.iconId = iconId;
		this.workSafe = workSafe;
		this.classic = classic;
		this.textOnly = textOnly;
        this.lastPage = lastPage;
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
    public boolean lastPage;
	
	public ChanPost stickyPosts[] = new ChanPost[0];
	public ChanPost threads[] = new ChanThread[0];
	public long lastFetched;
	
	public boolean defData = false;
	
	public ChanBoard copy() {
		ChanBoard copy = new ChanBoard(this.type, this.name, this.link, this.iconId,
				this.workSafe, this.classic, this.textOnly, this.lastPage);
		return copy;
	}

	public String toString() {
        return "Board " + link + " page: " + no + ", stickyPosts: " + stickyPosts.length + ", threads: " + threads.length;
    }

	private static List<ChanBoard> boards;
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

    public static List<ChanBoard> getSortedBoards(Context context) {
        List<ChanBoard> chanBoards = ChanBoard.getBoards(context);
        Collections.sort(chanBoards, new Comparator<ChanBoard>() {
            @Override
            public int compare(ChanBoard lhs, ChanBoard rhs) {
                return lhs.link.compareToIgnoreCase(rhs.link);
            }
        });
        return chanBoards;
    }

	public static boolean isValidBoardCode(Context context, String boardCode) {
        if (boards == null) {
   			initBoards(context);
   		}
        return boardByCode.containsKey(boardCode);
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
                ChanBoard b = new ChanBoard(boardType, boardName, boardCode, iconId, workSafe, true, false, false);
                boardsForType.add(b);
                boards.add(b);
                boardByCode.put(boardCode, b);
            }
            boardsByType.put(boardType, boardsForType);
        }
   	}

    private static int getImageResourceId(String boardCode) {
        int imageId = 0;
        try {
            imageId = R.drawable.class.getField(boardCode).getInt(null);
        } catch (Exception e) {
            try {
                imageId = R.drawable.class.getField("board_" + boardCode).getInt(null);
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
                        "co", ctx.getString(R.string.board_co),
                        "g", ctx.getString(R.string.board_g),
                        "tv", ctx.getString(R.string.board_tv),
                        "k", ctx.getString(R.string.board_k),
                        "o", ctx.getString(R.string.board_o),
                        "an", ctx.getString(R.string.board_an),
                        "tg", ctx.getString(R.string.board_tg),
                        "sp", ctx.getString(R.string.board_sp),
                        "sci", ctx.getString(R.string.board_sci),
                        "int", ctx.getString(R.string.board_int)
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

}
