package com.chanapps.four.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.chanapps.four.component.*;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.fragment.SettingsFragment;

import java.util.List;

/**
 * User: mpop
 * Date: 11/20/12
 * Time: 11:50 PM
 */
public class SettingsActivity extends Activity implements ChanIdentifiedActivity, ThemeSelector.ThemeActivity {

    public static final String TAG = SettingsActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final String PREF_SHOW_NSFW_BOARDS = "pref_show_nsfw_boards";
    public static final String PREF_NOTIFICATIONS = "pref_notifications";
    //public static final String PREF_START_WITH_FAVORITES = "pref_start_with_favorites";
    public static final String PREF_USE_FRIENDLY_IDS = "pref_use_friendly_ids";
    public static final String PREF_USE_GOOGLE_ANALYTICS = "pref_use_google_analytics";
    public static final String PREF_USE_HTTPS = "pref_use_https";
    public static final String PREF_USE_CATALOG = "pref_use_catalog";
    public static final String PREF_USE_VOLUME_SCROLL = "pref_use_volume_scroll";
    public static final String PREF_BOARD_SORT_TYPE = "pref_board_sort_type";
    public static final String PREF_FONT_SIZE = "pref_font_size";
    public static final String PREF_AUTOUPDATE_THREADS = "pref_autoupdate_threads";
    public static final String PREF_THEME = "pref_theme";
    public static final String PREF_AUTOLOAD_IMAGES = "pref_autoload_images";
    public static final String PREF_DOWNLOAD_IMAGES = "pref_download_images";
    public static final String PREF_USER_NAME = "pref_user_name";
    public static final String PREF_USER_EMAIL = "pref_user_email";
    public static final String PREF_USER_PASSWORD = "pref_user_password";
    public static final String PREF_CACHE_SIZE = "pref_cache_size";
    public static final String PREF_CLEAR_CACHE = "pref_clear_cache";
    //public static final String PREF_CLEAR_WATCHLIST = "pref_clear_watchlist";
    //public static final String PREF_CLEAR_FAVORITES = "pref_clear_favorites";
    public static final String PREF_RESET_TO_DEFAULTS = "pref_reset_to_defaults";
    public static final String PREF_BLOCKLIST_BUTTON = "pref_blocklist_button";
    public static final String PREF_ABOUT = "pref_about";
    public static final String PREF_PASS_TOKEN = "pref_pass_token";
    public static final String PREF_PASS_PIN = "pref_pass_pin";
    public static final String PREF_PASS_ENABLED = "pref_pass_enabled";
    public static final String PREF_WIDGET_BOARDS = "prefWidgetBoards";
    public static final String PREF_BLOCKLIST_TRIPCODE = "prefBlocklistTripcode";
    public static final String PREF_BLOCKLIST_NAME = "prefBlocklistName";
    public static final String PREF_BLOCKLIST_EMAIL = "prefBlocklistEmail";
    public static final String PREF_BLOCKLIST_ID = "prefBlocklistId";
    
    public static enum DownloadImages {ALL_IN_ONE, PER_BOARD, PER_THREAD};

    protected int themeId;
    protected ThemeSelector.ThemeReceiver broadcastThemeReceiver;

    public static boolean startActivity(final Activity from) {
        Intent intent = new Intent(from, SettingsActivity.class);
        from.startActivity(intent);
        return true;
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        broadcastThemeReceiver = new ThemeSelector.ThemeReceiver(this);
        broadcastThemeReceiver.register();
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public int getThemeId() {
        return themeId;
    }

    @Override
    public void setThemeId(int themeId) {
        this.themeId = themeId;
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsComponent.onStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsComponent.onStop(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        broadcastThemeReceiver.unregister();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        ActivityDispatcher.store(this);
    }

    @Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.SETTINGS_ACTIVITY);
	}

	@Override
	public Handler getChanHandler() {
		return null;
	}

    @Override
    public void refresh() {}

    @Override
    public void closeSearch() {}

    @Override
    public void setProgress(boolean on) {}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.purchase_menu).setVisible(!BillingComponent.getInstance(this).hasProkey());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                BoardActivity.startDefaultActivity(this);
                return true;
            case R.id.global_rules_menu:
                (new StringResourceDialog(this,
                        R.layout.board_rules_dialog,
                        R.string.global_rules_header,
                        R.string.global_rules_detail))
                        .show();
                return true;
            case R.id.web_menu:
                String url = ChanBoard.boardUrl(this, null);
                ActivityDispatcher.launchUrlInBrowser(this, url);
            case R.id.send_feedback_menu:
                return SendFeedback.email(this);
            case R.id.purchase_menu:
                return PurchaseActivity.startActivity(this);
            case R.id.about_menu:
                return AboutActivity.startActivity(this);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (DEBUG) Log.i(TAG, "onBackPressed()");
        navigateUp();
    }

    protected void navigateUp() { // either pop off stack, or go up to all boards
        if (DEBUG) Log.i(TAG, "navigateUp()");
        Pair<Integer, ActivityManager.RunningTaskInfo> p = ActivityDispatcher.safeGetRunningTasks(this);
        int numTasks = p.first;
        ActivityManager.RunningTaskInfo task = p.second;
        if (task != null) {
            if (DEBUG) Log.i(TAG, "navigateUp() top=" + task.topActivity + " base=" + task.baseActivity);
            if (task.baseActivity != null
                    && !getClass().getName().equals(task.baseActivity.getClassName())) {
                if (DEBUG) Log.i(TAG, "navigateUp() using finish instead of intents with me="
                        + getClass().getName() + " base=" + task.baseActivity.getClassName());
                finish();
                return;
            }
            else if (task.baseActivity != null && numTasks >= 2) {
                if (DEBUG) Log.i(TAG, "navigateUp() using finish as task has at least one parent, size=" + numTasks);
                finish();
                return;
            }
        }
        // otherwise go back to the default board page
        Intent intent = BoardActivity.createIntent(this, ChanBoard.defaultBoardCode(this), "");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

}

