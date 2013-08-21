package com.chanapps.four.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.BillingComponent;
import com.chanapps.four.component.StringResourceDialog;
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.fragment.AboutFragment;

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
    public static final String PREF_CONSUME_PROKEY = "pref_consume_prokey"; // TEST ONLY
    public static final int PURCHASE_REQUEST_CODE = 1987;

    protected int themeId;
    protected ThemeSelector.ThemeReceiver broadcastThemeReceiver;

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
        inflater.inflate(R.menu.about_menu, menu);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                BoardActivity.startActivity(this, ChanBoard.ALL_BOARDS_BOARD_CODE, "");
                return true;
            case R.id.global_rules_menu:
                (new StringResourceDialog(this,
                        R.layout.board_rules_dialog,
                        R.string.global_rules_header,
                        R.string.global_rules_detail))
                        .show();
                return true;
            case R.id.web_menu:
                String url = ChanBoard.boardUrl(null);
                ActivityDispatcher.launchUrlInBrowser(this, url);
            case R.id.settings_menu:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.about_menu:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
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

}
