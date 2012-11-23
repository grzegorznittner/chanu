package com.chanapps.four.data;

import android.content.Context;
import com.chanapps.four.activity.R;

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

    private Context ctx = null;
    private boolean isPosted = false;
    private String error = null;

    private static final Pattern SUCCESS_REG = Pattern.compile("(<title.*)(Post successful)");
    private static final Pattern ERROR_REG = Pattern.compile("(id=\"errmsg\"[^>]*>)([^<]*)");

    public ChanPostResponse(Context ctx, String page) {
        try {
            error = ctx.getString(R.string.post_reply_response_error);


            Matcher successMatch = SUCCESS_REG.matcher(page);
            Matcher errorMatch = ERROR_REG.matcher(page);

            if (successMatch.find()) {
                isPosted = true;
                error = null;
            }
            else if (errorMatch.find()) {
                isPosted = false;
                error = errorMatch.group(2).replaceFirst("Error: ", "");
            }
            else {
                isPosted = false;
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

}
