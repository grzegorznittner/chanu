package com.chanapps.four.data;

import android.content.Context;
import com.chanapps.four.component.URLFormatComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA. User: arley Date: 10/30/12 Time: 5:51 PM To
 * change this template use File | Settings | File Templates.
 */
public class Captcha {

    private String challenge;
    private String imageUrl;

    private static final Pattern IMG_REG = Pattern.compile("(<img .*src\\=\")([^\"]*)");
    private static final Pattern CHAL_REG = Pattern.compile("(name=\"c\" value=\")([^\"]*)");

    public Captcha() {

    }

    public Captcha(Context context, String page) {
        Matcher challengeMatch = CHAL_REG.matcher(page);
        Matcher imageMatch = IMG_REG.matcher(page);
        boolean bChal = challengeMatch.find();
        boolean bImg = imageMatch.find();

        if (bChal && bImg) {
            challenge = challengeMatch.group(2);
            imageUrl = String.format(URLFormatComponent.getUrl(context, URLFormatComponent.GOOGLE_RECAPTCHA_API_URL_FORMAT), imageMatch.group(2));
        }
    }

    public String getChallenge() {
        return challenge;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
