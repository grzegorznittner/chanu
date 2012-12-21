package com.chanapps.four.handler;

import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.chanapps.four.activity.ClickableLoaderActivity;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.ChanHelper;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 11/27/12
* Time: 2:41 PM
* To change this template use File | Settings | File Templates.
*/
public class LoaderHandler extends Handler {
    private ClickableLoaderActivity activity;
    private static final String TAG = LoaderHandler.class.getSimpleName();

    public static final int REFRESH_COMPLETE_MSG = 1;
    public static final int RESTART_LOADER_MSG = 2;

    public LoaderHandler() {}
    public LoaderHandler(ClickableLoaderActivity activity) {
        this.activity = activity;
    }
    @Override
    public void handleMessage(Message msg) {
        try {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH_COMPLETE_MSG:
                    Log.i(activity.getClass().getSimpleName(), ">>>>>>>>>>> complete message received resetting grid view");
                    activity.getGridView().onRefreshComplete();
                    break;
                case RESTART_LOADER_MSG:
                    Log.i(activity.getClass().getSimpleName(), ">>>>>>>>>>> restart message received restarting loader");
                    activity.getLoaderManager().restartLoader(0, null, activity);
                    break;
                default:
                    Log.i(activity.getClass().getSimpleName(), ">>>>>>>>>>> null message received doing nothing");

            }
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't handle message " + msg, e);
        }
    }
}
