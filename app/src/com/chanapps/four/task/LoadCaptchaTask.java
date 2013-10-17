package com.chanapps.four.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.data.Captcha;
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
    private ImageView recaptchaButton = null;
    private ImageView recaptchaLoading = null;
    private String recaptchaChallenge = null;
    private Bitmap recaptchaBitmap = null;
    private boolean changeBgColor;

    public LoadCaptchaTask(Context context, ImageView recaptchaButton, ImageView recaptchaLoading, boolean changeBgColor) {
        this.context = context;
        this.recaptchaButton = recaptchaButton;
        this.recaptchaLoading = recaptchaLoading;
        this.changeBgColor = changeBgColor;
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
                boolean invert = ThemeSelector.instance(context).isDark();
                if (changeBgColor)
                    recaptchaBitmap = colorMap(recaptchaBitmap, invert);
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

    protected static final int CUTOFF = 200;

    protected Bitmap colorMap(Bitmap src, boolean invert) {
        Bitmap bmOut = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        // color info
        int A, R, G, B;
        int pixelColor;
        // image size
        int height = src.getHeight();
        int width = src.getWidth();

        // scan through every pixel
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                // get one pixel
                pixelColor = src.getPixel(x, y);
                // saving alpha channel
                A = Color.alpha(pixelColor);
                int r = Color.red(pixelColor);
                int g = Color.green(pixelColor);
                int b = Color.blue(pixelColor);
                if (r >= CUTOFF && g >= CUTOFF && b >= CUTOFF) { // bg color
                    int bgId = invert
                            ? com.chanapps.four.activity.R.color.DarkPaletteCardListBg
                            : com.chanapps.four.activity.R.color.PaletteCardListBg;
                    int bgColor = context.getResources().getColor(bgId);
                    R = Color.red(bgColor);
                    G = Color.green(bgColor);
                    B = Color.blue(bgColor);
                }
                else if (invert) {
                    // inverting byte for each R/G/B channel
                    R = 255 - r;
                    G = 255 - g;
                    B = 255 - b;
                }
                else {
                    R = r;
                    G = g;
                    B = b;
                }
                // set newly-inverted pixel to output image
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        // return final bitmap
        return bmOut;
    }
}
