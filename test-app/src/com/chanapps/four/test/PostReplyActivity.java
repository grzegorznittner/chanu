package com.chanapps.four.test;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;
import com.chanapps.four.data.Captcha;
import com.chanapps.four.data.ChanDatabaseHelper;
import com.chanapps.four.data.Recaptcha;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class PostReplyActivity extends Activity {

	public static final String TAG = PostReplyActivity.class.getSimpleName();

    public static final String RECAPTCHA_NOSCRIPT_URL = "http://www.google.com/recaptcha/api/noscript?k=";
    public static final String RECAPTCHA_PUBLIC_KEY = "6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc";
    public static final String RECAPTCHA_URL = RECAPTCHA_NOSCRIPT_URL + RECAPTCHA_PUBLIC_KEY;

    String boardCode = null;
    int threadNo = 0;

    private void randomizeThread() {
        double d = Math.random();
        if (d >= 0.75) {
            setBoardCode("trv");
            threadNo = 609350;
        }
        else if (d >= 0.5) {
            setBoardCode("diy");
            threadNo = 100304;
        }
        else if (d >= 0.25) {
            setBoardCode("fit");
            threadNo = 4820056;
        }
        else {
            setBoardCode("po");
            threadNo = 430177;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.hasExtra("threadNo")) {
            setBoardCode(intent.getStringExtra("boardCode"));
            threadNo = intent.getIntExtra("threadNo", 0);
        } else {
            randomizeThread();
        }

        setContentView(R.layout.post_reply_activity_layout);
        LoadCaptchaTask loadCaptchaTask = new LoadCaptchaTask(getApplicationContext());
        loadCaptchaTask.execute(RECAPTCHA_URL);
    }

    private void loadCaptcha(Captcha captcha) {
        WebView recaptchaView = (WebView) findViewById(R.id.post_reply_recaptcha_webview);
        recaptchaView.loadUrl(captcha.ImageUrl);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUp();
                return true;
            case R.id.download_image_menu:
                Toast.makeText(getApplicationContext(), "Posting reply...", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void navigateUp() {
        Intent upIntent = new Intent(this, ThreadListActivity.class);
        upIntent.putExtra("boardCode", boardCode);
        upIntent.putExtra("threadNo", threadNo);
        NavUtils.navigateUpTo(this, upIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_relpy_menu, menu);
        return true;
    }

    private void setBoardCode(String code) {
        boardCode = code;
        if (getActionBar() != null) {
            getActionBar().setTitle("/" + boardCode + " " + getString(R.string.post_reply_activity));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private class LoadCaptchaTask extends AsyncTask<String, Void, HttpResponse> {

        private Context context = null;

        public LoadCaptchaTask(Context context) {
            this.context = context;
        }

        protected HttpResponse doInBackground(String... params) {
             try {
                 String url = params[0];
                 HttpGet request = new HttpGet(url);
                 AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
                 return client.execute(request);
             } catch (Exception e) {
                Log.e(TAG, "Error getting recaptcha url", e);
             }
             return null;
         }

         protected void onPostExecute(HttpResponse response) {
             if (response == null) {
                 return;
             }
             try {
                 BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                 StringBuilder s = new StringBuilder();
                 String line;
                 while ((line = r.readLine()) != null) {
                     s.append(line);
                 }
                 Captcha c = Recaptcha.GetCaptcha(s.toString());
                 loadCaptcha(c);
             } catch (Exception e) {
                Log.e(TAG, "Error reading recaptcha response", e);
             }
         }
    }

}
