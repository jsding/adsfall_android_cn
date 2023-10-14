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
import com.ivy.billing.PaymentVerifiedListener;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.PurchaseStateChangeData;
import com.ivy.event.CommonEvents;
import com.ivy.event.EventBus;
import com.ivy.event.EventListener;
import com.ivy.networks.tracker.EventTracker;
import com.ivy.util.CommonUtil;
import com.ivy.util.Logger;
import com.samsung.android.sdk.iap.lib.helper.HelperDefine;
import com.samsung.android.sdk.iap.lib.helper.IapHelper;
import com.samsung.android.sdk.iap.lib.listener.OnConsumePurchasedItemsListener;
import com.samsung.android.sdk.iap.lib.listener.OnGetOwnedListListener;
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener;
import com.samsung.android.sdk.iap.lib.listener.OnPaymentListener;
import com.samsung.android.sdk.iap.lib.vo.ConsumeVo;
import com.samsung.android.sdk.iap.lib.vo.ErrorVo;
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo;
import com.samsung.android.sdk.iap.lib.vo.ProductVo;
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Keep
public class PurchaseManagerImpl implements PurchaseManager, EventListener, OnGetProductsDetailsListener, OnPaymentListener, OnConsumePurchasedItemsListener {
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

  private final Map<String, ProductVo> storeSkuDetailsMap = new HashMap<>();

  private Map<String, JSONObject> storeItems = new HashMap<>();


  private IapHelper mIapHelper = null;


  @Override
  public void init(@NonNull Context context, @NonNull EventBus eventBus, @NonNull EventTracker eventLogger) {
    this.eventBus = eventBus;
    this.preferences = BillingConfigurator.setUpBillingPreferences();

    try {
      this.sp = context.getSharedPreferences("pays", Context.MODE_PRIVATE);

      mIapHelper = IapHelper.getInstance(context.getApplicationContext());

      if (IvySdk.isDebugMode()) {
        mIapHelper.setOperationMode(HelperDefine.OperationMode.OPERATION_MODE_PRODUCTION);
      } else {
        mIapHelper.setOperationMode(HelperDefine.OperationMode.OPERATION_MODE_TEST);
      }

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
    mIapHelper.getProductsDetails(TextUtils.join(",", skus), this);

    queryPurchase();
  }

  private SKUDetail convertToSKUDetail(ProductVo skuDetails, double usd) {
    String type = skuDetails.getType();
    String sku = skuDetails.getItemId();
    String price = skuDetails.getItemPriceString();
    long priceAmountMicros = 0L;
    String currencyCode = skuDetails.getCurrencyCode();

    String title = skuDetails.getItemName();
    String description = skuDetails.getItemDesc();
    return new SKUDetail(type, sku, price, priceAmountMicros, currencyCode, title, description, usd);
  }

  private void logPurchase(@NonNull PurchaseVo purchase) {
    String orderId = purchase.getOrderId();
    if ("".equals(orderId)) {
      return;
    }
    String itemid = purchase.getItemId();
    if (!storeSkuDetailsMap.containsKey(itemid)) {
      return;
    }

    ProductVo skuDetails = storeSkuDetailsMap.get(itemid);
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
            bundle.putString(EventParams.PARAM_PROVIDER, "google");
            if (currentBillItemId != null) {
              bundle.putString("reason", currentBillItemId);
            }
            bundle.putString(EventParams.PARAM_ITEMID, itemid);
            bundle.putString(EventParams.PARAM_ORDERID, data.getOrderId());

            ProductVo skuDetails = storeSkuDetailsMap.get(itemid);
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
      if (mIapHelper == null) {
        return;
      }

      if (developerPayload != null) {
        sp.edit().putString(productId, developerPayload).apply();
      } else {
        sp.edit().remove(productId).apply();
      }

      IvySdk.runOnUiThreadCustom(new Runnable() {
        @Override
        public void run() {
          mIapHelper.startPayment(productId, "", PurchaseManagerImpl.this);
        }
      });
    } catch (Throwable e) {
      Logger.error(TAG, "launchBillingFlow error", e);
    }
  }

  private void purchaseStateChange(@NonNull PurchaseState state, @NonNull OwnedProductVo purchase) {
    String sku = purchase.getItemId();
    long purchaseTime = Long.parseLong(purchase.getPurchaseDate());
    String orderID = "".equals(purchase.getPaymentId()) ? sku + "_" + purchaseTime : purchase.getPaymentId();
    String developerPayload = sp.contains(sku) ? sp.getString(sku, "") : null;
    String serverDeveloperPayload = sp.contains(sku + "_server") ? sp.getString(sku + "_server", "") : null;
    if (serverDeveloperPayload != null) {
      developerPayload = serverDeveloperPayload;
    }

    int quantity = 0;

    PurchaseStateChangeData changeData = new PurchaseStateChangeData(orderID, state, sku, quantity, purchaseTime, developerPayload, false, purchase.getPurchaseId(), false);
    changeData.setPackageName(AndroidSdk.getConfig(AndroidSdk.CONFIG_KEY_PACKAGE_NAME));

    SKUDetail skuDetail = skuDetailMap.get(sku);
    if (skuDetail != null) {
      changeData.setSkuJson(skuDetail.toString());
    }

    fireEvent(CommonEvents.BILLING_PURCHASE_STATE_CHANGE, changeData);
  }

  private void purchaseStateChange(@NonNull PurchaseState state, @NonNull PurchaseVo purchase) {
    String sku = purchase.getItemId();
    long purchaseTime = Long.parseLong(purchase.getPurchaseDate());
    String orderID = "".equals(purchase.getOrderId()) ? sku + "_" + purchaseTime : purchase.getOrderId();
    String developerPayload = sp.contains(sku) ? sp.getString(sku, "") : null;
    String serverDeveloperPayload = sp.contains(sku + "_server") ? sp.getString(sku + "_server", "") : null;
    if (serverDeveloperPayload != null) {
      developerPayload = serverDeveloperPayload;
    }

    int quantity = 0;

    PurchaseStateChangeData changeData = new PurchaseStateChangeData(orderID, state, sku, quantity, purchaseTime, developerPayload, false, purchase.getPurchaseId(), false);
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

    mIapHelper.getProductsDetails(TextUtils.join(",", unQueriedIaps), this);
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

  private boolean verifyPurchase(@NonNull PurchaseVo purchase) {
    String base64PublicKey = preferences.publicKey;

    if (base64PublicKey == null || "".equals(base64PublicKey)) {
      Logger.warning(TAG, "IAP public key is not configured, will NOT verify the purchase");
      return true;
    }

//    String signature = purchase.getSignature();
//    boolean verified = Security.verifyPurchase(base64PublicKey, purchaseData, signature);
//    if (!verified) {
//      Logger.error(TAG, "purchase verified failed");
//      Logger.debug(TAG, "OrderID: " + purchase.getOrderId());
//      Logger.debug(TAG, "Signature: " + purchase.getSignature());
//      Logger.debug(TAG, "PurchaseData: " + purchase.getOriginalJson());
//    } else {
//      Logger.debug(TAG, "Purchase Verified");
//    }
    return true;
  }

  /**
   * TODO
   *
   * @param purchase
   */
  private void processPurchase(@NonNull OwnedProductVo purchase) {
    String sku = purchase.getItemId();
    Logger.debug(TAG, "processPurchase >>> " + sku);

    purchaseStateChange(PurchaseState.PURCHASED, purchase);
  }

  private void processPurchase(@NonNull PurchaseVo purchase) {
    String sku = purchase.getItemId();
    Logger.debug(TAG, "processPurchase >>> " + sku);
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
  private void handleVerifiedPurchase(@NonNull PurchaseVo purchase, @NonNull PaymentVerifiedListener listener) {
    final String sku = purchase.getItemId();
    final String orderId = purchase.getOrderId();

    if (!verifyPurchase(purchase)) {
      listener.onFail(ERROR_SIGNED_FAILED);
      return;
    }

    logPurchase(purchase);
    listener.onSuccess();
  }

  @Override
  public void setBillItemId(String itemId) {
    currentBillItemId = itemId;
  }

  @Override
  public void consumePurchase(@NonNull String purchaseToken, @NonNull OrderConsumeListener orderConsumeListener) {
    Logger.debug(TAG, "Consume Purchase >>> " + purchaseToken);
    mIapHelper.consumePurchasedItems(purchaseToken, new OnConsumePurchasedItemsListener() {
      @Override
      public void onConsumePurchasedItems(ErrorVo _errorVO, ArrayList<ConsumeVo> _consumeList) {
        if (_errorVO == null) {
          orderConsumeListener.onConsumeSuccess(purchaseToken);
        } else {
          orderConsumeListener.onConsumeError(purchaseToken, String.valueOf(_errorVO.getErrorCode()), _errorVO.getErrorString());
        }
      }
    });
  }

  private void handleUnConsumedPurchases(boolean subs) {
    mIapHelper.getOwnedList(HelperDefine.PRODUCT_TYPE_ALL, new OnGetOwnedListListener() {
      @Override
      public void onGetOwnedProducts(ErrorVo _errorVO, ArrayList<OwnedProductVo> _ownedList) {
        if (_errorVO != null) {
          Logger.debug(TAG, "queryPurchases failed: " + _errorVO.getErrorCode() + ", " + _errorVO.getErrorString());
          return;
        }
        if (_ownedList != null && _ownedList.size() > 0) {
          for (OwnedProductVo purchase : _ownedList) {
            Logger.debug(TAG, "Handle purchased purchase: " + purchase);
            processPurchase(purchase);
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
    mIapHelper.dispose();
  }

  @Override
  public void onGetProducts(ErrorVo _errorVO, ArrayList<ProductVo> _productList) {
    if (_errorVO != null) {
      Logger.error(TAG, "Query inventory failed, errorCode: " + _errorVO.getErrorCode() + ", " + _errorVO.getErrorString());
      return;
    }

    Logger.debug(TAG, "Query in app inventory was successful");
    for (ProductVo details : _productList) {
      String productId = details.getItemId();
      Logger.debug(TAG, "Add product: " + productId);
      JSONObject storeItem = storeItems.get(productId);
      if (storeItem != null) {
        double price = storeItem.optDouble("usd", 0);
        skuDetailMap.put(details.getItemId(), convertToSKUDetail(details, price));
        storeSkuDetailsMap.put(details.getItemId(), details);
      } else {
        Logger.error(TAG, "SKU " + productId + " not configured in default.json");
      }
    }
  }

  @Override
  public void onConsumePurchasedItems(ErrorVo _errorVO, ArrayList<ConsumeVo> _consumeList) {
    if (_errorVO != null) {
      Logger.error(TAG, "onConsumePurchasedItems error: " + _errorVO.getErrorCode() + ", " + _errorVO.getErrorString());
      return;
    }
    Logger.debug(TAG, "onConsumePurchasedItems success");
  }

  @Override
  public void onPayment(ErrorVo _errorVO, PurchaseVo _purchaseVO) {
    // Logic from onActivityResult should be moved here.
    if (_errorVO != null) {
      Logger.error(TAG, "onPurchasesUpdated, purchases is empty, responseCode " + _errorVO.getErrorCode() + ", " + _errorVO.getErrorString());
      return;
    }
    if (_purchaseVO != null) {
      processPurchase(_purchaseVO);
    }
  }
}
