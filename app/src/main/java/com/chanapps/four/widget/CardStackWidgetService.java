package com.chanapps.four.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created with IntelliJ IDEA.
 * User: mpop
 * Date: 6/3/13
 * Time: 8:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class CardStackWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CardStackRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}
