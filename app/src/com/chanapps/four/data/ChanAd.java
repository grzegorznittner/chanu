package com.chanapps.four.data;

import com.chanapps.four.activity.R;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 4/26/13
 * Time: 10:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChanAd {

    // AD STUFF
    private static final String JLIST_AD_AFFILIATE_CODE = "4539";
    private static final String JLIST_AD_ROOT_URL = "http://anime.jlist.com";
    private static final String JLIST_AD_IMAGE_ROOT_URL = JLIST_AD_ROOT_URL + "/media/" + JLIST_AD_AFFILIATE_CODE;
    private static final String JLIST_AD_CLICK_ROOT_URL = JLIST_AD_ROOT_URL + "/click/" + JLIST_AD_AFFILIATE_CODE;

    private enum AdType {

        ADULT_SINGLE_PRODUCT(97, 180, 180, 60, 728, 90),
        PG_SINGLE_PRODUCT(121, 150, 150, 109, 728, 90);

        private final int code;
        private final int width;
        private final int height;
        private final int bannerCode;
        private final int bannerWidth;
        private final int bannerHeight;

        AdType(int code, int width, int height, int bannerCode, int bannerWidth, int bannerHeight) {
            this.code = code;
            this.width = width;
            this.height = height;
            this.bannerCode = bannerCode;
            this.bannerWidth = bannerWidth;
            this.bannerHeight = bannerHeight;
        }

        public int code() { return code; }
        public int width() { return width; }
        public int height() { return height; }
        public int bannerCode() { return bannerCode; }
        public int bannerWidth() { return bannerWidth; }
        public int bannerHeight() { return bannerHeight; }
    };

    private static final Random generator = new Random();
    private static final int NUM_DEFAULT_IMAGES = 10;

    private AdType adType;
    private int pos;

    public static final String defaultImageUrl() {
        return "drawable://" + defaultImageId();
    }

    public static final int defaultImageId() {
        switch (generator.nextInt(NUM_DEFAULT_IMAGES)) {
            case 0: return R.drawable.jlist_default_ad;
            case 1: return R.drawable.jlist_default_ad_2;
            case 2: return R.drawable.jlist_default_ad_3;
            case 3: return R.drawable.jlist_default_ad_4;
            case 4: return R.drawable.jlist_default_ad_5;
            case 5: return R.drawable.jlist_default_ad_6;
            case 6: return R.drawable.jlist_default_ad_7;
            case 7: return R.drawable.jlist_default_ad_8;
            case 8: return R.drawable.jlist_default_ad_9;
            case 9:
            default:
                    return R.drawable.jlist_default_ad_10;
        }
    }

    protected static final int NUM_BANNERS = 10;

    public static final ChanAd randomAd(boolean workSafe) {
        int pos = (int)Math.floor(Math.random() * NUM_BANNERS);
        return new ChanAd(workSafe, pos);
    }

    public ChanAd(boolean workSafe, int pos) {
        adType = workSafe ? AdType.PG_SINGLE_PRODUCT : AdType.ADULT_SINGLE_PRODUCT;
        this.pos = pos;
    }

    public String imageUrl() {
        return JLIST_AD_IMAGE_ROOT_URL + "/" + adType.code() + "?" + pos;
    }

    public String clickUrl() {
        return JLIST_AD_CLICK_ROOT_URL + "/" + adType.code();
    }

    public int tn_w() {
        return adType.width();
    }

    public int tn_h() {
        return adType.height();
    }

    public String bannerImageUrl() {
        return JLIST_AD_IMAGE_ROOT_URL + "/" + adType.bannerCode() + "?" + pos;
    }

    public String bannerClickUrl() {
        return JLIST_AD_CLICK_ROOT_URL + "/" + adType.bannerCode();
    }

    public int tn_w_banner() {
        return adType.bannerWidth();
    }

    public int tn_h_banner() {
        return adType.bannerHeight();
    }

}
