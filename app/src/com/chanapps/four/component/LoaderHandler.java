package com.chanapps.four.component;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.chanapps.four.activity.ClickableLoaderActivity;
import com.chanapps.four.activity.ThreadActivity;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 11/27/12
* Time: 2:41 PM
* To change this template use File | Settings | File Templates.
*/
public class LoaderHandler extends Handler {
    private ClickableLoaderActivity activity;
    public LoaderHandler() {}
    public LoaderHandler(ClickableLoaderActivity activity) {
        this.activity = activity;
    }
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        Log.i(activity.getClass().getSimpleName(), ">>>>>>>>>>> refresh message received restarting loader");
        activity.getLoaderManager().restartLoader(0, null, activity);
    }
}
