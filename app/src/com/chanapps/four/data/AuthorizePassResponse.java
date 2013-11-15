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
public class AuthorizePassResponse {

    private Context ctx = null;
    private String response = null;
    private boolean isAuthorized = false;
    private String error = null;

    /*
    RESPONSE GOOD:

<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
	<meta http-equiv="pragma" content="no-cache">

	<title>4chan Pass - Authorization Successful</title>

	<link rel="stylesheet" style="text/css" href="//static.4chan.org/css/yotsubanew.361.css">
</head>
<body>
<div class="boardBanner">
	<div class="boardTitle">4chan Pass</div>
</div>
<hr style="width: 90%">
<br>
<div style="text-align: center;"><span style="font-size: 14pt; color: red; font-weight: bold;">Success! Your device is now authorized.<br><br>You can begin using your Pass immediately&mdash;just visit any board and start posting!<br><br>[<a href="http ://www.4chan.org">Back to 4chan</a>]<br><br><div style="text-align: center;">[<a href="https ://sys.4chan.org/auth">Return</a>]</div></span></div>
</body>
</html>

RESPONSE ERROR:

<div style="text-align: center;"><span style="font-size: 14pt; color: red; font-weight: bold;">Incorrect Token or PIN.<br><br><div style="text-align: center;">[<a href="https ://sys.4chan.org/auth">Return</a>]</div></span></div>

     */

    private static final Pattern BAN_REG = Pattern.compile("<h2>([^<]*)<span class=\"banType\">([^<]*)</span>([^<]*)</h2>");
    private static final Pattern ERROR_REG = Pattern.compile("(id=\"errmsg\"[^>]*>)([^<]*)");
    private static final Pattern GENERIC_ERROR_REG = Pattern.compile("<div[^>]*><span[^>]*>(<strong[^>]*>)?([^<]*)");
    private static final Pattern SUCCESS_REG = Pattern.compile("(<title[^>]*>)?([^<]*Authorization\\s+Successful[^<]*|[^<]*-\\s+Authenticated[^<]*)");

    public AuthorizePassResponse(Context ctx, String response) {
        this.ctx = ctx;
        this.response = response;
    }

    public void processResponse() {
        isAuthorized = false;
        try {
            Matcher successMatch = SUCCESS_REG.matcher(response);
            Matcher banMatch = BAN_REG.matcher(response);
            Matcher errorMatch = ERROR_REG.matcher(response);
            Matcher genericErrorMatch = GENERIC_ERROR_REG.matcher(response);
            if ("".equals(response))
                error = ctx.getString(R.string.delete_post_response_error);
            else if (successMatch.find())
                isAuthorized = true;
            else if (banMatch.find())
                error = banMatch.group(1) + " " + banMatch.group(2) + " " + banMatch.group(3);
            else if (errorMatch.find())
                error = errorMatch.group(2).replaceFirst("Error: ", "");
            else if (genericErrorMatch.find())
                error = genericErrorMatch.group(2).replaceFirst("Error: ", "");
        }
        catch (Exception e) {
            error = e.getLocalizedMessage();
            isAuthorized = false;
        }
    }

    public String getResponse() {
        return response;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public String getError(Context ctx) {
        return error;
    }

}
