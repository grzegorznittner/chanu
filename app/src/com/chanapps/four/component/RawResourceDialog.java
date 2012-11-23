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
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.graphics.Color;
import android.view.Window;
import android.widget.TextView;
import com.chanapps.four.activity.R;

public class RawResourceDialog extends Dialog {
    private static Context mContext = null;
    private int rawResourceUp;
    private int rawResourceDown;

    public RawResourceDialog(Context context, int rawResourceUp, int rawResourceDown ) {
        super(context);
        mContext = context;
        this.rawResourceUp = rawResourceUp;
        this.rawResourceDown = rawResourceDown;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.about);
        TextView tv = (TextView) findViewById(R.id.legal_text);
        tv.setText(readRawTextFile(rawResourceUp));
        tv = (TextView) findViewById(R.id.info_text);
        tv.setText(Html.fromHtml(readRawTextFile(rawResourceDown)));
        tv.setLinkTextColor(Color.WHITE);
        Linkify.addLinks(tv, Linkify.ALL);

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