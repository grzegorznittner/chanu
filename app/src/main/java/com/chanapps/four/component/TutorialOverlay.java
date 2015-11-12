package com.chanapps.four.component;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
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
    protected static final boolean TEST_MODE = false;

    protected static final String SUBJECT_FONT = "fonts/Edmondsans-Regular.otf";
    private static Typeface subjectTypeface = null;

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

    public TutorialOverlay(View layout, Page page) {
        this.layout =  layout;
        this.page = page;
        if (layout == null)
            return;
        tutorialOverlay = (ViewGroup)layout.findViewById(R.id.tutorial_overlay);
        if (tutorialOverlay == null)
            return;
        if (!TEST_MODE && !displayNextTipForPage(page)) {
            tutorialOverlay.setVisibility(View.GONE);
            return;
        }

        setSubjectTypeface();
        addButtonHandlers();
        tutorialOverlay.setVisibility(View.VISIBLE);
    }

    protected boolean displayNextTipForPage(Page page) {
        NetworkProfileManager manager = NetworkProfileManager.instance();
        if (manager == null) {
            if (DEBUG) Log.i(TAG, "no network manager found");
            return false;
        }
        UserStatistics stats = manager.getUserStatistics();
        if (stats == null) {
            if (DEBUG) Log.i(TAG, "no user statistics found");
            return false;
        }
        feature = stats.nextTipForPage(page);
        if (feature == null) {
            if (DEBUG) Log.i(TAG, "no tutorial feature found");
            return false;
        }
        if (feature == UserStatistics.ChanFeature.NONE) {
            if (DEBUG) Log.i(TAG, "NONE tutorial feature found");
            return false;
        }
        if (DEBUG) Log.i(TAG, "found feature=" + feature);
        NetworkProfileManager.instance().getUserStatistics().tipDisplayed(feature);
        return true;
    }

    protected void setSubjectTypeface() {
        subjectTypeface = Typeface.createFromAsset(layout.getResources().getAssets(), SUBJECT_FONT);
        TextView subject = (TextView)tutorialOverlay.findViewById(R.id.tutorial_overlay_subject);
        if (subject != null && subjectTypeface != null)
            subject.setTypeface(subjectTypeface);
    }

    protected void addButtonHandlers() {
        if (tutorialOverlay == null)
            return;
        View tutorialOverlayDismiss = tutorialOverlay.findViewById(R.id.tutorial_overlay_dismiss);
        if (tutorialOverlayDismiss == null)
            return;
        tutorialOverlayDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialOverlay.setVisibility(View.GONE);
                NetworkProfileManager.instance().getUserStatistics().tipDisplayed(feature);
                NetworkProfileManager.instance().getUserStatistics().disableTips();
            }
        });
    }

}
