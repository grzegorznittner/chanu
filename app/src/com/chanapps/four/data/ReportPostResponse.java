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
public class ReportPostResponse {

    private Context ctx = null;
    private String response = null;
    private boolean isPosted = false;
    private String error = null;

    /*
    RESPONSE GOOD:
<body><h3><font color='#FF0000'>Report submitted! This window will close in 3 seconds...</font></h3></body>

RESPONSE BAD CAPTCHA:

<body><h3><font color='#FF0000'>Error: You seem to have mistyped the CAPTCHA. Please try again.<br><br>4chan Pass users can bypass this CAPTCHA. [<a href="https ://www.4chan.org/pass" target="_blank">Learn More</a>]</font></h3>

RESPONSE ERROR:

<span id="errmsg" style="color: red;">Error: Our system thinks your post is spam.</span>

     */

    private static final Pattern BAN_REG = Pattern.compile("<h2>([^<]*)<span class=\"banType\">([^<]*)</span>([^<]*)</h2>");
    private static final Pattern ERROR_REG = Pattern.compile("(id=\"errmsg\"[^>]*>)([^<]*)");
    private static final Pattern GENERIC_ERROR_REG = Pattern.compile("<h3>(<font[^>]*>)?([^<]*)");
    private static final Pattern SUCCESS_REG = Pattern.compile("<h3>(<font[^>]*>)?([^<]*Report submitted[^<]*)");

    public ReportPostResponse(Context ctx, String response) {
        this.ctx = ctx;
        this.response = response;
    }

    public void processResponse() {
        isPosted = false;
        try {
            Matcher successMatch = SUCCESS_REG.matcher(response);
            Matcher banMatch = BAN_REG.matcher(response);
            Matcher errorMatch = ERROR_REG.matcher(response);
            Matcher genericErrorMatch = GENERIC_ERROR_REG.matcher(response);
            if ("".equals(response))
                error = ctx.getString(R.string.delete_post_response_error);
            else if (successMatch.find())
                isPosted = true;
            else if (banMatch.find())
                error = banMatch.group(1) + " " + banMatch.group(2) + " " + banMatch.group(3);
            else if (errorMatch.find())
                error = errorMatch.group(2).replaceFirst("Error: ", "");
            else if (genericErrorMatch.find())
                error = genericErrorMatch.group(2).replaceFirst("Error: ", "");
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
