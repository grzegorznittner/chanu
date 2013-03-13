package com.chanapps.four.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.Captcha;
import com.chanapps.four.data.ChanHelper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/8/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoadCaptchaTask extends AsyncTask<String, Void, Integer> {

    public static final String TAG = LoadCaptchaTask.class.getSimpleName();

    private static final boolean DEBUG = false;

    private Context context = null;
    private ImageButton recaptchaButton = null;
    private ImageView recaptchaLoading = null;
    private String recaptchaChallenge = null;
    private Bitmap recaptchaBitmap = null;

    public LoadCaptchaTask(Context context, ImageButton recaptchaButton, ImageView recaptchaLoading) {
        this.context = context;
        this.recaptchaButton = recaptchaButton;
        this.recaptchaLoading = recaptchaLoading;
    }

    public String getRecaptchaChallenge() {
        return recaptchaChallenge;
    }

    @Override
    protected void onPreExecute() {
        Animation rotation = AnimationUtils.loadAnimation(context, R.animator.clockwise_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        recaptchaLoading.setVisibility(View.VISIBLE);
        recaptchaLoading.startAnimation(rotation);
        recaptchaButton.setVisibility(View.GONE);
        recaptchaButton.setImageBitmap(null);
        ChanHelper.safeClearImageView(recaptchaButton);
    }

    @Override
    protected Integer doInBackground(String... params) {
        String getCaptchaUrl = params[0];
        int result = loadRecaptcha(getCaptchaUrl);
        return result;
    }

    private Integer loadRecaptcha(String recaptchaUrl) {
        String captchaResponse = null;
        AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        try {
            HttpGet request = new HttpGet(recaptchaUrl);
            HttpResponse response = client.execute(request);
            BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder s = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                s.append(line);
            }
            captchaResponse = s.toString();
            if (captchaResponse == null || captchaResponse.isEmpty()) {
                Log.e(TAG, "Null captcha response for url=" + recaptchaUrl);
                return R.string.post_reply_captcha_error;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting recaptcha url", e);
            return R.string.post_reply_captcha_error;
        }
        finally {
            if (client != null) {
                client.close();
            }
        }

        try {
            Captcha captcha = new Captcha(captchaResponse);
            String challenge = captcha.getChallenge();
            String imageUrl = captcha.getImageUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                Log.e(TAG, "Null image url found in response=" + captchaResponse);
                return R.string.post_reply_captcha_error;
            }
            else if (challenge == null || challenge.isEmpty()) {
                Log.e(TAG, "Null challenge found in response=" + captchaResponse);
                return R.string.post_reply_captcha_error;
            }
            else {
                if (DEBUG) Log.i(TAG, "Found recaptcha imageUrl=" + imageUrl + " challenge=" + challenge);
                recaptchaChallenge = challenge;
                InputStream is = new URL(imageUrl).openStream();
                recaptchaBitmap = BitmapFactory.decodeStream(is);
                if (recaptchaBitmap == null) {
                    Log.e(TAG, "Null bitmap loaded from recaptcha imageUrl=" + imageUrl);
                    return R.string.post_reply_captcha_error;
                }
                return 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting recaptcha url", e);
            return R.string.post_reply_captcha_error;
        }
    }

    @Override
    protected void onCancelled() {
        if (DEBUG) Log.i(TAG, "Captcha load task cancelled");
        recaptchaLoading.clearAnimation();
        recaptchaLoading.setVisibility(View.GONE);
        if (recaptchaBitmap != null)
            recaptchaBitmap.recycle();
        recaptchaButton.setImageResource(R.drawable.captcha);
        recaptchaButton.setVisibility(View.VISIBLE);
        Toast.makeText(context, R.string.post_reply_captcha_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPostExecute(Integer result) {
        recaptchaLoading.clearAnimation();
        recaptchaLoading.setVisibility(View.GONE);
        if (result == 0) {
            recaptchaButton.setImageBitmap(recaptchaBitmap);
            recaptchaButton.setVisibility(View.VISIBLE);
        }
        else {
            if (recaptchaBitmap != null)
                recaptchaBitmap.recycle();
            recaptchaButton.setImageResource(R.drawable.captcha);
            recaptchaButton.setVisibility(View.VISIBLE);
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }
}
