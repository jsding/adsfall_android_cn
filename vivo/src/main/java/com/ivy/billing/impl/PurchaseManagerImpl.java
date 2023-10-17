package com.ivy.billing.impl;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

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
import com.ivy.billing.Constants;
import com.ivy.billing.PaymentVerifiedListener;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.PurchaseStateChangeData;
import com.ivy.event.CommonEvents;
import com.ivy.event.EventBus;
import com.ivy.event.EventListener;
import com.ivy.networks.tracker.EventTracker;
import com.ivy.util.Logger;
import com.ivy.vivo.VivoSignUtils;
import com.vivo.unionpay.sdk.open.VivoConstants;
import com.vivo.unionpay.sdk.open.VivoPayCallback;
import com.vivo.unionpay.sdk.open.VivoPayInfo;
import com.vivo.unionpay.sdk.open.VivoRoleInfo;
import com.vivo.unionpay.sdk.open.VivoUnionSDK;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Keep
public class PurchaseManagerImpl implements PurchaseManager, EventListener, VivoPayCallback {
  private static final String TAG = "Purchase";

  private String currentBillItemId;
  private EventBus eventBus;

  private EventTracker eventTracker = null;

  private BillingPreferences preferences;

  private SharedPreferences sp;

  private final Map<String, SKUDetail> skuDetailMap = new HashMap<>();

  private Map<String, JSONObject> storeItems = new HashMap<>();

  private String purchasingOrderId;

  private String purchasingItemId;

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

    for(String iapId: skus) {
      convertStoreItem(iapId);
    }
    queryPurchase();
  }

  private void convertStoreItem(String iapId) {
    JSONObject storeItem = storeItems.get(iapId);
    if (storeItem == null) {
      Logger.error(TAG, "iap " + iapId + " not configured");
      return;
    }
    //String type, String sku, String price, long priceAmountMicros, String currencyCode, String title, String description, double usd
    String type = "inapp";
    String price = "US$" + storeItem.optString("usd");
    long priceAmountMicros = 0L;
    String currencyCode = "USD";
    String title = storeItem.has("name") ? storeItem.optString("name") : iapId;
    double usd = storeItem.optDouble("usd");
    SKUDetail skuDetail = new SKUDetail(type, iapId, price, priceAmountMicros, currencyCode, title, title, usd);

    skuDetailMap.put(iapId, skuDetail);
  }

  private VivoPayInfo createPayInfo(String cpOrder, String productName, String productPrice, String uid) {
    Map<String, String> params = new HashMap<>();
    params.put(Constants.PARAM_APP_ID, preferences.appId);
    params.put(Constants.PARAM_CP_ORDER_ID, cpOrder);
    params.put(Constants.PARAM_PRODUCT_NAME, productName);
    params.put(Constants.PARAM_PRODUCT_PRICE, productPrice);
    params.put(Constants.PARAM_EXT_INFO, preferences.extInfo);
    params.put(Constants.PARAM_PARTNER_OPEN_ID, uid);
    params.put(Constants.PARAM_NOTIFY_URL, preferences.notifiyUrl);

    String sign = VivoSignUtils.getVivoSign(params, preferences.appSecret);

    VivoRoleInfo roleInfo = new VivoRoleInfo();
    roleInfo.setServiceAreaName("Global");   //区服信息
    roleInfo.setServiceAreaId("001");       //区服id
    roleInfo.setRoleName(uid);             //角色名称
    roleInfo.setRoleId(uid);                 //角色id
    roleInfo.setRoleGrade("0");           //角色等级

    return new VivoPayInfo.Builder().setExtInfo(preferences.extInfo)
      .setAppId(preferences.appId).setNotifyUrl(preferences.notifiyUrl).setProductName(productName).setProductPrice(productPrice).setSign(sign)
      .setSignType(Constants.PAY_SIGN_TYPE)
      .setUid(uid).setTransNo(cpOrder).setVivoRoleInfo(roleInfo)
      .build();
  }

  private void logPurchase(@NonNull String orderId, @NonNull String itemId) {
    float revenue = 0.0f;
    String currencyCode = "USD";
    String contentType = "inapp";
    JSONObject storeItem = storeItems.get(itemId);
    if (storeItem != null && storeItem.has("usd")) {
      currencyCode = "USD";
      revenue = (float) storeItem.optDouble("usd", 0);
    }
    eventTracker.logPurchase(contentType, itemId, currencyCode, revenue);
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

            float revenue = 0.0f;
            String currencyCode = "USD";

            JSONObject storeItem = storeItems.get(itemid);
            if (storeItem != null) {
              currencyCode = "USD";
              revenue = (float) storeItem.optDouble("usd", 0);
              bundle.putString("currency", currencyCode);
              bundle.putDouble(EventParams.PARAM_REVENUE, revenue);
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
          return;
      }
    }
  }

  @Override
  public void buy(@NonNull String productId, @Nullable String developerPayload) {
    try {
      JSONObject productInfo = storeItems.get(productId);
      if (productInfo == null) {
        Logger.error(TAG, "Product Info not exists!");
        return;
      }
      if (developerPayload != null) {
        sp.edit().putString(productId, developerPayload).apply();
      } else {
        sp.edit().remove(productId).apply();
      }

      this.purchasingItemId = productId;
      this.purchasingOrderId = UUID.randomUUID().toString().replaceAll("-", "");
      //String cpOrder, String productName, String productPrice, String uid, String serviceName, String serviceId, String roleName, String roleid, String rolegrade
      String uid = AndroidSdk.getFirebaseUserId();
      VivoPayInfo payInfo = createPayInfo(purchasingOrderId, productInfo.optString("name", ""), productInfo.optString("usd"), uid);
      VivoUnionSDK.pay(IvySdk.getActivity(), payInfo, this);
    } catch (Throwable e) {
      Logger.error(TAG, "launchBillingFlow error", e);
    }
  }

  private void purchaseStateChange(@NonNull PurchaseState state, @NonNull String orderId, @NonNull String itemId) {
    String sku = itemId;
    long purchaseTime = System.currentTimeMillis();
    String developerPayload = sp.contains(sku) ? sp.getString(sku, "") : null;
    String serverDeveloperPayload = sp.contains(sku + "_server") ? sp.getString(sku + "_server", "") : null;
    if (serverDeveloperPayload != null) {
      developerPayload = serverDeveloperPayload;
    }

    int quantity = 0;

    PurchaseStateChangeData changeData = new PurchaseStateChangeData(orderId, state, sku, quantity, purchaseTime, developerPayload, false, orderId, false);
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

    for(String iapId : unQueriedIaps) {
      convertStoreItem(iapId);
    }

    onSkuDetailsListener.onReceived();
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


  private void processPurchase(@NonNull String orderId, @NonNull String itemId) {
    Logger.debug(TAG, "processPurchase >>> " + itemId);
    handleVerifiedPurchase(orderId, itemId, new PaymentVerifiedListener() {
      @Override
      public void onSuccess() {
        Logger.debug(TAG, "handleVerifiedPurchase for inapp onSuccess");
        purchaseStateChange(PurchaseState.PURCHASED, orderId, itemId);
      }

      @Override
      public void onFail(int errorCode) {
        Logger.debug(TAG, "handleVerifiedPurchase for inapp onFail, errorCode: " + errorCode);
        purchaseStateChange(PurchaseState.ERROR, orderId, itemId);
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
   */
  private void handleVerifiedPurchase(@NonNull String orderId, @NonNull String itemId, @NonNull PaymentVerifiedListener listener) {
    logPurchase(orderId, itemId);
    listener.onSuccess();
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
  public void onVivoPayResult(int status) {
    switch (status) {
      case VivoConstants.VIVO_PAY_SUCCESS:
//        mLogView.printD(Constants.TIPS_PAY_SUCCESS);
        processPurchase(this.purchasingOrderId, this.purchasingItemId);
        break;
      case VivoConstants.VIVO_PAY_FAILED:
//        mLogView.printE(Constants.TIPS_PAY_FAILURE);
        break;
      case VivoConstants.VIVO_PAY_INVALID_PARAM:
//        mLogView.printE(Constants.TIPS_PAY_INVALID_PARAM);
        break;
      case VivoConstants.VIVO_PAY_ERROR:
//        mLogView.printE(Constants.TIPS_PAY_ERROR);
        break;
      case VivoConstants.VIVO_PAY_OVER_TIME:
//        mLogView.printE(Constants.TIPS_PAY_TIMEOUT);
        break;
      case VivoConstants.VIVO_PAY_CANCEL:
//        mLogView.printW(Constants.TIPS_PAY_CANCEL);
        break;
    }
  }
}
