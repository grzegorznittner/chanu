package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 3/31/13
 * Time: 7:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class ListOfLinksDialogFragment extends DialogFragment {

    public final static String TAG = ListOfLinksDialogFragment.class.getSimpleName();

    protected String[] urls;

    public ListOfLinksDialogFragment(final String[] urls) {
        this.urls = urls;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return (new AlertDialog.Builder(getActivity()))
                .setTitle(R.string.thread_go_to_link)
                .setItems(urls, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = urls[which];
                        ChanHelper.launchUrlInBrowser(getActivity(), url);
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
    }

}
