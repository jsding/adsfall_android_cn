package com.ivy.billing.impl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.client.AndroidSdk;
import com.android.client.OnSkuDetailsListener;
import com.android.client.OrderConsumeListener;
import com.android.client.SKUDetail;
import com.ivy.IvySdk;
import com.ivy.ads.events.EventID;
import com.ivy.ads.events.EventParams;
import com.ivy.billing.BillingConfigurator;
import com.ivy.billing.PaymentVerifiedListener;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.PurchaseStateChangeData;
import com.ivy.event.CommonEvents;
import com.ivy.event.EventBus;
import com.ivy.event.EventListener;
import com.ivy.networks.tracker.EventTracker;
import com.ivy.util.Logger;
import com.taptap.payment.api.ITapPayment;
import com.taptap.payment.api.bean.Item;
import com.taptap.payment.api.bean.Order;
import com.taptap.payment.shell.TapPayment;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Keep
public class PurchaseManagerImpl implements PurchaseManager, EventListener {
  private static final String TAG = "Purchase";

  private static final int ERROR_ORDER_FORMAT = 6;
  private static final int ERROR_SIGNED_FAILED = 1;
  private static final int ERROR_RESPONSE_NOT_SUCCESS = 2;
  private static final int ERROR_RESPONSE_EMPTY = 3;

  private String currentBillItemId;
  private EventBus eventBus;

  private EventTracker eventTracker = null;

  private BillingPreferences preferences;

  private SharedPreferences sp;

  private final Map<String, SKUDetail> skuDetailMap = new HashMap<>();

  private final Map<String, Item> storeSkuDetailsMap = new HashMap<>();

  private Map<String, JSONObject> storeItems = new HashMap<>();
  private final ITapPayment.Callback<Item[]> onGetProductCallback = new ITapPayment.Callback<Item[]>() {
    @Override
    public void onError(ITapPayment.Error error) {
      Logger.error(TAG, "Query inventory failed, errorCode: " + error.getMessage());
    }

    @Override
    public void onFinish(Item[] result) {
      Logger.debug(TAG, "Query in app inventory was successful");
      for (Item details : result) {
        String productId = details.id;
        Logger.debug(TAG, "Add product: " + productId);
        JSONObject storeItem = storeItems.get(productId);
        if (storeItem != null) {
          double price = storeItem.optDouble("usd", 0);
          skuDetailMap.put(productId, convertToSKUDetail(details, price));
          storeSkuDetailsMap.put(productId, details);
        } else {
          Logger.error(TAG, "SKU " + productId + " not configured in default.json");
        }
      }
    }
  };

  @Override
  public void init(@NonNull Context context, @NonNull EventBus eventBus, @NonNull EventTracker eventLogger) {
    this.eventBus = eventBus;
    this.preferences = BillingConfigurator.setUpBillingPreferences();

    try {
      this.sp = context.getSharedPreferences("pays", Context.MODE_PRIVATE);

      this.eventTracker = eventLogger;
      eventBus.addListener(CommonEvents.BILLING_PURCHASE_STATE_CHANGE, this);
    } catch (Throwable t) {
      Logger.error(TAG, "PurchaseManagerImpl initialize exception", t);
    }
  }


  /**
   * 检查Google Checkout状态，建立Google Play链接，并自动加载自动初始化的计费点信息.
   * 将自动加载订阅和自动初始化的计费点信息。
   */
  @Override
  public void checkBillingSupported(@NonNull final List<String> skus) {
    // the payment system is valid
    fireEvent(CommonEvents.BILLING_BECOMES_AVAILABLE, new ArrayList<>());
    String[] skuArrays = new String[skus.size()];
    TapPayment.queryItems(skus.toArray(skuArrays), onGetProductCallback);

    queryPurchase();
  }

  private SKUDetail convertToSKUDetail(Item skuDetails, double usd) {
    String type = String.valueOf(skuDetails.type);
    String sku = skuDetails.id;
    String price = String.valueOf(skuDetails.price);
    long priceAmountMicros = 0L;
    String currencyCode = skuDetails.currency;

    String title = skuDetails.name;
    String description = skuDetails.description;

    return new SKUDetail(type, sku, price, priceAmountMicros, currencyCode, title, description, usd);
  }

  private void logPurchase(@NonNull Order purchase) {
    String orderId = purchase.id;
    if ("".equals(orderId)) {
      return;
    }
    String itemid = purchase.itemId;
    if (!storeSkuDetailsMap.containsKey(itemid)) {
      return;
    }

    Item skuDetails = storeSkuDetailsMap.get(itemid);
    float revenue = 0.0f;
    String currencyCode = "USD";
    String contentType = "inapp";
    if (skuDetails != null) {
      contentType = String.valueOf(skuDetails.type);
      JSONObject storeItem = storeItems.get(itemid);
      if (storeItem != null && storeItem.has("usd")) {
        currencyCode = "USD";
        revenue = (float) storeItem.optDouble("usd", 0);
      }
    }
    eventTracker.logPurchase(contentType, itemid, currencyCode, revenue);
  }

  @Override
  public void onEvent(int eventId, Object eventData) {
    Logger.debug(TAG, "OnEvent called: " + eventId);
    if (eventId == CommonEvents.BILLING_PURCHASE_STATE_CHANGE) {
      final PurchaseStateChangeData data = (PurchaseStateChangeData) eventData;
      final String itemid = data.getItemId();
      switch (data.getPurchaseState()) {
        case PURCHASED:
          if (!data.getJustRestore()) {
            String orderId = data.getOrderId();
            if (orderId == null || "".equals(orderId)) {
              return;
            }
            boolean eventLogged = sp.getBoolean(orderId + "_logged", false);
            if (eventLogged) {
              Logger.debug(TAG, "orderID: " + orderId + " already logged");
              return;
            }

            Bundle bundle = new Bundle();
            bundle.putInt("times", IvySdk.appStartTimes);
            bundle.putString(EventParams.PARAM_PROVIDER, "google");
            if (currentBillItemId != null) {
              bundle.putString("reason", currentBillItemId);
            }
            bundle.putString(EventParams.PARAM_ITEMID, itemid);
            bundle.putString(EventParams.PARAM_ORDERID, data.getOrderId());

            Item skuDetails = storeSkuDetailsMap.get(itemid);
            float revenue = 0.0f;
            String currencyCode = "USD";
            if (skuDetails != null) {
              JSONObject storeItem = storeItems.get(itemid);
              if (storeItem != null) {
                currencyCode = "USD";
                revenue = (float) storeItem.optDouble("usd", 0);
              }
              bundle.putString(EventParams.PARAM_LABEL, String.valueOf(skuDetails.type));
              bundle.putString("currency", currencyCode);
              bundle.putDouble(EventParams.PARAM_REVENUE, revenue);
            } else {
              JSONObject storeItem = storeItems.get(itemid);
              if (storeItem != null) {
                currencyCode = "USD";
                revenue = (float) storeItem.optDouble("usd", 0);
                bundle.putString("currency", currencyCode);
                bundle.putDouble(EventParams.PARAM_REVENUE, revenue);
              }
            }

            int totalOrders = sp.getInt("total_orders", 0) + 1;
            bundle.putInt(EventParams.PARAM_TIMES, totalOrders);

            float totalPaid = sp.getFloat("total_revenue", 0) + revenue;
            bundle.putFloat("total_revenue", totalPaid);

            bundle.putFloat(EventParams.PARAM_VALUE, revenue);

            if (totalOrders == 1) {
              bundle.putString(EventParams.PARAM_CATALOG, "first_purchase");
            }

            try {
              String gameUUID = IvySdk.getUUID();
              if (gameUUID != null && !"".equals(gameUUID)) {
                bundle.putString("character", gameUUID);
              }

              String firebaseUserID = AndroidSdk.getFirebaseUserId();
              if (!"".equals(firebaseUserID)) {
                bundle.putString("roleId", firebaseUserID);
              }

              String savedMainLine = IvySdk.mmGetStringValue(IvySdk.KEY_GAME_MAIN_LINE, "");
              if (!"".equals(savedMainLine)) {
                JSONObject mainLineData = new JSONObject(savedMainLine);
                Iterator<String> keys = mainLineData.keys();
                while (keys.hasNext()) {
                  String key = keys.next();
                  int value = mainLineData.optInt(key);
                  if (value != 0) {
                    bundle.putInt(key, value);
                  }
                }
              }

              String remaingCurrency = IvySdk.mmGetStringValue(IvySdk.KEY_VIRUTAL_CURRENCY, "");
              if (!"".equals(remaingCurrency)) {
                JSONObject currencyData = new JSONObject(remaingCurrency);
                Iterator<String> keys = currencyData.keys();
                while (keys.hasNext()) {
                  String key = keys.next();
                  int value = currencyData.optInt(key);
                  if (value != 0) {
                    bundle.putInt(key, value);
                  }
                }
              }

              JSONObject localUserProperties = IvySdk.mmGetJsonValue(IvySdk.KEY_LOCAL_USER_PROPERIES);
              if (localUserProperties != null) {
                Iterator<String> keys = localUserProperties.keys();
                while (keys.hasNext()) {
                  String key = keys.next();
                  String value = localUserProperties.optString(key);
                  if (!"".equals(value)) {
                    bundle.putString(key, value);
                  }
                }
              }

              this.eventTracker.logEvent(EventID.IAP_PURCHASED, bundle);

              if (totalOrders == 1) {
                this.eventTracker.logEvent(EventID.FIRST_PURCHASE, bundle);
              }

              sp.edit()
                .putInt("total_orders", totalOrders)
                .putFloat("total_revenue", totalPaid)
                .putBoolean(orderId + "_logged", true).apply();

            } catch (Throwable ignored) {
            }
            return;
          }
          return;
        case CANCELED:
          Bundle bundle = new Bundle();
          bundle.putString(EventParams.PARAM_PROVIDER, "google");
          bundle.putString(EventParams.PARAM_ITEMID, itemid);
          this.eventTracker.logEvent(EventID.IAP_CANCEL, bundle);
          return;
        default:
      }
    }
  }

  @Override
  public void buy(@NonNull String productId, @Nullable String developerPayload) {
    try {
      Activity activity = IvySdk.getActivity();
      if (developerPayload != null) {
        sp.edit().putString(productId, developerPayload).apply();
      } else {
        sp.edit().remove(productId).apply();
      }

      IvySdk.runOnUiThreadCustom(new Runnable() {
        @Override
        public void run() {
          TapPayment.requestPayFlow(activity, productId, 1, developerPayload, new ITapPayment.PayCallback() {

            @Override
            public void onCancel(String extra) {

            }

            @Override
            public void onError(ITapPayment.Error error, String extra) {
            }

            @Override
            public void onFinish(Order order, String extra) {
              Logger.debug(TAG, "purchase success >>>");
              processPurchase(order, extra);
            }
          });
        }
      });
    } catch (Throwable e) {
      Logger.error(TAG, "launchBillingFlow error", e);
    }
  }

  private void purchaseStateChange(@NonNull PurchaseState state, @NonNull Order purchase, String extra) {
    String sku = purchase.itemId;
    long purchaseTime = System.currentTimeMillis();
    String orderID = purchase.id;

    String developerPayload = extra;
    String serverDeveloperPayload = sp.contains(sku + "_server") ? sp.getString(sku + "_server", "") : null;
    if (serverDeveloperPayload != null) {
      developerPayload = serverDeveloperPayload;
    }

    int quantity = 0;

    String purchaseToken = purchase.id + "|" + purchase.token;

    PurchaseStateChangeData changeData = new PurchaseStateChangeData(orderID, state, sku, quantity, purchaseTime, developerPayload, false, purchaseToken, false);
    changeData.setPackageName(AndroidSdk.getConfig(AndroidSdk.CONFIG_KEY_PACKAGE_NAME));

    SKUDetail skuDetail = skuDetailMap.get(sku);
    if (skuDetail != null) {
      changeData.setSkuJson(skuDetail.toString());
    }

    fireEvent(CommonEvents.BILLING_PURCHASE_STATE_CHANGE, changeData);
  }

  @Override
  public void setStoreItems(Map<String, JSONObject> storeItems) {
    this.storeItems = storeItems;
  }

  private boolean isAutoLoadIap(String productId) {
    JSONObject item = this.storeItems.get(productId);
    if (item != null) {
      return item.optInt("autoload") == 1;
    }
    return false;
  }

  private boolean isSubscription(String productId) {
    JSONObject item = this.storeItems.get(productId);
    if (item != null) {
      return item.optInt("repeat") == 0;
    }
    return false;
  }

  private boolean isConsumable(String productId) {
    JSONObject item = this.storeItems.get(productId);
    if (item != null) {
      return item.optInt("repeat") != 0;
    }
    return true;
  }

  @Override
  public void querySKUDetails(List<String> iapIds, @NonNull OnSkuDetailsListener onSkuDetailsListener) {
    List<String> unQueriedIaps = new ArrayList<>();
    for (String iapId : iapIds) {
      if (!this.skuDetailMap.containsKey(iapId)) {
        unQueriedIaps.add(iapId);
      }
    }

    if (unQueriedIaps.size() == 0) {
      onSkuDetailsListener.onReceived();
      return;
    }

    String[] itemIds = new String[unQueriedIaps.size()];
    TapPayment.queryItems(unQueriedIaps.toArray(itemIds), onGetProductCallback);
  }

  @Override
  public SKUDetail getSKUDetail(String iapId) {
    return this.skuDetailMap.get(iapId);
  }

  private void fireEvent(final int eventId, final Object eventData) {
    if (eventBus != null) {
      eventBus.fireEvent(eventId, eventData);
    }
  }

  private void processPurchase(@NonNull Order purchase, String extra) {
    String sku = purchase.id;
    Logger.debug(TAG, "processPurchase >>> " + sku);
    handleVerifiedPurchase(purchase, extra, new PaymentVerifiedListener() {
      @Override
      public void onSuccess() {
        Logger.debug(TAG, "handleVerifiedPurchase for inapp onSuccess");
        purchaseStateChange(PurchaseState.PURCHASED, purchase, extra);
      }

      @Override
      public void onFail(int errorCode) {
        Logger.debug(TAG, "handleVerifiedPurchase for inapp onFail, errorCode: " + errorCode);
        purchaseStateChange(PurchaseState.ERROR, purchase, extra);
      }
    });
  }

  /**
   * 处理游戏发货.
   * <p>
   * 1. 客户端先进行public key的校验, 如果失败，直接fail
   * 2. 如果没有定义verify-url，直接成功
   * 3. 如果定义了verify-url, 如果verify-url明确的失败，则失败，否则成功!
   *
   * @param purchase 要处理的purchase数据
   * @param listener
   */
  private void handleVerifiedPurchase(@NonNull Order purchase, String extra, @NonNull PaymentVerifiedListener listener) {
    final String sku = purchase.itemId;
    final String orderId = purchase.id;
    String token = purchase.token;

    Logger.debug(TAG, "handleVerifiedPurchase " + purchase + " with extra " + extra);
    TapPayment.consumeOrder(orderId, token, new ITapPayment.Callback<Order>() {
      @Override
      public void onError(ITapPayment.Error error) {
        listener.onFail(ERROR_SIGNED_FAILED);
      }

      @Override
      public void onFinish(Order order) {
        logPurchase(purchase);
        listener.onSuccess();
      }
    });
  }

  @Override
  public void setBillItemId(String itemId) {
    currentBillItemId = itemId;
  }

  @Override
  public JSONObject getStoreItem(String productId) {
    return null;
  }

  @Override
  public void queryPurchases(String productId) {

  }

  @Override
  public void queryUnconsumedPurchases() {

  }

  @Override
  public void consumePurchase(@NonNull String purchaseToken, @NonNull OrderConsumeListener orderConsumeListener) {
    Logger.debug(TAG, "Consume Purchase >>> " + purchaseToken);
    orderConsumeListener.onConsumeSuccess(purchaseToken);
  }

  private void handleUnConsumedPurchases(boolean subs) {
  }

  @Override
  public void queryPurchase() {
    Logger.debug(TAG, "Purchase System query");
    handleUnConsumedPurchases(false);
  }

  @Override
  public void onDestroy() {

  }

  @Override
  public void onResume(Activity activity) {

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

  }
}
