package com.chanapps.four.component;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.chanapps.four.activity.R;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 5/6/13
 * Time: 10:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class TutorialOverlay {

    public enum Page {
        BOARDLIST,
        RECENT,
        WATCHLIST,
        BOARD,
        THREAD;
    }

    protected ViewGroup tutorialOverlay;
    protected TextView tutorialOverlaySubject;
    protected TextView tutorialOverlayDetail;
    protected Button tutorialOverlayButton;

    public TutorialOverlay(View layout, Page page) {
        tutorialOverlay = (ViewGroup)layout.findViewById(R.id.tutorial_overlay);
        tutorialOverlaySubject = (TextView)layout.findViewById(R.id.tutorial_overlay_subject);
        tutorialOverlayDetail = (TextView)layout.findViewById(R.id.tutorial_overlay_detail);
        tutorialOverlayButton = (Button)layout.findViewById(R.id.tutorial_overlay_button);
        tutorialOverlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialOverlay.setVisibility(View.GONE);
            }
        });
        switch (page) {
            default:
            case BOARDLIST:
                tutorialOverlaySubject.setText(R.string.tutorial_overlay_subject_boardlist);
                tutorialOverlayDetail.setText(R.string.tutorial_overlay_detail_boardlist);
                break;
            case RECENT:
                tutorialOverlaySubject.setText(R.string.tutorial_overlay_subject_recent);
                tutorialOverlayDetail.setText(R.string.tutorial_overlay_detail_recent);
                break;
            case WATCHLIST:
                tutorialOverlaySubject.setText(R.string.tutorial_overlay_subject_watchlist);
                tutorialOverlayDetail.setText(R.string.tutorial_overlay_detail_watchlist);
                break;
            case BOARD:
                tutorialOverlaySubject.setText(R.string.tutorial_overlay_subject_board);
                tutorialOverlayDetail.setText(R.string.tutorial_overlay_detail_board);
                break;
            case THREAD:
                tutorialOverlaySubject.setText(R.string.tutorial_overlay_subject_thread);
                tutorialOverlayDetail.setText(R.string.tutorial_overlay_detail_thread);
                break;
        }

    }

}
