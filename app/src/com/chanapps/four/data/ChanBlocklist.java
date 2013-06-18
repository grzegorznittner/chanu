package com.chanapps.four.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/2/13
 * Time: 9:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanBlocklist {

    public enum BlockType {
        TRIPCODE ("Tripcode", ChanHelper.PREF_BLOCKLIST_TRIPCODE),
        NAME ("Name", ChanHelper.PREF_BLOCKLIST_NAME),
        EMAIL ("Email", ChanHelper.PREF_BLOCKLIST_EMAIL),
        ID ("Id", ChanHelper.PREF_BLOCKLIST_ID);
        private String displayString;
        private String blockPref;
        BlockType(String s, String t) {
            displayString = s;
            blockPref = t;
        }
        public String displayString() {
            return displayString;
        }
        public String blockPref() {
            return blockPref;
        }
    };

    private static Map<BlockType, Set<String>> blocklist;

    private static void initBlocklist(Context context) {
        if (blocklist == null)
            blocklist = new HashMap<BlockType, Set<String>>();
        blocklist.clear();
        for (int i = 0; i < BlockType.values().length; i++) {
            BlockType blockType = BlockType.values()[i];
            Set<String> blocks = PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getStringSet(blockType.blockPref(), new HashSet<String>());
            blocklist.put(blockType, blocks);
        }
    }

    public static Map<BlockType, Set<String>> getBlocklist(Context context) {
        if (blocklist == null)
            initBlocklist(context);
        return blocklist;
    }

    public static List<String> getSorted(Context context, BlockType blockType) {
        if (blocklist == null)
            initBlocklist(context);
        List<String> sorted = new ArrayList<String>();
        sorted.addAll(blocklist.get(blockType));
        Collections.sort(sorted);
        return sorted;
    }

    public static void removeAll(Context context, BlockType blockType, List<String> removeBlocks) {
        if (blocklist == null)
            initBlocklist(context);
        Set<String> blocks = blocklist.get(blockType);
        blocks.removeAll(removeBlocks);
        saveBlocklist(context);
    }

    public static void addAll(Context context, BlockType blockType, List<String> newBlocks) {
        if (blocklist == null)
            initBlocklist(context);
        Set<String> blocks = blocklist.get(blockType);
        blocks.addAll(newBlocks);
        saveBlocklist(context);
    }

    public static boolean contains(Context context, BlockType blockType, String block) {
        if (blocklist == null)
            initBlocklist(context);
        Set<String> typeList = blocklist.get(blockType);
        if (typeList == null)
            return false;
        return typeList.contains(block);
    }

    public static boolean isBlocked(Context context, ChanPost post) {
        return ChanBlocklist.contains(context, ChanBlocklist.BlockType.TRIPCODE, post.trip)
                || ChanBlocklist.contains(context, ChanBlocklist.BlockType.NAME, post.name)
                || ChanBlocklist.contains(context, ChanBlocklist.BlockType.EMAIL, post.email)
                || ChanBlocklist.contains(context, ChanBlocklist.BlockType.ID, post.id);
    }

    private static void saveBlocklist(Context context) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        for (int i = 0; i < BlockType.values().length; i++) {
            BlockType blockType = BlockType.values()[i];
            editor.putStringSet(blockType.blockPref(), blocklist.get(blockType));
        }
        editor.commit();
    }

}
