package com.chanapps.four.component;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
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

    private static final boolean DEBUG = true;

    public enum Page {
        BOARDLIST,
        RECENT,
        WATCHLIST,
        BOARD,
        THREAD;
    }

    protected UserStatistics.ChanFeature feature;
    protected ViewGroup tutorialOverlay;
    protected TextView tutorialOverlaySubject;
    protected TextView tutorialOverlayDetail;
    protected Button tutorialOverlayButton;

    public TutorialOverlay(View layout, Page page) {

        feature = NetworkProfileManager.instance().getUserStatistics().nextTipForPage(page);
        if (feature == UserStatistics.ChanFeature.NONE) {
            return;
        }

        String featureSubjectStringName = "tutorial_" + feature.toString().toLowerCase();
        String featureDetailStringName = "tutorial_" + feature.toString().toLowerCase() + "_detail";
        String subject = getStringResourceByName(layout.getContext(), featureSubjectStringName);
        String detail = getStringResourceByName(layout.getContext(), featureDetailStringName);
        if (subject == null || detail == null) {
            return;
        }

        tutorialOverlay = (ViewGroup)layout.findViewById(R.id.tutorial_overlay);
        tutorialOverlaySubject = (TextView)layout.findViewById(R.id.tutorial_overlay_subject);
        tutorialOverlaySubject.setText(subject);
        tutorialOverlayDetail = (TextView)layout.findViewById(R.id.tutorial_overlay_detail);
        tutorialOverlayDetail.setText(detail);
        tutorialOverlayButton = (Button)layout.findViewById(R.id.tutorial_overlay_button);
        tutorialOverlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialOverlay.setVisibility(View.GONE);
                NetworkProfileManager.instance().getUserStatistics().tipDisplayed(feature);
            }
        });
        tutorialOverlay.setVisibility(View.VISIBLE);
    }

    private String getStringResourceByName(Context context, String aString) {
        String packageName = context.getPackageName();
        int resId = context.getResources().getIdentifier(aString, "string", packageName);
        if (resId == 0)
            return null;
        return context.getString(resId);
    }
}
