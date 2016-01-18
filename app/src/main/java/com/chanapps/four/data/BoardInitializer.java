package com.chanapps.four.data;

import android.content.Context;

import com.chanapps.four.activity.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Greznor on 11/3/15.
 */
public class BoardInitializer
{


    public static String[][] initBoardCodes(Context ctx) {
        String[][] boardCodesByType = {
                {   BoardType.WATCHLIST.toString(),
                        ChanBoard.WATCHLIST_BOARD_CODE, ctx.getString(R.string.board_watch),
                },
                {   BoardType.FAVORITES.toString(),
                        ChanBoard.FAVORITES_BOARD_CODE, ctx.getString(R.string.board_favorites),
                },
                {   BoardType.POPULAR.toString(),
                        ChanBoard.POPULAR_BOARD_CODE, ctx.getString(R.string.board_popular)
                },
                {   BoardType.LATEST.toString(),
                        ChanBoard.LATEST_BOARD_CODE, ctx.getString(R.string.board_latest)
                },
                {   BoardType.LATEST_IMAGES.toString(),
                        ChanBoard.LATEST_IMAGES_BOARD_CODE, ctx.getString(R.string.board_latest_images)
                },
                {   BoardType.JAPANESE_CULTURE.toString(),
                        ChanBoard.META_JAPANESE_CULTURE_BOARD_CODE, ctx.getString(R.string.board_type_japanese_culture),
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
                        ChanBoard.META_INTERESTS_BOARD_CODE, ctx.getString(R.string.board_type_interests),
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
                        "biz", ctx.getString(R.string.board_biz),
                        "his", ctx.getString(R.string.board_his),
                },
                {   BoardType.CREATIVE.toString(),
                        ChanBoard.META_CREATIVE_BOARD_CODE, ctx.getString(R.string.board_type_creative),
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
                        ChanBoard.META_OTHER_BOARD_CODE, ctx.getString(R.string.board_type_other),
                        "trv", ctx.getString(R.string.board_trv),
                        "fit", ctx.getString(R.string.board_fit),
                        "x", ctx.getString(R.string.board_x),
                        "lit", ctx.getString(R.string.board_lit),
                        "adv", ctx.getString(R.string.board_adv),
                        "lgbt", ctx.getString(R.string.board_lgbt),
                        "mlp", ctx.getString(R.string.board_mlp),
                        "news", ctx.getString(R.string.board_news),
                        "qa", ctx.getString(R.string.board_qa),
                        "wsr", ctx.getString(R.string.board_desc_wsr)
                },
                {   BoardType.ADULT.toString(),
                        ChanBoard.META_ADULT_BOARD_CODE, ctx.getString(R.string.board_type_adult),
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
                        "aco", ctx.getString(R.string.board_aco)
                },
                {   BoardType.MISC.toString(),
                        ChanBoard.META_MISC_BOARD_CODE, ctx.getString(R.string.board_type_misc),
                        "b", ctx.getString(R.string.board_b),
                        "r", ctx.getString(R.string.board_r),
                        "r9k", ctx.getString(R.string.board_r9k),
                        "pol", ctx.getString(R.string.board_pol),
                        "soc", ctx.getString(R.string.board_soc),
                        "s4s", ctx.getString(R.string.board_s4s),
                        "trash", ctx.getString(R.string.board_trash)
                },
                {
                        BoardType.ALL_BOARDS.toString(),
                        ChanBoard.ALL_BOARDS_BOARD_CODE, ctx.getString(R.string.board_all_boards),
                        "a", ctx.getString(R.string.board_a),
                        "aco", ctx.getString(R.string.board_aco),
                        "adv", ctx.getString(R.string.board_adv),
                        "asp", ctx.getString(R.string.board_asp),
                        "an", ctx.getString(R.string.board_an),
                        "b", ctx.getString(R.string.board_b),
                        "biz", ctx.getString(R.string.board_biz),
                        "c", ctx.getString(R.string.board_c),
                        "cgl", ctx.getString(R.string.board_cgl),
                        "ck", ctx.getString(R.string.board_ck),
                        "cm", ctx.getString(R.string.board_cm),
                        "co", ctx.getString(R.string.board_co),
                        "d", ctx.getString(R.string.board_d),
                        "diy", ctx.getString(R.string.board_diy),
                        "e", ctx.getString(R.string.board_e),
                        "fa", ctx.getString(R.string.board_fa),
                        "fit", ctx.getString(R.string.board_fit),
                        "g", ctx.getString(R.string.board_g),
                        "gd", ctx.getString(R.string.board_gd),
                        "gif", ctx.getString(R.string.board_gif),
                        "h", ctx.getString(R.string.board_h),
                        "hc", ctx.getString(R.string.board_hc),
                        "hm", ctx.getString(R.string.board_hm),
                        "his", ctx.getString(R.string.board_his),
                        "hr", ctx.getString(R.string.board_hr),
                        "i", ctx.getString(R.string.board_i),
                        "ic", ctx.getString(R.string.board_ic),
                        "int", ctx.getString(R.string.board_int),
                        "jp", ctx.getString(R.string.board_jp),
                        "k", ctx.getString(R.string.board_k),
                        "lgbt", ctx.getString(R.string.board_lgbt),
                        "lit", ctx.getString(R.string.board_lit),
                        "m", ctx.getString(R.string.board_m),
                        "mlp", ctx.getString(R.string.board_mlp),
                        "mu", ctx.getString(R.string.board_mu),
                        "n", ctx.getString(R.string.board_n),
                        "news", ctx.getString(R.string.board_news),
                        "o", ctx.getString(R.string.board_o),
                        "out", ctx.getString(R.string.board_out),
                        "p", ctx.getString(R.string.board_p),
                        "po", ctx.getString(R.string.board_po),
                        "pol", ctx.getString(R.string.board_pol),
                        "qa", ctx.getString(R.string.board_qa),
                        "r", ctx.getString(R.string.board_r),
                        "r9k", ctx.getString(R.string.board_r9k),
                        "s", ctx.getString(R.string.board_s),
                        "s4s", ctx.getString(R.string.board_s4s),
                        "sci", ctx.getString(R.string.board_sci),
                        "sp", ctx.getString(R.string.board_sp),
                        "soc", ctx.getString(R.string.board_soc),
                        "t", ctx.getString(R.string.board_t),
                        "tg", ctx.getString(R.string.board_tg),
                        "toy", ctx.getString(R.string.board_toy),
                        "trash", ctx.getString(R.string.board_trash),
                        "trv", ctx.getString(R.string.board_trv),
                        "tv", ctx.getString(R.string.board_tv),
                        "u", ctx.getString(R.string.board_u),
                        "v", ctx.getString(R.string.board_v),
                        "vg", ctx.getString(R.string.board_vg),
                        "vr", ctx.getString(R.string.board_vr),
                        "vp", ctx.getString(R.string.board_vp),
                        "w", ctx.getString(R.string.board_w),
                        "wg", ctx.getString(R.string.board_wg),
                        "wsg", ctx.getString(R.string.board_wsg),
                        "wsr", ctx.getString(R.string.board_wsr),
                        "x", ctx.getString(R.string.board_x),
                        "y", ctx.getString(R.string.board_y),
                        "3", ctx.getString(R.string.board_3),
                }
        };
        return boardCodesByType;
    }

    public static HashMap<String, int[]> initBoardDrawables() {
        HashMap<String, int[]> boardDrawables = new HashMap<String, int[]>();

        boardDrawables.put("a", new int[]{ R.drawable.a, R.drawable.a_2, R.drawable.a_3 } );
        boardDrawables.put("aco", new int[]{ R.drawable.aco, R.drawable.aco, R.drawable.aco } );
        boardDrawables.put("adv", new int[]{ R.drawable.adv, R.drawable.adv_2, R.drawable.adv_3 } );
        boardDrawables.put("an", new int[]{ R.drawable.an, R.drawable.an_2, R.drawable.an_3 } );
        boardDrawables.put("asp", new int[]{ R.drawable.asp, R.drawable.asp_2, R.drawable.asp_3 } );
        boardDrawables.put("b", new int[]{ R.drawable.b, R.drawable.b_2, R.drawable.b_3 } );
        boardDrawables.put("biz", new int[]{ R.drawable.biz, R.drawable.biz_2, R.drawable.biz_3 } );
        boardDrawables.put("c", new int[]{ R.drawable.c, R.drawable.c_2, R.drawable.c_3 } );
        boardDrawables.put("cgl", new int[]{ R.drawable.cgl, R.drawable.cgl_2, R.drawable.cgl_3 } );
        boardDrawables.put("ck", new int[]{ R.drawable.ck, R.drawable.ck_2, R.drawable.ck_3 } );
        boardDrawables.put("cm", new int[]{ R.drawable.cm, R.drawable.cm_2, R.drawable.cm_3 } );
        boardDrawables.put("co", new int[]{ R.drawable.co, R.drawable.co_2, R.drawable.co_3 } );
        boardDrawables.put("d", new int[]{ R.drawable.d, R.drawable.d_2, R.drawable.d_3 } );
        boardDrawables.put("diy", new int[]{ R.drawable.diy, R.drawable.diy_2, R.drawable.diy_3 } );
        boardDrawables.put("e", new int[]{ R.drawable.e, R.drawable.e_2, R.drawable.e_3 } );
        boardDrawables.put("fa", new int[]{ R.drawable.fa, R.drawable.fa_2, R.drawable.fa_3 } );
        boardDrawables.put("fit", new int[]{ R.drawable.fit, R.drawable.fit_2, R.drawable.fit_3 } );
        boardDrawables.put("g", new int[]{ R.drawable.g, R.drawable.g_2, R.drawable.g_3 } );
        boardDrawables.put("gd", new int[]{ R.drawable.gd, R.drawable.gd_2, R.drawable.gd_3 } );
        boardDrawables.put("gif", new int[]{ R.drawable.gif, R.drawable.gif_2, R.drawable.gif_3 } );
        boardDrawables.put("h", new int[]{ R.drawable.h, R.drawable.h_2, R.drawable.h_3 } );
        boardDrawables.put("hc", new int[]{ R.drawable.hc, R.drawable.hc_2, R.drawable.hc_3 } );
        boardDrawables.put("his", new int[]{ R.drawable.his, R.drawable.his_2, R.drawable.his_3 } );
        boardDrawables.put("hm", new int[]{ R.drawable.hm, R.drawable.hm_2, R.drawable.hm_3 } );
        boardDrawables.put("hr", new int[]{ R.drawable.hr, R.drawable.hr_2, R.drawable.hr_3 } );
        boardDrawables.put("i", new int[]{ R.drawable.i, R.drawable.i_2, R.drawable.i_3 } );
        boardDrawables.put("ic", new int[]{ R.drawable.ic, R.drawable.ic_2, R.drawable.ic_3 } );
        boardDrawables.put("int", new int[]{ R.drawable.board_int, R.drawable.board_int_2, R.drawable.board_int_3 } );
        boardDrawables.put("k", new int[]{ R.drawable.k, R.drawable.k_2, R.drawable.k_3 } );
        boardDrawables.put("jp", new int[]{ R.drawable.jp, R.drawable.jp_2, R.drawable.jp_3 } );
        boardDrawables.put("lgbt", new int[]{ R.drawable.lgbt, R.drawable.lgbt_2, R.drawable.lgbt_3 } );
        boardDrawables.put("lit", new int[]{ R.drawable.lit, R.drawable.lit_2, R.drawable.lit_3 } );
        boardDrawables.put("m", new int[]{ R.drawable.m, R.drawable.m_2, R.drawable.m_3 } );
        boardDrawables.put("mlp", new int[]{ R.drawable.mlp, R.drawable.mlp_2, R.drawable.mlp_3 } );
        boardDrawables.put("mu", new int[]{ R.drawable.mu, R.drawable.mu_2, R.drawable.mu_3 } );
        boardDrawables.put("n", new int[]{ R.drawable.n, R.drawable.n_2, R.drawable.n_3 } );
        boardDrawables.put("news", new int[]{ R.drawable.news, R.drawable.news, R.drawable.news } );
        boardDrawables.put("p", new int[]{ R.drawable.p, R.drawable.p_2, R.drawable.p_3 } );
        boardDrawables.put("po", new int[]{ R.drawable.po, R.drawable.po_2, R.drawable.po_3 } );
        boardDrawables.put("pol", new int[]{ R.drawable.pol, R.drawable.pol_2, R.drawable.pol_3 } );
        boardDrawables.put("o", new int[]{ R.drawable.o, R.drawable.o_2, R.drawable.o_3 } );
        boardDrawables.put("out", new int[]{ R.drawable.out, R.drawable.out_2, R.drawable.out_3 } );
        //boardDrawables.put("q", new int[]{ R.drawable.q, R.drawable.q_2, R.drawable.q_3 } );
        boardDrawables.put("qa", new int[]{ R.drawable.q, R.drawable.q_2, R.drawable.q_3 } );
        boardDrawables.put("r", new int[]{ R.drawable.r, R.drawable.r_2, R.drawable.r_3 } );
        boardDrawables.put("r9k", new int[]{ R.drawable.r9k, R.drawable.r9k_2, R.drawable.r9k_3 } );
        boardDrawables.put("s4s", new int[]{ R.drawable.s4s, R.drawable.s4s_2, R.drawable.s4s_3 } );
        boardDrawables.put("s", new int[]{ R.drawable.s, R.drawable.s_2, R.drawable.s_3 } );
        boardDrawables.put("sci", new int[]{ R.drawable.sci, R.drawable.sci_2, R.drawable.sci_3 } );
        boardDrawables.put("sp", new int[]{ R.drawable.sp, R.drawable.sp_2, R.drawable.sp_3 } );
        boardDrawables.put("soc", new int[]{ R.drawable.soc, R.drawable.soc_2, R.drawable.soc_3 } );
        boardDrawables.put("t", new int[]{ R.drawable.t, R.drawable.t_2, R.drawable.t_3 } );
        boardDrawables.put("tg", new int[]{ R.drawable.tg, R.drawable.tg_2, R.drawable.tg_3 } );
        boardDrawables.put("toy", new int[]{ R.drawable.toy, R.drawable.toy_2, R.drawable.toy_3 } );
        boardDrawables.put("trv", new int[]{ R.drawable.trv, R.drawable.trv_2, R.drawable.trv_3 } );
        boardDrawables.put("trash", new int[]{ R.drawable.trash, R.drawable.trash, R.drawable.trash } );
        boardDrawables.put("tv", new int[]{ R.drawable.tv, R.drawable.tv_2, R.drawable.tv_3 } );
        boardDrawables.put("u", new int[]{ R.drawable.u, R.drawable.u_2, R.drawable.u_3 } );
        boardDrawables.put("v", new int[]{ R.drawable.v, R.drawable.v_2, R.drawable.v_3 } );
        boardDrawables.put("vg", new int[]{ R.drawable.vg, R.drawable.vg_2, R.drawable.vg_3 } );
        boardDrawables.put("vp", new int[]{ R.drawable.vp, R.drawable.vp_2, R.drawable.vp_3 } );
        boardDrawables.put("vr", new int[]{ R.drawable.vr, R.drawable.vr_2, R.drawable.vr_3 } );
        boardDrawables.put("w", new int[]{ R.drawable.w, R.drawable.w_2, R.drawable.w_3 } );
        boardDrawables.put("wg", new int[]{ R.drawable.wg, R.drawable.wg_2, R.drawable.wg_3 } );
        boardDrawables.put("wsg", new int[]{ R.drawable.wsg, R.drawable.wsg_2, R.drawable.wsg_3 } );
        boardDrawables.put("wsr", new int[]{ R.drawable.wsr, R.drawable.wsr, R.drawable.wsr } );
        boardDrawables.put("x", new int[]{ R.drawable.x, R.drawable.x_2, R.drawable.x_3 } );
        boardDrawables.put("y", new int[]{ R.drawable.y, R.drawable.y_2, R.drawable.y_3 } );
        boardDrawables.put("3", new int[]{ R.drawable.board_3, R.drawable.board_3_2, R.drawable.board_3_3 } );

        boardDrawables.put("popular", new int[]{ R.drawable.popular, R.drawable.popular_2, R.drawable.popular_3 } );
        boardDrawables.put("latest", new int[]{ R.drawable.latest, R.drawable.latest_2, R.drawable.latest_3 } );
        boardDrawables.put("images", new int[]{ R.drawable.images, R.drawable.images_2, R.drawable.images_3 } );
        boardDrawables.put("watchlist", new int[]{ R.drawable.watch, R.drawable.watch_2, R.drawable.watch_3 } );

        for (String boardCode : ChanBoard.VIRTUAL_BOARDS) {
            if (ChanBoard.isPopularBoard(boardCode))
                continue;
            if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode))
                continue;
            BoardType type = BoardType.valueOfBoardCode(boardCode);
            if (type != null)
                boardDrawables.put(boardCode, new int[]{ type.drawableId(), type.drawableId(), type.drawableId() });
        }

        return boardDrawables;
    }


    public static String[][] initRelatedBoards() {
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
}
