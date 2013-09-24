package com.chanapps.four.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.data.BoardType;
import com.chanapps.four.data.ChanBoard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class
        AbstractDrawerActivity
        extends AbstractBoardSpinnerActivity
        implements ChanIdentifiedActivity
{
    protected static final String TAG = AbstractDrawerActivity.class.getSimpleName();
    protected static final boolean DEBUG = false;

    protected int mDrawerArrayId;
    protected String[] mDrawerArray;
    protected ListView mDrawerList;
    protected DrawerLayout mDrawerLayout;
    protected SimpleAdapter mDrawerAdapter;
    protected ActionBarDrawerToggle mDrawerToggle;

    protected int activityLayout() {
        return R.layout.drawer_activity_layout;
    }

    protected void createPreViews() {
        createDrawer();
    }

    abstract protected void createViews(Bundle bundle);

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        mDrawerToggle.onConfigurationChanged(config);
    }

    @Override
    protected void setAdapters() {
        super.setAdapters();
        setDrawerAdapter();
    }

    @Override
    protected boolean allAdaptersSet() {
        return super.allAdaptersSet() && mDrawerAdapter != null;
    }

    protected static final String ROW_ID = "rowid";
    protected static final String TEXT = "text";
    protected static final String DRAWABLE_ID = "drawableid";

    protected static final String[] adapterFrom = {
            ROW_ID,
            TEXT,
            DRAWABLE_ID
    };

    protected static final int[] adapterTo = {
            R.id.drawer_list_item,
            R.id.drawer_list_item_text,
            R.id.drawer_list_item_icon
    };

    protected void setDrawerAdapter() {
        mDrawerArrayId = mShowNSFW
                ? R.array.long_drawer_array
                : R.array.long_drawer_array_worksafe;
        mDrawerArray = getResources().getStringArray(mDrawerArrayId);
        List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i < mDrawerArray.length; i++) {
            String drawerText = mDrawerArray[i];
            BoardType type = BoardType.valueOfDrawerString(this, drawerText);
            int drawableId = type.drawableId();
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(ROW_ID, "" + i);
            map.put(TEXT, drawerText);
            map.put(DRAWABLE_ID, "" + drawableId);
            fillMaps.add(map);
        }
        mDrawerAdapter = new SimpleAdapter(this, fillMaps, R.layout.drawer_list_item, adapterFrom, adapterTo) {
            @Override
            public boolean isEnabled(int position) {
                String drawerText = mDrawerArray[position];
                BoardType type = BoardType.valueOfDrawerString(AbstractDrawerActivity.this, drawerText);
                if (type == BoardType.META)
                    return false;
                else
                    return true;
            }
        };
        mDrawerAdapter.setViewBinder(mViewBinder);
        mDrawerList.setAdapter(mDrawerAdapter);
    }

    protected SimpleAdapter.ViewBinder mViewBinder = new SimpleAdapter.ViewBinder() {
        public boolean setViewValue(View view, Object data, String textRepresentation) {
            switch (view.getId()) {
                case R.id.drawer_list_item:
                    // find item
                    int pos = Integer.valueOf((String)data);
                    Map<String, String> item = (Map<String, String>)mDrawerAdapter.getItem(pos);
                    String drawerText = item.get(TEXT);
                    BoardType type = BoardType.valueOfDrawerString(AbstractDrawerActivity.this, drawerText);

                    // set checked state
                    int selector;
                    if (type.boardCode().equals(boardCode))
                        selector = R.drawable.drawer_list_selector_checked_bg;
                    else if (ThemeSelector.instance(getApplicationContext()).isDark())
                        selector = R.drawable.drawer_list_selector_inverse_bg_dark;
                    else
                        selector = R.drawable.drawer_list_selector_inverse_bg;
                    FrameLayout child = (FrameLayout)view.findViewById(R.id.frame_child);
                    Drawable selectorDrawable = getLayoutInflater().getContext().getResources().getDrawable(selector);
                    child.setForeground(selectorDrawable);

                    // set title state
                    ImageView icon = (ImageView)view.findViewById(R.id.drawer_list_item_icon);
                    TextView text = (TextView)view.findViewById(R.id.drawer_list_item_text);
                    TextView title = (TextView)view.findViewById(R.id.drawer_list_item_title);
                    View divider = view.findViewById(R.id.drawer_list_item_divider);
                    if (type == BoardType.META) {
                        title.setText(drawerText);
                        icon.setVisibility(View.GONE);
                        text.setVisibility(View.GONE);
                        title.setVisibility(View.VISIBLE);
                        divider.setVisibility(View.VISIBLE);
                    }
                    else {
                        icon.setVisibility(View.VISIBLE);
                        text.setVisibility(View.VISIBLE);
                        title.setVisibility(View.GONE);
                        divider.setVisibility(View.GONE);
                    }

                    // set text color
                    int textColor;
                    if (type == BoardType.valueOfBoardCode(boardCode))
                        textColor = R.color.PaletteWhite;
                    else
                        textColor = R.color.PaletteDrawerDividerText;
                    text.setTextColor(getResources().getColor(textColor));

                    if (DEBUG) Log.i(TAG, "mViewBinder:setViewValue() item pos=" + pos + " checked=" + type.boardCode().equals(boardCode) + " type=" + type + " text=" + text + " item=" + item);

                    return true;

                default:
                    return false;
            }
        }
    };

    protected void createDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mDrawerList.setOnItemClickListener(drawerClickListener);
        setDrawerAdapter();

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void refresh() {
    }

    @Override
    public void closeSearch() {}

    @Override
    public void setProgress(boolean on) {
        Handler handler = getChanHandler();
        if (handler != null)
            setProgressBarIndeterminateVisibility(on);
    }

    abstract public boolean isSelfBoard(String boardAsMenu);

    protected ListView.OnItemClickListener drawerClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mDrawerLayout.closeDrawer(mDrawerList);
            HashMap<String, String> item = (HashMap<String, String>)parent.getItemAtPosition(position);
            String boardAsMenu = item.get(TEXT);
            if (DEBUG) Log.i(TAG, "onItemClick boardAsMenu=" + boardAsMenu);
            handleSelectItem(boardAsMenu);
        }
    };

    protected boolean handleSelectItem(String boardAsMenu) {
        if (DEBUG) Log.i(TAG, "handleSelectItem(\"" + boardAsMenu + "\")");
        if (isSelfBoard(boardAsMenu)) {
            if (DEBUG) Log.i(TAG, "self board, returning");
            return false;
        }
        /*
        if (getString(R.string.about_menu).equals(boardAsMenu)) {
            Intent intent = AboutActivity.createIntent(this);
            startActivity(intent);
            return true;
        }
        */
        BoardType boardType = BoardType.valueOfDrawerString(this, boardAsMenu);
        if (boardType != null) {
            /*
            if (boardType == BoardType.META) {
                if (DEBUG) Log.i(TAG, "meta category board not clickable, exiting");
                return false;
            }
            */
            String boardTypeCode = boardType.boardCode();
            if (boardTypeCode.equals(boardCode)) {
                if (DEBUG) Log.i(TAG, "matched existing board code, exiting");
                return false;
            }
            if (DEBUG) Log.i(TAG, "matched board type /" + boardTypeCode + "/, starting");
            Intent intent = BoardActivity.createIntent(this, boardTypeCode, "");
            startActivity(intent);
            finish();
            return true;
        }
        Pattern p = Pattern.compile(BOARD_CODE_PATTERN);
        Matcher m = p.matcher(boardAsMenu);
        if (!m.matches()) {
            if (DEBUG) Log.i(TAG, "matched nothing, bailing");
            return false;
        }
        String boardCodeForJump = m.group(1);
        if (boardCodeForJump == null || boardCodeForJump.isEmpty() || isSelfBoard(boardCodeForJump)) {
            if (DEBUG) Log.i(TAG, "null match, bailing");
            return false;
        }
        if (boardCodeForJump.equals(boardCode)) {
            if (DEBUG) Log.i(TAG, "matched same board code, no jump done");
            return false;
        }
        Intent intent = BoardActivity.createIntent(this, boardCodeForJump, "");
        if (DEBUG) Log.i(TAG, "matched board /" + boardCodeForJump + "/, starting");
        startActivity(intent);
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean drawerEnabled;
        if (threadNo > 0)
            drawerEnabled = false;
        else
            drawerEnabled = true;
        if (DEBUG) Log.i(TAG, "onResume() drawerEnabled=" + drawerEnabled);
        mDrawerToggle.setDrawerIndicatorEnabled(drawerEnabled);
    }

}
