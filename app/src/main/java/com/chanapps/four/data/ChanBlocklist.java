package com.chanapps.four.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import com.chanapps.four.activity.SettingsActivity;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/2/13
 * Time: 9:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanBlocklist {

    private static final String TAG = ChanBlocklist.class.getSimpleName();
    private static final boolean DEBUG = false;

    public enum BlockType {
        TEXT ("text", SettingsActivity.PREF_BLOCKLIST_TEXT),
        TRIPCODE ("tripcode", SettingsActivity.PREF_BLOCKLIST_TRIPCODE),
        NAME ("name", SettingsActivity.PREF_BLOCKLIST_NAME),
        EMAIL ("email", SettingsActivity.PREF_BLOCKLIST_EMAIL),
        ID ("id", SettingsActivity.PREF_BLOCKLIST_ID),
        THREAD ("thread", SettingsActivity.PREF_BLOCKLIST_THREAD);
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
    private static Pattern testPattern = null;

     private static void initBlocklist(Context context) {
        if (blocklist != null)
            return;
        blocklist = new HashMap<BlockType, Set<String>>();
        synchronized (blocklist) {
            blocklist.clear();
            for (int i = 0; i < BlockType.values().length; i++) {
                BlockType blockType = BlockType.values()[i];
                Set<String> savedBlocks = PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .getStringSet(blockType.blockPref(), new HashSet<String>());
                // copy to avoid android getStringSet bug
                Set<String> blocks = new HashSet<String>(savedBlocks.size());
                blocks.addAll(savedBlocks);
                blocklist.put(blockType, blocks);
            }
            compileTestPattern();
        }
    }

    public static List<Pair<String, BlockType>> getSorted(Context context) {
        if (blocklist == null)
            initBlocklist(context);
        List<Pair<String, BlockType>> sorted = new ArrayList<Pair<String, BlockType>>();
        for (BlockType blockType : BlockType.values()) {
            Set<String> blocks = blocklist.get(blockType);
            if (DEBUG) Log.i(TAG, "getSorted() type=" + blockType + " blocks=" + blocks);
            if (blocks == null || blocks.isEmpty())
                continue;
            for (String block : blocks) {
                if (block != null && !block.isEmpty())
                    sorted.add(new Pair<String, BlockType>(block, blockType));
            }
        }
        Collections.sort(sorted, blocklistComparator);
        return sorted;

    }

    protected static Comparator<Pair<String, BlockType>> blocklistComparator = new Comparator<Pair<String, BlockType>>() {
        @Override
        public int compare(Pair<String, BlockType> lhs, Pair<String, BlockType> rhs) {
            int comp1 = lhs.first.compareToIgnoreCase(rhs.first);
            if (comp1 != 0)
                return comp1;
            return lhs.second.compareTo(rhs.second);
        }
    };

    public static void removeAll(Context context, BlockType blockType, List<String> removeBlocks) {
        if (blocklist == null)
            initBlocklist(context);
        Set<String> blocks = blocklist.get(blockType);
        blocks.removeAll(removeBlocks);
        saveBlocklist(context, blockType);
    }

    public static void remove(Context context, BlockType blockType, String removeBlock) {
        if (blocklist == null)
            initBlocklist(context);
        Set<String> blocks = blocklist.get(blockType);
        saveBlocklist(context, blockType);
        List<String> removeBlocks = new ArrayList<String>(1);
        removeBlocks.add(removeBlock);
        blocks.removeAll(removeBlocks);
        removeAll(context, blockType, removeBlocks);
    }

    public static void removeMatching(Context context, BlockType blockType, String substring) { // substring non-regexp match
        if (substring == null || substring.isEmpty())
            return;
        if (blocklist == null)
            initBlocklist(context);
        Set<String> blocks = blocklist.get(blockType);
        List<String> removeBlocks = new ArrayList<String>();
        for (String b : blocks) {
            if (b != null && b.contains(substring))
                removeBlocks.add(b);
        }
        blocks.removeAll(removeBlocks);
        saveBlocklist(context, blockType);
    }

    public static boolean hasMatching(Context context, BlockType blockType, String substring) { // substring non-regexp match
        if (substring == null || substring.isEmpty())
            return false;
        if (blocklist == null)
            initBlocklist(context);
        Set<String> blocks = blocklist.get(blockType);
        if (blocks == null)
            return false;
        for (String b : blocks) {
            if (b != null && b.contains(substring))
                return true;
        }
        return false;
    }

    public static void add(Context context, BlockType blockType, String newBlock) {
        if (blocklist == null)
            initBlocklist(context);
        Set<String> blocks = blocklist.get(blockType);
        List<String> newBlocks = new ArrayList<String>(1);
        newBlocks.add(newBlock);
        blocks.addAll(newBlocks);
        saveBlocklist(context, blockType);
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
        if (blocklist == null)
            initBlocklist(context);
        boolean simpleMatch =  contains(context, BlockType.THREAD, post.uniqueId())
                || contains(context, BlockType.TRIPCODE, post.trip)
                || contains(context, BlockType.NAME, post.name)
                || contains(context, BlockType.EMAIL, post.email)
                || contains(context, BlockType.ID, post.id)
                ;
        if (simpleMatch)
            return true;
        if (testPattern == null)
            return false;
        if (post.sub != null && testPattern.matcher(post.sub).find())
            return true;
        if (post.com != null && testPattern.matcher(post.com).find())
            return true;
        return false;
    }

    public static boolean isBlocked(Context context, ChanThread thread) {
        if (isBlocked(context, (ChanPost)thread))
            return true;
        if (thread.lastReplies == null)
            return false;
        for (ChanPost post : thread.lastReplies)
            if (isBlocked(context, post))
                return true;
        return false;
    }

    private static void saveBlocklist(Context context, BlockType blockType) {
        if (blockType == BlockType.TEXT) // precreate regex for efficient matching
            compileTestPattern();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        Set<String> blocks = blocklist.get(blockType);
        Set<String> copy = new HashSet<String>(blocks.size());
        copy.addAll(blocks);
        if (DEBUG) Log.i(TAG, "saveBlocklist() type=" + blockType + " blocks=" + copy);
        editor.putStringSet(blockType.blockPref(), copy).apply();
    }

    private static void compileTestPattern() {
        Set<String> blocks = blocklist.get(BlockType.TEXT);
        if (blocks.isEmpty()) {
            testPattern = null;
            return;
        }
        List<String> regexList = new ArrayList<String>();
        for (String block : blocks) {
            if (block == null || block.isEmpty())
                continue;
            String regex = block.replaceAll("[()|]", "");
            if (regex.isEmpty())
                continue;
            regexList.add(regex);
        }
        if (regexList.isEmpty()) {
            testPattern = null;
            return;
        }
        String regex = "(" + StringUtils.join(regexList, "|") + ")";
        testPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public static void save(Context context, List<Pair<String, BlockType>> newBlocks) {
        if (blocklist == null)
            initBlocklist(context);
        synchronized (blocklist) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            for (BlockType blockType : BlockType.values()) // out with the old
                blocklist.get(blockType).clear();
            for (Pair<String, BlockType> block : newBlocks) // and in with the new
                if (block.first != null && !block.first.isEmpty() && block.second != null)
                    blocklist.get(block.second).add(block.first);
            for (BlockType blockType : BlockType.values()) {
                if (blockType == BlockType.TEXT) // precreate regex for efficient matching
                    compileTestPattern();
                Set<String> blocks = blocklist.get(blockType);
                if (DEBUG) Log.i(TAG, "save() type=" + blockType + " blocks=" + blocks);
                editor.putStringSet(blockType.blockPref(), blocks);
            }
            editor.apply();
        }
    }

}
