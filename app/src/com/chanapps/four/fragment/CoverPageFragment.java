package com.chanapps.four.fragment;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.chanapps.four.activity.*;
import com.chanapps.four.data.*;
import com.chanapps.four.loader.PopularCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class CoverPageFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    public static final String TAG = CoverPageFragment.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final String SUBJECT_FONT = "fonts/Roboto-BoldCondensed.ttf";

    private static ImageLoader imageLoader = null;
    private static DisplayImageOptions displayImageOptions;
    private static Typeface subjectTypeface = null;
    private View topTextWrapper = null;
    private View leftTextWrapper = null;
    private View rightTextWrapper = null;

    private static void initStatics(View view) {
        subjectTypeface = Typeface.createFromAsset(view.getResources().getAssets(), SUBJECT_FONT);
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

    protected ImageLoadingListener topImageLoadingListener = null;
    protected ImageLoadingListener leftImageLoadingListener = null;
    protected ImageLoadingListener rightImageLoadingListener = null;

    protected ImageLoadingListener createImageLoadingListener(final View textWrapperView) {
        return new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {}
            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                makeTextWrapperVisible(textWrapperView, false);
            }
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                makeTextWrapperVisible(textWrapperView, true);
            }
            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                makeTextWrapperVisible(textWrapperView, false);
            }
        };
    }

    protected void makeTextWrapperVisible(View textWrapperView, boolean imageLoaded) {
        if (textWrapperView == null)
            return;
        if (imageLoaded && textWrapperView.getVisibility() != View.VISIBLE)
            textWrapperView.setVisibility(View.VISIBLE);
        //if (!imageLoaded)
        //    textWrapperView.setBackgroundColor(R.color.PaletteBlack);
    }

    public void refresh() {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    getLoaderManager().restartLoader(0, null, CoverPageFragment.this);
                }
            });
    }

    public void backgroundRefresh() {
        Handler handler = NetworkProfileManager.instance().getActivity().getChanHandler();
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    getLoaderManager().restartLoader(0, null, CoverPageFragment.this);
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
        layout = inflater.inflate(R.layout.cover_page_layout, container, false);
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
        //new TutorialOverlay(layout, TutorialOverlay.Page.POPULAR);
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
                    .show(activity.getSupportFragmentManager(), CoverPageFragment.TAG);
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

        View topItem = layout.findViewById(R.id.cover_page_top_item);
        setTitleTypeface(topItem);
        topTextWrapper = topItem.findViewById(R.id.text_wrapper);
        topTextWrapper.setVisibility(View.GONE);
        topImageLoadingListener = createImageLoadingListener(topTextWrapper);
        setClickTarget(topTextWrapper, ChanBoard.POPULAR_BOARD_CODE);

        View leftItem = layout.findViewById(R.id.cover_page_left_item);
        setTitleTypeface(leftItem);
        leftTextWrapper = leftItem.findViewById(R.id.text_wrapper);
        leftTextWrapper.setVisibility(View.GONE);
        leftImageLoadingListener = createImageLoadingListener(leftTextWrapper);
        setClickTarget(leftTextWrapper, ChanBoard.LATEST_BOARD_CODE);

        View rightItem = layout.findViewById(R.id.cover_page_right_item);
        setTitleTypeface(rightItem);
        rightTextWrapper = rightItem.findViewById(R.id.text_wrapper);
        rightTextWrapper.setVisibility(View.GONE);
        rightImageLoadingListener = createImageLoadingListener(rightTextWrapper);
        setClickTarget(rightTextWrapper, ChanBoard.LATEST_IMAGES_BOARD_CODE);

        cursor.moveToPosition(-1);
        //int i = 0;
        int popular = 0;
        int latest = 0;
        int recent = 0;
        String popularText = "";
        String latestText = "";
        String recentText = "";
        int recommended = 0;
        //List<Integer> popularList = new ArrayList<Integer>(NUM_POPULAR);
        //boolean isFirst = false;
        while (cursor.moveToNext()) {
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            if ((flags & ChanThread.THREAD_FLAG_POPULAR_THREAD) > 0) {
                ImageView item = (ImageView)topItem.findViewById(getPopularImageViewId(popular++));
                if (item != null)
                    setImageValue(item, cursor, topImageLoadingListener);
                String subject = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
                if (subject != null && !subject.isEmpty())
                    popularText += (popularText.isEmpty() ? "" : " - ") + subject;
                //popularList.add(i);
            }
            else if ((flags & ChanThread.THREAD_FLAG_LATEST_POST) > 0) {
                ImageView item = (ImageView)leftItem.findViewById(getLatestImageViewId(latest++));
                if (item != null)
                    setImageValue(item, cursor, leftImageLoadingListener);
                String subject = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
                if (DEBUG) Log.i(TAG, "Latest subject=[" + subject + "]");
                if (subject != null && !subject.isEmpty())
                    latestText += (latestText.isEmpty() ? "" : " - ") + subject;
            }
            else if ((flags & ChanThread.THREAD_FLAG_RECENT_IMAGE) > 0) {
                ImageView item = (ImageView)rightItem.findViewById(getRecentImageViewId(recent++));
                if (item != null)
                    setImageValue(item, cursor, rightImageLoadingListener);
                String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                if (boardCode != null && !boardCode.isEmpty()) {
                    String boardName = ChanBoard.getBoardByCode(getBaseContext(), boardCode).name;
                    recentText += (recentText.isEmpty() ? "" : " - ") + boardName;
                }
            }/*
            else if ((flags & ChanThread.THREAD_FLAG_BOARD) > 0) {
                ImageView item = (ImageView)layout.findViewById(getImageViewId(recommended++));
                if (item != null)
                    setImageValue(item, cursor);
            }
            */
            //i++;
        }
        if (!popularText.isEmpty()) {
            TextView tv = (TextView)topItem.findViewById(R.id.popular_subject);
            if (tv != null)
                tv.setText(popularText);
        }
        if (DEBUG) Log.i(TAG, "Latest text=[" + latestText + "]");
        if (!latestText.isEmpty()) {
            TextView tv = (TextView)leftItem.findViewById(R.id.latest_subject);
            if (tv != null)
                tv.setText(latestText);
            if (DEBUG) Log.i(TAG, "Latest tv=[" + tv + "]");
        }
        if (!recentText.isEmpty()) {
            TextView tv = (TextView)rightItem.findViewById(R.id.recent_subject);
            if (tv != null)
                tv.setText(recentText);
        }
        //displayPopular(cursor, popularList);
    }

    protected void setLastFetchedText() {
    /*
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
        */
    }

    protected void setClickTarget(View item, String boardCode) {
        item.setTag(R.id.BOARD_CODE, boardCode);
        item.setOnClickListener(this);
    }

    protected void setClickTarget(View item, String boardCode, long threadNo) {
        item.setTag(R.id.BOARD_CODE, boardCode);
        item.setTag(R.id.THREAD_NO, threadNo);
        item.setOnClickListener(this);
    }

    protected void setClickTarget(View item, String boardCode, long threadNo, long postNo) {
        item.setTag(R.id.BOARD_CODE, boardCode);
        item.setTag(R.id.THREAD_NO, threadNo);
        item.setTag(R.id.POST_NO, postNo);
        item.setOnClickListener(this);
    }

    protected int getPopularImageViewId(int i) {
        switch(i) {
            case 0: return R.id.popular_image_0;
            case 1: return R.id.popular_image_1;
            case 2: return R.id.popular_image_2;
            case 3: return R.id.popular_image_3;
            case 4: return R.id.popular_image_4;
            case 5: return R.id.popular_image_5;
            case 6: return R.id.popular_image_6;
            case 7: return R.id.popular_image_7;
            case 8: return R.id.popular_image_8;
            default: return 0;
        }
    }

    protected int getLatestImageViewId(int i) {
        switch(i) {
            case 0: return R.id.latest_image_0;
            case 1: return R.id.latest_image_1;
            case 2: return R.id.latest_image_2;
            case 3: return R.id.latest_image_3;
            case 4: return R.id.latest_image_4;
            case 5: return R.id.latest_image_5;
            case 6: return R.id.latest_image_6;
            case 7: return R.id.latest_image_7;
            case 8: return R.id.latest_image_8;
            default: return 0;
        }
    }

    protected int getRecentImageViewId(int i) {
        switch(i) {
            case 0: return R.id.recent_image_0;
            case 1: return R.id.recent_image_1;
            case 2: return R.id.recent_image_2;
            default: return 0;
        }
    }

    protected void setTitleTypeface(View item) {
        TextView title = (TextView)item.findViewById(R.id.title);
        if (title != null)
            title.setTypeface(subjectTypeface);
    }

    protected void setImageValue(ImageView imageView, Cursor cursor, ImageLoadingListener imageLoadingListener) {
        //int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
        //String subject = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
        String boardCode = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
        long threadNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
        long postNo = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_JUMP_TO_POST_NO));
        String thumbUrl = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_THUMBNAIL_URL));
        //String boardName = ChanBoard.getBoardByCode(getBaseContext(), boardCode).name;

        //TextView boardNameView = (TextView)item.findViewById(R.id.board_name);
        //TextView subjectView = (TextView)item.findViewById(R.id.subject);
        //final View textWrapperView = item.findViewById(R.id.text_wrapper);
        /*
        boardNameView.setText(boardName);
        if ((flags & (ChanThread.THREAD_FLAG_RECENT_IMAGE | ChanThread.THREAD_FLAG_BOARD)) > 0) {
            subjectView.setVisibility(View.GONE);
            subjectView.setText("");
        }
        else {
            subjectView.setText(Html.fromHtml(subject));
            subjectView.setVisibility(View.VISIBLE);
        }
        textWrapperView.setVisibility(View.GONE);
        */
        imageLoader.displayImage(thumbUrl, imageView, displayImageOptions, imageLoadingListener);
        if (postNo > 0)
            setClickTarget(imageView, boardCode, threadNo, postNo);
        else
            setClickTarget(imageView, boardCode, threadNo);
        //if (DEBUG) Log.i(TAG, "/" + boardCode + " sub=[" + subject + "] url=[" + thumbUrl + "]");
    }

    //private static final int NUM_POPULAR = 10;

    @Override
    public void onClick(View view) {
        String boardCode = (String)view.getTag(R.id.BOARD_CODE);
        Long threadNoObj = (Long)view.getTag(R.id.THREAD_NO);
        long threadNo = threadNoObj == null ? 0 : threadNoObj;
        Long postNoObj = (Long)view.getTag(R.id.POST_NO);
        long postNo = postNoObj == null ? 0 : postNoObj;
        Activity activity = getActivity();
        if (activity == null)
            return;
        if (boardCode != null && threadNo > 0 && postNo > 0)
            ThreadActivity.startActivity(activity, boardCode, threadNo, postNo, ""); // FIXME: add post no
        else if (boardCode != null && threadNo > 0)
            ThreadActivity.startActivity(activity, boardCode, threadNo, ""); // FIXME: add post no
        else if (boardCode != null)
            BoardActivity.startActivity(activity, boardCode, ""); // FIXME: add post no
    }

    /*
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
        displayTargetRatio(cursor, aspectList, 1, getPopularImageViewId(popular++));
        
        while (!aspectList.isEmpty())
            displayItem(cursor, aspectList, aspectList.get(0), getPopularImageViewId(popular++));
    }

    protected void displayTargetRatio(Cursor cursor, List<Pair<Integer, Double>> aspectList,
                                      double targetRatio, int viewId) {
        Pair<Integer, Double> bestFit = null;
        for (Pair<Integer, Double> p : aspectList)
            if (bestFit == null || Math.abs(p.second - targetRatio) < Math.abs(bestFit.second - targetRatio))
                bestFit = p;
        displayItem(cursor, aspectList, bestFit, viewId);
    }

    protected void displayItem(Cursor cursor, List<Pair<Integer, Double>> aspectList, Pair<Integer, Double> bestFit,
                               int viewId) {
        ImageView item = (ImageView)layout.findViewById(viewId);
        cursor.moveToPosition(bestFit.first);
        setImageValue(item, cursor);
        aspectList.remove(bestFit);
    }
    */
}
