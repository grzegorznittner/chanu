package com.chanapps.four.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.chanapps.four.adapter.ThreadCursorAdapter;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.ChanWatchlist;
import com.chanapps.four.handler.LoaderHandler;
import com.chanapps.four.loader.ThreadCursorLoader;
import com.chanapps.four.service.ThreadLoadService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import javax.security.auth.login.LoginException;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/27/12
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadActivity extends BoardActivity {

    protected static final String TAG = ThreadActivity.class.getSimpleName();

    protected long threadNo;
    protected String text;
    protected String imageUrl;
    protected int imageWidth;
    protected int imageHeight;
    protected boolean hideAllText = false;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        cursorLoader = new ThreadCursorLoader(this, boardCode, threadNo);
        return cursorLoader;
    }

    public static void startActivity(Activity from, AdapterView<?> adapterView, View view, int position, long id) {
        startActivity(from, adapterView, view, position, id, false);
    }

    public static void startActivity(Activity from, AdapterView<?> adapterView, View view, int position, long id, boolean fromParent) {
            Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final long threadTim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
        Log.d(TAG, "threadTim: " + threadTim);
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        final String boardName = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
        final String text = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        final String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        final int tn_w = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_TN_W));
        final int tn_h = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_TN_H));
        Intent intent = new Intent(from, ThreadActivity.class);
        intent.putExtra(ChanHelper.TIM, threadTim);
        intent.putExtra(ChanHelper.BOARD_CODE, boardName);
        intent.putExtra(ChanHelper.THREAD_NO, postId);
        intent.putExtra(ChanHelper.TEXT, text);
        intent.putExtra(ChanHelper.IMAGE_URL, imageUrl);
        intent.putExtra(ChanHelper.IMAGE_WIDTH, tn_w);
        intent.putExtra(ChanHelper.IMAGE_HEIGHT, tn_h);
        intent.putExtra(ChanHelper.LAST_BOARD_POSITION, adapterView.getFirstVisiblePosition());
        intent.putExtra(ChanHelper.LAST_THREAD_POSITION, 0);
        if (fromParent)
            intent.putExtra(ChanHelper.FROM_PARENT, true);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(from).edit();
        editor.putInt(ChanHelper.LAST_THREAD_POSITION, 0); // reset it
        editor.commit();
        Log.i(TAG, "Calling thread activity with id=" + id);
        from.startActivity(intent);
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
        Log.i(TAG, "Thread no read from intent: " + threadNo);
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
        return showPopupText(adapterView, view, position, id);
    }

    @Override
    protected void startLoadService(boolean force) {
        ThreadLoadService.startService(this, boardCode, threadNo, force);
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
                tv.setText(shortText);
            }
            return true;
        } else if (view instanceof ImageView && view.getId() == R.id.grid_item_image) {
            String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
            ImageView iv = (ImageView) view;
            if (imageUrl != null && !imageUrl.isEmpty()) {
                smartSetImageView(iv, imageUrl, imageLoader, displayImageOptions);
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
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        if (hideAllText) {
            menu.findItem(R.id.hide_all_text).setTitle(R.string.pref_hide_all_text_on);
        }
        else {
            menu.findItem(R.id.hide_all_text).setTitle(R.string.pref_hide_all_text);
        }
        return true;
    }

    protected void toggleHideAllText() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        hideAllText = !hideAllText; // invert
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, hideAllText);
        editor.commit();
        invalidateOptionsMenu();
        createGridView();
        ensureHandler().sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // FIXME: know that I'm coming from watching and return there
                Intent upIntent = new Intent(this, BoardActivity.class);
                upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                upIntent.putExtra(ChanHelper.LAST_BOARD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            case R.id.refresh_thread_menu:
                Toast.makeText(this, R.string.refresh_thread_menu, Toast.LENGTH_LONG);
                startLoadService(true);
                return true;
            case R.id.hide_all_text:
                toggleHideAllText();
                return true;
            case R.id.watch_thread_menu:
                ChanWatchlist.watchThread(this, tim, boardCode, threadNo, text, imageUrl, imageWidth, imageHeight);
                return true;
            case R.id.download_all_images_menu:
                Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.settings_menu:
                Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.layout.about_dialog, R.raw.help_header, R.raw.help_board_grid);
                rawResourceDialog.show();
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
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thread_menu, menu);
        return true;
    }
    /*
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }
    */

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
    protected void displayHighlightButton(final long postNo) { // board-level doesn't highlight, only thread-level does
        if (postNo > 0) {
            highlightButton.setVisibility(View.VISIBLE);
            highlightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HighlighterTask task = new HighlighterTask(ThreadActivity.this, (ThreadCursorAdapter)adapter, boardCode, threadNo);
                    task.execute(postNo);
                    popupWindow.dismiss();
                }
            });
            dismissButton.setVisibility(View.VISIBLE);
            fullWidthDismissButton.setVisibility(View.GONE);
        }
        else {
            highlightButton.setVisibility(View.GONE);
            dismissButton.setVisibility(View.GONE);
            fullWidthDismissButton.setVisibility(View.VISIBLE);
        }
    }

    private class HighlighterTask extends AsyncTask<Long, Void, String> {
        private Context context = null;
        private ThreadCursorAdapter threadAdapter = null;
        private String boardCode = null;
        private long threadNo = 0;
        public HighlighterTask(Context context, ThreadCursorAdapter adapter, String boardCode, long threadNo) {
            this.context = context;
            this.threadAdapter = adapter;
            this.boardCode = boardCode;
            this.threadNo = threadNo;
        }
        @Override
        protected String doInBackground(Long... postNos) {
            String result = null;
            long postNo = postNos[0];
            long[] prevPosts = null;
            long[] nextPosts = null;
            try {
                ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
                if (thread != null) {
                    prevPosts = thread.getPrevPostsReferenced(postNo);
                    nextPosts = thread.getNextPostsReferredTo(postNo);
                }
                else {
                    result = context.getString(R.string.thread_couldnt_load);
                    Log.e(TAG, "Coludn't load thread " + boardCode + "/" + threadNo);
                }
            }
            catch (Exception e) {
                result = context.getString(R.string.thread_couldnt_load);
                Log.e(TAG, "Exception while getting thread post highlights", e);
            }
            threadAdapter.setHighlightPosts(postNo, prevPosts, nextPosts);
            if ((prevPosts == null || prevPosts.length == 0) && (nextPosts == null || nextPosts.length == 0)) {
                result = context.getString(R.string.thread_no_replies_found);
            }
            else {
                String msg = context.getString(R.string.thread_replies_found);
                result = String.format(msg, prevPosts == null ? 0 : prevPosts.length, nextPosts == null ? 0 : nextPosts.length);
            }
            Log.i(TAG, "Set highlight posts prev=" + Arrays.toString(prevPosts) + " next=" + Arrays.toString(nextPosts));
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            threadAdapter.notifyDataSetChanged();
        }
    }
}
