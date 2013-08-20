package com.chanapps.four.component;

import android.app.Activity;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 8/16/13
 * Time: 7:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class BillingComponent {

    protected static final String TAG = BillingComponent.class.getSimpleName();
    protected static final boolean DEBUG = true;

    protected static final String GOOGLE_LICENSE_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjGGm5BxvO9AYTYKEwNqsLnNDwrCWxR6i8y7/6oO3HM83wCu6snmefngL/7DuJc9/6xcAtrb7DKSCCKoqKP+7m9hku6e8uHKiaZK2ixU/GGgzKR7QcxtyckaCuYE9vc8nXetpn82/OPxraJzB769dI7hXwpcZeOVxHgYhby0vgl6BBNtQMohklKx4M8Kn2kJklRbQUZS/5vTLB3V4wEK9AGKq/S3ETo5ChCRgd55gHlci9yWrcvdUWIHikzsTPwrZaSvnRnT208Ona1UhyUOtcVytbzi65yKh2P9weqygIzivbizAXZvHLCtfuySbJZYojDEPdwGei0kQaRk0Sm4aPQIDAQAB";
    protected static final String STORAGE_SECRET_KEY = "MsouCDI0ZDRbvse4SFm3vEhAl2wVvCRPvL1yjf6LnBUb7yYLfnzfDbHGl7HIstoa";

    public static final String NO_ADS_NONCONS_PRODUCT_ID = "prokey.basic";
    public static final String TEST_NONCONS_PRODUCT_ID = "android.test.purchased";
    protected static final String PROKEY_ID = NO_ADS_NONCONS_PRODUCT_ID;

    protected static BillingComponent singleton;

    public static BillingComponent getInstance() {
        init();
        return singleton;
    }

    public BillingComponent() {
    }

    protected static synchronized void init() {
        if (singleton != null)
            return;
        singleton = new BillingComponent();
        if (DEBUG) Log.i(TAG, "init()");
        if (DEBUG) Log.i(TAG, "init() finished");
    }

    protected NonConsumableItem findItem(String productId) {
        init();
        List<NonConsumableItem> items = Arrays.asList(ITEMS);
        if (items == null || items.size() == 0) {
            if (DEBUG) Log.i(TAG, "findItem() no consumable items found in storage");
            return null;
        }
        NonConsumableItem foundItem = null;
        for (NonConsumableItem item : items) {
            if (DEBUG) Log.i(TAG, "findItem() iterating item id=" + item.getItemId());
            if (productId.equals(item.getItemId())) {
                foundItem = item;
                break;
            }
        }
        if (foundItem == null) {
            if (DEBUG) Log.i(TAG, "findItem() did not find stored item id=" + productId);
            return null;
        }
        if (DEBUG) Log.i(TAG, "findItem() found stored item id=" + productId);
        return foundItem;
    }

    public boolean hasItem(String productId) {
        NonConsumableItem item = findItem(productId);
        if (item == null) {
            if (DEBUG) Log.i(TAG, "hasItem() no item found for id=" + productId);
            return false;
        }
        NonConsumableItemsStorage storage = new NonConsumableItemsStorage();
        boolean hasItem = storage.nonConsumableItemExists(item);
        if (DEBUG) Log.i(TAG, "hasItem() storage hasItem=" + hasItem);
        return hasItem;
    }

    public void purchaseItem(String productId) throws NoItemException {
        NonConsumableItem item = findItem(productId);
        if (item == null)
            throw new NoItemException("no prokey item found with id=" + productId);
        // purchase
    }

    public static class NoItemException extends Exception {
        public NoItemException(String message) {
            super(message);
        }
    }

    protected static class NonConsumableItem {
        private String productId;
        public NonConsumableItem(String productId) {
            this.productId = productId;
        }
        public String getItemId() {
            return productId;
        }
    }

    protected static final NonConsumableItem[] ITEMS = {
            new NonConsumableItem(NO_ADS_NONCONS_PRODUCT_ID)
    };

    protected static class NonConsumableItemsStorage {
        public NonConsumableItemsStorage() {

        }
        public boolean nonConsumableItemExists(NonConsumableItem item) {
            return false;
        }
    }
}
