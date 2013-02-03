package com.chanapps.four.activity;

import android.content.Context;
import android.support.v4.app.FragmentManager;

public interface RefreshableActivity {

    public void refreshActivity();

    public Context getBaseContext();

    public FragmentManager getSupportFragmentManager();

}
