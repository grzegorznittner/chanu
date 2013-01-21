package com.chanapps.four.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.ExifInterface;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanWatchlist;
import com.chanapps.four.fragment.PostingReplyDialogFragment;
import com.chanapps.four.multipartmime.FilePart;
import com.chanapps.four.multipartmime.MultipartEntity;
import com.chanapps.four.multipartmime.Part;
import com.chanapps.four.multipartmime.StringPart;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.PostReplyActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanPostResponse;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/8/12
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostReplyTask extends AsyncTask<PostingReplyDialogFragment, Void, String> {

    public static final String TAG = PostReplyTask.class.getSimpleName();

    public static final String POST_URL_ROOT = "https://sys.4chan.org/";
    public static final String MAX_FILE_SIZE = "3145728";
    public static final boolean DEBUG = true;

    private PostReplyActivity activity = null;
    private Context context = null;
    private PostingReplyDialogFragment dialogFragment = null;

    public PostReplyTask(PostReplyActivity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    @Override
    protected String doInBackground(PostingReplyDialogFragment... params) { // dialog is for callback
        dialogFragment = params[0];
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
            String imageUrl = activity.imageUri == null ? null : activity.imageUri.toString();
            if (imageUrl != null) {
                if (DEBUG) Log.i(TAG, "Trying to load image for imageUrl=" + imageUrl + " imagePath="+activity.imagePath+" contentType="+activity.contentType);
                File file = null;
                if (activity.imagePath == null || activity.imagePath.startsWith("http")) { // non-local path, load to tmp
                    InputStream in = null;
                    try {
                        String prefix = UUID.randomUUID().toString();
                        String suffix = MimeTypeMap.getFileExtensionFromUrl(imageUrl);
                        String fileName = prefix + "." + suffix;
                        file = new File(activity.getExternalCacheDir(), fileName);
                        in = activity.getContentResolver().openInputStream(activity.imageUri);
                        FileUtils.copyInputStreamToFile(in, file);

                    }
                    catch (Exception e) {
                        Log.e(TAG, "Couldn't get file from uri=" + activity.imageUri, e);
                    }
                    finally {
                        if (in != null) {
                            in.close();
                        }
                    }
                }
                else { // local file, load it directly
                    file = new File(activity.imagePath);
                }
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                if (DEBUG) Log.i(TAG, "Found exif orientation: " + exif.getAttribute(ExifInterface.TAG_ORIENTATION));

                if (file != null) {
                    FilePart filePart = new FilePart("upfile", file.getName(), file, activity.contentType, "UTF-8");
                    partsList.add(filePart);
                }
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

            if (DEBUG) Log.i(TAG, "Calling URL: " + request.getURI());
            HttpResponse response = client.execute(request);
            if (DEBUG) Log.i(TAG, "Response: " + (response == null ? "null" : "length: " + response.toString().length()));
            if (response == null) {
                Log.e(TAG, context.getString(R.string.post_reply_no_response));
                Toast.makeText(context, R.string.post_reply_no_response, Toast.LENGTH_SHORT).show();
                return null;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder s = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                if (DEBUG) Log.i(TAG, "Response Line:" + line);
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
        Toast.makeText(context, R.string.post_reply_cancelled, Toast.LENGTH_SHORT).show();
        activity.reloadCaptcha(); // always need to do this after each request
    }

    @Override
    protected void onPostExecute(String response) {
        dialogFragment.dismiss();
        if (DEBUG) Log.i(TAG, "Response: " + response);
        if (response == null || response.isEmpty()) {
            if (DEBUG) Log.i(TAG, "Null response posting");
            Toast.makeText(context, R.string.post_reply_error, Toast.LENGTH_SHORT).show();
            activity.reloadCaptcha();
            return;
        }
        ChanPostResponse chanPostResponse = new ChanPostResponse(context, response);
        if (DEBUG) Log.i(TAG, "isPosted:" + chanPostResponse.isPosted());
        if (chanPostResponse.isPosted()) {
            if (activity.threadNo == 0) {
                Toast.makeText(context, R.string.post_reply_posted_thread, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context, R.string.post_reply_posted_reply, Toast.LENGTH_SHORT).show();
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean addThreadToWatchlist = prefs.getBoolean(SettingsActivity.PREF_AUTOMATICALLY_MANAGE_WATCHLIST, true);
            long tim = activity.tim != 0 ? activity.tim : 1000 * (new Date()).getTime();// approximate until we get it back from the api
            long postThreadNo = chanPostResponse.getThreadNo(); // direct from 4chan post response parsing
            long threadNo = postThreadNo != 0 ? postThreadNo : activity.threadNo; // fallback
            if (DEBUG) Log.i(TAG, "posted " + activity.boardCode + "/" + threadNo + " tim:" + tim + " addToWatchlist:" + addThreadToWatchlist);
            // forcing thread/board refresh
            ChanActivityId activityId = NetworkProfileManager.instance().getActivityId();
            if (activityId != null) {
            	if (activityId.activity == LastActivity.THREAD_ACTIVITY) {
            		ChanFileStorage.resetLastFetched(activityId.threadNo);
                    FetchChanDataService.scheduleThreadFetchWithPriority(activity, activity.boardCode, activity.threadNo);
            	} else if (activityId.activity == LastActivity.BOARD_ACTIVITY) {
            		ChanFileStorage.resetLastFetched(activityId.boardCode);
                    FetchChanDataService.scheduleBoardFetchWithPriority(activity, activity.boardCode);
            	}
            }
            if (addThreadToWatchlist && threadNo > 0) {
                ChanWatchlist.watchThread(context,
                        tim,
                        activity.boardCode,
                        threadNo,
                        ChanWatchlist.DEFAULT_WATCHTEXT,
                        activity.imageUri == null ? null : activity.imageUri.toString(),
                        250,
                        250);
            }
            activity.imageUri = null; // now we've processed so don't use it again
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
