package com.chanapps.four;

import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.data.DataManager;
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
        }
        return mDataManager;
    }
}
