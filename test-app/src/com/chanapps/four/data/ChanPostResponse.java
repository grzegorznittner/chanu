package com.chanapps.four.data;

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
    private String error = null;

    public ChanPostResponse(String page) {
        try {
            Pattern successReg = Pattern.compile("(<title.*)(Post successful)");
            Pattern errorReg = Pattern.compile("(id=\"errmsg\"[^>]*>)([^<]*)");

            Matcher successMatch = successReg.matcher(page);
            Matcher errorMatch = errorReg.matcher(page);

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
                error = "Couldn't post";
            }
        }
        catch(Exception e) {
            isPosted = false;
            error = "Couldn't post";
        }
    }

    public boolean isPosted() {
        return isPosted;
    }

    public String getError() {
        return error;
    }

}
