package com.chanapps.four.data;

import android.content.Context;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import com.chanapps.four.activity.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ChanBoard {
	public static final String TAG = ChanBoard.class.getSimpleName();

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
	
	public enum Type {JAPANESE_CULTURE, INTERESTS, CREATIVE, ADULT, OTHER, MISC, FAVORITES};
	
	public MatrixCursor cursor = null;
	
	public String board;
	public String name;
    public String link;
    public int iconId;
	public int no;

	public Type type;
	public boolean workSafe;
	public boolean classic;
	public boolean textOnly;

	public String toString() {
        return "Board " + board + " page " + no;
    }

	private static List<ChanBoard> boards = null;
	private static Map<Type, List<ChanBoard>> boardsByType = null;

    public static String getBoardTypeName(Context ctx, Type boardType) {
        switch (boardType) {
            case JAPANESE_CULTURE: return ctx.getString(R.string.board_type_japanese_culture);
            case INTERESTS: return ctx.getString(R.string.board_type_interests);
            case CREATIVE: return ctx.getString(R.string.board_type_creative);
            case ADULT: return ctx.getString(R.string.board_type_adult);
            case MISC: return ctx.getString(R.string.board_type_misc);
            case OTHER: return ctx.getString(R.string.board_type_other);
            case FAVORITES: return ctx.getString(R.string.board_type_favorites);
            default:
                return ctx.getString(R.string.board_type_japanese_culture);
        }
    }

	public static List<ChanBoard> getBoards(Context context) {
		if (boards == null) {
			initBoards(context);
		}
		return boards;
	}
	
	public static boolean isValidBoardCode(Context context, String boardCode) {
        List<ChanBoard> boards = getBoards(context);
		for (ChanBoard board : boards) {
			if (board.link.equals(boardCode)) {
				return true;
			}
		}
		return false;
	}
	
	public static List<ChanBoard> getBoardsByType(Context context, Type type) {
		if (boards == null) {
			initBoards(context);
		}
		return boardsByType.get(type);
	}

	private static void initBoards(Context ctx) {
		boards = new ArrayList<ChanBoard>();
		boardsByType = new HashMap<Type, List<ChanBoard>>();
        String[][] boardCodesByType = initBoardCodes(ctx);

        for (String[] boardCodesForType : boardCodesByType) {
            Type boardType = Type.valueOf(boardCodesForType[0]);
            List<ChanBoard> boardCodes = new ArrayList<ChanBoard>();
            for (int i = 1; i < boardCodesForType.length; i+=2) {
                String boardCode = boardCodesForType[i];
                String boardName = boardCodesForType[i+1];
                boolean workSafe = !(boardType == Type.ADULT || boardType == Type.MISC);
                ChanBoard b = new ChanBoard(boardType, boardName, boardCode, 0, workSafe, true, false);
                boardCodes.add(b);
                boards.add(b);
            }
            boardsByType.put(boardType, boardCodes);
        }
   	}

    private static String[][] initBoardCodes(Context ctx) {
        String[][] boardCodesByType = {
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
                {   Type.OTHER.toString(),
                        "q", ctx.getString(R.string.board_q),
                        "trv", ctx.getString(R.string.board_trv),
                        "fit", ctx.getString(R.string.board_fit),
                        "x", ctx.getString(R.string.board_x),
                        "lit", ctx.getString(R.string.board_lit),
                        "adv", ctx.getString(R.string.board_adv),
                        "mlp", ctx.getString(R.string.board_mlp)
                },
                {   Type.MISC.toString(),
                        "b", ctx.getString(R.string.board_b),
                        "r", ctx.getString(R.string.board_r),
                        "r9k", ctx.getString(R.string.board_r9k),
                        "pol", ctx.getString(R.string.board_pol),
                        "soc", ctx.getString(R.string.board_soc)
                },
                {   Type.FAVORITES.toString(),
                        "watch", ctx.getString(R.string.board_watch)
                }

        };
        return boardCodesByType;
    }

}
