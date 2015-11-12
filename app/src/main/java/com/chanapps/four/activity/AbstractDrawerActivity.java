package com.chanapps.four.activity;

import android.content.Intent;
import android.content.res.Configuration;
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
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanThread;

import java.util.*;

@SuppressWarnings("unchecked")
abstract public class
        AbstractDrawerActivity
        extends AbstractBoardSpinnerActivity
        implements ChanIdentifiedActivity
{
    protected static final String TAG = AbstractDrawerActivity.class.getSimpleName();
    protected static final boolean DEBUG = false;

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

    protected String[] mDrawerArray;
    protected ListView mDrawerList;
    protected DrawerLayout mDrawerLayout;
    protected SimpleAdapter mDrawerAdapter;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected boolean hasFavorites = false;
    protected boolean hasWatchlist = false;

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
        super.onConfigurationChanged(config);
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

    protected void loadDrawerArray() {
        List<String> drawer = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.long_drawer_array)));
        loadFavorites(drawer);
        loadWatchlist(drawer);
        loadFooter(drawer);
        mDrawerArray = drawer.toArray(new String[drawer.size()]);
        Handler callbackHandler = getChanHandler();
        if (callbackHandler != null)
            callbackHandler.post(setAdaptersCallback);
    }

    protected void loadFooter(List<String> drawer) {
        List<String> items = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.long_drawer_array_footer)));
        drawer.addAll(items);
    }

    protected void loadFavorites(List<String> drawer) {
        List<String> items = new ArrayList<String>();
        ChanBoard board = ChanFileStorage.loadBoardData(this, ChanBoard.FAVORITES_BOARD_CODE);
        if (board != null && board.hasData()) {
            for (ChanThread thread : board.threads) {
                String boardName = "/" + thread.board + "/ " + ChanBoard.getName(this, thread.board);
                items.add(boardName);
            }
        }
        hasFavorites = items.size() > 0;
        drawer.add(getString(R.string.board_favorites));
        Collections.sort(items);
        drawer.addAll(items);
    }
    
    protected void loadWatchlist(List<String> drawer) {
        List<String> items = new ArrayList<String>();
        ChanBoard board = ChanFileStorage.loadBoardData(this, ChanBoard.WATCHLIST_BOARD_CODE);
        if (board != null && board.hasData()) {
            for (ChanThread thread : board.threads) {
                String threadText = thread.drawerSubject(this);
                items.add(threadText);
            }
        }
        hasWatchlist = items.size() > 0;
        drawer.add(getString(R.string.board_watch));
        Collections.sort(items);
        drawer.addAll(items);
    }

    protected void setDrawerAdapter() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadDrawerArray();
            }
        }).start();
    }

    protected Runnable setAdaptersCallback = new Runnable() {
        @Override
        public void run() {
            List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
            for (int i = 0; i < mDrawerArray.length; i++) {
                String drawerText = mDrawerArray[i];
                BoardType type = BoardType.valueOfDrawerString(AbstractDrawerActivity.this, drawerText);
                int drawableId;
                if (type != null)
                    drawableId = type.drawableId();
                else if (getString(R.string.settings_menu).equals(drawerText))
                    drawableId = R.drawable.gear;
                else if (getString(R.string.send_feedback_menu).equals(drawerText))
                    drawableId = R.drawable.speech_bubble_ellipsis;
                else
                    drawableId = 0;
                if (DEBUG) Log.v(TAG, "row=" + i + " text=" + drawerText + " drawableId=" + drawableId);
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(ROW_ID, "" + i);
                map.put(TEXT, drawerText);
                map.put(DRAWABLE_ID, "" + drawableId);
                fillMaps.add(map);
            }
            mDrawerAdapter = new SimpleAdapter(AbstractDrawerActivity.this, fillMaps, R.layout.drawer_list_item, adapterFrom, adapterTo) {
                @Override
                public boolean isEnabled(int position) {
                    String drawerText = mDrawerArray[position];
                    BoardType type = BoardType.valueOfDrawerString(AbstractDrawerActivity.this, drawerText);
                    //if (type == BoardType.META)
                    //    return false;
                    //else
                        return true;
                }
            };
            mDrawerAdapter.setViewBinder(mViewBinder);
            mDrawerList.setAdapter(mDrawerAdapter);
        }
    };

    protected SimpleAdapter.ViewBinder mViewBinder = new SimpleAdapter.ViewBinder() {
        protected int pickSelector(BoardType type) {
            int selector;
            if (type != null && type.boardCode() != null && boardCode != null && type.boardCode().equals(boardCode))
                selector = R.drawable.drawer_list_selector_checked_bg;

            else if (getApplicationContext() != null && ThemeSelector.instance(getApplicationContext()).isDark())
                selector = R.drawable.drawer_list_selector_inverse_bg_dark;
            else
                selector = R.drawable.drawer_list_selector_inverse_bg;
            return selector;
        }
        public boolean setViewValue(View view, Object data, String textRepresentation) {
            switch (view.getId()) {
                case R.id.drawer_list_item:
                    // find item
                    int pos = Integer.valueOf((String)data);
                    Map<String, String> item = (Map<String, String>)mDrawerAdapter.getItem(pos);
                    String drawerText = item.get(TEXT);
                    int drawableId = Integer.valueOf(item.get(DRAWABLE_ID));
                    BoardType type = BoardType.valueOfDrawerString(AbstractDrawerActivity.this, drawerText);
                    int selector = pickSelector(type);
                    FrameLayout child = (FrameLayout)view.findViewById(R.id.frame_child);
                    Drawable selectorDrawable = getLayoutInflater().getContext().getResources().getDrawable(selector);
                    child.setForeground(selectorDrawable);

                    // set title state
                    ImageView icon = (ImageView)view.findViewById(R.id.drawer_list_item_icon);
                    TextView text = (TextView)view.findViewById(R.id.drawer_list_item_text);
                    TextView title = (TextView)view.findViewById(R.id.drawer_list_item_title);
                    TextView detail = (TextView)view.findViewById(R.id.drawer_list_item_detail);
                    View divider = view.findViewById(R.id.drawer_list_item_divider);

                    if (//type == BoardType.META ||
                            (type != null && type == BoardType.FAVORITES && hasFavorites) ||
                                    (type != null && type == BoardType.WATCHLIST && hasWatchlist)) {
                        title.setText(drawerText);
                        detail.setText("");
                        text.setText("");
                        icon.setVisibility(View.GONE);
                        text.setVisibility(View.GONE);
                        title.setVisibility(View.VISIBLE);
                        detail.setVisibility(View.GONE);
                        divider.setVisibility(View.VISIBLE);
                    }
                    else if (type != null) {
                        title.setText("");
                        detail.setText("");
                        text.setText(drawerText);
                        icon.setVisibility(View.VISIBLE);
                        text.setVisibility(View.VISIBLE);
                        title.setVisibility(View.GONE);
                        divider.setVisibility(View.GONE);
                        detail.setVisibility(View.GONE);
                    }
                    else if (drawableId > 0) {
                        title.setText("");
                        detail.setText("");
                        text.setText(drawerText);
                        icon.setVisibility(View.VISIBLE);
                        text.setVisibility(View.VISIBLE);
                        title.setVisibility(View.GONE);
                        divider.setVisibility(View.GONE);
                        detail.setVisibility(View.GONE);
                    }
                    else if (drawerText.isEmpty()) {
                        title.setText("");
                        detail.setText("");
                        text.setText("");
                        title.setVisibility(View.GONE);
                        detail.setVisibility(View.GONE);
                        icon.setVisibility(View.GONE);
                        text.setVisibility(View.GONE);
                        title.setVisibility(View.GONE);
                        divider.setVisibility(View.VISIBLE);
                    }
                    else {
                        title.setText("");
                        detail.setText(drawerText);
                        text.setText("");
                        title.setVisibility(View.GONE);
                        detail.setVisibility(View.VISIBLE);
                        icon.setVisibility(View.GONE);
                        text.setVisibility(View.GONE);
                        title.setVisibility(View.VISIBLE);
                        divider.setVisibility(View.GONE);
                    }

                    // set text color
                    /*
                    int textColor;
                    if (type == BoardType.valueOfBoardCode(boardCode))
                        textColor = R.color.PaletteWhite;
                    else
                        textColor = R.color.PaletteDrawerDividerText;
                    text.setTextColor(getResources().getColor(textColor));
                    */

                    if (DEBUG) Log.v(TAG, "mViewBinder:setViewValue() item pos=" + pos
                            + " checked=" + (selector == R.drawable.drawer_list_selector_checked_bg) + " type=" + type
                            + " text=" + text + " item=" + item);

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
                loadDrawerArray();
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
        closeDrawer();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void refresh() {
    }

    @Override
    public void closeSearch() {}

    abstract public boolean isSelfDrawerMenu(String boardAsMenu);

    protected ListView.OnItemClickListener drawerClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (DEBUG) Log.i(TAG, "onItemClick parent=" + parent + " view=" + view + " pos=" + position + " id=" + id);
            closeDrawer();
            HashMap<String, String> item = (HashMap<String, String>)parent.getItemAtPosition(position);
            String boardAsMenu = item.get(TEXT);
            if (DEBUG) Log.i(TAG, "onItemClick boardAsMenu=" + boardAsMenu + " calling handleSelectItem");
            handleSelectItem(boardAsMenu);
            if (DEBUG) Log.i(TAG, "onItemClick boardAsMenu=" + boardAsMenu + " complete");
        }
    };

    @Override
    protected void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean drawerEnabled;
        //if (threadNo > 0)
        //    drawerEnabled = false;
        //else
            drawerEnabled = true;
        if (DEBUG) Log.i(TAG, "onResume() drawerEnabled setting to=" + drawerEnabled);
        mDrawerToggle.setDrawerIndicatorEnabled(drawerEnabled);
        if (DEBUG) Log.i(TAG, "onResume() drawerEnabled set to=" + drawerEnabled);
    }

    @Override
    public void switchBoard(String boardCode, String query) {}

}
