package com.chanapps.four.task;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.URLFormatComponent;
import com.chanapps.four.data.ReportPostResponse;
import com.chanapps.four.fragment.ReportingPostDialogFragment;
import com.chanapps.four.multipartmime.MultipartEntity;
import com.chanapps.four.multipartmime.Part;
import com.chanapps.four.multipartmime.PartBase;
import com.chanapps.four.multipartmime.StringPart;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/8/12
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReportPostTask extends AsyncTask<ReportingPostDialogFragment, Void, Integer> {

    public static final String TAG = ReportPostTask.class.getSimpleName();
    public static final boolean DEBUG = false;

    private String boardCode = null;
    private long[] postNos = {};
    private int reportTypeIndex;
    private String recaptchaResponse;
    private Context context = null;
    private ReportingPostDialogFragment dialogFragment = null;

    public ReportPostTask(ChanIdentifiedActivity refreshableActivity,
                          String boardCode, long threadNo, long[] postNos,
                          String reportType, int reportTypeIndex,
                          String recaptchaResponse)
    {
        this.context = refreshableActivity.getBaseContext();
        this.boardCode = boardCode;
        this.postNos = postNos;
        this.reportTypeIndex = reportTypeIndex;
        this.recaptchaResponse = recaptchaResponse;
    }

    @Override
    protected Integer doInBackground(ReportingPostDialogFragment... params) { // dialog is for callback
        dialogFragment = params[0];
        try {
            MultipartEntity entity = buildMultipartEntity();
            if (entity == null) {
                Log.e(TAG, "Null entity returned building report post");
                return R.string.report_post_error;
            }

            String response = executeReportPost(entity);
            if (response == null || response.isEmpty()) {
                Log.e(TAG, "Null response posting report post");
                return R.string.report_post_error;
            }

            ReportPostResponse reportPostResponse = new ReportPostResponse(context, response);
            reportPostResponse.processResponse();

            if (!postSuccessful(reportPostResponse))
                return R.string.report_post_error;

            return 0;
        }
        catch (Exception e) {
            Log.e(TAG, "Error posting", e);
            return R.string.report_post_error;
        }
    }

    protected String reportTypeCode(int reportTypeIndex) {
        switch (reportTypeIndex) {
            case 0: return "vio";
            case 1: return "illegal";
            case 2:
            default: return "spam";
        }
    }

    protected MultipartEntity buildMultipartEntity() {
        List<StringPart> partsList = new ArrayList<StringPart>();
        partsList.add(new StringPart("mode", "report", PartBase.ASCII_CHARSET));
        partsList.add(new StringPart("cat", reportTypeCode(reportTypeIndex), PartBase.ASCII_CHARSET));
        partsList.add(new StringPart("board", boardCode, PartBase.ASCII_CHARSET));
        partsList.add(new StringPart("no", Long.toString(postNos[0]), PartBase.ASCII_CHARSET));
        partsList.add(new StringPart("g-recaptcha-response", recaptchaResponse, PartBase.ASCII_CHARSET));
        Part[] parts = partsList.toArray(new Part[partsList.size()]);
        if (DEBUG)
            dumpPartsList(partsList);
        MultipartEntity entity = new MultipartEntity(parts);
        return entity;
    }

    protected void dumpPartsList(List<StringPart> partsList) {
        if (DEBUG) Log.i(TAG, "Dumping mime parts list:");
        for (Part p : partsList) {
            if (!(p instanceof StringPart))
                continue;
            StringPart s = (StringPart)p;
            String line = s.getName() + ": " + s.getValue() + ", ";
            if (DEBUG) Log.i(TAG, line);
        }
    }

    protected String executeReportPost(MultipartEntity entity) {
        String url = String.format(URLFormatComponent.getUrl(context, URLFormatComponent.CHAN_POST_URL_DELETE_FORMAT), boardCode);;
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
                Log.e(TAG, context.getString(R.string.report_post_no_response));
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

    protected boolean postSuccessful(ReportPostResponse reportPostResponse) {
        errorMessage = reportPostResponse.getError(context);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return false;
        }

        if (DEBUG) Log.i(TAG, "isPosted:" + reportPostResponse.isPosted());
        if (!reportPostResponse.isPosted()) {
            Log.e(TAG, "Unable to post response=" + reportPostResponse.getResponse());
            return false;
        }

        return true;
    }

    @Override
    protected void onCancelled() {
        Log.e(TAG, "Post cancelled");
        Toast.makeText(context, R.string.report_post_cancelled, Toast.LENGTH_SHORT).show();
        dialogFragment.dismiss();
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != 0) {
            String error = context.getString(result) + (errorMessage == null ? "" : ": " + errorMessage);
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(context, R.string.report_post_successful, Toast.LENGTH_SHORT).show();
            // actually no reason to refresh when reporting posts, as nothing happens
            //refreshableActivity.refresh();
        }
        dialogFragment.dismiss();
    }

}
