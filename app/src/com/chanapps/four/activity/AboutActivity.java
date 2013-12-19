package com.chanapps.four.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.component.*;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.fragment.AboutFragment;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 4/14/13
 * Time: 9:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class AboutActivity extends Activity implements ChanIdentifiedActivity, ThemeSelector.ThemeActivity {

    protected static final boolean DEBUG = false;
    public static final String TAG = AboutActivity.class.getSimpleName();
    public static final String PREF_PURCHASE_CATEGORY = "pref_about_developer_category";
    public static final String PREF_PURCHASE_PROKEY = "pref_purchase_prokey";
    public static final String PREF_INSTALLED_PROKEY = "pref_installed_prokey";
    public static final int PURCHASE_REQUEST_CODE = 1987;

    protected int themeId;
    protected ThemeSelector.ThemeReceiver broadcastThemeReceiver;
    protected Handler handler;

    public static boolean startActivity(final Activity from) {
        Intent intent = new Intent(from, AboutActivity.class);
        from.startActivity(intent);
        return true;
    }

    public static Intent createIntent(Context from) {
        return new Intent(from, AboutActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        broadcastThemeReceiver = new ThemeSelector.ThemeReceiver(this);
        broadcastThemeReceiver.register();
        getFragmentManager().beginTransaction().replace(android.R.id.content, new AboutFragment()).commit();
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
    protected void onStart() {
        super.onStart();
        handler = new Handler();
        AnalyticsComponent.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler = null;
        AnalyticsComponent.onStop(this);
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
        return new ChanActivityId(LastActivity.ABOUT_ACTIVITY);
    }

    @Override
    public Handler getChanHandler() {
        return handler;
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
        inflater.inflate(R.menu.about_menu, menu);
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
                        R.string.global_rules_menu,
                        R.string.global_rules_detail))
                        .show();
                return true;
            case R.id.web_menu:
                String url = ChanBoard.boardUrl(this, null);
                ActivityDispatcher.launchUrlInBrowser(this, url);
            case R.id.settings_menu:
                return SettingsActivity.startActivity(this);
            case R.id.send_feedback_menu:
                return SendFeedback.email(this);
            case R.id.purchase_menu:
                return PurchaseActivity.startActivity(this);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) Log.i(TAG, "onActivityResult requestCode=" + requestCode + " resultCode=" + resultCode + " data=" + data);
        if (requestCode != PURCHASE_REQUEST_CODE)
            return;
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "Error while processing purchase request resultCode=" + resultCode);
            Toast.makeText(this, R.string.purchase_error, Toast.LENGTH_SHORT).show();
            return;
        }
        BillingComponent.getInstance(getApplicationContext()).processPurchaseResponse(data, new Handler());
    }

    @Override
    public void onBackPressed() {
        if (DEBUG) android.util.Log.i(TAG, "onBackPressed()");
        navigateUp();
    }

    protected void navigateUp() { // either pop off stack, or go up to all boards
        if (DEBUG) android.util.Log.i(TAG, "navigateUp()");
        Pair<Integer, ActivityManager.RunningTaskInfo> p = ActivityDispatcher.safeGetRunningTasks(this);
        int numTasks = p.first;
        ActivityManager.RunningTaskInfo task = p.second;
        if (task != null) {
            if (DEBUG) android.util.Log.i(TAG, "navigateUp() top=" + task.topActivity + " base=" + task.baseActivity);
            if (task.baseActivity != null
                    && !getClass().getName().equals(task.baseActivity.getClassName())) {
                if (DEBUG) android.util.Log.i(TAG, "navigateUp() using finish instead of intents with me="
                        + getClass().getName() + " base=" + task.baseActivity.getClassName());
                finish();
                return;
            }
            else if (task.baseActivity != null && numTasks >= 2) {
                if (DEBUG) android.util.Log.i(TAG, "navigateUp() using finish as task has at least one parent, size=" + numTasks);
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
