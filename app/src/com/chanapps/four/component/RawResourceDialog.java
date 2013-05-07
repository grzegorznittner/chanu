package com.chanapps.four.component;

/**
 * Created with IntelliJ IDEA.
 * User: mpop
 * Date: 11/22/12
 * Time: 12:27 AM
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Dialog;
import android.content.Context;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.TextView;
import com.chanapps.four.activity.R;

public class RawResourceDialog extends Dialog {

    private static final String TAG = RawResourceDialog.class.getSimpleName();
    private static Context mContext = null;
    private int layoutId;
    private int headerId;
    private int detailId;

    public RawResourceDialog(Context context, int layoutId, int headerId, int detailId) {
        super(context);
        mContext = context;
        this.layoutId = layoutId;
        this.headerId = headerId;
        this.detailId = detailId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(layoutId);
        setTitle(readRawTextFile(headerId));
        View v = findViewById(R.id.detail_html);
        if (v instanceof TextView) {
            TextView detail = (TextView)v;
            detail.setText(Html.fromHtml(readRawTextFile(detailId)));
        }
        else {
            Log.e(TAG, "Unsupported view class=" + v.getClass() + " for resourceId=" + detailId);
        }

    }

    public static String readRawTextFile(int id) {
        InputStream inputStream = mContext.getResources().openRawResource(id);
        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader buf = new BufferedReader(in);
        String line;
        StringBuilder text = new StringBuilder();
        try {
            while ((line = buf.readLine()) != null) text.append(line);
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }
}