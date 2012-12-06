package com.chanapps.four.data;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.chanapps.four.component.FilePart;
import com.chanapps.four.component.MultipartEntity;
import com.chanapps.four.component.Part;
import com.chanapps.four.component.StringPart;
import com.chanapps.four.activity.PostReplyActivity;
import com.chanapps.four.activity.R;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/8/12
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostReplyTask extends AsyncTask<String, Void, String> {

    public static final String TAG = PostReplyTask.class.getSimpleName();

    public static final String POST_URL_ROOT = "https://sys.4chan.org/";
    public static final String MAX_FILE_SIZE = "3145728";

    private PostReplyActivity activity = null;
    private Context context = null;

    public PostReplyTask(PostReplyActivity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    @Override
    protected String doInBackground(String... params) {
        AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        try {
            String url = POST_URL_ROOT + activity.boardCode + "/post";
            HttpPost request = new HttpPost(url);

            List<Part> partsList = new ArrayList<Part>();
            partsList.add(new StringPart("MAX-FILE-SIZE", MAX_FILE_SIZE));
            partsList.add(new StringPart("mode", "regist"));
            partsList.add(new StringPart("resto", Long.toString(activity.threadNo)));
            partsList.add(new StringPart("name", ""));
            partsList.add(new StringPart("email", ""));
            partsList.add(new StringPart("sub", ""));
            partsList.add(new StringPart("com", activity.getMessage()));
            partsList.add(new StringPart("recaptcha_challenge_field", activity.getRecaptchaChallenge()));
            partsList.add(new StringPart("recaptcha_response_field", activity.getRecaptchaResponse()));
            String imageUrl = activity.getImageUrl();
            if (imageUrl != null) {
                File file = new File(activity.imagePath);
                FilePart filePart = new FilePart("upfile", file.getName(), file, activity.contentType, "UTF-8");
                partsList.add(filePart);
            }
            partsList.add(new StringPart("pwd", activity.generatePwd()));

            Part[] parts = partsList.toArray(new Part[partsList.size()]);

            String foo = "";
            for (Part p : partsList) {
                if (!(p instanceof StringPart))
                    continue;
                StringPart s = (StringPart)p;
                foo += s.getName() + ": " + s.getValue() + ", ";
            }

            MultipartEntity entity = new MultipartEntity(parts);
            request.setEntity(entity);

            Log.e(TAG, "Calling URL: " + request.getURI());
            HttpResponse response = client.execute(request);
            Log.e(TAG, "Response: " + response);
            if (response == null) {
                Log.e(TAG, context.getString(R.string.post_reply_no_response));
                Toast.makeText(context, R.string.post_reply_no_response, Toast.LENGTH_SHORT).show();
                return null;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder s = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                s.append(line);
            }
            return s.toString();
        }
        catch (Exception e) {
            Log.e(TAG, "Error posting", e);
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
        Log.e(TAG, "Post cancelled");
        Toast.makeText(context, R.string.post_reply_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPostExecute(String response) {
        Log.e(TAG, "Response: " + response);
        if (response == null || response.isEmpty()) {
            Log.e(TAG, "Null response posting");
            Toast.makeText(context, R.string.post_reply_error, Toast.LENGTH_SHORT).show();
            activity.reloadCaptcha();
            return;
        }
        ChanPostResponse chanPostResponse = new ChanPostResponse(context, response);
        if (chanPostResponse.isPosted()) {
            if (activity.threadNo == 0) {
                Toast.makeText(context, R.string.post_reply_posted_thread, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context, R.string.post_reply_posted_reply, Toast.LENGTH_SHORT).show();
            }
            activity.navigateUp();
        }
        else {
            Toast.makeText(
                    context,
                    context.getString(R.string.post_reply_error) + ": " + chanPostResponse.getError(context),
                    Toast.LENGTH_SHORT)
                    .show();
            activity.reloadCaptcha();
        }
    }

}
