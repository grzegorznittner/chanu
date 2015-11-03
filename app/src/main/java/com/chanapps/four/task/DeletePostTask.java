package com.chanapps.four.task;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.URLFormatComponent;
import com.chanapps.four.data.*;
import com.chanapps.four.fragment.DeletingPostDialogFragment;
import com.chanapps.four.multipartmime.*;
import com.chanapps.four.multipartmime.PartBase;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/8/12
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class DeletePostTask extends AsyncTask<DeletingPostDialogFragment, Void, Integer> {

    public static final String TAG = DeletePostTask.class.getSimpleName();
    public static final boolean DEBUG = false;

    private ChanIdentifiedActivity activity = null;
    private String boardCode = null;
    private long threadNo = 0;
    private long[] postNos = {};
    private String password = null;
    private boolean imageOnly = false;
    private Context context = null;
    private DeletingPostDialogFragment dialogFragment = null;

    public DeletePostTask(ChanIdentifiedActivity activity,
                          String boardCode, long threadNo, long[] postNos, String password, boolean imageOnly) {
        this.activity = activity;
        this.context = activity.getBaseContext();
        this.boardCode = boardCode;
        this.threadNo = threadNo;
        this.postNos = postNos;
        this.password = password;
        this.imageOnly = imageOnly;
    }

    @Override
    protected Integer doInBackground(DeletingPostDialogFragment... params) { // dialog is for callback
        dialogFragment = params[0];
        try {
            MultipartEntity entity = buildMultipartEntity();
            if (entity == null) {
                Log.e(TAG, "Null entity returned building post delete");
                return R.string.delete_post_error;
            }

            String response = executeDeletePost(entity);
            if (response == null || response.isEmpty()) {
                Log.e(TAG, "Null response posting delete");
                return R.string.delete_post_error;
            }

            DeletePostResponse deletePostResponse = new DeletePostResponse(context, response);
            deletePostResponse.processResponse();

            if (!postSuccessful(deletePostResponse))
                return R.string.delete_post_error;

            return updateLastFetched();
        }
        catch (Exception e) {
            Log.e(TAG, "Error posting", e);
            return R.string.delete_post_error;
        }
    }

    protected MultipartEntity buildMultipartEntity() {
        List<Part> partsList = new ArrayList<Part>();
        partsList.add(new StringPart("mode", "usrdel", PartBase.ASCII_CHARSET));
        partsList.add(new StringPart("res", Long.toString(threadNo), PartBase.ASCII_CHARSET));
        partsList.add(new StringPart("pwd", password, PartBase.ASCII_CHARSET));
        for (long postNo : postNos)
            partsList.add(new StringPart(Long.toString(postNo), "delete", PartBase.ASCII_CHARSET));
        if (imageOnly)
            partsList.add(new StringPart("onlyimgdel", "on", PartBase.ASCII_CHARSET));
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

    protected String executeDeletePost(MultipartEntity entity) {
        // success: 	<meta http-equiv="refresh" content="0;URL=https ://boards.4chan.org/a/res/79766271#p79766271">
        String url = String.format(URLFormatComponent.getUrl(context, URLFormatComponent.CHAN_POST_URL_DELETE_FORMAT), boardCode);
        AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        try {
            HttpPost request = new HttpPost(url);
            entity.setContentEncoding(PartBase.ASCII_CHARSET);
            request.setEntity(entity);
            if (DEBUG)
                dumpRequestContent(request.getEntity().getContent());
            if (DEBUG) Log.i(TAG, "Calling URL: " + request.getURI());
            HttpResponse httpResponse = client.execute(request);
            if (DEBUG) Log.i(TAG, "Response: " + (httpResponse == null ? "null" : "length: " + httpResponse.toString().length()));
            if (httpResponse == null) {
                Log.e(TAG, context.getString(R.string.delete_post_no_response));
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

    protected boolean postSuccessful(DeletePostResponse deletePostResponse) {
        errorMessage = deletePostResponse.getError(context);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return false;
        }

        if (DEBUG) Log.i(TAG, "isPosted:" + deletePostResponse.isPosted());
        if (!deletePostResponse.isPosted()) {
            Log.e(TAG, "Unable to post response=" + deletePostResponse.getResponse());
            return false;
        }

        return true;
    }

    protected int updateLastFetched() {
        // forcing thread/board refresh
        ChanFileStorage.deletePosts(context, boardCode, threadNo, postNos, imageOnly);
        /*
        ChanActivityId refreshableActivityId = NetworkProfileManager.instance().getActivityId();
        if (refreshableActivityId != null) {
            if (refreshableActivityId.activity == LastActivity.THREAD_ACTIVITY) {
                ChanFileStorage.resetLastFetched(boardCode, threadNo);
                FetchChanDataService.scheduleThreadFetchWithPriority(context, boardCode, threadNo);
            }
        }
        */
        return 0;
    }

    @Override
    protected void onCancelled() {
        Log.e(TAG, "Post cancelled");
        Toast.makeText(context, R.string.delete_post_cancelled, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != 0) {
            String error = context.getString(result) + (errorMessage == null ? "" : ": " + errorMessage);
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            dialogFragment.dismiss();
            return;
        }
        int msgId = imageOnly ? R.string.delete_post_successful_image : R.string.delete_post_successful;
        Toast.makeText(context, msgId, Toast.LENGTH_SHORT).show();
        activity.refresh();
        dialogFragment.dismiss();
    }

}
