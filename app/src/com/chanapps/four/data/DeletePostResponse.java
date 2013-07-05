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
public class DeletePostResponse {

    private Context ctx = null;
    private String response = null;
    private boolean isPosted = false;
    private String error = null;

    private static final Pattern BAN_REG = Pattern.compile("<h2>([^<]*)<span class=\"banType\">([^<]*)</span>([^<]*)</h2>");
    private static final Pattern ERROR_REG = Pattern.compile("(id=\"errmsg\"[^>]*>)([^<]*)");

    public DeletePostResponse(Context ctx, String response) {
        this.ctx = ctx;
        this.response = response;
    }

    public void processResponse() {
        isPosted = false;
        try {
            Matcher banMatch = BAN_REG.matcher(response);
            Matcher errorMatch = ERROR_REG.matcher(response);
            if ("".equals(response))
                error = ctx.getString(R.string.delete_post_response_error);
            else if (banMatch.find())
                error = banMatch.group(1) + " " + banMatch.group(2) + " " + banMatch.group(3);
            else if (errorMatch.find())
                error = errorMatch.group(2).replaceFirst("Error: ", "");
            else
                isPosted = true;
        }
        catch (Exception e) {
            error = e.getLocalizedMessage();
            isPosted = false;
        }
    }

    public String getResponse() {
        return response;
    }

    public boolean isPosted() {
        return isPosted;
    }

    public String getError(Context ctx) {
        return error;
    }

}
