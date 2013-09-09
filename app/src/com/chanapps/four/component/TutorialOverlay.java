package com.chanapps.four.component;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.gallery3d.ui.Log;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.UserStatistics;
import com.chanapps.four.service.NetworkProfileManager;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/6/13
 * Time: 10:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class TutorialOverlay {

    protected static final String TAG = TutorialOverlay.class.getSimpleName();
    protected static final boolean DEBUG = false;

    public enum Page {
        //BOARDLIST,
        //POPULAR,
        //WATCHLIST,
        BOARD
        //,THREAD
        ;
    }

    protected UserStatistics.ChanFeature feature;
    protected Page page;
    protected View layout;
    protected ViewGroup tutorialOverlay;
    protected TextView tutorialOverlaySubject;
    protected TextView tutorialOverlayDetail;
    protected Button tutorialOverlayButton;
    protected TextView tutorialOverlayDismiss;

    public TutorialOverlay(View layout, Page page) {
        this.layout =  layout;
        this.page = page;
        tutorialOverlay = (ViewGroup)layout.findViewById(R.id.tutorial_overlay);
        if (!displayNextTipForPage(page)) {
            tutorialOverlay.setVisibility(View.GONE);
            return;
        }
        addButtonHandlers();
        tutorialOverlay.setVisibility(View.VISIBLE);
    }

    protected boolean displayNextTipForPage(Page page) {
        NetworkProfileManager manager = NetworkProfileManager.instance();
        if (manager == null) {
            Log.e(TAG, "no network manager found");
            return false;
        }
        UserStatistics stats = manager.getUserStatistics();
        if (stats == null) {
            Log.e(TAG, "no user statistics found");
            return false;
        }
        feature = stats.nextTipForPage(page);
        if (feature == null) {
            Log.e(TAG, "no tutorial feature found");
            return false;
        }
        if (feature == UserStatistics.ChanFeature.NONE) {
            Log.e(TAG, "NONE tutorial feature found");
            return false;
        }
        if (DEBUG) Log.i(TAG, "found feature=" + feature);
        String featureSubjectStringName = "tutorial_" + feature.toString().toLowerCase();
        String featureDetailStringName = "tutorial_" + feature.toString().toLowerCase() + "_detail";
        String subject = getStringResourceByName(layout.getContext(), featureSubjectStringName);
        String detail = getStringResourceByName(layout.getContext(), featureDetailStringName);
        if (subject == null || detail == null) {
            Log.e(TAG, "string data missing for feature=" + feature);
            return false;
        }
        setTipText(subject, detail);
        NetworkProfileManager.instance().getUserStatistics().tipDisplayed(feature);
        return true;
    }

    protected void setTipText(String subject, String detail) {
        tutorialOverlaySubject = (TextView)layout.findViewById(R.id.tutorial_overlay_subject);
        tutorialOverlaySubject.setText(subject);
        tutorialOverlayDetail = (TextView)layout.findViewById(R.id.tutorial_overlay_detail);
        tutorialOverlayDetail.setText(detail);
    }

    protected void addButtonHandlers() {
        tutorialOverlayButton = (Button)layout.findViewById(R.id.tutorial_overlay_button);
        tutorialOverlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!displayNextTipForPage(page))
                    tutorialOverlay.setVisibility(View.GONE);
                if (feature == UserStatistics.ChanFeature.FINISHED_DESC)
                    updateButtonsToFinished();
            }
        });
        tutorialOverlayDismiss = (TextView)layout.findViewById(R.id.tutorial_overlay_dismiss);
        tutorialOverlayDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialOverlay.setVisibility(View.GONE);
                NetworkProfileManager.instance().getUserStatistics().disableTips();
            }
        });
    }

    protected void updateButtonsToFinished() {
        tutorialOverlayDismiss.setVisibility(View.GONE);
        tutorialOverlayButton.setText(R.string.tutorial_overlay_finished);
    }

    private String getStringResourceByName(Context context, String aString) {
        String packageName = context.getPackageName();
        int resId = context.getResources().getIdentifier(aString, "string", packageName);
        if (resId == 0)
            return null;
        return context.getString(resId);
    }

}
