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
        TRIPCODE,
        NAME,
        EMAIL,
        ID
    };

    public static final String[] BLOCK_PREFS = {
            ChanHelper.PREF_BLOCKLIST_TRIPCODE,
            ChanHelper.PREF_BLOCKLIST_NAME,
            ChanHelper.PREF_BLOCKLIST_EMAIL,
            ChanHelper.PREF_BLOCKLIST_ID
    };
    
    private static Map<BlockType, Set<String>> blocklist;

    private static void initBlocklist(Context context) {
        if (blocklist == null)
            blocklist = new HashMap<BlockType, Set<String>>();
        blocklist.clear();
        for (int i = 0; i < BlockType.values().length; i++) {
            BlockType blockType = BlockType.values()[i];
            String blockPref = BLOCK_PREFS[i];
            Set<String> blocks = PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getStringSet(blockPref, new HashSet<String>());
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
        return blocklist.get(blockType).contains(block);
    }

    private static void saveBlocklist(Context context) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        for (int i = 0; i < BlockType.values().length; i++) {
            BlockType blockType = BlockType.values()[i];
            String blockPref = BLOCK_PREFS[i];
            editor.putStringSet(blockPref, blocklist.get(blockType));
        }
        editor.commit();
    }

}
