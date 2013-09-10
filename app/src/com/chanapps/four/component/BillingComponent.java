package com.chanapps.four.component;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.android.vending.billing.IInAppBillingService;
import com.chanapps.four.activity.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 8/16/13
 * Time: 7:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class BillingComponent {

    protected static final String TAG = BillingComponent.class.getSimpleName();
    protected static final boolean DEBUG = false;

    protected static final String GOOGLE_LICENSE_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjGGm5BxvO9AYTYKEwNqsLnNDwrCWxR6i8y7/6oO3HM83wCu6snmefngL/7DuJc9/6xcAtrb7DKSCCKoqKP+7m9hku6e8uHKiaZK2ixU/GGgzKR7QcxtyckaCuYE9vc8nXetpn82/OPxraJzB769dI7hXwpcZeOVxHgYhby0vgl6BBNtQMohklKx4M8Kn2kJklRbQUZS/5vTLB3V4wEK9AGKq/S3ETo5ChCRgd55gHlci9yWrcvdUWIHikzsTPwrZaSvnRnT208Ona1UhyUOtcVytbzi65yKh2P9weqygIzivbizAXZvHLCtfuySbJZYojDEPdwGei0kQaRk0Sm4aPQIDAQAB";
    protected static final String STORAGE_SECRET_KEY = "MsouCDI0ZDRbvse4SFm3vEhAl2wVvCRPvL1yjf6LnBUb7yYLfnzfDbHGl7HIstoa";

    protected static final String NO_ADS_NONCONS_PRODUCT_ID = "prokey.basic";
    //protected static final String NO_ADS_NONCONS_PRODUCT_ID = "android.test.purchased"; // test mode
    protected static final String PREF_PURCHASES = "purchasedProductIds";
    protected static final String LAST_PURCHASE_TOKEN = "lastPurchaseToken";

    protected static BillingComponent singleton;
    protected Context context;
    protected IInAppBillingService mService;
    protected ServiceConnection mServiceConn;
    protected Set<String> purchases;

    public static synchronized BillingComponent getInstance(Context context) {
        if (singleton != null)
            return singleton;
        singleton = new BillingComponent(context);
        singleton.loadFromPrefs();
        return singleton;
    }

    protected BillingComponent(Context context) {
        this.context = context;
    }

    protected void loadFromPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        synchronized (this) {
            purchases = prefs.getStringSet(PREF_PURCHASES, new HashSet<String>());
        }
    }

    protected NonConsumableItem findItem(String productId) {
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

    public boolean hasProkey() {
        return isItemPurchased(NO_ADS_NONCONS_PRODUCT_ID);
    }

    protected boolean isItemPurchased(String productId) {
        boolean purchased = purchases.contains(productId);
        if (DEBUG) Log.i(TAG, "isItemPurchased productId=" + productId + " is " + purchased);
        return purchased;
    }

    public void purchaseProkey(Activity activity) { //}, DialogFragment fragment) {
        purchaseItem(activity, BillingComponent.NO_ADS_NONCONS_PRODUCT_ID); //, fragment);
    }

    protected static class NonConsumableItem {
        private String productId;
        private String signature;
        private String purchaseData;
        private String purchaseToken;
        public NonConsumableItem(String productId) {
            this.productId = productId;
        }
        public NonConsumableItem(String productId, String signature, String purchaseData) {
            this(productId);
            this.signature = signature;
            this.purchaseData = purchaseData;
        }
        public String getItemId() {
            return productId;
        }
    }

    protected static final NonConsumableItem[] ITEMS = {
            new NonConsumableItem(NO_ADS_NONCONS_PRODUCT_ID)
    };

    public void checkForNewPurchases() {
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                if (DEBUG) Log.i(TAG, "onServiceDisconnected() name=" + name);
                mService = null;
            }
            @Override
            public void onServiceConnected(ComponentName name,
                                           IBinder service) {
                if (DEBUG) Log.i(TAG, "onServiceConnected() name=" + name + " service=" + service);
                mService = IInAppBillingService.Stub.asInterface(service);
                checkForPurchasedItems();
                if (mServiceConn != null) {
                    context.unbindService(mServiceConn); // don't hold resource open
                }
            }
        };
        boolean bound = context.bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"),
                mServiceConn, Context.BIND_AUTO_CREATE);
        if (DEBUG) Log.i(TAG, "startService bound=" + bound);
    }

    protected void checkForPurchasedItems() {
        try {
            if (mService == null) {
                Log.e(TAG, "checkForPurchasedItems null service");
                return;
            }
            Bundle ownedItems = mService.getPurchases(3, context.getPackageName(), "inapp", null);
            if (DEBUG) Log.i(TAG, "checkForPurchasedItems returned bundle=" + ownedItems);
            if (ownedItems == null) {
                Log.e(TAG, "checkForPurchasedItems null bundle");
                return;
            }
            int responseCode = ownedItems.getInt("RESPONSE_CODE");
            if (responseCode != 0) {
                Log.e(TAG, "checkForPurchasedItems bad responseCode=" + responseCode);
                return;
            }
            List<NonConsumableItem> items = extractPurchasedItems(ownedItems);
            if (DEBUG) Log.i(TAG, "checkForPurchasedItems found itemCount=" + items.size());
            if (items.size() == 0)
                return;
            Set<String> newItems = new HashSet<String>();
            for (NonConsumableItem item : items) {
                newItems.add(item.productId);
            }
            overwritePurchasedItems(newItems);
        }
        catch (RemoteException e) {
            Log.e(TAG, "checkForPurchasedItems() remote exception", e);
        }
    }

    protected List<NonConsumableItem> extractPurchasedItems(Bundle ownedItems) {
        Set<String> supportedSkus = new HashSet<String>();
        for (NonConsumableItem item : ITEMS) {
            supportedSkus.add(item.productId);
        }

        List<NonConsumableItem> items = new ArrayList<NonConsumableItem>();
        if (ownedItems == null) {
            if (DEBUG) Log.i(TAG, "extractPurchasedItems no owned items found");
            return items;
        }

        List<String> ownedSkus =
                ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
        List<String> purchaseDataList =
                ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
        List<String> signatureList =
                ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE");
        //String continuationToken =
        //        ownedItems.getString("INAPP_CONTINUATION_TOKEN");
        if (ownedSkus == null) {
            if (DEBUG) Log.i(TAG, "extractPurchasedItems no owned skus found");
            return items;
        }

        for (int i = 0; i < ownedSkus.size(); ++i) {
            String sku = ownedSkus.get(i);
            String signature = (signatureList == null || i > signatureList.size() - 1) ? null : signatureList.get(i);
            String purchaseData = (purchaseDataList == null || i > purchaseDataList.size() - 1) ? null : purchaseDataList.get(i);
            if (supportedSkus.contains(sku)) {
                if (DEBUG) Log.i(TAG, "extractPurchasedItems found supported purchased sku=" + sku);
                items.add(new NonConsumableItem(sku, signature, purchaseData));
            }
        }
        return items;
    }

    protected void overwritePurchasedItems(Set<String> newItems) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean oldHasProkey = prefs.getBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, false);
        boolean newHasProkey = newItems.contains(NO_ADS_NONCONS_PRODUCT_ID);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putStringSet(PREF_PURCHASES, newItems);
        if (newHasProkey != oldHasProkey)
            edit.putBoolean(SettingsActivity.PREF_SHOW_NSFW_BOARDS, newHasProkey);
        edit.commit();
        synchronized (this) {
            purchases = newItems;
        }
        if (newHasProkey != oldHasProkey)
            BoardActivity.refreshAllBoards();
    }

    protected void purchaseItem(final Activity activity, final String productId) { //, final DialogFragment fragment) {
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                if (DEBUG) Log.i(TAG, "onServiceDisconnected() name=" + name);
                //if (fragment != null)
                //    fragment.dismiss();
                mService = null;
            }
            @Override
            public void onServiceConnected(ComponentName name,
                                           IBinder service) {
                if (DEBUG) Log.i(TAG, "onServiceConnected() name=" + name + " service=" + service);
                mService = IInAppBillingService.Stub.asInterface(service);
                Handler handler = activity instanceof ChanIdentifiedActivity ? ((ChanIdentifiedActivity)activity).getChanHandler() : null;
                purchaseItemViaService(activity, productId, handler);
                //if (activity instanceof ChanIdentifiedActivity) {
                //    Handler handler = ((ChanIdentifiedActivity) activity).getChanHandler();
                //    if (handler != null)
                //        handler.post(new Runnable() {
                //            @Override
                //            public void run() {
                //                fragment.dismiss();
                //            }
                //        });
                //}
                if (mServiceConn != null) {
                    context.unbindService(mServiceConn); // don't hold resource open
                }
            }
        };
        boolean bound = context.bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"),
                mServiceConn, Context.BIND_AUTO_CREATE);
        if (DEBUG) Log.i(TAG, "startService bound=" + bound);
    }

    protected void purchaseItemViaService(Activity activity, String sku, Handler handler) {
        try {
            if (mService == null) {
                Log.e(TAG, "purchaseItemViaService null service");
                makeToast(handler, R.string.purchase_error);
                return;
            }
            if (DEBUG) Log.i(TAG, "purchaseItemViaService getting buy intent from service");
            Bundle buyIntentBundle = mService.getBuyIntent(3, context.getPackageName(),
                    sku, "inapp", "");
            if (DEBUG) Log.i(TAG, "purchaseItemViaService returned bundle=" + buyIntentBundle);
            if (buyIntentBundle == null) {
                Log.e(TAG, "purchaseItemViaService null bundle");
                makeToast(handler, R.string.purchase_error);
                return;
            }
            int responseCode = buyIntentBundle.getInt("BILLING_RESPONSE_RESULT_OK");
            if (responseCode != 0) {
                Log.e(TAG, "purchaseItemViaService bad responseCode=" + responseCode);
                makeToast(handler, R.string.purchase_error);
                return;
            }
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            if (pendingIntent == null) {
                Log.e(TAG, "purchaseItemViaService bad pendingIntent=" + pendingIntent);
                makeToast(handler, R.string.purchase_error);
                return;
            }
            if (DEBUG) Log.i(TAG, "purchaseItemViaService calling purchase from pendingIntent=" + pendingIntent
                    + " sender=" + pendingIntent.getIntentSender());
            try {
                activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                        AboutActivity.PURCHASE_REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                        Integer.valueOf(0));
            }
            catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "purchaseItemViaService couldn't start purchase intent", e);
                makeToast(handler, R.string.purchase_error);
            }
        }
        catch (RemoteException e) {
            Log.e(TAG, "purchaseItemViaService() remote exception", e);
            makeToast(handler, R.string.purchase_error);
        }
    }

    public void processPurchaseResponse(Intent data, Handler handler) {
        if (data == null) {
            Log.e(TAG, "processPurchaseResponse null intent from response");
            makeToast(handler, R.string.purchase_error);
            return;
        }
        int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
        if (responseCode != 0) {
            Log.e(TAG, "processPurchaseResponse non-zero response_code=" + responseCode);
            makeToast(handler, R.string.purchase_error);
            return;
        }
        String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
        String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
        try {
            JSONObject jo = new JSONObject(purchaseData);
            String sku = jo.getString("productId");
            String purchaseToken = jo.getString("purchaseToken");
            if (sku == null || sku.isEmpty()) {
                Log.e(TAG, "processPurchaseResponse invalid sku received");
                makeToast(handler, R.string.purchase_error);
                return;
            }
            NonConsumableItem item = findItem(sku);
            if (item == null) {
                Log.e(TAG, "processPurchaseResponse item not found for sku=" + sku);
                makeToast(handler, R.string.purchase_error);
                return;
            }
            item.purchaseToken = purchaseToken;
            if (DEBUG) Log.i(TAG, "processPurchaseResponse recording purchase for sku=" + sku + " token=" + purchaseToken);
            recordPurchase(item);
            makeToast(handler, R.string.purchase_success);
        }
        catch (JSONException e) {
            Log.e(TAG, "processPurchaseResponse JSON exception", e);
            makeToast(handler, R.string.purchase_error);
            return;
        }
    }

    protected void makeToast(Handler handler, final int stringId) {
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
                }
            });
    }

    protected void recordPurchase(NonConsumableItem item) {
        purchases.add(item.productId);
        overwritePurchasedItems(purchases);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(LAST_PURCHASE_TOKEN, item.purchaseToken).commit();
    }

}
