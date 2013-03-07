package com.chanapps.four.activity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.GoToBoardDialogFragment;
import com.chanapps.four.handler.LoaderHandler;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class BoardActivity
        extends FragmentActivity
        implements ClickableLoaderActivity, ChanIdentifiedActivity, RefreshableActivity
{
	public static final String TAG = BoardActivity.class.getSimpleName();
	public static final boolean DEBUG = true;

    private static final String DEFAULT_BOARD_CODE = "a";

    public static final int LOADER_RESTART_INTERVAL_MED_MS = 2000;
    public static final int LOADER_RESTART_INTERVAL_SHORT_MS = 1000;
    public static final int LOADER_RESTART_INTERVAL_MICRO_MS = 100;

    protected static final int IMAGE_URL_HASHCODE_KEY = R.id.grid_item_image;

    protected AbstractBoardCursorAdapter adapter;
    protected AbsListView absListView;
    protected Class absListViewClass = GridView.class;
    protected Handler handler;
    protected BoardCursorLoader cursorLoader;
    protected int scrollOnNextLoaderFinished = 0;
    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;
    protected ProgressBar progressBar;
    protected SharedPreferences prefs;
    protected long tim;
    protected String boardCode;

    public static void startActivity(Activity from, String boardCode) {
        Intent intent = createIntentForActivity(from, boardCode);
        from.startActivity(intent);
    }

    public static Intent createIntentForActivity(Context context, String boardCode) {
        String intentBoardCode = boardCode == null || boardCode.isEmpty() ? ChanBoard.DEFAULT_BOARD_CODE : boardCode;
        Intent intent = new Intent(context, BoardActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, intentBoardCode);
        intent.putExtra(ChanHelper.PAGE, 0);
        intent.putExtra(ChanHelper.LAST_BOARD_POSITION, 0);
        intent.putExtra(ChanHelper.FROM_PARENT, true);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(ChanHelper.LAST_BOARD_POSITION, 0); // reset it
        editor.commit();
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		if (DEBUG) Log.v(TAG, "************ onCreate");
        super.onCreate(savedInstanceState);
        loadFromIntentOrPrefs();
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(
                new ImageLoaderConfiguration
                        .Builder(this)
                        .imageDownloader(new ExtendedImageDownloader(this))
                        .build());
        //        .createDefault(this));
        displayImageOptions = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.stub_image)
                .cacheOnDisc()
                .imageScaleType(ImageScaleType.EXACT)
                .build();
        createAbsListView();
        ensureHandler();
        LoaderManager.enableDebugLogging(true);
        if (DEBUG) Log.v(TAG, "onCreate init loader");
        progressBar = (ProgressBar)findViewById(R.id.board_progress_bar);
        getLoaderManager().initLoader(0, null, this);
        progressBar.setVisibility(View.VISIBLE);
    }

    protected void sizeGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer((GridView)absListView, display, ChanGridSizer.ServiceType.BOARD);
        cg.sizeGridToDisplay();
    }

    protected void initAdapter() {
        adapter = new BoardGridCursorAdapter(this,
                R.layout.board_grid_item,
                this,
                new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_SHORT_TEXT, ChanHelper.POST_TEXT, ChanHelper.POST_COUNTRY_URL},
                new int[] {R.id.grid_item_image, R.id.grid_item_text_top, R.id.grid_item_text, R.id.grid_item_country_flag});
        absListView.setAdapter(adapter);
    }

    protected int getLayoutId() {
        if (GridView.class.equals(absListViewClass))
            return R.layout.board_grid_layout;
        else
            return R.layout.board_list_layout;
    }

    protected void setAbsListViewClass() { // override to change
        absListViewClass = GridView.class; // always for board view
    }

    protected void initAbsListView() {
        if (GridView.class.equals(absListViewClass)) {
            absListView = (GridView)findViewById(R.id.board_grid_view);
            sizeGridToDisplay();
        }
        else {
            absListView = (ListView)findViewById(R.id.board_list_view);
        }
    }

    protected void createAbsListView() {
        setAbsListViewClass();
        setContentView(getLayoutId());
        initAbsListView();
        initAdapter();
        absListView.setClickable(true);
        absListView.setOnItemClickListener(this);
        absListView.setLongClickable(false);
    }

    protected synchronized Handler ensureHandler() {
        if (handler == null) {
            if (ChanHelper.onUIThread())
                handler = new LoaderHandler(this);
            else
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handler = new LoaderHandler(BoardActivity.this);
                    }
                });
        }
        return handler;
    }

    @Override
    protected void onStart() {
        super.onStart();
		if (DEBUG) Log.v(TAG, "onStart");
    }

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG) Log.v(TAG, "onResume");
        restoreInstanceState();
		NetworkProfileManager.instance().activityChange(this);
		Loader loader = getLoaderManager().getLoader(0);
		if (loader == null) {
			getLoaderManager().initLoader(0, null, this);
            progressBar.setVisibility(View.VISIBLE);
		}
	}

    public void setProgressFinished() {
        progressBar.setVisibility(View.GONE);
    }

    public GridView getGridView() {
        return (GridView)absListView;
    }

    protected String getLastPositionName() {
        return ChanHelper.LAST_BOARD_POSITION;
    }

    protected void scrollToLastPosition() {
        String intentExtra = getLastPositionName();
        int lastPosition = getIntent().getIntExtra(intentExtra, 0);
        if (lastPosition == 0) {
            lastPosition = ensurePrefs().getInt(intentExtra, 0);
        }
        if (lastPosition != 0)
            scrollOnNextLoaderFinished = lastPosition;
        if (DEBUG) Log.v(TAG, "Scrolling to:" + lastPosition);
    }

    @Override
	public void onWindowFocusChanged (boolean hasFocus) {
		if (DEBUG) Log.v(TAG, "onWindowFocusChanged hasFocus: " + hasFocus);
	}

    @Override
	protected void onPause() {
        super.onPause();
        if (DEBUG) Log.v(TAG, "onPause");
        saveInstanceState();
    }

    protected SharedPreferences ensurePrefs() {
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs;
    }

    protected void loadFromIntentOrPrefs() {
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            List<String> params = data.getPathSegments();
            String uriBoardCode = params.get(0);
            if (ChanBoard.getBoardByCode(this, uriBoardCode) != null) {
                boardCode = uriBoardCode;
                if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from url intent");
            }
            else {
                if (DEBUG) Log.e(TAG, "Received invalid boardCode=" + uriBoardCode + " from url intent, ignoring");
            }
        }
        else if (intent.hasExtra(ChanHelper.BOARD_CODE)) {
            boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
            if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from board code intent");
        }
        else {
            boardCode = ensurePrefs().getString(ChanHelper.BOARD_CODE, DEFAULT_BOARD_CODE);
            if (DEBUG) Log.i(TAG, "loaded boardCode=" + boardCode + " from prefs or default");
        }
    }

    protected void restoreInstanceState() {
        if (DEBUG) Log.i(TAG, "Restoring instance state...");
        loadFromIntentOrPrefs();
        setActionBarTitle();
        scrollToLastPosition();
    }

    protected void saveInstanceState() {
        if (DEBUG) Log.i(TAG, "Saving instance state...");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(ChanHelper.BOARD_CODE, boardCode);
        editor.putLong(ChanHelper.THREAD_NO, 0);
        editor.putInt(ChanHelper.LAST_BOARD_POSITION, absListView.getFirstVisiblePosition());
        editor.commit();
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    protected void onStop () {
    	super.onStop();
    	if (DEBUG) Log.v(TAG, "onStop");
    	getLoaderManager().destroyLoader(0);
    	handler = null;
    }

    @Override
	protected void onDestroy () {
		super.onDestroy();
		if (DEBUG) Log.v(TAG, "onDestroy");
		getLoaderManager().destroyLoader(0);
		handler = null;
	}

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        switch (view.getId()) {
            case R.id.grid_item_text_top:
                return setHeaderViewValue((TextView) view, cursor);
            case R.id.grid_item_text:
                return setTextViewValue((TextView) view, cursor);
            case R.id.grid_item_image:
                return setImageViewValue((ImageView) view, cursor);
            case R.id.grid_item_country_flag:
                return setCountryFlagValue((ImageView) view, cursor);
        }
        return false;
    }

    protected boolean setHeaderViewValue(TextView tv, Cursor cursor) {
        String shortText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        tv.setText(Html.fromHtml(shortText.replace("Subject: ", "")));
        return true;
    }

    protected boolean setTextViewValue(TextView tv, Cursor cursor) {
        String shortText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT));
        int idx = shortText.lastIndexOf('\n');
        if (idx < 0)
            idx = 0;
        String threadInfo = shortText.substring(idx);
        tv.setText(Html.fromHtml(threadInfo));
        return true;
    }

    protected boolean setImageViewValue(ImageView iv, Cursor cursor) {
        String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        int imageResourceId = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_THUMBNAIL_ID));
        if (imageResourceId > 0)
            smartSetImageView(iv, "", imageLoader, displayImageOptions, imageResourceId);
        else if (!imageUrl.isEmpty())
            smartSetImageView(iv, imageUrl, imageLoader, displayImageOptions);
        else
            iv.setImageBitmap(null);
        return true;
    }

    protected boolean setCountryFlagValue(ImageView iv, Cursor cursor) {
        String countryFlagImageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_COUNTRY_URL));
        if (DEBUG) Log.v(TAG, "Country flag url=" + countryFlagImageUrl);
        if (countryFlagImageUrl != null && !countryFlagImageUrl.isEmpty())
            smartSetImageView(iv, countryFlagImageUrl, imageLoader, displayImageOptions);
        else
            iv.setImageBitmap(null); // blank
        return true;
    }

    public static void smartSetImageView(ImageView iv, String imageUrl,
                                         ImageLoader imageLoader, DisplayImageOptions displayImageOptions) {
        smartSetImageView(iv, imageUrl, imageLoader, displayImageOptions, 0);
    }

    public static void smartSetImageView(ImageView iv,
                                         String imageUrl,
                                         ImageLoader imageLoader,
                                         DisplayImageOptions displayImageOptions,
                                         int imageResourceId)
    {
        try {
            Integer viewHashCodeInt = (Integer)iv.getTag(IMAGE_URL_HASHCODE_KEY);
            int viewHashCode = viewHashCodeInt != null ? viewHashCodeInt : 0;
            int urlHashCode = imageUrl != null && !imageUrl.isEmpty() ? imageUrl.hashCode() : imageResourceId;
            if (DEBUG) Log.i(TAG, "iv urlhash=" + urlHashCode + " viewhash=" + viewHashCode);
            if (iv.getDrawable() == null || viewHashCode != urlHashCode) {
            	if (imageResourceId > 0) // load from board
            		imageUrl = "drawable://" + imageResourceId;
                if (DEBUG) Log.i(TAG, "calling imageloader for " + imageUrl);
                iv.setImageBitmap(null);
                iv.setTag(IMAGE_URL_HASHCODE_KEY, urlHashCode);
                imageLoader.displayImage(imageUrl, iv, displayImageOptions); // load async
            }
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Couldn't set image view after number format exception with url=" + imageUrl, nfe);
            iv.setImageBitmap(null);
        }
        catch (Exception e) {
            Log.e(TAG, "Exception setting image view with url=" + imageUrl, e);
            iv.setImageBitmap(null);
        }
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        progressBar.setVisibility(View.VISIBLE);
        cursorLoader = new BoardCursorLoader(this, boardCode);
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoadFinished");
		adapter.swapCursor(data);
        setProgressFinished();
        if (absListView != null) {
            if (scrollOnNextLoaderFinished > 0) {
                absListView.setSelection(scrollOnNextLoaderFinished);
                scrollOnNextLoaderFinished = 0;
            }
        }
    }

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoaderReset");
        progressBar.setVisibility(View.VISIBLE);
		adapter.swapCursor(null);
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        ChanHelper.fadeout(this, view);
        if (adItem > 0) {
            final String adUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
            launchUrlInBrowser(adUrl);
            return;
        }

        ThreadActivity.startActivity(this, adapterView, view, position, id, true);
    }

    protected void launchUrlInBrowser(String url) {
        ChanHelper.launchUrlInBrowser(this, url);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, BoardSelectorActivity.class);
                intent.putExtra(ChanHelper.BOARD_TYPE, ChanBoard.getBoardByCode(this, boardCode).type.toString());
                intent.putExtra(ChanHelper.IGNORE_DISPATCH, true);
                NavUtils.navigateUpTo(this, intent);
                return true;
            case R.id.refresh_board_menu:
                progressBar.setVisibility(View.VISIBLE);
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            case R.id.new_thread_menu:
                Intent replyIntent = new Intent(this, PostReplyActivity.class);
                replyIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
                replyIntent.putExtra(ChanHelper.THREAD_NO, 0);
                replyIntent.putExtra(ChanHelper.POST_NO, 0);
                replyIntent.putExtra(ChanHelper.TIM, 0);
                replyIntent.putExtra(ChanHelper.TEXT, "");
                startActivity(replyIntent);
                return true;
            case R.id.offline_board_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, boardCode);
                return true;
            case R.id.offline_chan_view_menu:
            	GalleryViewActivity.startOfflineAlbumViewActivity(this, null);
                return true;
            case R.id.settings_menu:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
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
            case R.id.exit_menu:
                ChanHelper.exitApplication(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void displayBoardRules() {
        int boardRulesId = R.raw.global_rules_detail;
        try {
            boardRulesId = R.raw.class.getField("board_" + boardCode + "_rules").getInt(null);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't find rules for board:" + boardCode);
        }
        RawResourceDialog rawResourceDialog
                = new RawResourceDialog(this, R.layout.board_rules_dialog, R.raw.board_rules_header, boardRulesId);
        rawResourceDialog.show();

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setupActionBarBoardSpinner(menu);
        return true;
    }

    private void setupActionBarBoardSpinner(Menu menu) {
        boolean showNSFW = ChanBoard.showNSFW(this);
        MenuItem item = menu.findItem(R.id.board_jump_spinner_menu);
        MenuItem itemNSFW = menu.findItem(R.id.board_jump_spinner_nsfw_menu);
        Spinner spinner;
        if (showNSFW) {
            item.setVisible(false);
            itemNSFW.setVisible(true);
            spinner = (Spinner)itemNSFW.getActionView();
        }
        else {
            item.setVisible(true);
            itemNSFW.setVisible(false);
            spinner = (Spinner)item.getActionView();
        }
        spinner.setOnItemSelectedListener(null);
        int arrayId = showNSFW ? R.array.board_array : R.array.board_array_worksafe;
        String[] boards = getResources().getStringArray(arrayId);
        int position = -1;
        for (int i = 0; i < boards.length; i++) {
            if (boards[i].matches("/" + boardCode + "/.*")) {
                position = i;
                break;
            }
        }
        if (position >= 0)
            spinner.setSelection(position, false);
        else
            spinner.setSelected(false);
        spinner.setOnItemSelectedListener(actionBarSpinnerHandler);
    }

    private ActionBarSpinnerHandler actionBarSpinnerHandler = new ActionBarSpinnerHandler();

    private class ActionBarSpinnerHandler implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { // for action bar spinner
            String boardAsMenu = (String) parent.getItemAtPosition(position);
            if (boardAsMenu == null || boardAsMenu.isEmpty() || position == -1)
                return;
            if (boardAsMenu.equals(getString(R.string.board_watch))) {
                BoardSelectorActivity.startActivity(BoardActivity.this, ChanBoard.Type.WATCHLIST);
                return;
            }
            Pattern p = Pattern.compile("/([^/]*)/.*");
            Matcher m = p.matcher(boardAsMenu);
            if (!m.matches())
                return;
            String boardCodeForJump = m.group(1);
            if (boardCodeForJump == null || boardCodeForJump.isEmpty() || boardCodeForJump.equals(boardCode))
                return;
            BoardActivity.startActivity(BoardActivity.this, boardCodeForJump);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { // for action bar spinner
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_menu, menu);
        return true;
    }

    protected void setActionBarTitle() {
        ActionBar a = getActionBar();
        if (a == null)
            return;
        invalidateOptionsMenu(); // because onPrepare isn't called when it should be
        a.setDisplayShowTitleEnabled(false);
        a.setDisplayHomeAsUpEnabled(true);
    }

	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.BOARD_ACTIVITY, boardCode);
	}

	@Override
	public Handler getChanHandler() {
        return ensureHandler();
	}

    @Override
    public void refreshActivity() {
        invalidateOptionsMenu();
        createAbsListView();
        ensureHandler().sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
    }

}
