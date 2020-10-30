package com.chanapps.four.task;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.chanapps.four.activity.ChanIdentifiedActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.URLFormatComponent;
import com.chanapps.four.data.LogoutPassResponse;
import com.chanapps.four.data.PersistentCookieStore;
import com.chanapps.four.fragment.LogoutPassDialogFragment;
import com.chanapps.four.multipartmime.MultipartEntity;
import com.chanapps.four.multipartmime.Part;
import com.chanapps.four.multipartmime.PartBase;
import com.chanapps.four.multipartmime.StringPart;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

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
public class LogoutPassTask extends AsyncTask<LogoutPassDialogFragment, Void, Integer> {

    public static final String TAG = LogoutPassTask.class.getSimpleName();
    public static final boolean DEBUG = false;
    protected String errorMessage = null;
    private ChanIdentifiedActivity refreshableActivity;
    private Context context;
    private String passToken;
    private String passPIN;
    private LogoutPassDialogFragment dialogFragment;
    private PersistentCookieStore cookieStore;

    public LogoutPassTask(ChanIdentifiedActivity refreshableActivity, String passToken, String passPIN) {
        this.refreshableActivity = refreshableActivity;
        this.context = refreshableActivity.getBaseContext();
        this.passToken = passToken;
        this.passPIN = passPIN;
    }

    @Override
    protected Integer doInBackground(LogoutPassDialogFragment... params) { // dialog is for callback
        dialogFragment = params[0];
        int errorCode = 0;
        try {
            MultipartEntity entity = buildMultipartEntity();
            if (entity == null) {
                Log.e(TAG, "Null entity returned building report post");
                errorCode = R.string.logout_pass_error;
            } else {
                String response = executeReportPost(entity);
                if (response == null || response.isEmpty()) {
                    Log.e(TAG, "Null response posting report post");
                    errorCode = R.string.logout_pass_error;
                } else {
                    LogoutPassResponse logoutPassResponse = new LogoutPassResponse(context, response);
                    logoutPassResponse.processResponse();

                    if (!postSuccessful(logoutPassResponse)) errorCode = R.string.logout_pass_error;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error posting", e);
            return R.string.logout_pass_error;
        } finally {
            if (errorCode != 0) {
                if (DEBUG) Log.i(TAG, "Unable to logout 4chan pass");
                return errorCode;
            } else {
                if (DEBUG) Log.i(TAG, "4chan pass successfully logged out");
                return 0;
            }
        }
    }

    protected MultipartEntity buildMultipartEntity() {
        List<Part> partsList = new ArrayList<Part>();
        partsList.add(new StringPart("act", "logout", PartBase.ASCII_CHARSET));
        //partsList.add(new StringPart("id", passToken, PartBase.ASCII_CHARSET));
        //partsList.add(new StringPart("pin", passPIN, PartBase.ASCII_CHARSET));
        Part[] parts = partsList.toArray(new Part[partsList.size()]);
        if (DEBUG) dumpPartsList(partsList);
        MultipartEntity entity = new MultipartEntity(parts);
        return entity;
    }

    protected void dumpPartsList(List<Part> partsList) {
        if (DEBUG) Log.i(TAG, "Dumping mime parts list:");
        for (Part p : partsList) {
            if (!(p instanceof StringPart)) continue;
            StringPart s = (StringPart) p;
            String line = s.getName() + ": " + s.getValue() + ", ";
            if (DEBUG) Log.i(TAG, line);
        }
    }

    protected String executeReportPost(MultipartEntity entity) {
        String url = URLFormatComponent.getUrl(context, URLFormatComponent.CHAN_AUTH_URL);
        AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        try {
            // setup cookies
            cookieStore = new PersistentCookieStore(context);
            HttpContext localContext = new BasicHttpContext();
            localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // do post
            HttpPost request = new HttpPost(url);
            entity.setContentEncoding(PartBase.ASCII_CHARSET);
            request.setEntity(entity);
            if (DEBUG) dumpRequestContent(request.getEntity().getContent());
            if (DEBUG) Log.i(TAG, "Calling URL: " + request.getURI());
            HttpResponse httpResponse = client.execute(request, localContext);
            if (DEBUG)
                Log.i(TAG, "Response: " + (httpResponse == null ? "null" : "length: " + httpResponse.toString().length()));
            if (DEBUG) Log.i(TAG, "Cookies: " + cookieStore.dump());

            // check response
            if (httpResponse == null) {
                Log.e(TAG, context.getString(R.string.logout_pass_no_response));
                return null;
            }

            // read response
            BufferedReader r = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            StringBuilder s = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                if (DEBUG) Log.i(TAG, "Response Line:" + line);
                s.append(line);
            }
            String response = s.toString();
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Exception while posting to url=" + url, e);
            return null;
        } finally {
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
            while ((l = r.readLine()) != null) if (DEBUG) Log.i(TAG, l);
        } catch (IOException e) {
            if (DEBUG) Log.i(TAG, "Exception reading message for logging", e);
        }
    }

    protected boolean postSuccessful(LogoutPassResponse logoutPassResponse) {
        errorMessage = logoutPassResponse.getError(context);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return false;
        }

        if (DEBUG) Log.i(TAG, "isLogoutd:" + logoutPassResponse.isLoggedOut());
        if (!logoutPassResponse.isLoggedOut()) {
            Log.e(TAG, "Unable to post response=" + logoutPassResponse.getResponse());
            return false;
        }
        return true;
    }

    @Override
    protected void onCancelled() {
        Log.e(TAG, "Post cancelled");
        Toast.makeText(context, R.string.logout_pass_cancelled, Toast.LENGTH_SHORT).show();
        dialogFragment.dismiss();
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result != 0) {
            String error = context.getString(result) + ("".equals(errorMessage) ? "" : ": " + errorMessage);
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.logout_pass_successful, Toast.LENGTH_SHORT).show();
        }
        dialogFragment.dismiss();
        refreshableActivity.refresh();
    }

}
