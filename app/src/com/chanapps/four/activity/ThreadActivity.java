package com.chanapps.four.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.chanapps.four.adapter.AbstractThreadCursorAdapter;
import com.chanapps.four.adapter.ThreadListCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.loader.ThreadCursorLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/27/12
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadActivity extends BoardActivity implements ChanIdentifiedActivity, RefreshableActivity {

    protected static final String TAG = ThreadActivity.class.getSimpleName();
    public static final boolean DEBUG = false;

    public static final int WATCHLIST_ACTIVITY_THRESHOLD = 7; // arbitrary from experience
    private static final int MAX_TEXT_LINES = 9; // most we support, maybe change per screen layout?

    protected long threadNo;
    protected String text;
    protected String imageUrl;
    protected int imageWidth;
    protected int imageHeight;
    protected boolean hidePostNumbers = true;
    protected UserStatistics userStats = null;
    protected boolean inWatchlist = false;
    protected ThreadPostPopup threadPostPopup;
    protected ChanThread thread = null;
    private int threadPos = 0;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        if (threadNo > 0) {
        	cursorLoader = new ThreadCursorLoader(this, boardCode, threadNo, absListView);
        }
        return cursorLoader;
    }

    public static void startActivity(Activity from, AdapterView<?> adapterView, int position) {
        startActivity(from, adapterView, null, position, 0, false);
    }

    public static void startActivity(Activity from, AdapterView<?> adapterView, View view, int position, long id, boolean fromParent) {
            Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final long threadTim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
        if (DEBUG) Log.d(TAG, "threadTim: " + threadTim);
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        final String boardName = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
        ChanBoard board = ChanFileStorage.loadBoardData(from, boardName); // better way to do this? bad to run on UI thread
        if (board != null && board.defData) // def data are not clicable
        	return;
        final String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        final String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        final int tn_w = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_TN_W));
        final int tn_h = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_TN_H));
        final int pos = adapterView.getFirstVisiblePosition();
        Intent intent = createIntentForActivity(from, boardName, postId, text, imageUrl, tn_w, tn_h, threadTim, fromParent, pos);
        if (DEBUG) Log.i(TAG, "Calling thread activity with postId=" + postId);
        from.startActivity(intent);
    }
    
    public static void startActivity(Activity from, ChanThread thread, View view, long id, boolean fromParent) {
	    final long threadTim = thread.tim;
	    if (DEBUG) Log.d(TAG, "threadTim: " + threadTim);
	    final long postId = thread.no;
	    final String boardName = thread.board;
        /* still needed?
	    ChanBoard board = ChanFileStorage.loadBoardData(from, boardName); // better way to do this? bad to run on UI thread
	    if (board != null && board.defData) // def data are not clicable
	    	return;
	    */
	    final String text = thread.getFullText();
	    final String imageUrl = thread.getImageUrl();
	    final int tn_w = thread.tn_w;
	    final int tn_h = thread.tn_h;
	    final int pos = 0;
	    Intent intent = createIntentForActivity(from, boardName, postId, text, imageUrl, tn_w, tn_h, threadTim, fromParent, pos);
	    if (DEBUG) Log.i(TAG, "Calling thread activity with postId=" + postId);
	    from.startActivity(intent);
    }

    public static void startActivity(Activity from, String boardCode, long threadNo) {
        if (threadNo <= 0) {
            startActivity(from, boardCode);
            return;
        }
        final long threadTim = 0;
	    final String text = "";
	    final String imageUrl = "";
	    final int tn_w = 0;
	    final int tn_h = 0;
	    final int pos = 0;
	    Intent intent = createIntentForActivity(from, boardCode, threadNo, text, imageUrl, tn_w, tn_h, threadTim, false, pos);
	    from.startActivity(intent);
    }

    public static Intent createIntentForThread(Context context, ChanPost thread) {
        return createIntentForActivity(
                context,
                thread.board,
                thread.no,
                thread.getHeaderText(),
                thread.getThumbnailUrl(),
                thread.tn_w,
                thread.tn_h,
                thread.tim,
                false,
                0,
                true
        );
    }

    public static Intent createIntentForActivity(Context context,
            final String boardCode,
            final long threadNo,
            final String text,
            final String imageUrl,
            final int tn_w,
            final int tn_h,
            final long tim,
            final boolean fromParent,
            final int firstVisiblePosition)
    {
    	return createIntentForActivity(context, boardCode, threadNo, text, imageUrl, tn_w, tn_h, tim, fromParent, firstVisiblePosition, false);
    }

    public static Intent createIntentForActivity(Context context,
                                                 final String boardCode,
                                                 final long threadNo,
                                                 final String text,
                                                 final String imageUrl,
                                                 final int tn_w,
                                                 final int tn_h,
                                                 final long tim,
                                                 final boolean fromParent,
                                                 final int firstVisiblePosition,
                                                 final boolean refreshBoard)
    {
        Intent intent = new Intent(context, ThreadActivity.class);
        intent.putExtra(ChanHelper.TIM, tim);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.TEXT, text);
        intent.putExtra(ChanHelper.IMAGE_URL, imageUrl);
        intent.putExtra(ChanHelper.IMAGE_WIDTH, tn_w);
        intent.putExtra(ChanHelper.IMAGE_HEIGHT, tn_h);
        intent.putExtra(ChanHelper.LAST_BOARD_POSITION, firstVisiblePosition);
        intent.putExtra(ChanHelper.LAST_THREAD_POSITION, 0);
        intent.putExtra(ChanHelper.TRIGGER_BOARD_REFRESH, refreshBoard);
        if (fromParent)
            intent.putExtra(ChanHelper.FROM_PARENT, true);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(ChanHelper.LAST_THREAD_POSITION, 0); // reset it
        editor.commit();
        return intent;
    }

    @Override
    protected void loadFromIntentOrPrefs() {
        ensurePrefs();
        Intent intent = getIntent();
        boardCode = intent.hasExtra(ChanHelper.BOARD_CODE)
                ? intent.getStringExtra(ChanHelper.BOARD_CODE)
                : prefs.getString(ChanHelper.BOARD_CODE, "a");
        threadNo = intent.hasExtra(ChanHelper.THREAD_NO)
                ? intent.getLongExtra(ChanHelper.THREAD_NO, 0)
                : prefs.getLong(ChanHelper.THREAD_NO, 0);
        tim = intent.hasExtra(ChanHelper.TIM)
                ? intent.getLongExtra(ChanHelper.TIM, 0)
                : prefs.getLong(ChanHelper.TIM, 0);
        text = intent.hasExtra(ChanHelper.TEXT)
                ? intent.getStringExtra(ChanHelper.TEXT)
                : prefs.getString(ChanHelper.TEXT, null);
        imageUrl = intent.hasExtra(ChanHelper.IMAGE_URL)
                ? intent.getStringExtra(ChanHelper.IMAGE_URL)
                : prefs.getString(ChanHelper.IMAGE_URL, null);
        imageWidth = intent.hasExtra(ChanHelper.IMAGE_WIDTH)
                ? intent.getIntExtra(ChanHelper.IMAGE_WIDTH, 0)
                : prefs.getInt(ChanHelper.IMAGE_WIDTH, 0);
        imageHeight = intent.hasExtra(ChanHelper.IMAGE_HEIGHT)
                ? intent.getIntExtra(ChanHelper.IMAGE_HEIGHT, 0)
                : prefs.getInt(ChanHelper.IMAGE_HEIGHT, 0);
        if (intent.getBooleanExtra(ChanHelper.TRIGGER_BOARD_REFRESH, false)) {
        	FetchChanDataService.scheduleBoardFetch(getBaseContext(), boardCode);
        }
        if (DEBUG) Log.i(TAG, "Thread no read from intent: " + threadNo);
    }

    @Override
    protected void saveInstanceState() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(ChanHelper.BOARD_CODE, boardCode);
        editor.putLong(ChanHelper.THREAD_NO, threadNo);
        editor.putInt(ChanHelper.LAST_THREAD_POSITION, absListView.getFirstVisiblePosition());
        editor.putLong(ChanHelper.TIM, tim);
        editor.putString(ChanHelper.TEXT, text);
        editor.putString(ChanHelper.IMAGE_URL, imageUrl);
        editor.putInt(ChanHelper.IMAGE_WIDTH, imageWidth);
        editor.putInt(ChanHelper.IMAGE_HEIGHT, imageHeight);
        editor.commit();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    protected void initAdapter() {
        adapter = new ThreadListCursorAdapter(this,
                R.layout.thread_list_item,
                this,
                new String[] {
                        ChanHelper.POST_IMAGE_URL,
                        ChanHelper.POST_SHORT_TEXT,
                        ChanHelper.POST_TEXT,
                        ChanHelper.POST_COUNTRY_URL,
                        ChanHelper.POST_DATE_TEXT,
                        ChanHelper.POST_IMAGE_DIMENSIONS,
                        ChanHelper.POST_EXPAND_BUTTON,
                        ChanHelper.POST_EXPAND_BUTTON
                },
                new int[] {
                        R.id.list_item_image,
                        R.id.list_item_header,
                        R.id.list_item_text,
                        R.id.list_item_country_flag,
                        R.id.list_item_date,
                        R.id.list_item_image_overlay
                });
        absListView.setAdapter(adapter);
    }

    @Override
    protected String getLastPositionName() {
        return ChanHelper.LAST_THREAD_POSITION;
    }

    @Override
    protected void sizeGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer((GridView)absListView, display, ChanGridSizer.ServiceType.THREAD);
        cg.sizeGridToDisplay();
    }

    @Override
    protected void initAbsListView() {
        absListView = (ListView)findViewById(R.id.thread_list_view);
    }

    @Override
    protected void createAbsListView() {
        setAbsListViewClass();
        setContentView(getLayoutId());
        initAbsListView();
        initAdapter();
        absListView.setClickable(false);
        absListView.setLongClickable(false);
    }

    @Override
    public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
        //if (!ensureThreadCache()) {
        //    view.setVisibility(View.GONE);
        //    return false;
        //}
        //ChanPost post = getPost(cursor);
        //if (post == null)
        //    return false;
        switch (view.getId()) {
            case R.id.list_item:
                return setItem((RelativeLayout)view, cursor);
            case R.id.list_item_image:
                return setItemImage((ImageView)view, cursor);
            case R.id.list_item_country_flag:
                return setItemCountryFlag((ImageView)view, cursor);
            case R.id.list_item_header:
                return setItemHeaderValue((TextView)view, cursor);
            case R.id.list_item_text:
                return setItemMessageValue((TextView)view, cursor);
            case R.id.list_item_date:
                return setItemDateValue((TextView)view, cursor);
            case R.id.list_item_image_overlay:
                return setItemImageOverlayValue((TextView) view, cursor);
            default:
                return false;
        }
    }

    private boolean setItem(final RelativeLayout v, final Cursor cursor) {
        final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        if (adItem > 0)
            v.setOnClickListener(new AdOnClickListener(cursor));
        else
            v.setOnClickListener(new PopupOnClickListener(cursor.getPosition()));
        return true;
    }

    private boolean setItemHeaderValue(final TextView tv, final Cursor cursor) {
        String shortText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT));
        tv.setText(Html.fromHtml(shortText));
        final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        if (adItem > 0)
            tv.setOnClickListener(new AdOnClickListener(cursor));
        else
            tv.setOnClickListener(new PopupOnClickListener(cursor.getPosition()));
        return true;
    }

    private boolean setItemMessageValue(final TextView tv, final Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        tv.setText(Html.fromHtml(text));
        final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        if (adItem > 0)
            tv.setOnClickListener(new AdOnClickListener(cursor));
        else
            tv.setOnClickListener(new PopupOnClickListener(cursor.getPosition()));
        return true;
    }

    private class PopupOnClickListener implements View.OnClickListener {
        private int position = 0;
        public PopupOnClickListener(final int position) {
            this.position = position;
        }
        @Override
        public void onClick(View v) {
            if (v instanceof TextView) {
                TextView tv = (TextView)v;
                int start = tv.getSelectionStart();
                int end = tv.getSelectionEnd();
                Log.e(TAG, "Exception clicked view=" + v + " start=" + start + " end=" + end);
                if (start == -1 || end == -1 || start == end) // non-web click
                    ensurePopup().showFromCursor(ThreadActivity.this.absListView, position);
            }
            else {
                Log.e(TAG, "Exception clicked view=" + v);
                ensurePopup().showFromCursor(ThreadActivity.this.absListView, position);
            }
        }
    }

    private boolean setItemDateValue(final TextView tv, final Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_DATE_TEXT));
        tv.setText(text == null ? "" : text);
        tv.setOnClickListener(new PopupOnClickListener(cursor.getPosition()));
        return true;
    }

    private boolean setItemImageOverlayValue(final TextView tv, final Cursor cursor) {
        String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_DIMENSIONS));
        tv.setText(text == null ? "" : text);
        tv.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
        tv.setOnClickListener(new ImageOnClickListener(cursor));
        return true;
    }

    private boolean setItemImage(final ImageView iv, final Cursor cursor) {
        super.setImageViewValue(iv, cursor);
        final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        if (adItem > 0)
            iv.setOnClickListener(new AdOnClickListener(cursor));
        else
            iv.setOnClickListener(new ImageOnClickListener(cursor));
        return true;
    }

    private class AdOnClickListener implements View.OnClickListener {

        private String adUrl = null;

        public AdOnClickListener(Cursor cursor) {
            adUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        }

        @Override
        public void onClick(View v) {
            ChanHelper.fadeout(ThreadActivity.this, v);
            launchUrlInBrowser(adUrl);
        }

    }

    private class ImageOnClickListener implements View.OnClickListener {

        long postId = 0;
        long resto = 0;
        int w = 0;
        int h = 0;
        int position = 0;

        public ImageOnClickListener(Cursor cursor) {
            postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
            resto = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_RESTO));
            w = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_W));
            h = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_H));
            position = cursor.getPosition();
        }

        @Override
        public void onClick(View v) {
            ChanHelper.fadeout(ThreadActivity.this, v);
            incrementCounterAndAddToWatchlistIfActive();
            GalleryViewActivity.startActivity(
                    ThreadActivity.this, boardCode, threadNo, postId, w, h, position);
        }
    }

    private boolean setItemCountryFlag(final ImageView iv, final Cursor cursor) {
        String countryFlagImageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_COUNTRY_URL));
        if (countryFlagImageUrl != null && !countryFlagImageUrl.isEmpty())
            smartSetImageView(iv, countryFlagImageUrl, imageLoader, displayImageOptions);
        else
            iv.setImageBitmap(null); // blank
        return true;
    }


    private boolean ensureThreadCache() {
        if (thread == null)
            thread = ChanFileStorage.loadThreadData(this, boardCode, threadNo);
        if (thread == null || thread.posts == null || thread.posts.length == 0)
            return false; // couldn't get thread
        return true;
    }

    private ChanPost getPost(final Cursor cursor) {
        int pos = cursor.getPosition();
        if (pos == 0) { // the thread
            threadPos = 0; // index for tracking current post
        }
        long postNo = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        while (threadPos < thread.posts.length && thread.posts[threadPos].no != postNo) // spin past removed posts
            threadPos++;
        if (threadPos >= thread.posts.length)
            return null; // couldn't find post
        return thread.posts[threadPos];
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
/*
        MenuItem item = menu.findItem(R.id.hide_post_numbers);
        if (boardCode.equals("b")) {
            item.setEnabled(false);
            item.setVisible(false);
        }
        else {
            item.setEnabled(true);
            item.setVisible(true);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, true);
            if (hidePostNumbers)
                item.setTitle(R.string.pref_hide_post_numbers_turn_off);
            else
                item.setTitle(R.string.pref_hide_post_numbers_turn_on);
        }
*/
        ChanBoard.setupActionBarBoardSpinner(this, menu, boardCode);
        return true;
    }

    @Override
    protected void setAbsListViewClass() {
        absListViewClass = ListView.class;
    }

    protected void toggleHidePostNumbers() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, false);
        hidePostNumbers = !hidePostNumbers; // invert
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, hidePostNumbers);
        editor.commit();
        refreshActivity();
    }

    private void postReply() {
        Intent replyIntent = new Intent(getApplicationContext(), PostReplyActivity.class);
        replyIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        replyIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
        replyIntent.putExtra(ChanHelper.POST_NO, 0);
        replyIntent.putExtra(ChanHelper.TIM, tim);
        replyIntent.putExtra(ChanHelper.TEXT, "");
        startActivity(replyIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // FIXME: know that I'm coming from watching and return there
                Intent upIntent = new Intent(this, BoardActivity.class);
                upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                upIntent.putExtra(ChanHelper.LAST_BOARD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));

                if (DEBUG) Log.i(TAG, "Made up intent with board=" + boardCode);
//                if (NavUtils.shouldUpRecreateTask(this, upIntent)) { // needed when calling from widget
//                    if (DEBUG) Log.i(TAG, "Should recreate task");
                    TaskStackBuilder.create(this).addParentStack(this).startActivities();
                    this.finish();
//                }
//                else {
//                    if (DEBUG) Log.i(TAG, "Navigating up...");
//                    NavUtils.navigateUpTo(this, upIntent);
//                }
                return true;
            case R.id.refresh_thread_menu:
                progressBar.setVisibility(View.VISIBLE);
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            case R.id.post_reply_menu:
                postReply();
                return true;
            case R.id.view_image_gallery_menu:
                GalleryViewActivity.startAlbumViewActivity(this, boardCode, threadNo);
                return true;
/*
            case R.id.hide_post_numbers:
                toggleHidePostNumbers();
                return true;
*/
            case R.id.watch_thread_menu:
                addToWatchlist();
                return true;
            case R.id.download_all_images_menu:
            	ThreadImageDownloadService.startDownloadToBoardFolder(getBaseContext(), boardCode, threadNo);
                Toast.makeText(this, R.string.download_all_images_notice_prefetch, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.download_all_images_to_gallery_menu:
            	ThreadImageDownloadService.startDownloadToGalleryFolder(getBaseContext(), boardCode, threadNo, null);
                Toast.makeText(this, R.string.download_all_images_notice, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.go_to_top_position_menu:
                if (absListView != null && absListView.getAdapter() != null && absListView.getAdapter().getCount() > 0)
                    absListView.setSelection(0);
                return true;
            case R.id.go_to_end_position_menu:
                if (absListView != null && absListView.getAdapter() != null && absListView.getAdapter().getCount() > 0)
                    absListView.setSelection(absListView.getAdapter().getCount());
                return true;
            case R.id.settings_menu:
                if (DEBUG) Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.layout.about_dialog, R.raw.help_header, R.raw.help_thread_list);
                rawResourceDialog.show();
                return true;
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
            case R.id.exit_menu:
                ChanHelper.exitApplication(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (DEBUG) Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thread_menu, menu);
        return true;
    }

    @Override
    protected void setActionBarTitle() {
        ActionBar a = getActionBar();
        if (a == null) {
            return;
        }
        //String title = "/" + boardCode + "/" + threadNo;
        //a.setTitle(title);
        a.setDisplayShowTitleEnabled(false);
        a.setDisplayHomeAsUpEnabled(true);
        invalidateOptionsMenu();
    }

    protected void initPopup() {
        threadPostPopup = new ThreadPostPopup(this,
                this.getLayoutInflater(),
                imageLoader,
                displayImageOptions,
                (AbstractThreadCursorAdapter)adapter);
    }

    protected UserStatistics ensureUserStats() {
        if (userStats == null) {
            userStats = ChanFileStorage.loadUserStats(getBaseContext());
        }
        return userStats;
    }

    protected void incrementCounterAndAddToWatchlistIfActive() {
        ensureUserStats().threadUse(boardCode, threadNo);
        String key = boardCode + "/" + threadNo;
        ChanThreadStat stat = ensureUserStats().boardThreadStats.get(key);
        if (stat != null && stat.usage >= WATCHLIST_ACTIVITY_THRESHOLD && !inWatchlist) {
            int stringId = ChanWatchlist.watchThread(this, tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
            if (stringId == R.string.thread_added_to_watchlist)
                Toast.makeText(this, R.string.thread_added_to_watchlist_activity_based, Toast.LENGTH_SHORT).show();
            inWatchlist = true;
        }
    }

    protected void addToWatchlist() {
        int stringId = ChanWatchlist.watchThread(this, tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
        Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show();
        inWatchlist = true;
    }

    public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.THREAD_ACTIVITY, boardCode, threadNo);
	}

    @Override
    protected int getLayoutId() {
        if (GridView.class.equals(absListViewClass))
            return R.layout.thread_list_layout;
        else
            return R.layout.thread_list_layout;
    }

    protected ThreadPostPopup ensurePopup() {
        if (threadPostPopup == null) {
            initPopup();
        }
        return threadPostPopup;
    }

}
