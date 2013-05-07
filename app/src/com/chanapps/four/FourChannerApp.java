package com.chanapps.four;

import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.data.DataManager;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.UserStatistics;
import com.chanapps.four.gallery.ChanOffLineSource;
import com.chanapps.four.gallery.ChanSource;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FourChannerApp extends GalleryAppImpl {
	public synchronized DataManager getDataManager() {
        if (mDataManager == null) {
            mDataManager = new DataManager(this);
            mDataManager.initializeSourceMap();
            mDataManager.addSource(new ChanSource(this));
            mDataManager.addSource(new ChanOffLineSource(this));
        }
        return mDataManager;
    }
}
