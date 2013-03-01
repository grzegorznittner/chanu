package com.chanapps.four.loader;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Random;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.*;

public class BoardCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = BoardCursorLoader.class.getSimpleName();
    private static final boolean DEBUG = true;

    protected static final double AD_PROBABILITY = 0.20;
    protected static final double AD_ADULT_PROBABILITY_ON_ADULT_BOARD = 1.0;
    protected static final int MINIMUM_AD_SPACING = 4;
    protected static final String JLIST_AD_AFFILIATE_CODE = "4539";
    protected static final int[] JLIST_AD_CODES = { 118, 113, 68 };
    protected static final int[] JLIST_AD_SMALL_CODES = { 21, 97, 104, 121, 120 };
    protected static final int[] JLIST_AD_ADULT_CODES = { 122, 70 };
    protected static final String JLIST_AD_ROOT_URL = "http://anime.jlist.com";
    protected static final String JLIST_AD_IMAGE_ROOT_URL = JLIST_AD_ROOT_URL + "/media/" + JLIST_AD_AFFILIATE_CODE;
    protected static final String JLIST_AD_CLICK_ROOT_URL = JLIST_AD_ROOT_URL + "/click/" + JLIST_AD_AFFILIATE_CODE;

    protected final ForceLoadContentObserver mObserver;

    protected Cursor mCursor;
    protected Context context;

    protected String boardName;

    protected long generatorSeed;
    protected Random generator;

    protected BoardCursorLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    public BoardCursorLoader(Context context, String boardName) {
        this(context);
        this.context = context;
        this.boardName = boardName;
        initRandomGenerator();
    }

    protected void initRandomGenerator() { // to allow repeatable positions for ads
        generatorSeed = boardName.hashCode();
        generator = new Random(generatorSeed);
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	if (DEBUG) Log.i(TAG, "loadInBackground");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean hidePostNumbers = boardName.equals("b") ? false : prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, true);
        boolean useFriendlyIds = prefs.getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        ChanBoard board = ChanFileStorage.loadBoardData(getContext(), boardName);
        MatrixCursor matrixCursor = new MatrixCursor(ChanHelper.POST_COLUMNS);
        if (board != null && board.threads != null && board.threads.length > 0 && !board.defData) { // show loading
            if (DEBUG) Log.i(TAG, "Loading " + board.threads.length + " threads");
            int adSpace = MINIMUM_AD_SPACING;

            for (ChanPost thread : board.threads) {

                if (ChanBlocklist.contains(context, thread.id))
                    continue;

                thread.hidePostNumbers = hidePostNumbers;
                thread.useFriendlyIds = useFriendlyIds;

                if (thread.tn_w <= 0 || thread.tim == 0)
                    addTextOnlyRow(matrixCursor, thread);
                else
                    addImageRow(matrixCursor, thread);

                if (generator.nextDouble() < AD_PROBABILITY && !(adSpace > 0)) {
                    addAdRow(matrixCursor);
                    adSpace = MINIMUM_AD_SPACING;
                }
                else {
                    adSpace--;
                }
            }
        }
        registerContentObserver(matrixCursor, mObserver);
        return matrixCursor;
    }

    protected void addTextOnlyRow(MatrixCursor matrixCursor, ChanPost thread) {
        matrixCursor.addRow(new Object[] {
                thread.no, boardName, 0,
                "",
                thread.getCountryFlagUrl(),
                thread.getBoardText(),
                thread.getDateText(),
                thread.getFullText(),
                thread.tn_w, thread.tn_h, thread.w, thread.h, thread.tim, thread.spoiler,
                thread.getSpoilerText(), thread.getExifText(), thread.id, thread.trip, thread.name, thread.email, thread.getImageDimensions(),
                thread.isDead ? 1 : 0, thread.closed, 0, 0});
    }

    protected void addImageRow(MatrixCursor matrixCursor, ChanPost thread) {
        matrixCursor.addRow(new Object[] {
                thread.no, boardName, 0,
                thread.getThumbnailUrl(), thread.getCountryFlagUrl(),
                thread.getBoardText(),
                thread.getDateText(),
                thread.getFullText(),
                thread.tn_w, thread.tn_h, thread.w, thread.h, thread.tim, thread.spoiler,
                thread.getSpoilerText(), thread.getExifText(), thread.id, thread.trip, thread.name, thread.email, thread.getImageDimensions(),
                thread.isDead ? 1 : 0, thread.closed, 0, 0});
    }

    protected void addAdRow(MatrixCursor matrixCursor) {
        ChanBoard board = ChanBoard.getBoardByCode(this.getContext(), boardName);
        boolean adultBoard = board != null && !board.workSafe ? true : false;
        int adCode =
            (adultBoard && generator.nextDouble() < AD_ADULT_PROBABILITY_ON_ADULT_BOARD)
            ? JLIST_AD_ADULT_CODES[generator.nextInt(JLIST_AD_ADULT_CODES.length)]
            : JLIST_AD_CODES[generator.nextInt(JLIST_AD_CODES.length)];
        String imageUrl = JLIST_AD_IMAGE_ROOT_URL + "/" + adCode;
        String clickUrl = JLIST_AD_CLICK_ROOT_URL + "/" + adCode;
        matrixCursor.addRow(new Object[] {
                2, boardName, 0, imageUrl,
                "",
                "",
                "", clickUrl,
                -1, -1, -1, -1, 0, 0,
                "", "", "", "", "", "", "",
                0, 0, 0, 1});
    }

    /**
     * Registers an observer to get notifications from the content provider
     * when the cursor needs to be refreshed.
     */
    void registerContentObserver(Cursor cursor, ContentObserver observer) {
        cursor.registerContentObserver(mObserver);
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Cursor cursor) {
		if (DEBUG) Log.i(TAG, "deliverResult isReset(): " + isReset());
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
    	if (DEBUG) Log.i(TAG, "onStartLoading mCursor: " + mCursor);
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
    	if (DEBUG) Log.i(TAG, "onStopLoading");
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
    	if (DEBUG) Log.i(TAG, "onCanceled cursor: " + cursor);
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        if (DEBUG) Log.i(TAG, "onReset cursor: " + mCursor);
        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("boardName="); writer.println(boardName);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
