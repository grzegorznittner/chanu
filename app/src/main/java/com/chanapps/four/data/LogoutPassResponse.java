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
public class LogoutPassResponse {

    private Context ctx = null;
    private String response = null;
    private boolean isLoggedOut = false;
    private String error = null;

    /*
    RESPONSE GOOD:

<title>4chan Pass - Logged Out</title>

RESPONSE ERROR:

<div style="text-align: center;"><span style="font-size: 14pt; color: red; font-weight: bold;">Incorrect Token or PIN.<br><br><div style="text-align: center;">[<a href="https ://sys.4chan.org/auth">Return</a>]</div></span></div>

     */

    private static final Pattern BAN_REG = Pattern.compile("<h2>([^<]*)<span class=\"banType\">([^<]*)</span>([^<]*)</h2>");
    private static final Pattern ERROR_REG = Pattern.compile("(id=\"errmsg\"[^>]*>)([^<]*)");
    private static final Pattern GENERIC_ERROR_REG = Pattern.compile("<div[^>]*><span[^>]*>(<strong[^>]*>)?([^<]*)");
    private static final Pattern SUCCESS_REG = Pattern.compile("(<title[^>]*>)?([^<]*-\\s+Logged Out[^<]*)");

    public LogoutPassResponse(Context ctx, String response) {
        this.ctx = ctx;
        this.response = response;
    }

    public void processResponse() {
        isLoggedOut = false;
        try {
            Matcher successMatch = SUCCESS_REG.matcher(response);
            Matcher banMatch = BAN_REG.matcher(response);
            Matcher errorMatch = ERROR_REG.matcher(response);
            Matcher genericErrorMatch = GENERIC_ERROR_REG.matcher(response);
            if ("".equals(response))
                error = ctx.getString(R.string.delete_post_response_error);
            else if (successMatch.find())
                isLoggedOut = true;
            else if (banMatch.find())
                error = banMatch.group(1) + " " + banMatch.group(2) + " " + banMatch.group(3);
            else if (errorMatch.find())
                error = errorMatch.group(2).replaceFirst("Error: ", "");
            else if (genericErrorMatch.find())
                error = genericErrorMatch.group(2).replaceFirst("Error: ", "");
        }
        catch (Exception e) {
            error = e.getLocalizedMessage();
            isLoggedOut = false;
        }
    }

    public String getResponse() {
        return response;
    }

    public boolean isLoggedOut() {
        return isLoggedOut;
    }

    public String getError(Context ctx) {
        return error;
    }

}
