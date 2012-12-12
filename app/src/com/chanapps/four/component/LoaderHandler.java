package com.chanapps.four.component;

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

    public enum MessageType {
        RESTART_LOADER,
        REFRESH_COMPLETE,
    }

    public LoaderHandler() {}
    public LoaderHandler(ClickableLoaderActivity activity) {
        this.activity = activity;
    }
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        Log.i(activity.getClass().getSimpleName(), ">>>>>>>>>>> refresh message received restarting loader");
        MessageType type = msg.arg1 >= 0 ? MessageType.values()[msg.arg1] : MessageType.RESTART_LOADER;
        switch (type) {
            case RESTART_LOADER:
                activity.getLoaderManager().restartLoader(0, null, activity);
                break;
            case REFRESH_COMPLETE:
                activity.getGridView().onRefreshComplete();
                break;
            default:
                activity.getLoaderManager().restartLoader(0, null, activity);
        }
    }
}
