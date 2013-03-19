package com.chanapps.four.task;

import java.io.*;
import java.util.*;

import android.os.Message;
import com.chanapps.four.data.*;
import com.chanapps.four.multipartmime.*;
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
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

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
    private boolean usePass = false;
    private String password = null;
    private Context context = null;
    private PostingReplyDialogFragment dialogFragment = null;
    private String charset = null;

    public PostReplyTask(PostReplyActivity activity) {
        this.activity = activity;
        this.usePass = activity.usePass();
        this.context = activity.getApplicationContext();
        this.charset = ChanBoard.isAsciiOnlyBoard(activity.boardCode) ? PartBase.ASCII_CHARSET : PartBase.UTF8_CHARSET;
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
        partsList.add(new StringPart("MAX-FILE-SIZE", MAX_FILE_SIZE, charset));
        partsList.add(new StringPart("mode", "regist", charset));
        partsList.add(new StringPart("resto", Long.toString(activity.threadNo), charset));
        partsList.add(new StringPart("name", activity.getName(), charset));
        partsList.add(new StringPart("email", activity.getEmail(), charset));
        partsList.add(new StringPart("sub", activity.getSubject(), charset));
        partsList.add(new StringPart("com", activity.getMessage(), charset));
        partsList.add(new StringPart("pwd", password, charset));
        if (!usePass) {
            partsList.add(new StringPart("recaptcha_challenge_field", activity.getRecaptchaChallenge(), charset));
            partsList.add(new StringPart("recaptcha_response_field", activity.getRecaptchaResponse(), charset));
        }
        if (activity.hasSpoiler()) {
            partsList.add(new StringPart("spoiler", "on", charset));
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
        if (DEBUG) Log.i(TAG, "Dumping mime parts list:");
        for (Part p : partsList) {
            if (!(p instanceof StringPart))
                continue;
            StringPart s = (StringPart)p;
            String line = s.getName() + ": " + s.getValue() + ", ";
            if (DEBUG) Log.i(TAG, line);
        }
    }

    protected boolean addImage(List<Part> partsList) {
        String imageUrl = activity.imageUri == null ? null : activity.imageUri.toString();
        if (imageUrl == null && activity.threadNo == 0 && !ChanBoard.requiresThreadImage(activity.boardCode)) {
            partsList.add(new StringPart("textonly", "on", charset));
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
                    FilePart filePart = new FilePart("upfile", file.getName(), file, activity.contentType, charset);
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
            // setup post
            HttpPost request = new HttpPost(url);
            entity.setContentEncoding(charset);
            request.setEntity(entity);
            if (DEBUG)
                dumpRequestContent(request.getEntity().getContent());
            if (DEBUG) Log.i(TAG, "Calling URL: " + request.getURI());

            // make call
            HttpResponse httpResponse;
            if (usePass) {
                if (DEBUG) Log.i(TAG, "Using 4chan pass, attaching cookies to request");
                PersistentCookieStore cookieStore = new PersistentCookieStore(context);
                HttpContext localContext = new BasicHttpContext();
                localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
                httpResponse = client.execute(request, localContext);
                if (DEBUG) Log.i(TAG, "Cookies: " + cookieStore.dump());
            }
            else {
                if (DEBUG) Log.i(TAG, "Not using 4chan pass, executing with captcha");
                httpResponse = client.execute(request);
            }
            if (DEBUG) Log.i(TAG, "Response: " + (httpResponse == null ? "null" : "length: " + httpResponse.toString().length()));

            // check if response
            if (httpResponse == null) {
                Log.e(TAG, context.getString(R.string.post_reply_no_response));
                return null;
            }

            // process response
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

    protected void dumpRequestContent(InputStream is) {
        if (DEBUG) Log.i(TAG, "Request Message Body:");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String l;
            while ((l = r.readLine()) != null)
                if (DEBUG) Log.i(TAG, l);
        }
        catch (IOException e) {
            if (DEBUG) Log.i(TAG, "Exception reading message for logging", e);
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
                ChanFileStorage.resetLastFetched(activityId.boardCode, activityId.threadNo);
                FetchChanDataService.scheduleThreadFetchWithPriority(activity, activity.boardCode, activity.threadNo);
            } else if (activityId.activity == LastActivity.BOARD_ACTIVITY) {
                ChanFileStorage.resetLastFetched(activityId.boardCode);
                FetchChanDataService.scheduleBoardFetchWithPriority(activity, activity.boardCode);
            }
        }

        String threadText = activity.getSubject().trim();
        if ("".equals(threadText))
            threadText = activity.getMessage().trim();
        if ("".equals(threadText))
            threadText = ChanWatchlist.DEFAULT_WATCHTEXT;
        if (threadText.length() > ChanPost.MAX_SINGLELINE_TEXT_LEN)
            threadText = threadText.substring(0, ChanPost.MAX_SINGLELINE_TEXT_LEN);
        if (addThreadToWatchlist && threadNo > 0) {
            ChanWatchlist.watchThread(context,
                    tim,
                    activity.boardCode,
                    threadNo,
                    threadText,
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
        Message.obtain(activity.getHandler(), PostReplyActivity.POST_FINISHED).sendToTarget();
        //activity.navigateUp(); // had trouble with this
    }

}
