package com.chanapps.four.fragment;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.widget.*;
import com.chanapps.four.activity.*;
import com.chanapps.four.component.TutorialOverlay;
import com.chanapps.four.data.*;
import com.chanapps.four.loader.PopularCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.util.*;

public class PopularFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    private static final String TAG = PopularFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static ImageLoader imageLoader = null;
    private static DisplayImageOptions displayImageOptions;

    private static void initStatics(View view) {
        imageLoader = ChanImageLoader.getInstance(view.getContext());
        displayImageOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.NONE)
                .cacheOnDisc()
                .cacheInMemory()
                .resetViewBeforeLoading()
                .build();
    }

    private View layout;
    private TextView emptyText;

    protected Handler handler;
    protected Loader<Cursor> cursorLoader;

    protected ImageLoadingListener imageLoadingListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {}
        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            makeTextWrapperVisible(view, false);
        }
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            makeTextWrapperVisible(view, true);
        }
        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            makeTextWrapperVisible(view, false);
        }
    };

    protected void makeTextWrapperVisible(View imageView, boolean imageLoaded) {
        ViewParent parent = imageView.getParent();
        View parentView = (View)parent;
        View textWrapperView = parentView.findViewById(R.id.text_wrapper);
        textWrapperView.setVisibility(View.VISIBLE);
        if (!imageLoaded)
            textWrapperView.setBackgroundColor(R.color.PaletteBlack);
    }

    public void refresh() {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    getLoaderManager().restartLoader(0, null, PopularFragment.this);
                }
            });
    }

    public void backgroundRefresh() {
        Handler handler = NetworkProfileManager.instance().getActivity().getChanHandler();
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    getLoaderManager().restartLoader(0, null, PopularFragment.this);
                }
            });
    }

    public Context getBaseContext() {
        return getActivity().getBaseContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        layout = inflater.inflate(R.layout.popular_layout, container, false);
        emptyText = (TextView)layout.findViewById(R.id.board_empty_text);
        if (imageLoader == null)
            initStatics(layout);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (handler == null)
            handler = new Handler();
    }

    public Handler getChanHandler() {
        return handler;
    }

    @Override
    public void onPause() {
        super.onPause();
        handler = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart");
        if (handler == null)
            handler = new Handler();
        //if (absListView.getCount() <= 0) {
        //    if (DEBUG) Log.i(TAG, "No data displayed, starting loader");
        //    getLoaderManager().restartLoader(0, null, this);
        //}
        new TutorialOverlay(layout, TutorialOverlay.Page.POPULAR);
    }

    @Override
    public void onStop () {
    	super.onStop();
        handler = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        cursorLoader = new PopularCursorLoader(getBaseContext());
        getActivity().setProgressBarIndeterminateVisibility(true);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swapCursor(data);
        if (data.getCount() > 0) {
            emptyText.setVisibility(View.GONE);
        }
        else {
            emptyText.setText(R.string.board_empty_popular);
            emptyText.setVisibility(View.VISIBLE);
        }
        getActivity().setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /*
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        int threadFlags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        if ((threadFlags & ChanThread.THREAD_FLAG_AD) > 0) {
            return;
        }
        final FragmentActivity activity = getActivity();

        final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TITLE));
        final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        if ((threadFlags & ChanThread.THREAD_FLAG_TITLE) > 0
                && title != null && !title.isEmpty()
                && desc != null && !desc.isEmpty()) {
            (new GenericDialogFragment(title.replaceAll("<[^>]*>", " "), desc))
                    .show(activity.getSupportFragmentManager(), PopularFragment.TAG);
            return;
        }

        final String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        final long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
        if (DEBUG) Log.i(TAG, "clicked thread " + boardCode + "/" + threadNo);
        ThreadActivity.startActivity(getActivity(), boardCode, threadNo, "");
    }
    */

    protected void swapCursor(Cursor cursor) {
        setLastFetchedText();
        cursor.moveToPosition(-1);
        int i = 0;
        int popular = 0;
        int latest = 0;
        int recent = 0;
        List<Integer> popularList = new ArrayList<Integer>(NUM_POPULAR);
        while (cursor.moveToNext()) {
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            View item;
            if ((flags & ChanThread.THREAD_FLAG_POPULAR_THREAD) > 0) {
                //item = layout.findViewById(getPopularViewId(popular++));
                popularList.add(i);
                item = null;
            }
            else if ((flags & ChanThread.THREAD_FLAG_LATEST_POST) > 0) {
                item = layout.findViewById(getLatestViewId(latest++));
            }
            else if ((flags & ChanThread.THREAD_FLAG_RECENT_IMAGE) > 0) {
                item = layout.findViewById(getRecentViewId(recent++));
            }
            else {
                item = null;
            }
            if (item != null)
                setViewValue(item, cursor);
            i++;
        }
        displayPopular(cursor, popularList);
    }

    protected void setLastFetchedText() {
        ChanBoard board = ChanFileStorage.loadBoardData(getBaseContext(), ChanBoard.POPULAR_BOARD_CODE);
        TextView lastFetchedView = (TextView)layout.findViewById(R.id.last_fetched);
        if (board != null && board.lastFetched > 0) {
            long now = System.currentTimeMillis();
            if (DEBUG) Log.i(TAG, "lastFetched=" + board.lastFetched);
            CharSequence lastFetched = Math.abs(now - board.lastFetched) < 60000
                    ? getString(R.string.board_just_now)
                    : DateUtils.getRelativeTimeSpanString(board.lastFetched, now, DateUtils.SECOND_IN_MILLIS);
            lastFetchedView.setText(lastFetched);
        }
        else {
            lastFetchedView.setText("");
        }
    }

    protected int getPopularViewId(int i) {
        switch(i) {
            case 0: return R.id.popular_item_0;
            case 1: return R.id.popular_item_1;
            case 2: return R.id.popular_item_2;
            case 3: return R.id.popular_item_3;
            case 4: return R.id.popular_item_4;
            case 5: return R.id.popular_item_5;
            case 6: return R.id.popular_item_6;
            case 7: return R.id.popular_item_7;
            case 8: return R.id.popular_item_8;
            case 9: return R.id.popular_item_9;
            default: return 0;
        }
    }

    protected int getLatestViewId(int i) {
        switch(i) {
            case 0: return R.id.latest_item_0;
            case 1: return R.id.latest_item_1;
            case 2: return R.id.latest_item_2;
            case 3: return R.id.latest_item_3;
            case 4: return R.id.latest_item_4;
            case 5: return R.id.latest_item_5;
            case 6: return R.id.latest_item_6;
            case 7: return R.id.latest_item_7;
            case 8: return R.id.latest_item_8;
            case 9: return R.id.latest_item_9;
            default: return 0;
        }
    }

    protected int getRecentViewId(int i) {
        switch(i) {
            case 0: return R.id.recent_item_0;
            case 1: return R.id.recent_item_1;
            case 2: return R.id.recent_item_2;
            default: return 0;
        }
    }

    protected void setViewValue(View item, Cursor cursor) {
        String subject = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        String thumbUrl = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL));
        String boardName = ChanBoard.getBoardByCode(getBaseContext(), boardCode).name;
        long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));

        TextView boardNameView = (TextView)item.findViewById(R.id.board_name);
        TextView subjectView = (TextView)item.findViewById(R.id.subject);
        final View textWrapperView = item.findViewById(R.id.text_wrapper);
        View clickTargetView = item.findViewById(R.id.click_target);

        ImageView imageView = (ImageView)item.findViewById(R.id.image);
        boardNameView.setText(boardName);
        subjectView.setText(Html.fromHtml(subject));
        imageView.setImageDrawable(null);
        textWrapperView.setVisibility(View.GONE);
        imageLoader.displayImage(thumbUrl, imageView, displayImageOptions, imageLoadingListener);

        clickTargetView.setTag(R.id.BOARD_CODE, boardCode);
        clickTargetView.setTag(R.id.THREAD_NO, threadNo);
        clickTargetView.setOnClickListener(this);
    }

    private static final int NUM_POPULAR = 10;

    @Override
    public void onClick(View view) {
        String boardCode = (String)view.getTag(R.id.BOARD_CODE);
        Long threadNo = (Long)view.getTag(R.id.THREAD_NO);
        Activity activity = getActivity();
        if (boardCode != null && threadNo != null && activity != null)
            ThreadActivity.startActivity(activity, boardCode, threadNo, "");
    }

    protected void displayPopular(Cursor cursor, List<Integer> popularList) {
        List<Pair<Integer, Double>> aspectList = new ArrayList<Pair<Integer, Double>>(NUM_POPULAR - 1);
        for (int i : popularList) {
            cursor.moveToPosition(i);
            long tn_w = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_TN_W));
            long tn_h = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_TN_H));
            double aspectRatio = (double)tn_w / (double)tn_h;
            aspectList.add(new Pair<Integer, Double>(i, aspectRatio));
        }
        Collections.sort(aspectList, new Comparator<Pair<Integer, Double>>() {
            @Override
            public int compare(Pair<Integer, Double> lhs, Pair<Integer, Double> rhs) {
                double diff = lhs.second - rhs.second;
                if (diff == 0)
                    return 0;
                else if (diff < 0)
                    return -1;
                else
                    return 1;
            }
        });

        int popular = 0;
        displayTargetRatio(cursor, aspectList, 1, getPopularViewId(popular++));
        
        Pair<Integer, Double> mostTall = aspectList.get(0);
        View tallItem = layout.findViewById(getPopularViewId(popular++));
        cursor.moveToPosition(mostTall.first);
        setViewValue(tallItem, cursor);
        aspectList.remove(mostTall);

        for (int i = 0; i < 4; i++)
            displayTargetRatio(cursor, aspectList, 1.6, getPopularViewId(popular++));

        Collections.reverse(aspectList);
        for (Pair<Integer, Double> p : aspectList) {
            View item = layout.findViewById(getPopularViewId(popular++));
            cursor.moveToPosition(p.first);
            setViewValue(item, cursor);
        }
    }

    protected void displayTargetRatio(Cursor cursor, List<Pair<Integer, Double>> aspectList,
                                      double targetRatio, int viewId) {
        Pair<Integer, Double> bestFit = null;
        for (Pair<Integer, Double> p : aspectList)
            if (bestFit == null || Math.abs(p.second - targetRatio) < Math.abs(bestFit.second - targetRatio))
                bestFit = p;
        View item = layout.findViewById(viewId);
        cursor.moveToPosition(bestFit.first);
        setViewValue(item, cursor);
        aspectList.remove(bestFit);
    }
    
}
