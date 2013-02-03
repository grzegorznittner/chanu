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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chanapps.four.adapter.ThreadCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.GoToBoardDialogFragment;
import com.chanapps.four.loader.ThreadCursorLoader;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.ThreadImageDownloadService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

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

    protected long threadNo;
    protected String text;
    protected String imageUrl;
    protected int imageWidth;
    protected int imageHeight;
    protected boolean hideAllText = false;
    protected boolean hidePostNumbers = true;
    protected UserStatistics userStats = null;
    protected boolean inWatchlist = false;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        if (threadNo > 0) {
        	cursorLoader = new ThreadCursorLoader(this, boardCode, threadNo, gridView);
        }
        return cursorLoader;
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
        if (DEBUG) Log.i(TAG, "Calling thread activity with id=" + id);
        from.startActivity(intent);
    }

    public static Intent createIntentForThread(Context context, ChanPost thread) {
        return createIntentForActivity(
                context,
                thread.board,
                thread.no,
                thread.getThreadText(),
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
        editor.putInt(ChanHelper.LAST_THREAD_POSITION, gridView.getFirstVisiblePosition());
        editor.putLong(ChanHelper.TIM, tim);
        editor.putString(ChanHelper.TEXT, text);
        editor.putString(ChanHelper.IMAGE_URL, imageUrl);
        editor.putInt(ChanHelper.IMAGE_WIDTH, imageWidth);
        editor.putInt(ChanHelper.IMAGE_HEIGHT, imageHeight);
        editor.commit();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    protected void initGridAdapter() {
        adapter = new ThreadCursorAdapter(this,
                R.layout.thread_grid_item,
                this,
                new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_SHORT_TEXT, ChanHelper.POST_COUNTRY_URL},
                new int[] {R.id.grid_item_image, R.id.grid_item_text, R.id.grid_item_country_flag});
        gridView.setAdapter(adapter);
    }

    @Override
    protected String getLastPositionName() {
        return ChanHelper.LAST_THREAD_POSITION;
    }

    @Override
    protected void sizeGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(gridView, display, ChanGridSizer.ServiceType.THREAD);
        cg.sizeGridToDisplay();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        incrementCounterAndAddToWatchlistIfActive();
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        if (imageUrl == null || imageUrl.isEmpty()) {
            showPopupText(adapterView, view, position, id);
        }
        else {
            FullScreenImageActivity.startActivity(this, adapterView, view, position, id);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        incrementCounterAndAddToWatchlistIfActive();
        return showPopupText(adapterView, view, position, id);
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        return setViewValue(view, cursor, columnIndex, imageLoader, displayImageOptions, hideAllText);
    }

    public static boolean setViewValue(View view, Cursor cursor, int columnIndex,
                                       ImageLoader imageLoader, DisplayImageOptions displayImageOptions,
                                       boolean hideAllText) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            long resto = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_RESTO));
            String shortText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT));
            if ((resto != 0 && hideAllText) || shortText == null || shortText.isEmpty()) {
                tv.setText("");
                tv.setVisibility(View.INVISIBLE);
            }
            else {
                tv.setText(Html.fromHtml(shortText));
            }
            return true;
        } else if (view instanceof ImageView && view.getId() == R.id.grid_item_image) {
            String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
            int loading = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOADING_ITEM));
            int spoiler = cursor.getInt(cursor.getColumnIndex(ChanHelper.SPOILER));
            ImageView iv = (ImageView) view;
            if (spoiler > 0) {
                String boardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
                smartSetImageView(iv, ChanBoard.spoilerThumbnailUrl(boardCode), imageLoader, displayImageOptions);
            }
            else if (imageUrl != null && !imageUrl.isEmpty() && loading == 0) {
                smartSetImageView(iv, imageUrl, imageLoader, displayImageOptions);
            }
            else if (loading > 0) {
                setImageViewToLoading(iv);
            }
            else {
                iv.setImageBitmap(null); // blank
            }
            return true;
        } else if (view instanceof ImageView && view.getId() == R.id.grid_item_country_flag) {
            ImageView iv = (ImageView) view;
            String countryFlagImageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_COUNTRY_URL));
            if (countryFlagImageUrl != null && !countryFlagImageUrl.isEmpty()) {
                smartSetImageView(iv, countryFlagImageUrl, imageLoader, displayImageOptions);
            }
            else {
                iv.setImageBitmap(null); // blank
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        if (hideAllText)
            menu.findItem(R.id.hide_all_text).setTitle(R.string.pref_hide_all_text_on);
        else
            menu.findItem(R.id.hide_all_text).setTitle(R.string.pref_hide_all_text);
        hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, true);
        if (hidePostNumbers)
            menu.findItem(R.id.hide_post_numbers).setTitle(R.string.pref_hide_post_numbers_turn_off);
        else
            menu.findItem(R.id.hide_post_numbers).setTitle(R.string.pref_hide_post_numbers_turn_on);
        return true;
    }

    protected void toggleHideAllText() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        hideAllText = !hideAllText; // invert
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, hideAllText);
        editor.commit();
        refreshActivity();
    }

    protected void toggleHidePostNumbers() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        hidePostNumbers = prefs.getBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, false);
        hidePostNumbers = !hidePostNumbers; // invert
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SettingsActivity.PREF_HIDE_POST_NUMBERS, hidePostNumbers);
        editor.commit();
        invalidateOptionsMenu();
        createGridView();
        ensureHandler().sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
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
//                    Log.i(TAG, "Should recreate task");
                    TaskStackBuilder.create(this).addParentStack(this).startActivities();
                    this.finish();
//                }
//                else {
//                    Log.i(TAG, "Navigating up...");
//                    NavUtils.navigateUpTo(this, upIntent);
//                }
                return true;
            case R.id.refresh_thread_menu:
                //Toast.makeText(this, R.string.refresh_thread_menu, Toast.LENGTH_LONG);
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            case R.id.post_reply_menu:
                postReply();
                return true;
            case R.id.hide_all_text:
                toggleHideAllText();
                return true;
            case R.id.hide_post_numbers:
                toggleHidePostNumbers();
                return true;
            case R.id.watch_thread_menu:
                addToWatchlist();
                return true;
            case R.id.download_all_images_menu:
            	ThreadImageDownloadService.startDownloadToBoardFolder(getBaseContext(), boardCode, threadNo);
                Toast.makeText(this, "Download of all images scheduled", Toast.LENGTH_SHORT).show();
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
            case R.id.go_to_board_menu:
                new GoToBoardDialogFragment().show(getSupportFragmentManager(), GoToBoardDialogFragment.TAG);
                return true;
            case R.id.board_rules_menu:
                displayBoardRules();
                return true;
            case R.id.about_menu:
                RawResourceDialog aboutDialog = new RawResourceDialog(this, R.layout.about_dialog, R.raw.about_header, R.raw.about_detail);
                aboutDialog.show();
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
        String title = "/" + boardCode + " " + threadNo;
        a.setTitle(title);
        a.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void initPopup() {
        boardThreadPopup = new ThreadPostPopup(this,
                this.getLayoutInflater(),
                imageLoader,
                displayImageOptions,
                (ThreadCursorAdapter)adapter);
    }

    protected UserStatistics ensureUserStats() {
        if (userStats == null) {
            userStats = ChanFileStorage.loadUserStats(getBaseContext());
        }
        return userStats;
    }

    protected void incrementCounterAndAddToWatchlistIfActive() {
        ensureUserStats().threadUse(boardCode, threadNo);
        ChanThreadStat stat = ensureUserStats().threadStats.get(threadNo);
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

    protected int getLayoutId() {
        return R.layout.thread_grid_layout;
    }

}
