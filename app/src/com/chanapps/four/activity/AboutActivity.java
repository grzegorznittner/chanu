package com.chanapps.four.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.fragment.AboutFragment;
import com.chanapps.four.fragment.SettingsFragment;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 4/14/13
 * Time: 9:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class AboutActivity extends Activity implements ChanIdentifiedActivity {

    public static final String TAG = AboutActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new AboutFragment()).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveInstanceState();
    }

    protected void saveInstanceState() {
        DispatcherHelper.saveActivityToPrefs(this);
    }

    @Override
    public ChanActivityId getChanActivityId() {
        return new ChanActivityId(ChanHelper.LastActivity.ABOUT_ACTIVITY);
    }

    @Override
    public Handler getChanHandler() {
        return null;
    }

    @Override
    public void refresh() {}

}
