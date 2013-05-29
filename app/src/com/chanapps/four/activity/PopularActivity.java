package com.chanapps.four.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ListView;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.BoardListFragment;
import com.chanapps.four.fragment.PopularFragment;
import com.chanapps.four.service.NetworkProfileManager;

public class
        PopularActivity
        extends AbstractDrawerActivity
        implements ChanIdentifiedActivity,
        ListView.OnItemClickListener
{
    public static final String TAG = PopularActivity.class.getSimpleName();
    public static final boolean DEBUG = false;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, PopularActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    @Override
    protected void createFragment() {
        Fragment fragment = new PopularFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.popular_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.refresh_menu:
                setProgressBarIndeterminateVisibility(true);
                NetworkProfileManager.instance().manualRefresh(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.POPULAR_ACTIVITY);
	}

    @Override
    public boolean isSelfBoard(String boardAsMenu) {
        return isPopular(boardAsMenu);
    }

}
