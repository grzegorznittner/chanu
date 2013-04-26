package com.chanapps.four.data;

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

        ADULT_SINGLE_PRODUCT(70, 140, 187, true),
        PG_SINGLE_PRODUCT(68, 140, 187, false);

        private final int code;
        private final int width;
        private final int height;
        private final boolean adult;

        AdType(int code, int width, int height, boolean adult) {
            this.code = code;
            this.width = width;
            this.height = height;
            this.adult = adult;
        }

        public int code() { return code; }
        public int width() { return width; }
        public int height() { return height; }
        public boolean adult() { return adult; }
    };

    private static Random generator = new Random();

    private AdType adType;

    public static final ChanAd randomAd(boolean workSafe) {
        return new ChanAd(workSafe);
    }

    public ChanAd(boolean workSafe) {
        adType = workSafe ? AdType.PG_SINGLE_PRODUCT : AdType.ADULT_SINGLE_PRODUCT;
    }

    public String imageUrl() {
        return JLIST_AD_IMAGE_ROOT_URL + "/" + adType.code() + "?" + generator.nextInt();
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

}
