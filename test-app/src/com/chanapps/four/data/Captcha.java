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
public class Captcha {
    public static final String apiBaseUrl = "http://api.recaptcha.net/";

    private String challenge;
    private String imageUrl;

    public Captcha(String page) throws Exception {
        try {
            Pattern imgReg = Pattern.compile("(<img .*src\\=\")([^\"]*)");
            Pattern chalReg = Pattern.compile("(id=\"recaptcha_challenge_field\" value=\")([^\"]*)");

            Matcher challengeMatch = chalReg.matcher(page);
            Matcher imageMatch = imgReg.matcher(page);
            boolean bChal = challengeMatch.find();
            boolean bImg = imageMatch.find();

            if (bChal && bImg) {
                challenge = challengeMatch.group(2);
                imageUrl = apiBaseUrl+imageMatch.group(2);
            }
        }
        catch(Exception e) {
            return;
        }
    }

    public String getChallenge() {
        return challenge;
    }

    public String getImageUrl() {
        return imageUrl;
    }

}
