package com.chanapps.four.activity;

import android.app.LoaderManager;
import android.database.Cursor;
import android.widget.AdapterView;

import com.chanapps.four.component.ChanViewHelper;
import com.chanapps.four.component.ImageTextCursorAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/27/12
 * Time: 2:39 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ClickableLoaderActivity extends
        AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        ImageTextCursorAdapter.ViewBinder {
    public LoaderManager getLoaderManager();
    public ChanViewHelper.ServiceType getServiceType();
}
