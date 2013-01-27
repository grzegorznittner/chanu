package com.chanapps.four.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.chanapps.four.component.ToastRunnable;
import com.chanapps.four.data.*;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.ExifInterface;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.PostReplyActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.PostingReplyDialogFragment;
import com.chanapps.four.multipartmime.FilePart;
import com.chanapps.four.multipartmime.MultipartEntity;
import com.chanapps.four.multipartmime.Part;
import com.chanapps.four.multipartmime.StringPart;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/8/12
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostReplyTask extends AsyncTask<PostingReplyDialogFragment, Void, Integer> {

    public static final String TAG = PostReplyTask.class.getSimpleName();

    public static final String POST_URL_ROOT = "https://sys.4chan.org/";
    public static final String MAX_FILE_SIZE = "3145728";
    public static final boolean DEBUG = false;

    private PostReplyActivity activity = null;
    private String password = null;
    private Context context = null;
    private PostingReplyDialogFragment dialogFragment = null;

    public PostReplyTask(PostReplyActivity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    @Override
    protected Integer doInBackground(PostingReplyDialogFragment... params) { // dialog is for callback
        dialogFragment = params[0];
        try {
            password = activity.getPassword(); // save for later use

            MultipartEntity entity = buildMultipartEntity();
            if (entity == null) {
                Log.e(TAG, "Null entity returned building post");
                return R.string.post_reply_error;
            }

            String response = executePostReply(entity);
            if (response == null || response.isEmpty()) {
                Log.e(TAG, "Null response posting");
                return R.string.post_reply_error;
            }

            ChanPostResponse chanPostResponse = new ChanPostResponse(context, response);
            chanPostResponse.processResponse();

            if (!postSuccessful(chanPostResponse))
                return R.string.post_reply_error;

            return updateThreadsAndWatchlist(chanPostResponse);
        }
        catch (Exception e) {
            Log.e(TAG, "Error posting", e);
            return R.string.post_reply_error;
        }
    }

    protected MultipartEntity buildMultipartEntity() {
        List<Part> partsList = new ArrayList<Part>();
        partsList.add(new StringPart("MAX-FILE-SIZE", MAX_FILE_SIZE));
        partsList.add(new StringPart("mode", "regist"));
        partsList.add(new StringPart("resto", Long.toString(activity.threadNo)));
        partsList.add(new StringPart("name", activity.getName()));
        partsList.add(new StringPart("email", activity.getEmail()));
        partsList.add(new StringPart("sub", activity.getSubject()));
        partsList.add(new StringPart("com", activity.getMessage()));
        partsList.add(new StringPart("pwd", password));
        partsList.add(new StringPart("recaptcha_challenge_field", activity.getRecaptchaChallenge()));
        partsList.add(new StringPart("recaptcha_response_field", activity.getRecaptchaResponse()));
        if (activity.hasSpoiler()) {
            partsList.add(new StringPart("spoiler", "on"));
        }
        if (!addImage(partsList))
            return null;

        Part[] parts = partsList.toArray(new Part[partsList.size()]);

        if (DEBUG)
            dumpPartsList(partsList);

        MultipartEntity entity = new MultipartEntity(parts);
        return entity;
    }

    protected void dumpPartsList(List<Part> partsList) {
        Log.i(TAG, "Dumping mime parts list:");
        for (Part p : partsList) {
            if (!(p instanceof StringPart))
                continue;
            StringPart s = (StringPart)p;
            String line = s.getName() + ": " + s.getValue() + ", ";
            Log.i(TAG, line);
        }
    }

    protected boolean addImage(List<Part> partsList) {
        String imageUrl = activity.imageUri == null ? null : activity.imageUri.toString();
        if (imageUrl == null && activity.threadNo == 0 && !ChanBoard.requiresThreadImage(activity.boardCode)) {
            partsList.add(new StringPart("textonly", "on"));
        }
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
                    return false;
                }
                finally {
                    if (in != null) {
                        try {
                            in.close();
                        }
                        catch (Exception e) {
                            Log.e(TAG, "Couldn't close input stream:" + in, e);
                        }
                    }
                }
            }
            else { // local file, load it directly
                file = new File(activity.imagePath);
            }

            try {
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                if (DEBUG) Log.i(TAG, "Found exif orientation: " + exif.getAttribute(ExifInterface.TAG_ORIENTATION));
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't read exif interface for file:" + file.getAbsolutePath(), e);
            }

            try {
                if (file != null) {
                    FilePart filePart = new FilePart("upfile", file.getName(), file, activity.contentType, "UTF-8");
                    partsList.add(filePart);
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't add file to entity, file=" + file.getAbsolutePath(), e);
                return false;
            }
        }
        return true;
    }

    protected String executePostReply(MultipartEntity entity) {
        String url = POST_URL_ROOT + activity.boardCode + "/post";
        AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(entity);
            if (DEBUG) Log.i(TAG, "Calling URL: " + request.getURI());
            HttpResponse httpResponse = client.execute(request);
            if (DEBUG) Log.i(TAG, "Response: " + (httpResponse == null ? "null" : "length: " + httpResponse.toString().length()));
            if (httpResponse == null) {
                Log.e(TAG, context.getString(R.string.post_reply_no_response));
                return null;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            StringBuilder s = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                if (DEBUG) Log.i(TAG, "Response Line:" + line);
                s.append(line);
            }
            String response = s.toString();
            return response;
        }
        catch (Exception e) {
            Log.e(TAG, "Exception while posting to url=" + url, e);
            return null;
        }
        finally {
            if (client != null) {
                client.close();
            }
        }
    }

    protected String errorMessage = null;

    protected boolean postSuccessful(ChanPostResponse chanPostResponse) {
        errorMessage = chanPostResponse.getError(activity);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return false;
        }

        if (DEBUG) Log.i(TAG, "isPosted:" + chanPostResponse.isPosted());
        if (!chanPostResponse.isPosted()) {
            Log.e(TAG, "Unable to post response=" + chanPostResponse.getResponse());
            return false;
        }

        return true;
    }

    protected int updateThreadsAndWatchlist(ChanPostResponse chanPostResponse) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean addThreadToWatchlist = prefs.getBoolean(SettingsActivity.PREF_AUTOMATICALLY_MANAGE_WATCHLIST, true);
        long tim = activity.tim != 0 ? activity.tim : 1000 * (new Date()).getTime();// approximate until we get it back from the api
        long postThreadNo = chanPostResponse.getThreadNo(); // direct from 4chan post response parsing
        long threadNo = postThreadNo != 0 ? postThreadNo : activity.threadNo; // fallback
        long postNo = chanPostResponse.getPostNo();
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

        ChanPostlist.addPost(context, activity.boardCode, threadNo, postNo, password);

        activity.imageUri = null; // now we've processed so don't use it again
        return 0;
    }

    @Override
    protected void onCancelled() {
        Log.e(TAG, "Post cancelled");
        Toast.makeText(context, R.string.post_reply_cancelled, Toast.LENGTH_SHORT).show();
        activity.reloadCaptcha(); // always need to do this after each request
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != 0) {
            String error = context.getString(result) + (errorMessage == null ? "" : ": " + errorMessage);
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            activity.reloadCaptcha();
            dialogFragment.dismiss();
            return;
        }

        if (activity.threadNo == 0) {
            Toast.makeText(context, R.string.post_reply_posted_thread, Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(context, R.string.post_reply_posted_reply, Toast.LENGTH_SHORT).show();
        }
        dialogFragment.dismiss();
        activity.finish();
        //activity.navigateUp(); // had trouble with this
    }

}
