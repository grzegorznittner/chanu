package com.chanapps.four.activity;

import android.app.LoaderManager;
import android.database.Cursor;
import android.widget.AdapterView;

import android.widget.GridView;
import com.chanapps.four.adapter.AbstractBoardCursorAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/27/12
 * Time: 2:39 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ClickableLoaderActivity extends
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        AbstractBoardCursorAdapter.ViewBinder {
    public LoaderManager getLoaderManager();
    public GridView getGridView();
}
