package com.chanapps.four.component;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.RefreshableActivity;
import com.chanapps.four.adapter.ThreadCursorAdapter;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanThread;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/2/13
 * Time: 11:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadPostPopup extends BoardThreadPopup {

    public static final String TAG = ThreadPostPopup.class.getSimpleName();
    protected static final boolean DEBUG = false;

    protected ThreadCursorAdapter adapter;

    public ThreadPostPopup(RefreshableActivity activity,
                           LayoutInflater layoutInflater,
                           ImageLoader imageLoader,
                           DisplayImageOptions displayImageOptions,
                           ThreadCursorAdapter adapter)
    {
        super(activity, layoutInflater, imageLoader, displayImageOptions);
        this.adapter = adapter;
    }

    @Override
    protected void setGoToThreadButton(final AdapterView<?> adapterView, final View view, final int position, final long id) {
        goToThreadButtonLine.setVisibility(View.GONE); // we are already at thread level
        goToThreadButton.setVisibility(View.GONE); // we are already at thread level
    }

    @Override
    protected void displayHighlightButton(final String boardCode, final long threadNo, final long postNo) { // board-level doesn't highlight, only thread-level does
        if (postNo > 0) {
            highlightButtonLine.setVisibility(View.VISIBLE);
            highlightButton.setVisibility(View.VISIBLE);
            highlightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HighlighterTask task = new HighlighterTask(activity.getBaseContext(), adapter, boardCode, threadNo);
                    task.execute(postNo);
                    popupWindow.dismiss();
                }
            });
        }
        else {
            highlightButtonLine.setVisibility(View.GONE);
            highlightButton.setVisibility(View.GONE);
        }
    }

    private class HighlighterTask extends AsyncTask<Long, Void, String> {
        private Context context = null;
        private ThreadCursorAdapter threadAdapter = null;
        private String boardCode = null;
        private long threadNo = 0;
        public HighlighterTask(Context context, ThreadCursorAdapter adapter, String boardCode, long threadNo) {
            this.context = context;
            this.threadAdapter = adapter;
            this.boardCode = boardCode;
            this.threadNo = threadNo;
        }
        @Override
        protected String doInBackground(Long... postNos) {
            String result = null;
            long postNo = postNos[0];
            long[] prevPosts = null;
            long[] nextPosts = null;
            try {
                ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
                if (thread != null) {
                    prevPosts = thread.getPrevPostsReferenced(postNo);
                    nextPosts = thread.getNextPostsReferredTo(postNo);
                }
                else {
                    result = context.getString(R.string.thread_couldnt_load);
                    Log.e(TAG, "Coludn't load thread " + boardCode + "/" + threadNo);
                }
            }
            catch (Exception e) {
                result = context.getString(R.string.thread_couldnt_load);
                Log.e(TAG, "Exception while getting thread post highlights", e);
            }
            threadAdapter.setHighlightPosts(postNo, prevPosts, nextPosts);
            if ((prevPosts == null || prevPosts.length == 0) && (nextPosts == null || nextPosts.length == 0)) {
                result = context.getString(R.string.thread_no_replies_found);
            }
            else {
                String msg = context.getString(R.string.thread_replies_found);
                result = String.format(msg, prevPosts == null ? 0 : prevPosts.length, nextPosts == null ? 0 : nextPosts.length);
            }
            if (DEBUG) Log.i(TAG, "Set highlight posts prev=" + Arrays.toString(prevPosts) + " next=" + Arrays.toString(nextPosts));
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            threadAdapter.notifyDataSetChanged();
        }
    }

}
