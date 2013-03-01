package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.preference.PreferenceManager;
import android.util.Log;

import android.widget.AbsListView;
import android.widget.GridView;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.*;

public class ThreadCursorLoader extends BoardCursorLoader {

    private static final String TAG = ThreadCursorLoader.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int DEFAULT_NUM_GRID_COLUMNS_PORTRAIT = 2;
    private static final int DEFAULT_NUM_GRID_COLUMNS_LANDSCAPE = 3;

    protected SharedPreferences prefs;
    protected long threadNo;
    protected int numGridColumns;
    private boolean hidePostNumbers;
    private boolean useFriendlyIds;

    protected ThreadCursorLoader(Context context) {
        super(context);
    }

    public ThreadCursorLoader(Context context, String boardName, long threadNo, AbsListView absListView) {
        this(context);
        this.context = context;
        this.boardName = boardName;
        this.threadNo = threadNo;
        initRandomGenerator();
        ChanHelper.Orientation orientation = ChanHelper.getOrientation(context);
        int defaultNumColumns = (orientation == ChanHelper.Orientation.PORTRAIT) ? DEFAULT_NUM_GRID_COLUMNS_PORTRAIT : DEFAULT_NUM_GRID_COLUMNS_LANDSCAPE;
        if (absListView instanceof GridView) {
            GridView gridView = (GridView)absListView;
            int currentNumGridColumns = gridView.getNumColumns();
            this.numGridColumns = (gridView == null || currentNumGridColumns <= 0) ? defaultNumColumns : currentNumGridColumns;
        }
        else {
            this.numGridColumns = 0;
        }
        if (threadNo == 0) {
            throw new ExceptionInInitializerError("Can't have zero threadNo in a thread cursor loader");
        }
        ChanPost.initClickForMore(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected void initRandomGenerator() { // to allow repeatable positions for ads
        generatorSeed = threadNo;
        generator = new Random(generatorSeed);
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
        try {
            hidePostNumbers = boardName.equals("b") ? false : prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, true);
            useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
            ChanThread thread = null;
            try {
                thread = ChanFileStorage.loadThreadData(getContext(), boardName, threadNo);
            } catch (Exception e) {
                Log.e(TAG, "Couldn't load thread from storage " + boardName + "/" + threadNo, e);
                thread = null;
            }
            int isDead = thread != null && thread.isDead ? 1 : 0;
            if (DEBUG) Log.i(TAG, "loadInBackground " + thread.board + "/" + thread.no + " num posts " + (thread.posts != null ? thread.posts.length : 0));
            if (DEBUG) Log.i(TAG, "Thread dead status for " + boardName + "/" + threadNo + " is " + isDead);
            if (DEBUG) Log.i(TAG, "Thread closed status for " + boardName + "/" + threadNo + " is closed=" + thread.closed);
            MatrixCursor matrixCursor = new MatrixCursor(ChanHelper.POST_COLUMNS);

            if (thread != null && thread.posts != null && thread.posts.length > 0) { // show loading for no thread data
                int adSpace = MINIMUM_AD_SPACING;
                for (ChanPost post : thread.posts) {
                    if (ChanBlocklist.contains(context, post.id))
                        continue;
                    post.isDead = thread.isDead; // inherit from parent
                    post.closed = thread.closed; // inherit
                    post.hidePostNumbers = hidePostNumbers;
                    post.useFriendlyIds = useFriendlyIds;
                    if (post.resto == 0) {
                        addThreadHeaderRows(matrixCursor, post);
                    }
                    else if (post.tn_w <= 0 || post.tim == 0) {
                        addTextOnlyRow(matrixCursor, post);
                    } else {
                        addImageRow(matrixCursor, post);
                    }

                    if (generator.nextDouble() < AD_PROBABILITY && !(adSpace > 0)) {
                        addAdRow(matrixCursor);
                        adSpace = MINIMUM_AD_SPACING;
                    }
                    else {
                        adSpace--;
                    }
                }
                int remainingToLoad = thread.posts[0].replies - thread.posts.length;
                if (DEBUG) Log.i(TAG, "Remaining to load:" + remainingToLoad);
            }
            registerContentObserver(matrixCursor, mObserver);
            return matrixCursor;
    	} catch (Exception e) {
    		Log.e(TAG, "loadInBackground", e);
    		return null;
    	}
    }

    protected void addThreadHeaderRows(MatrixCursor matrixCursor, ChanPost post) {
        Object[] currentRow;
        if (post.tn_w <= 0 || post.tim == 0) { // text-only thread header
            currentRow = new Object[] {
                    post.no, boardName, threadNo,
                    "", post.getCountryFlagUrl(),
                    post.getUserHeaderText(), post.getDateText(), post.getFullText(),
                    post.tn_w, post.tn_h, post.w, post.h, post.tim, post.spoiler,
                    post.getSpoilerText(), post.getExifText(), post.id, post.trip, post.name, post.email, post.getImageDimensions(),
                    post.isDead ? 1 : 0, post.closed, 0, 0};
            matrixCursor.addRow(currentRow);
            if (DEBUG) Log.v(TAG, "added cursor row text-only no=" + post.no + " text=" + post.com);
        } else {
            currentRow = new Object[] { // image header
                    post.no, boardName, threadNo,
                    post.getThumbnailUrl(), post.getCountryFlagUrl(),
                    post.getUserHeaderText(), post.getDateText(), post.getFullText(),
                    post.tn_w, post.tn_h, post.w, post.h, post.tim, post.spoiler,
                    post.getSpoilerText(), post.getExifText(), post.id, post.trip, post.name, post.email, post.getImageDimensions(),
                    post.isDead ? 1 : 0, post.closed, 0, 0};
            matrixCursor.addRow(currentRow);
            if (DEBUG) Log.v(TAG, "added cursor row image+text no=" + post.no + " spoiler=" + post.spoiler + " text=" + post.com);
        }
        // for initial thread, add extra null item to support full-width header
        if (numGridColumns > 0) {
            if (DEBUG) Log.v(TAG, "added extra null rows for grid columns=1.." + numGridColumns);
            for (int i = 1; i < numGridColumns; i++) {
                Object[] nullRow = currentRow.clone();
                nullRow[0] = 0; // set postNo to zero to signal to rest of system that this is a null post
                matrixCursor.addRow(nullRow);
            }
        }
    }

    @Override
    protected void addTextOnlyRow(MatrixCursor matrixCursor, ChanPost post) {
        Object[] currentRow;
        currentRow = new Object[] {
                post.no, boardName, threadNo,
                "", post.getCountryFlagUrl(),
                post.getUserHeaderText(), post.getDateText(), post.getFullText(),
                post.tn_w, post.tn_h, post.w, post.h, post.tim, post.spoiler,
                post.getSpoilerText(), post.getExifText(), post.id, post.trip, post.name, post.email, post.getImageDimensions(),
                post.isDead ? 1 : 0, post.closed, 0, 0};
        matrixCursor.addRow(currentRow);
        if (DEBUG) Log.v(TAG, "added cursor row text-only no=" + post.no + " text=" + post.com);
    }

    @Override
    protected void addImageRow(MatrixCursor matrixCursor, ChanPost post) {
        Object[] currentRow = new Object[] {
                post.no, boardName, threadNo,
                post.getThumbnailUrl(), post.getCountryFlagUrl(),
                post.getUserHeaderText(), post.getDateText(), post.getFullText(),
                post.tn_w, post.tn_h, post.w, post.h, post.tim, post.spoiler,
                post.getSpoilerText(), post.getExifText(), post.id, post.trip, post.name, post.email, post.getImageDimensions(),
                post.isDead ? 1 : 0, post.closed, 0, 0};
        matrixCursor.addRow(currentRow);
        if (DEBUG) Log.v(TAG, "added cursor row image+text no=" + post.no + " spoiler=" + post.spoiler + " text=" + post.com);
    }

    @Override
    protected void addAdRow(MatrixCursor matrixCursor) {
        ChanBoard board = ChanBoard.getBoardByCode(this.getContext(), boardName);
        boolean adultBoard = board != null && !board.workSafe ? true : false;
        int adCode =
                (adultBoard && generator.nextDouble() < AD_ADULT_PROBABILITY_ON_ADULT_BOARD)
                        ? JLIST_AD_ADULT_CODES[generator.nextInt(JLIST_AD_ADULT_CODES.length)]
                        : JLIST_AD_SMALL_CODES[generator.nextInt(JLIST_AD_SMALL_CODES.length)];
        String imageUrl = JLIST_AD_IMAGE_ROOT_URL + "/" + adCode;
        String clickUrl = JLIST_AD_CLICK_ROOT_URL + "/" + adCode;
        matrixCursor.addRow(new Object[] {
                2, boardName, 0,
                imageUrl, "",
                getContext().getString(R.string.jlist_ad_message), "", clickUrl,
                -1, -1, -1, -1, 0, 0,
                "", "", "", "", "", "", "",
                0, 0, 0, 1});
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("boardName="); writer.println(boardName);
        writer.print(prefix); writer.print("threadNo="); writer.println(threadNo);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
