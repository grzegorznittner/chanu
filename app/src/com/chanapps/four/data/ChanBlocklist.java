package com.chanapps.four.data;

import android.content.Context;
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

    private static Set<String> blocklist;

    private static void initBlocklist(Context context) {
        blocklist = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(ChanHelper.PREF_BLOCKLIST, new HashSet<String>());
    }

    public static String[] getSorted(Context context) {
        if (blocklist == null)
            initBlocklist(context);
        List<String> sorted = new ArrayList<String>();
        sorted.addAll(blocklist);
        Collections.sort(sorted);
        String[] blocklistArray = new String[sorted.size()];
        return sorted.toArray(blocklistArray);
    }

    public static void remove(Context context, String id) {
        if (blocklist == null)
            initBlocklist(context);
        if (blocklist.contains(id)) {
            blocklist.remove(id);
            saveBlocklist(context);
        }
    }

    public static void add(Context context, String id) {
        if (blocklist == null)
            initBlocklist(context);
        if (!blocklist.contains(id)) {
            blocklist.add(id);
            saveBlocklist(context);
        }
    }

    public static boolean contains(Context context, String id) {
        if (blocklist == null)
            initBlocklist(context);
        return blocklist.contains(id);
    }

    private static void saveBlocklist(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(ChanHelper.PREF_BLOCKLIST, blocklist)
                .commit();
    }

}
