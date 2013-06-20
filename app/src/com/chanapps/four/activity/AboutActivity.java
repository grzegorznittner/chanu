package com.chanapps.four.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.fragment.AboutFragment;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 4/14/13
 * Time: 9:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class AboutActivity extends Activity implements ChanIdentifiedActivity {

    public static final String TAG = AboutActivity.class.getSimpleName();

    public static Intent createIntent(Context from) {
        return new Intent(from, AboutActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new AboutFragment()).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
    public void startProgress() {}

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
                BoardActivity.startActivity(this, ChanBoard.META_BOARD_CODE, "");
                return true;
            case R.id.global_rules_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this,
                        R.layout.board_rules_dialog, R.raw.global_rules_header, R.raw.global_rules_detail);
                rawResourceDialog.show();
                return true;
            case R.id.web_menu:
                String url = ChanBoard.boardUrl(null);
                ChanHelper.launchUrlInBrowser(this, url);
            case R.id.settings_menu:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.about_menu:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            case R.id.exit_menu:
                ChanHelper.exitApplication(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
