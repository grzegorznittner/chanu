package com.chanapps.four.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.ThreadActivity;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 4/3/13
* Time: 10:55 PM
* To change this template use File | Settings | File Templates.
*/
public class HighlightRepliesTask extends AsyncTask<long[], Void, String> {

    private static final boolean DEBUG = false;

    private Context context = null;
    private AbsListView absListView = null;
    private String boardCode = null;
    private long threadNo = 0;
    private SearchType searchType = SearchType.PREVIOUS_POSTS;
    private Set<Long> origSet = new HashSet<Long>();
    private Set<Long> repliesSet = new HashSet<Long>();

    public enum SearchType {
        PREVIOUS_POSTS,
        POST_REPLIES,
        SAME_POSTERS
    }
    
    public HighlightRepliesTask(Context context, AbsListView absListView, String boardCode, long threadNo, SearchType searchType) {
        this.context = context;
        this.absListView = absListView;
        this.boardCode = boardCode;
        this.threadNo = threadNo;
        this.searchType = searchType;
    }

    protected void addPostsToReplies(long[] replies) {
        if (replies == null)
            return;
        for (long postNo : replies)
            repliesSet.add(postNo);
    }

    @Override
    protected String doInBackground(long[]... postNosArgs) {
        long[] postNos = postNosArgs[0];
        origSet.clear();
        for (long postNo : postNos)
            origSet.add(postNo);
        repliesSet.clear();
        try {
            ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
            if (thread != null) {
                for (long postNo : postNos) {
                    switch (searchType) {
                        case PREVIOUS_POSTS:
                            addPostsToReplies(thread.getPrevPostsReferenced(postNo));
                            break;
                        case POST_REPLIES:
                            addPostsToReplies(thread.getNextPostsReferredTo(postNo));
                            break;
                        case SAME_POSTERS:
                            ChanPost post = thread.getPost(postNo);
                            if (post == null)
                                break;
                            addPostsToReplies(thread.getIdPosts(postNo, post.id));
                            addPostsToReplies(thread.getTripcodePosts(postNo, post.trip));
                            addPostsToReplies(thread.getNamePosts(postNo, post.name));
                            addPostsToReplies(thread.getEmailPosts(postNo, post.email));
                            break;
                    }
                }
            }
            else {
                Log.e(ThreadActivity.TAG, "Coludn't load thread " + boardCode + "/" + threadNo);
                return context.getString(R.string.thread_couldnt_load);
            }
        }
        catch (Exception e) {
            Log.e(ThreadActivity.TAG, "Exception while getting thread post replies", e);
            return context.getString(R.string.thread_couldnt_load);
        }
        if (DEBUG) Log.i(ThreadActivity.TAG, "Set highlight posts=" + Arrays.toString(repliesSet.toArray()));

        if (repliesSet.size() == 0)
            switch (searchType) {
                case PREVIOUS_POSTS:
                    return context.getString(R.string.thread_no_prev_posts_found);
                case POST_REPLIES:
                default:
                    return context.getString(R.string.thread_no_replies_found);
                case SAME_POSTERS:
                    return context.getString(R.string.thread_no_same_poster_found);
            }

        switch (searchType) {
            case PREVIOUS_POSTS:
                return String.format(context.getString(R.string.thread_prev_replies_found), repliesSet.size());
            case POST_REPLIES:
            default:
                return String.format(context.getString(R.string.thread_next_replies_found), repliesSet.size());
            case SAME_POSTERS:
                return String.format(context.getString(R.string.thread_id_found), repliesSet.size());
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        for (int pos = 0; pos < absListView.getCount(); pos++) {
            Long id = absListView.getItemIdAtPosition(pos);
            if (!origSet.contains(id))
                absListView.setItemChecked(pos, repliesSet.contains(id));
        }
    }

}
