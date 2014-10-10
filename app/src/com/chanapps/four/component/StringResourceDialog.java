package com.chanapps.four.component;

/**
 * Created with IntelliJ IDEA.
 * User: mpop
 * Date: 11/22/12
 * Time: 12:27 AM
 */

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.chanapps.four.activity.R;

public class StringResourceDialog extends Dialog {

    private static final String TAG = StringResourceDialog.class.getSimpleName();
    private int layoutId;
    private int headerId;
    private int detailId;

    public StringResourceDialog(Context context, int layoutId, int headerId, int detailId) {
        super(context);
        this.layoutId = layoutId;
        this.headerId = headerId;
        this.detailId = detailId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(layoutId);
        setTitle(headerId);
        View v = findViewById(R.id.detail_html);
        if (v instanceof TextView) {
            TextView detail = (TextView)v;
            detail.setText(Html.fromHtml(v.getContext().getString(detailId)));
        }
        else {
            Log.e(TAG, "Unsupported view class=" + v.getClass() + " for resourceId=" + detailId);
        }
        Button doneButton = (Button)findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

    }

}