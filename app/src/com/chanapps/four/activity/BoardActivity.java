package com.chanapps.four.activity;

import java.util.List;

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import com.chanapps.four.adapter.BoardCursorAdapter;
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
	public static final boolean DEBUG = false;

    private static final String DEFAULT_BOARD_CODE = "a";

    public static final int LOADER_RESTART_INTERVAL_MED_MS = 2000;
    public static final int LOADER_RESTART_INTERVAL_SHORT_MS = 1000;
    public static final int LOADER_RESTART_INTERVAL_MICRO_MS = 100;

    protected static final int IMAGE_URL_HASHCODE_KEY = R.id.grid_item_image;

    protected BoardCursorAdapter adapter;
    protected GridView gridView;
    protected Handler handler;
    protected BoardCursorLoader cursorLoader;
    protected int scrollOnNextLoaderFinished = 0;
    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;
    protected SharedPreferences prefs;
    protected BoardThreadPopup boardThreadPopup;
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
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));
        displayImageOptions = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.stub_image)
                .cacheOnDisc()
                .imageScaleType(ImageScaleType.EXACT)
                .build();
        createGridView();
        ensureHandler();
        LoaderManager.enableDebugLogging(true);
        if (DEBUG) Log.v(TAG, "onCreate init loader");
        getLoaderManager().initLoader(0, null, this);
    }

    protected void sizeGridToDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        ChanGridSizer cg = new ChanGridSizer(gridView, display, ChanGridSizer.ServiceType.BOARD);
        cg.sizeGridToDisplay();
    }

    protected void initGridAdapter() {
        adapter = new BoardCursorAdapter(this,
                R.layout.board_grid_item,
                this,
                new String[] {ChanHelper.POST_IMAGE_URL, ChanHelper.POST_SHORT_TEXT, ChanHelper.POST_COUNTRY_URL},
                new int[] {R.id.grid_item_image, R.id.grid_item_text, R.id.grid_item_country_flag});
        gridView.setAdapter(adapter);
    }

    protected int getLayoutId() {
        return R.layout.board_grid_layout;
    }

    protected void createGridView() {
        setContentView(getLayoutId());
        gridView = (GridView)findViewById(R.id.board_grid_view);
        sizeGridToDisplay();
        initGridAdapter();
        gridView.setClickable(true);
        gridView.setOnItemClickListener(this);
        gridView.setLongClickable(true);
        gridView.setOnItemLongClickListener(this);
    }

    protected synchronized Handler ensureHandler() {
        if (handler == null) {
            handler = new LoaderHandler(this);
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
		}
	}

    public GridView getGridView() {
        return gridView;
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
        editor.putInt(ChanHelper.LAST_BOARD_POSITION, gridView.getFirstVisiblePosition());
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
        return setViewValue(view, cursor, columnIndex, imageLoader, displayImageOptions);
    }

    public static boolean setViewValue(View view, Cursor cursor, int columnIndex,
                                       ImageLoader imageLoader, DisplayImageOptions displayImageOptions) {
        if (view instanceof TextView) { // only really works with hideAllText, otherwise it breaks views
            TextView tv = (TextView) view;
            String shortText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SHORT_TEXT));
            if (shortText == null || shortText.isEmpty()) {
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
            if (DEBUG) Log.v(TAG, "Country flag url=" + countryFlagImageUrl);
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

    public static void smartSetImageView(ImageView iv, String imageUrl,
                                         ImageLoader imageLoader, DisplayImageOptions displayImageOptions) {
        try {
            Integer viewHashCodeInt = (Integer)iv.getTag(IMAGE_URL_HASHCODE_KEY);
            int viewHashCode = viewHashCodeInt != null ? viewHashCodeInt : 0;
            int urlHashCode = imageUrl.hashCode();
            if (DEBUG) Log.i(TAG, "iv urlhash=" + urlHashCode + " viewhash=" + viewHashCode);
            if (iv.getDrawable() == null || viewHashCode != urlHashCode) {
                if (DEBUG) Log.i(TAG, "calling imageloader for " + imageUrl);
                iv.setImageBitmap(null);
                iv.setTag(IMAGE_URL_HASHCODE_KEY, urlHashCode);
                imageLoader.displayImage(imageUrl, iv, displayImageOptions);
            }
        } catch (NumberFormatException nfe) {
            try {
                iv.setImageURI(Uri.parse(imageUrl));
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't set image view after number format exception with url=" + imageUrl, e);
                iv.setImageBitmap(null);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Exception setting image view with url=" + imageUrl, e);
            iv.setImageBitmap(null);
        }
    }

    protected static void setImageViewToLoading(ImageView iv) {
        iv.setImageResource(R.drawable.navigation_refresh_light);
        Animation rotation = AnimationUtils.loadAnimation(iv.getContext(), R.animator.clockwise_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onCreateLoader");
        cursorLoader = new BoardCursorLoader(this, boardCode);
        return cursorLoader;
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoadFinished");
		adapter.swapCursor(data);
        if (gridView != null) {
            if (scrollOnNextLoaderFinished > 0) {
                gridView.setSelection(scrollOnNextLoaderFinished);
                scrollOnNextLoaderFinished = 0;
            }
        }
    }

    @Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (DEBUG) Log.v(TAG, ">>>>>>>>>>> onLoaderReset");
		adapter.swapCursor(null);
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final int loadItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOADING_ITEM));
        final int lastItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.LAST_ITEM));
        final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        if (loadItem > 0 || lastItem > 0)
            return;
        if (adItem > 0) {
            final String adUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
            launchAdLinkInBrowser(adUrl);
            return;
        }

        ThreadActivity.startActivity(this, adapterView, view, position, id, true);
    }

    protected void launchAdLinkInBrowser(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final int loadItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.LOADING_ITEM));
        final int lastItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.LAST_ITEM));
        final int adItem = cursor.getInt(cursor.getColumnIndex(ChanHelper.AD_ITEM));
        if (loadItem > 0 || lastItem > 0 || adItem > 0)
            return false;

        return showPopupText(adapterView, view, position, id);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        if (DEBUG) Log.v(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.board_menu, menu);
        return true;
    }

    protected void setActionBarTitle() {
        ActionBar a = getActionBar();
        if (a == null) {
        }
        String title = "/" + boardCode; // + " " + getString(R.string.board_activity);
        a.setTitle(title);
        a.setDisplayHomeAsUpEnabled(true);
    }

    protected void initPopup() {
        boardThreadPopup = new BoardThreadPopup(this, this.getLayoutInflater(), imageLoader, displayImageOptions);
    }

    protected BoardThreadPopup ensurePopup() {
        if (boardThreadPopup == null) {
            initPopup();
        }
        return boardThreadPopup;
    }

    public boolean showPopupText(AdapterView<?> adapterView, View view, int position, long id) {
        ensurePopup().showFromCursor(adapterView, view, position, id);
        return true;
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
        createGridView();
        ensureHandler().sendEmptyMessageDelayed(0, LOADER_RESTART_INTERVAL_SHORT_MS);
    }

}
