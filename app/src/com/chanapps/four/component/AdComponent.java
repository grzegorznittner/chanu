package com.chanapps.four.component;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
//import com.adsdk.sdk.banner.AdView;
import com.chanapps.four.activity.AbstractBoardSpinnerActivity;
import com.chanapps.four.activity.PurchaseActivity;
import com.chanapps.four.activity.R;
//import com.google.ads.Ad;
//import com.google.ads.AdListener;
//import com.google.ads.AdRequest;
//import com.google.ads.AdView;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 8/22/13
* Time: 11:47 AM
* To change this template use File | Settings | File Templates.
*/
public class AdComponent {

    protected static final String TAG = AdComponent.class.getSimpleName();
    protected static final boolean DEBUG = false;

    protected Context context;
    protected View advert;

    protected static boolean hasDisabled = false; // manual dismiss of disabled until restart

    public AdComponent(Context context, View advert) {
        this.context = context;
        this.advert = advert;
    }

    public void hideOrDisplayAds() {
        boolean hasProkey = BillingComponent.getInstance(context).hasProkey();
        if (hasProkey || hasDisabled)
            disableAds();
        else
            enableAds();
    }

    protected void enableAds() {
        if (DEBUG) Log.i(TAG, "enableAds() enabling ads");
        if (!hasAdContainer())
            return;
        /*
        AdView adView = (AdView)advert.findViewById(R.id.adView);
        if (adView == null)
            return;
        adView.setAdListener(adListener);
        adView.setEnabled(true);
        advert.setVisibility(View.VISIBLE);
        adView.loadAd(new AdRequest()); // on ui thread performance?
        */
        /*
        ViewGroup parentView = (ViewGroup)advert;
        safeRemoveExistingAds(parentView);
        MobclixAdView adView = new MobclixMMABannerXLAdView(context);
        parentView.addView(adView);
        ViewGroup.LayoutParams params = adView.getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = context.getResources().getDimensionPixelSize(R.dimen.ad_layout_height);
        }
        adView.setVisibility(View.VISIBLE);
        */
        ViewGroup parentView = (ViewGroup)advert;
        parentView.setVisibility(View.VISIBLE);
        bindListeners();
    }

    /*
    protected void safeRemoveExistingAds(ViewGroup parentView) {
        if (parentView.getChildCount() > 0) {
            View child = parentView.getChildAt(0);
            if (child != null && child instanceof MobclixMMABannerXLAdView)
                ((MobclixAdView)child).pause();
            parentView.removeAllViews();
        }
        parentView.setVisibility(View.GONE);
    }
    */

    protected void disableAds() {
        if (DEBUG) Log.i(TAG, "disableAds() has prokey, disabling ads");
        hasDisabled = true;
        if (!hasAdContainer())
            return;
        /*
        AdView adView = (AdView)advert.findViewById(R.id.adView);
        if (adView != null)
            adView.setEnabled(false);
        advert.setVisibility(View.GONE);
        */
        ViewGroup parentView = (ViewGroup)advert;
        parentView.setVisibility(View.GONE);
        //safeRemoveExistingAds(parentView);
    }

    protected boolean hasAdContainer() {
        if (advert == null)
            return false;
        if (!(advert instanceof ViewGroup))
            return false;
        return true;
    }

    /*
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
    */

    protected void bindListeners() {
        if (advert == null)
            return;
        View adPurchase = advert.findViewById(R.id.ad_purchase);
        if (adPurchase != null)
            adPurchase.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (context instanceof Activity)
                        PurchaseActivity.startActivity((Activity)context);
                }
            });
        View adDismiss = advert.findViewById(R.id.ad_dismiss);
        if (adDismiss != null)
            adDismiss.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    disableAds();
                }
            });
    }

}
