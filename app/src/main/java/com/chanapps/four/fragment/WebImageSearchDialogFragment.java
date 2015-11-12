package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.URLFormatComponent;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class WebImageSearchDialogFragment extends DialogFragment {

    public static final String TAG = WebImageSearchDialogFragment.class.getSimpleName();

    private EditText searchTextView;

    public WebImageSearchDialogFragment() {
        super();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.web_search_image_dialog_fragment, null);
        searchTextView = (EditText)view.findViewById(R.id.text);
        searchTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                startWebSearch();
                return true;
            }
        });
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.search_title, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startWebSearch();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WebImageSearchDialogFragment.this.dismiss();
                    }
                })
                .create();
    }

    protected void startWebSearch() {
        String query = searchTextView != null && searchTextView.getText() != null
                ? searchTextView.getText().toString()
                : null;
        if (query != null && !query.isEmpty()) {
            String url = String.format(
                    URLFormatComponent.getUrl(getActivity(), URLFormatComponent.GOOGLE_QUERY_IMAGE_URL_FORMAT), query);
            ActivityDispatcher.launchUrlInBrowser(getActivity(), url);
        }
        else {
            ActivityDispatcher.launchUrlInBrowser(getActivity(),
                    URLFormatComponent.getUrl(getActivity(), URLFormatComponent.GOOGLE_IMAGE_SEARCH_URL));
        }
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        (new Handler()).postDelayed(new Runnable() {
            public void run() {
                searchTextView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                searchTextView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));

            }
        }, 200);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        //getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
    }

}
