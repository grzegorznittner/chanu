package com.chanapps.four.component;

import android.content.Context;
import android.util.Log;
import android.view.View;
import com.chanapps.four.activity.AbstractBoardSpinnerActivity;
import com.chanapps.four.activity.R;
import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdView;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 8/22/13
* Time: 11:47 AM
* To change this template use File | Settings | File Templates.
*/
public class AdComponent {

    protected static final String TAG = AdComponent.class.getSimpleName();
    protected static final boolean DEBUG = true;

    protected Context context;
    protected View advert;

    public AdComponent(Context context, View advert) {
        this.context = context;
        this.advert = advert;
    }

    public void hideOrDisplayAds() {
        boolean hasProkey = BillingComponent.getInstance(context).hasProkey();
        if (hasProkey)
            disableAds();
        else
            enableAds();
    }

    protected void enableAds() {
        if (DEBUG) Log.i(TAG, "enableAds() enabling ads");
        if (advert == null)
            return;
        AdView adView = (AdView)advert.findViewById(R.id.adView);
        if (adView == null)
            return;
        adView.setAdListener(adListener);
        adView.setEnabled(true);
        advert.setVisibility(View.VISIBLE);
        adView.loadAd(new AdRequest()); // on ui thread performance?
    }

    protected void disableAds() {
        if (DEBUG) Log.i(TAG, "disableAds() has prokey, disabling ads");
        if (advert == null)
            return;
        AdView adView = (AdView)advert.findViewById(R.id.adView);
        if (adView == null)
            return;
        adView.setEnabled(false);
        advert.setVisibility(View.GONE);
        return;
    }

    protected AdListener adListener = new AdListener() {
        @Override
        public void onReceiveAd(Ad ad) {
            advert.setVisibility(View.VISIBLE);
        }
        @Override
        public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode errorCode) {
            advert.setVisibility(View.GONE);
        }
        @Override
        public void onPresentScreen(Ad ad) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
        @Override
        public void onDismissScreen(Ad ad) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
        @Override
        public void onLeaveApplication(Ad ad) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    };

}
