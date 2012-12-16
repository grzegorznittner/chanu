package com.chanapps.four.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import com.chanapps.four.component.ChanViewHelper;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanWatchlist;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/27/12
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadActivity extends BoardActivity {

    @Override
    public ChanViewHelper.ServiceType getServiceType() {
        return ChanViewHelper.ServiceType.THREAD;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        if (imageUrl == null || imageUrl.isEmpty()) {
            viewHelper.showPopupText(adapterView, view, position, id);
        }
        else {
            viewHelper.startFullImageActivity(adapterView, view, position, id);
        }
    }

    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        return viewHelper.showPopupText(adapterView, view, position, id);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        if (hideAllText) {
            menu.findItem(R.id.hide_all_text).setTitle(R.string.pref_show_all_text);
        }
        else {
            menu.findItem(R.id.hide_all_text).setTitle(R.string.pref_hide_all_text);
        }
        return true;
    }

    protected void toggleHideAllText() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, !hideAllText);
        editor.commit();
        invalidateOptionsMenu();
        refresh();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(this, BoardActivity.class);
                upIntent.putExtra(ChanHelper.BOARD_CODE, ChanHelper.BOARD_CODE);
                upIntent.putExtra(ChanHelper.LAST_BOARD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            case R.id.hide_all_text:
                toggleHideAllText();
                return true;
            case R.id.watch_thread_menu:
                ChanWatchlist.watchThread(
                        this,
                        viewHelper.getTim(),
                        viewHelper.getBoardCode(),
                        viewHelper.getThreadNo(),
                        viewHelper.getText(),
                        viewHelper.getImageUrl(),
                        viewHelper.getImageWidth(),
                        viewHelper.getImageHeight());
                Toast.makeText(this, R.string.dialog_added_to_watchlist, Toast.LENGTH_SHORT).show();
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
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.raw.help_header, R.raw.help_board_grid);
                rawResourceDialog.show();
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

}
