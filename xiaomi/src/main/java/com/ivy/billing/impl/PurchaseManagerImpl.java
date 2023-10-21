package com.ivy.billing.impl;

import android.app.Activity;
import android.content.Context;
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
import com.ivy.util.CommonUtil;
import com.ivy.util.Logger;
import com.xiaomi.billingclient.api.BillingClient;
import com.xiaomi.billingclient.api.BillingClientStateListener;
import com.xiaomi.billingclient.api.BillingFlowParams;
import com.xiaomi.billingclient.api.BillingResult;
import com.xiaomi.billingclient.api.Purchase;
import com.xiaomi.billingclient.api.PurchasesUpdatedListener;
import com.xiaomi.billingclient.api.SkuDetails;
import com.xiaomi.billingclient.api.SkuDetailsParams;
import com.xiaomi.billingclient.api.SkuDetailsResponseListener;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Keep
public class PurchaseManagerImpl implements PurchaseManager, EventListener, PurchasesUpdatedListener {
  private static final String TAG = "Purchase";

  private static final int ERROR_ORDER_FORMAT = 6;
  private static final int ERROR_SIGNED_FAILED = 1;
  private static final int ERROR_RESPONSE_NOT_SUCCESS = 2;
  private static final int ERROR_RESPONSE_EMPTY = 3;

  private String currentBillItemId;
  private EventBus eventBus;

  private EventTracker eventTracker = null;

  private BillingClient billingClient;

  private BillingPreferences preferences;

  private SharedPreferences sp;

  private final Map<String, SKUDetail> skuDetailMap = new HashMap<>();

  private final Map<String, SkuDetails> storeSkuDetailsMap = new HashMap<>();

  private Map<String, JSONObject> storeItems = new HashMap<>();

  private final SkuDetailsResponseListener mGotInventoryInappListener = (billingResult, productDetails) -> {
    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK || productDetails == null) {
      Logger.error(TAG, "Query inventory failed, errorCode: " + billingResult.getResponseCode());
      return;
    }

    Logger.debug(TAG, "Query in app inventory was successful");
    for (SkuDetails details : productDetails) {
      String productId = details.getSku();
      Logger.debug(TAG, "Add product: " + productId);
      JSONObject storeItem = storeItems.get(productId);
      if (storeItem != null) {
        double price = storeItem.optDouble("usd", 0);
        skuDetailMap.put(details.getSku(), getSKUDetailFromXiaomi(details, price));
        storeSkuDetailsMap.put(details.getSku(), details);
      } else {
        Logger.error(TAG, "SKU " + productId + " not configured in default.json");
      }
    }

    Logger.debug(TAG, "queryPurchases: SUBS ");
    handleUnConsumedPurchases(true);
  };

  @Override
  public void init(@NonNull Context context, @NonNull EventBus eventBus, @NonNull EventTracker eventLogger) {
    this.eventBus = eventBus;
    this.preferences = BillingConfigurator.setUpBillingPreferences();

    try {
      this.sp = context.getSharedPreferences("pays", Context.MODE_PRIVATE);
      this.billingClient = BillingClient.newBuilder(context).setListener(this).build();
      this.billingClient.enableFloatView((Activity) context);
      this.eventTracker = eventLogger;
      eventBus.addListener(CommonEvents.BILLING_PURCHASE_STATE_CHANGE, this);
    } catch (Throwable t) {
      Logger.error(TAG, "PurchaseManagerImpl initialize exception", t);
    }
  }

  @Override
  public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
    // Logic from onActivityResult should be moved here.
    int responseCode = billingResult.getResponseCode();
    Logger.debug(TAG, "onPurchasesUpdated: " + responseCode);
    if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
      for (Purchase purchase : purchases) {
        if (purchase != null) {
          processPurchase(purchase);
        }
      }
    } else {
      // Handle an error caused by a user cancelling or  the purchase flow.
      Logger.error(TAG, "onPurchasesUpdated, purchases is empty, responseCode " + responseCode);
      if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
        // already owned the item, but purchase is null, we will query the purchase history and restore
        // TODO:
      }
    }
  }

  /**
   * 检查Google Checkout状态，建立Google Play链接，并自动加载自动初始化的计费点信息.
   * 将自动加载订阅和自动初始化的计费点信息。
   */
  @Override
  public void checkBillingSupported(@NonNull final List<String> skus) {
    if (billingClient == null) {
      return;
    }

    billingClient.startConnection(new BillingClientStateListener() {
      @Override
      public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        int responseCode = billingResult.getResponseCode();
        Logger.debug(TAG, "onBillingSetupFinished, response Code: " + responseCode);
        // Logic from ServiceConnection.onServiceConnected should be moved here.
        if (responseCode == BillingClient.BillingResponseCode.OK) {
          Logger.debug(TAG, "Setup successful. Querying inventory");

          // the payment system is valid
          fireEvent(CommonEvents.BILLING_BECOMES_AVAILABLE, new ArrayList<>());

          List<String> products = new ArrayList<>();
          for (String sku : skus) {
            if (isAutoLoadIap(sku)) {
              products.add(sku);
            }
          }
          // query the iaps
          if (products.size() > 0) {
            billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder().setSkusList(products).setType(BillingClient.SkuType.INAPP).build(), mGotInventoryInappListener);
          }
        }
      }

      @Override
      public void onBillingServiceDisconnected() {
        // Logic from ServiceConnection.onServiceDisconnected should be moved here.
        Logger.warning(TAG, "onBillingServiceDisconnected, should retry to connect later");
      }
    });
  }

  private SKUDetail getSKUDetailFromXiaomi(SkuDetails skuDetails, double usd) {
    String type = skuDetails.getType();
    String sku = skuDetails.getSku();
    String price = "";
    long priceAmountMicros = 0L;
    String currencyCode = "USD";
    if (BillingClient.SkuType.INAPP.equals(type)) {
      price = skuDetails.getPrice();
      try {
        priceAmountMicros = Long.parseLong(skuDetails.getPriceAmountMicros());
      } catch(Throwable t) {
        t.printStackTrace();
      }
      currencyCode = skuDetails.getPriceCurrencyCode();
    } else {
      SkuDetails.SubscriptionOfferDetails purchaseOfferDetails = skuDetails.getSubscriptionOfferDetails().get(0);

      price = purchaseOfferDetails.getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
      priceAmountMicros = purchaseOfferDetails.getPricingPhases().getPricingPhaseList().get(0).getPriceAmountMicros();
      currencyCode = purchaseOfferDetails.getPricingPhases().getPricingPhaseList().get(0).getPriceCurrencyCode();
    }
    String title = skuDetails.getTitle();
    String description = skuDetails.getDescription();

    if ("".equals(price)) {
      price = "US$" + usd;
    }
    return new SKUDetail(type, sku, price, priceAmountMicros, currencyCode, title, description, usd);
  }

  private void querySkuAndPurchase(String sku) {
    boolean isConsumable = isConsumable(sku);

    List<String> products = new ArrayList<>();
    products.add(sku);

    billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder().setSkusList(products).setType(BillingClient.SkuType.INAPP).build(), (billingResult, skuDetailsList) -> {
      if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
        Logger.error(TAG, "Query inventory failed, errorCode: " + billingResult.getResponseCode());
        return;
      }

      if (skuDetailsList == null || skuDetailsList.size() == 0) {
        return;
      }

      Logger.debug(TAG, "Query inapp inventory was successful");
      for (SkuDetails details : skuDetailsList) {
        String sku1 = details.getSku();
        Logger.debug(TAG, "Add SKU: " + sku1);
        JSONObject storeItem = storeItems.get(sku1);
        if (storeItem != null) {
          double usd = storeItem.optDouble("usd", 0);
          skuDetailMap.put(details.getSku(), getSKUDetailFromXiaomi(details, usd));
          storeSkuDetailsMap.put(details.getSku(), details);
        } else {
          Logger.error(TAG, "StoreItem " + sku1 + " not defined");
        }
      }

      SkuDetails productDetails = storeSkuDetailsMap.get(sku);
      if (productDetails == null) {
        Logger.debug(TAG, "sku " + sku + " not found, removed from store?");
        return;
      }

      // fire purchase

      BillingFlowParams flowParams = BillingFlowParams.newBuilder().setSkuDetails(productDetails).setObfuscatedAccountId(IvySdk.getUUID()).build();
      Activity activity = IvySdk.getActivity();
      if (activity == null) {
        Logger.error(TAG, "activity is disposed");
        return;
      }
      startLaunchBillingFlow(activity, flowParams);
    });
  }

  private void logPurchase(@NonNull Purchase purchase) {
    String orderId = purchase.getOrderId();
    if ("".equals(orderId)) {
      return;
    }
    String itemid = purchase.getSkus().get(0);
    if (!storeSkuDetailsMap.containsKey(itemid)) {
      return;
    }
    if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
      return;
    }
    SkuDetails skuDetails = storeSkuDetailsMap.get(itemid);
    float revenue = 0.0f;
    String currencyCode = "USD";
    String contentType = "inapp";
    if (skuDetails != null) {
      contentType = skuDetails.getType();
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
            bundle.putString(EventParams.PARAM_PROVIDER, IvySdk.getPaymentChannel());
            if (currentBillItemId != null) {
              bundle.putString("reason", currentBillItemId);
            }
            bundle.putString(EventParams.PARAM_ITEMID, itemid);
            bundle.putString(EventParams.PARAM_ORDERID, data.getOrderId());

            SkuDetails skuDetails = storeSkuDetailsMap.get(itemid);
            float revenue = 0.0f;
            String currencyCode = "USD";
            if (skuDetails != null) {
              JSONObject storeItem = storeItems.get(itemid);
              if (storeItem != null) {
                currencyCode = "USD";
                revenue = (float) storeItem.optDouble("usd", 0);
              }
              bundle.putString(EventParams.PARAM_LABEL, skuDetails.getType());
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
          bundle.putString(EventParams.PARAM_PROVIDER, IvySdk.getPaymentChannel());
          bundle.putString(EventParams.PARAM_ITEMID, itemid);
          this.eventTracker.logEvent(EventID.IAP_CANCEL, bundle);
          return;
        default:
          return;
      }
    }
  }

  @Override
  public void buy(@NonNull String productId, @Nullable String developerPayload) {
    try {
      if (billingClient == null) {
        return;
      }

      if (!billingClient.isReady()) {
        Logger.debug(TAG, "Billing Client not ready, reconnect...");
        billingClient.startConnection(new BillingClientStateListener() {
          @Override
          public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
            Logger.debug(TAG, "onBillingSetupFinished, response Code: " + billingResult.getResponseCode());
            // Logic from ServiceConnection.onServiceConnected should be moved here.
            Logger.debug(PurchaseManagerImpl.TAG, "Setup finished");
            int responseCode = billingResult.getResponseCode();
            if (responseCode == BillingClient.BillingResponseCode.OK) {
              Logger.debug(TAG, "Setup successful. Querying inventory");
              // the payment system is valid
              fireEvent(CommonEvents.BILLING_BECOMES_AVAILABLE, new ArrayList<>());
              // restart this purchase flow
              buy(productId, developerPayload);
            }
          }

          @Override
          public void onBillingServiceDisconnected() {
            // Logic from ServiceConnection.onServiceDisconnected should be moved here.
            Logger.warning(TAG, "onBillingServiceDisconnected, should retry to connect later");
          }
        });
        return;
      }

      if (developerPayload != null) {
        sp.edit().putString(productId, developerPayload).apply();
      } else {
        sp.edit().remove(productId).apply();
      }

      if (!storeSkuDetailsMap.containsKey(productId)) {
        Logger.debug(TAG, "iapId " + productId + " not preload, we try to load and start buy process");
        querySkuAndPurchase(productId);
        return;
      }

      SkuDetails productDetails = storeSkuDetailsMap.get(productId);
      if (productDetails == null) {
        Logger.error(TAG, "Billing Client is not ready for iap: " + productId);
        return;
      }

      BillingFlowParams flowParams = BillingFlowParams.newBuilder().setSkuDetails(productDetails).setObfuscatedAccountId(IvySdk.getUUID()).build();
      Activity activity = IvySdk.getActivity();
      if (activity == null) {
        Logger.error(TAG, "activity is disposed");
        return;
      }

      startLaunchBillingFlow(activity, flowParams);
    } catch (Throwable e) {
      Logger.error(TAG, "launchBillingFlow error", e);
    }
  }

  private void purchaseStateChange(@NonNull PurchaseState state, @NonNull Purchase purchase) {
    String sku = purchase.getSkus().get(0);
    long purchaseTime = Long.parseLong(purchase.getPurchaseTime());
    String orderID = "".equals(purchase.getOrderId()) ? sku + "_" + purchaseTime : purchase.getOrderId();
    String developerPayload = sp.contains(sku) ? sp.getString(sku, "") : null;
    String serverDeveloperPayload = sp.contains(sku + "_server") ? sp.getString(sku + "_server", "") : null;
    if (serverDeveloperPayload != null) {
      developerPayload = serverDeveloperPayload;
    }

    boolean isAutoRenewing = purchase.isAutoRenewing();
    int quantity = 0;

    PurchaseStateChangeData changeData = new PurchaseStateChangeData(orderID, state, sku, quantity, purchaseTime, developerPayload, false, purchase.getPurchaseToken(), isAutoRenewing);
    changeData.setSignature(purchase.getSignature());
    changeData.setPackageName(purchase.getPackageName());

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

    billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder().setSkusList(unQueriedIaps).build(), (billingResult, skuDetailsList) -> {
      if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK || skuDetailsList == null) {
        Logger.error(TAG, "Query inventory failed, errorCode: " + billingResult.getResponseCode());
        onSkuDetailsListener.onReceived();
        return;
      }

      Logger.debug(TAG, "Query inapp inventory was successful");
      for (SkuDetails details : skuDetailsList) {
        String sku = details.getSku();
        Logger.debug(TAG, "Add SKU: " + sku);
        JSONObject storeItem = storeItems.get(sku);
        if (storeItem != null) {
          double usd = storeItem.optDouble("usd", 0);
          skuDetailMap.put(sku, getSKUDetailFromXiaomi(details, usd));
          storeSkuDetailsMap.put(sku, details);
        } else {
          Logger.error(TAG, "StoreItem " + sku + " not defined");
        }
      }
      onSkuDetailsListener.onReceived();
    });
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

  private void processPurchase(@NonNull Purchase purchase) {
    if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
      Logger.warning(TAG, "Purchase state not PURCHASED, " + purchase.getPurchaseState());
      return;
    }

    String sku = purchase.getSkus().get(0);
    Logger.debug(TAG, "processPurchase >>> " + sku);
    if (isConsumable(sku)) {
      handleVerifiedPurchase(purchase, new PaymentVerifiedListener() {
        @Override
        public void onSuccess() {
          Logger.debug(TAG, "handleVerifiedPurchase for inapp onSuccess");
          purchaseStateChange(PurchaseState.PURCHASED, purchase);
        }

        @Override
        public void onFail(int errorCode) {
          Logger.debug(TAG, "handleVerifiedPurchase for inapp onFail, errorCode: " + errorCode);
          purchaseStateChange(PurchaseState.ERROR, purchase);
        }
      });
    } else {
      handleVerifiedPurchase(purchase, new PaymentVerifiedListener() {
        @Override
        public void onSuccess() {
          Logger.debug(TAG, "handleVerifiedPurchase for subscription onSuccess");
          Logger.debug(TAG, "Subscription required acknowledged");
          billingClient.acknowledgePurchase(purchase.getPurchaseToken(), billingResult -> {
            int responseCode = billingResult.getResponseCode();
            if (responseCode == BillingClient.BillingResponseCode.OK) {
              Logger.debug(TAG, "Good! purchase acknowledge successfully");
              purchaseStateChange(PurchaseState.PURCHASED, purchase);
            } else {
              Logger.debug(TAG, "Acknowledge purchase response Code: " + responseCode);
            }
          });
        }

        @Override
        public void onFail(int errorCode) {
          Logger.debug(TAG, "handleVerifiedPurchase for subscription onFail, errorCode: " + errorCode);
          purchaseStateChange(PurchaseState.ERROR, purchase);
        }
      });
    }
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
  private void handleVerifiedPurchase(@NonNull Purchase purchase, @NonNull PaymentVerifiedListener listener) {
    final String sku = purchase.getSkus().get(0);
    final String orderId = purchase.getOrderId();

    String verifyUrl = preferences.verifyUrl;
    if (verifyUrl == null || "".equals(verifyUrl)) {
      logPurchase(purchase);
      listener.onSuccess();
      return;
    }

    int billId = 0;
    JSONObject storeItem = storeItems.get(sku);
    if (storeItem != null) {
      billId = storeItem.optInt("billId", 0);
    }

    String countryCode = IvySdk.getCountryCode();
    String appid = IvySdk.getGridConfigString("appid");

    SKUDetail skuDetail = skuDetailMap.get(sku);
    FormBody.Builder formBuilder = new FormBody.Builder()
      .add("country", countryCode)
      .add("sku", sku)
      .add("payId", String.valueOf(billId))
      .add("orderId", orderId)
      .add("purchaseTime", String.valueOf(purchase.getPurchaseTime()))
      .add("purchaseToken", purchase.getPurchaseToken())
      .add("purchaseState", String.valueOf(purchase.getPurchaseState()))
      .add("uuid", IvySdk.getUUID())
      .add("packageName", purchase.getPackageName())
      .add("signature", purchase.getSignature())
      .add("sku_json", skuDetail != null ? skuDetail.toString() : "{}")
      .add("appid", appid);

    boolean hasPayload = sp.contains(sku);
    if (hasPayload) {
      String developerPayload = sp.getString(sku, null);
      if (developerPayload != null) {
        formBuilder.add("payload", developerPayload);
        Logger.debug(TAG, "payload -> " + developerPayload);
      }
    }

    RequestBody formBody = formBuilder.build();

    Logger.debug(TAG, "payId -> " + billId);
    Logger.debug(TAG, "OrderID -> " + purchase.getOrderId());
    Logger.debug(TAG, "Signature -> " + purchase.getSignature());

    Request request = new Request.Builder()
      .url(verifyUrl)
      .post(formBody)
      .build();

    Logger.debug(TAG, "Start send verify URL >>> " + verifyUrl);
    IvySdk.getOkHttpClient().newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(@NonNull Call call, @NonNull IOException e) {
        Logger.error(TAG, "Verify iap failed", e);
        Logger.warning(TAG, "The verify server is down?");
        if (preferences.mustVerify) {
          listener.onFail(ERROR_RESPONSE_NOT_SUCCESS);
        } else {
          listener.onSuccess();
        }
      }

      @Override
      public void onResponse(@NonNull Call call, @NonNull Response response) {
        if (!response.isSuccessful()) {
          Logger.error(TAG, "Verify server response error: " + response.code());
          listener.onFail(ERROR_RESPONSE_NOT_SUCCESS);
          return;
        }

        try {
          String responseBody = response.body().string();
          Logger.debug(TAG, "Receiving response >>>> " + responseBody);

          String s;
          if (responseBody.startsWith("{") && responseBody.endsWith("}")) {
            s = responseBody;
          } else {
            s = CommonUtil.decodeWithKey(responseBody, "c3fcd3d76192e4007dfb496cca67e13b");
          }

          if (s == null || "".equals(s)) {
            Logger.error(TAG, "Empty response, verify failed");
            listener.onFail(ERROR_RESPONSE_EMPTY);
            return;
          }

          Logger.debug(TAG, "Verify Result: " + s);
          JSONObject o = new JSONObject(s);
          int status = o.optInt("status");
          if (status == 1) {
            // use the server payload
            if (o.has("payload")) {
              String serverPayload = o.optString("payload", "");
              Logger.debug(TAG, "Receiving payload " + serverPayload);
              sp.edit().putString(sku + "_server", serverPayload).apply();
            }


            logPurchase(purchase);

            listener.onSuccess();
          } else {
            Logger.error(TAG, "Status Not Success >>> " + status);
            listener.onFail(ERROR_RESPONSE_NOT_SUCCESS);
          }
        } catch (Throwable t) {
          Logger.error(TAG, "Error parse the verify response", t);
          if (preferences.mustVerify) {
            Logger.warning(TAG, "Force check enabled, onFail");
            listener.onFail(ERROR_RESPONSE_NOT_SUCCESS);
          } else {
            Logger.warning(TAG, "Force check disabled, also onSuccess");
            listener.onSuccess();
          }
        }

        try {
          response.close();
        } catch (Throwable t) {
          //
        }
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

    billingClient.consumeAsync(purchaseToken, (billingResult, purchaseToken1) -> {
      int responseCode = billingResult.getResponseCode();
      Logger.debug(TAG, "billingResult" + responseCode + ", " + billingResult.getDebugMessage());
      if (responseCode == BillingClient.BillingResponseCode.OK) {
        orderConsumeListener.onConsumeSuccess(purchaseToken1);
      } else {
        orderConsumeListener.onConsumeError(purchaseToken1, String.valueOf(responseCode), codeToMessage(responseCode));
      }
    });
  }

  private void handleUnConsumedPurchases(boolean subs) {
    if (!billingClient.isReady()) {
      Logger.debug(TAG, "billing client not ready");
      return;
    }
    billingClient.queryPurchasesAsync(subs ? BillingClient.SkuType.SUBS : BillingClient.SkuType.INAPP, (billingResult, purchases) -> {
      if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
        Logger.debug(TAG, "queryPurchases failed: " + billingResult.getResponseCode() + ", " + billingResult.getDebugMessage());
        return;
      }

      if (purchases.size() > 0) {
        for (Purchase purchase : purchases) {
          if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            Logger.debug(TAG, "Handle purchased purchase: " + purchase);
            processPurchase(purchase);
          } else {
            Logger.debug(TAG, "Purchase state: " + purchase.getPurchaseState());
            Logger.debug(TAG, "Purchase: " + purchase);
          }
        }
      }
    });
  }

  @Override
  public void queryPurchase() {
    Logger.debug(TAG, "Purchase System query");
    handleUnConsumedPurchases(false);
  }

  @Override
  public void onDestroy() {
    if (billingClient != null) {
      billingClient.dismissFloatView();
    }
    if (billingClient != null) {
      billingClient.endConnection();
    }
  }

  @Override
  public void onResume(Activity activity) {

  }

  private void startLaunchBillingFlow(@NonNull Activity activity, @NonNull BillingFlowParams flowParams) {
    IvySdk.runOnUiThreadCustom(() -> {
      BillingResult billingResult = billingClient.launchBillingFlow(activity, flowParams);

      int billingResponseCode = billingResult.getResponseCode();
      if (billingResponseCode != BillingClient.BillingResponseCode.OK) {
        Logger.error(TAG, "launchBillingFlow failed, code: " + billingResponseCode + ", " + billingResult.getDebugMessage());
      }

      if (billingResponseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
        // in this case, will try to consume this
      }
    });
  }

  private String codeToMessage(int code) {
    switch (code) {
      case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
        return "Billing API version is not supported for the type requested";
      case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
        return "Invalid arguments provided to the API";
      case BillingClient.BillingResponseCode.ERROR:
        return "Fatal error during the API action";
      case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
        return "Requested feature is not supported by Play Store on the current device";
      case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
        return "Failure to purchase since item is already owned";
      case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
        return "Failure to consume since item is not owned";
      case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
        return "Requested product is not available for purchase";
      case BillingClient.BillingResponseCode.OK:
        return "Success";
      case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
        return "Play Store service is not connected now - potentially transient state";
      case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
        return "The request has reached the maximum timeout before Google Play responds";
      case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
        return "Network connection is down";
      case BillingClient.BillingResponseCode.USER_CANCELED:
        return "User pressed back or canceled a dialog";
      default:
        return "Unknown error code";
    }
  }
}
