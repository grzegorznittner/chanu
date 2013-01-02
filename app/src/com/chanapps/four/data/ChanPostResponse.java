package com.chanapps.four.data;

import android.content.Context;
import android.nfc.Tag;
import android.util.Log;
import com.chanapps.four.activity.R;
import com.chanapps.four.task.PostReplyTask;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 10/30/12
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanPostResponse {

    private boolean isPosted = false;
    private long threadNo = 0;
    private long postNo = 0;
    private String error = null;

    private static final Pattern SUCCESS_REG = Pattern.compile("(<title.*)(Post successful)");
    private static final Pattern POST_REG = Pattern.compile("thread:([0-9]*),no:([0-9]*)"); // <!-- thread:44593688,no:44595010 -->
    private static final Pattern ERROR_REG = Pattern.compile("(id=\"errmsg\"[^>]*>)([^<]*)");

    public ChanPostResponse(Context ctx, String page) {
        try {
            error = ctx.getString(R.string.post_reply_response_error);

            Matcher successMatch = SUCCESS_REG.matcher(page);
            if (successMatch.find()) {
                isPosted = true;
                error = null;
                try {
                    Matcher threadMatch = POST_REG.matcher(page);
                    if (threadMatch.find()) {
                        threadNo = Long.valueOf(threadMatch.group(1));
                        postNo = Long.valueOf(threadMatch.group(2));
                        if (threadNo == 0) { // API strangely uses postNo instead of threadNo when you post a new thread
                            threadNo = postNo;
                            postNo = 0;
                        }
                        Log.i(PostReplyTask.TAG, "Found threadNo:" + threadNo + " postNo:" + postNo);
                    }
                }
                catch (Exception e) {
                    threadNo = 0;
                    postNo = 0;
                }
            }
            else {
                Matcher errorMatch = ERROR_REG.matcher(page);
                if (errorMatch.find()) {
                    error = errorMatch.group(2).replaceFirst("Error: ", "");
                }
            }
        }
        catch(Exception e) {
            isPosted = false;
        }
    }

    public boolean isPosted() {
        return isPosted;
    }

    public String getError(Context ctx) {
        return error;
    }

    public long getThreadNo() {
        return threadNo;
    }

    public long getPostNo() {
        return postNo;
    }
}
