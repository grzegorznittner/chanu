package com.chanapps.four.data;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/8/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoadCaptchaTask extends AsyncTask<String, Void, String> {

    public static final String TAG = LoadCaptchaTask.class.getSimpleName();

    public static final String CAPTCHA_DEFAULT_URL = "file:///android_res/drawable/captcha.png";

    private Context context = null;
    private WebView recaptchaView = null;
    private String recaptchaChallenge = null;

    public LoadCaptchaTask(Context context, WebView recaptchaView) {
        this.context = context;
        this.recaptchaView = recaptchaView;
    }

    public String getRecaptchaChallenge() {
        return recaptchaChallenge;
    }

    @Override
    protected String doInBackground(String... params) {
        AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        try {
            String url = params[0];
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder s = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                s.append(line);
            }
            return s.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting recaptcha url", e);
        }
        finally {
            if (client != null) {
                client.close();
            }
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        Log.e(TAG, "Captcha load task cancelled");
        Toast.makeText(context, R.string.post_reply_captcha_error, Toast.LENGTH_SHORT).show();
        recaptchaView.loadUrl(CAPTCHA_DEFAULT_URL);
    }

    @Override
    protected void onPostExecute(String response) {
        if (response == null) {
            Log.e(TAG, "Null response loading recaptcha");
            Toast.makeText(context, R.string.post_reply_captcha_error, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Captcha captcha = new Captcha(response);
            String challenge = captcha.getChallenge();
            String url = captcha.getImageUrl();
            if (url != null && !url.isEmpty()
                    && challenge != null && !challenge.isEmpty()) {
                Log.e(TAG, "Found recaptcha url: " + url);
                recaptchaChallenge = challenge;
                recaptchaView.loadUrl(url);
            }
            else {
                Log.e(TAG, "Error reading recaptcha response");
                Toast.makeText(context, R.string.post_reply_captcha_error, Toast.LENGTH_SHORT).show();
                recaptchaView.loadUrl(CAPTCHA_DEFAULT_URL);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading recaptcha response", e);
            Toast.makeText(context, R.string.post_reply_captcha_error, Toast.LENGTH_SHORT).show();
            recaptchaView.loadUrl(CAPTCHA_DEFAULT_URL);
        }
    }
}
