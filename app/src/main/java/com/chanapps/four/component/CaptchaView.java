package com.chanapps.four.component;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class CaptchaView extends WebView {

    private static final String BASE_URL = "http://chanu.4chan.org";
    private String captchaResponse;
    
    public class CaptchaCallback {

        @JavascriptInterface
        public void captchaEntered(String response) {
            setCaptchaResponse(response);
        }
    }

    public CaptchaView(Context context) {
        super(context);
    }

    public CaptchaView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CaptchaView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void initCaptcha() {
        try {
            String body = IOUtils.toString(new BufferedInputStream(getResources().getAssets().open("captcha.html")));
            
            getSettings().setJavaScriptEnabled(true);
            setWebChromeClient(new WebChromeClient());
            addJavascriptInterface(new CaptchaCallback(), "CaptchaCallback");
            loadDataWithBaseURL(BASE_URL, body, "text/html", "UTF-8", null);
            setBackgroundColor(Color.TRANSPARENT);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCaptchaResponse() {
        return captchaResponse;
    }

    public void setCaptchaResponse(String captchaResponse) {
        this.captchaResponse = captchaResponse;
    }
}
