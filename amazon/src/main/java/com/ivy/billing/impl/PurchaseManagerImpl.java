package com.ivy.billing.impl;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.amazon.device.drm.LicensingService;
import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.RequestId;
import com.amazon.device.iap.model.UserData;
import com.amazon.device.iap.model.UserDataResponse;
import com.android.client.AndroidSdk;
import com.android.client.OnSkuDetailsListener;
import com.android.client.OrderConsumeListener;
import com.android.client.SKUDetail;
import com.ivy.IvySdk;
import com.ivy.ads.events.EventID;
import com.ivy.ads.events.EventParams;
import com.ivy.billing.BillingConfigurator;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.PurchaseStateChangeData;
import com.ivy.event.CommonEvents;
import com.ivy.event.EventBus;
import com.ivy.event.EventListener;
import com.ivy.networks.tracker.EventTracker;
import com.ivy.networks.ui.dialog.ImmersiveDialog;
import com.ivy.util.Logger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PurchaseManagerImpl implements PurchaseManager, EventListener, PurchasingListener {
  private static final String TAG = "Purchase";

  private EventBus eventBus;
  private EventTracker eventTracker;

  private BillingPreferences preferences;

  private SharedPreferences sp;

  private final Map<String, SKUDetail> skuDetailMap = new HashMap<>();

  private Map<String, JSONObject> storeItems = new HashMap<>();

  @Override
  public void onUserDataResponse(UserDataResponse response) {
    Logger.debug(TAG, "onUserDataResponse " + response.toString());
    final UserDataResponse.RequestStatus status = response.getRequestStatus();
    switch (status) {
      case SUCCESSFUL:
        String currentUserId = response.getUserData().getUserId();
        String currentMarketplace = response.getUserData().getMarketplace();
        break;
      case FAILED:
      case NOT_SUPPORTED:
        break;
    }
  }

  @Override
  public void onProductDataResponse(ProductDataResponse response) {
    Logger.debug(TAG, "onProductDataResponse received " + response);
    if (response.getRequestStatus() != ProductDataResponse.RequestStatus.SUCCESSFUL) {
      Logger.error(TAG, "onProductDataResponse error with status ");
      return;
    }

    for (String s : response.getUnavailableSkus()) {
      Logger.error(TAG, "不可用SKU：" + s);
    }

    Map<String, Product> products = response.getProductData();
    for (String key : products.keySet()) {
      Product product = products.get(key);
      if (product == null) {
        continue;
      }
      double usd = 0.0f;
      JSONObject storeItem = storeItems.get(key);
      if (storeItem != null) {
        usd = storeItem.optDouble("usd", 0.0f);
      }

      // String type, String sku, String price, long priceAmountMicros, String currencyCode, String title, String description
      String type = product.getProductType().toString();
      String sku  = product.getSku();

      String price = product.getPrice();
      long priceAmountMicros = 0L;
      String priceCurrencyCode = "USD";

      String title  = product.getTitle();
      String description = product.getDescription();

      SKUDetail skuDetail = new SKUDetail(type, sku, price, priceAmountMicros, priceCurrencyCode, title, description, usd);
      skuDetailMap.put(key, skuDetail);

      Logger.debug(TAG, String.format("产品：%s\n 类型：%s\n SKU：%s\n 价格：%s\n 描述：%s\n", product.getTitle(), product.getProductType(), product.getSku(), product.getPrice(), product.getDescription()));
    }

    if (this.currentSkuDetailsListener != null) {
      this.currentSkuDetailsListener.onReceived();
    }
  }

  @Override
  public void onPurchaseResponse(PurchaseResponse purchaseResponse) {
    Logger.debug(TAG, "onPurchaseResponse " + purchaseResponse);
    hideProgressBar(IvySdk.getActivity());
    switch (purchaseResponse.getRequestStatus()) {
      case SUCCESSFUL:
      case ALREADY_PURCHASED:
        handleConsumablePurchase(purchaseResponse.getReceipt(), purchaseResponse.getUserData());
        break;
      case FAILED:
        IvySdk.showMessageDialog("Failed", "We are not able to complete your purchase now. Please try again later.");
        break;
      case INVALID_SKU:
        IvySdk.showMessageDialog("Invalid Product", "The product was not available! ");
        break;
      case PENDING:
        break;
      case NOT_SUPPORTED:
        IvySdk.showMessageDialog("Not Supported", "This purchase feature was not supported! ");
        break;
    }
  }

  public void handleConsumablePurchase(final @NonNull Receipt receipt, final UserData userData) {
    try {

      if (receipt.isCanceled()) {
        // revokeConsumablePurchase(receipt, userData);
      } else {
        // 我们强烈建议您在服务器端验证收据
//        if (!verifyReceiptFromYourService(receipt.getReceiptId(), userData)) {
//          // 如果无法验证购买，
//          // 请向客户显示相关的错误消息。
//          mainActivity.showMessage("无法验证购买，请稍后重试。");
//          return;
//        }
//        if (receiptAlreadyFulfilled(receipt.getReceiptId(), userData)) {
        // 如果之前已履行收据，则只需再次通知亚马逊
        // 应用商店收据已履行。
//          PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
//          return;
//        }

        // grantConsumablePurchase(receipt, userData);
        purchaseStateChange(PurchaseState.PURCHASED, receipt, false);


        PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
      }
    } catch (final Throwable e) {
      Logger.error(TAG, "handleConsumablePurchase exception", e);
    }
  }

  private void purchaseStateChange(PurchaseState state, @NonNull Receipt receipt, boolean justRestore) {
    purchaseStateChange(state, receipt, justRestore, false, false);
  }

  private void purchaseStateChange(PurchaseState state, @NonNull Receipt receipt, boolean justRestore, boolean verified, boolean force) {
    String sku = receipt.getSku();
    long purchaseTime = receipt.getPurchaseDate().getTime();
    String orderID = receipt.getReceiptId();
    String developerPayload = sp.contains(sku) ? sp.getString(sku, "") : null;
    String serverDeveloperPayload = sp.contains(sku + "_server") ? sp.getString(sku + "_server", "") : null;
    if (serverDeveloperPayload != null) {
      developerPayload = serverDeveloperPayload;
    }

    if (sku != null && isConsumable(sku) && !"".equals(orderID)) {
      if (PurchaseState.PURCHASED.equals(state)) {
        if (sp.contains(orderID + "_r_send")) {
          Logger.debug(TAG, "orderId already filled");
          return;
        }
      }
    }

    boolean isAutoRenewing = false;
    int quantity = 0;

    PurchaseStateChangeData changeData = new PurchaseStateChangeData(orderID, state, sku, quantity, purchaseTime, developerPayload, justRestore, receipt != null ? receipt.getReceiptId() : null, isAutoRenewing);
    changeData.setSignature(receipt.getReceiptId());
    changeData.setReceipt(receipt.getReceiptId());
    changeData.setPackageName(AndroidSdk.getConfig(AndroidSdk.CONFIG_KEY_PACKAGE_NAME));

    fireEvent(CommonEvents.BILLING_PURCHASE_STATE_CHANGE, changeData);

    if (sku != null && isConsumable(sku) && !"".equals(orderID)) {
      if (PurchaseState.PURCHASED.equals(state)) {
        sp.edit().putString(orderID + "_r_send", "send").apply();
      }
    }
  }

  @Override
  public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse response) {
    Logger.debug(TAG, "onPurchaseUpdatesResponse " + response);
    switch (response.getRequestStatus()) {
      case SUCCESSFUL:
        for (final Receipt receipt : response.getReceipts()) {
          Logger.debug(TAG, "Process receipt " + receipt);
          handleConsumablePurchase(receipt, response.getUserData());
        }
        if (response.hasMore()) {
          PurchasingService.getPurchaseUpdates(false);
        }
        break;
      case FAILED:
        break;
    }

  }

  public PurchaseManagerImpl() {

  }

  @Override
  public void init(@NonNull Context context, @NonNull EventBus eventBus, @NonNull EventTracker eventLogger) {
    this.eventBus = eventBus;
    this.eventTracker = eventLogger;

    this.preferences = BillingConfigurator.setUpBillingPreferences();

    this.sp = context.getSharedPreferences("pays", Context.MODE_PRIVATE);
    PurchasingService.registerListener(context, this);
    //启用待定购买
    PurchasingService.enablePendingPurchases();

    eventBus.addListener(CommonEvents.BILLING_PURCHASE_STATE_CHANGE, this);

    Logger.debug(TAG, "Appstore SDK Mode: " + LicensingService.getAppstoreSDKMode()); //检查应用是否处于测试模式
  }


  private void querySkuAndPurchase(@NonNull String sku) {
    Set<String> skuSets = new HashSet<>();
    skuSets.add(sku);

    // 触发PurchasingListener.onProductDataResponse()
    PurchasingService.getProductData(skuSets);
  }

  private void queryInventoryAsync(List<String> iaps, List<String> subs) {
  }

  public void checkBillingSupported(@NonNull final List<String> skus) {
    Set<String> skuSets = new HashSet<>();

    // 触发PurchasingListener.onProductDataResponse()
    for (String sku : storeItems.keySet()) {
      JSONObject skuInfo = storeItems.get(sku);
      if (skuInfo != null && skuInfo.optInt("autoload") == 1) {
        skuSets.add(sku);
      }
    }
    PurchasingService.getProductData(skuSets);
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
            bundle.putString(EventParams.PARAM_PROVIDER, "amazon");
            bundle.putString(EventParams.PARAM_ITEMID, itemid);
            bundle.putString(EventParams.PARAM_ORDERID, data.getOrderId());

            float revenue = 0.0f;
            if (storeItems != null) {
              JSONObject storeItem = storeItems.get(itemid);
              if (storeItem != null) {
                revenue = (float) storeItem.optDouble("usd", 0);
              }
              bundle.putString("currency", "USD");
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

            if (currentBuyingBillingItem != null) {
              bundle.putString(EventParams.PARAM_REASON, currentBuyingBillingItem);
            }

            this.eventTracker.logEvent(EventID.IAP_PURCHASED, bundle);
            eventTracker.logPurchase("iap", itemid, "USD", revenue);

            sp.edit()
              .putInt("total_orders", totalOrders)
              .putFloat("total_revenue", totalPaid)
              .putBoolean(orderId + "_logged", true).apply();
            return;
          }
          return;
        case CANCELED:
          Bundle bundle = new Bundle();
          bundle.putString(EventParams.PARAM_PROVIDER, "amazon");
          bundle.putString(EventParams.PARAM_ITEMID, itemid);
          this.eventTracker.logEvent("iap_cancel", bundle);
          return;
        default:
          return;
      }
    }
    Logger.error(TAG, "Unknown eventId=" + eventId);
  }


  private void handleUnConsumedPurchases(boolean subs) {
    Logger.debug(TAG, "handleUnConsumedPurchases, subs: " + subs);

  }

  @Override
  public void buy(@NonNull String iapId, String developerPayload) {
    try {

      showProgressBar(IvySdk.getActivity());
      final RequestId requestId = PurchasingService.purchase(iapId);
      Logger.debug(TAG, "onBuyOrangeClick: requestId (" + requestId + ")");

      if (developerPayload != null) {
        sp.edit().putString(iapId, developerPayload).apply();
      } else {
        sp.edit().remove(iapId).apply();
      }

      if (!skuDetailMap.containsKey(iapId)) {
        Logger.debug(TAG, "iapId " + iapId + " not preload, we try to load and start buy process");
        querySkuAndPurchase(iapId);
      }
    } catch (Throwable e) {
      Logger.error(TAG, "launchBillingFlow error", e);
    }
  }


  @Override
  public void setStoreItems(Map<String, JSONObject> storeItems) {
    this.storeItems = storeItems;
  }

  private boolean isConsumable(String productId) {
    JSONObject item = this.storeItems.get(productId);
    if (item != null) {
      if (item.optInt("repeat") == 0) {
        return false;
      }
    }
    return true;
  }

  private OnSkuDetailsListener currentSkuDetailsListener = null;
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

    this.currentSkuDetailsListener = onSkuDetailsListener;
    Set<String> skuSets = new HashSet<>(unQueriedIaps);

    // 触发PurchasingListener.onProductDataResponse()
    PurchasingService.getProductData(skuSets);
  }

  private String currentBuyingBillingItem = null;

  @Override
  public void setBillItemId(String itemId) {
    currentBuyingBillingItem = itemId;
  }

  @Override
  public void consumePurchase(@NonNull String purchaseToken, @NonNull OrderConsumeListener orderConsumeListener) {
    Logger.debug(TAG, "consumePurchase : " + purchaseToken);
    PurchasingService.notifyFulfillment(purchaseToken, FulfillmentResult.FULFILLED);
  }

  @Override
  public void queryPurchase() {

  }

  @Override
  public void onDestroy() {
  }

  @Override
  public JSONObject getStoreItem(String productId) {
    return storeItems.get(productId);
  }

  @Override
  public SKUDetail getSKUDetail(String iapId) {
    return this.skuDetailMap.get(iapId);
  }

  @Override
  public void queryPurchases(String productId) {
    if (isConsumable(productId)) {
      handleUnConsumedPurchases(false);
    } else {
      handleUnConsumedPurchases(true);
    }
  }

  @Override
  public void queryUnconsumedPurchases() {
    handleUnConsumedPurchases(false);
  }


  private void fireEvent(final int eventId, final Object eventData) {
    IvySdk.runOnUiThreadCustom(() -> PurchaseManagerImpl.this.eventBus.fireEvent(eventId, eventData));
  }


  private Dialog progress = null;

  public void hideProgressBar(Activity activity) {
    if (activity == null || activity.isFinishing()) {
      return;
    }
    activity.runOnUiThread(() -> {
      if (progress != null) {
        try {
          progress.dismiss();
          progress = null;
        } catch (Exception e) {
          // ignore
        }
      }
    });
  }

  public void showProgressBar(final Activity activity) {
    if (activity == null || activity.isFinishing()) {
      return;
    }
    activity.runOnUiThread(() -> {
      try {
        if (progress != null) {
          progress.dismiss();
        }
        progress = new ImmersiveDialog(activity);
        progress.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        ProgressBar progressBar = new ProgressBar(activity, null, 16842874);
        progress.requestWindowFeature(1);
        progress.setContentView(progressBar);
        progress.setOwnerActivity(activity);
        progress.show();
      } catch (Throwable e) {
        // ignore
      }
    });
  }

  public static final int ERROR_ORDER_FORMAT = 6;
  public static final int ERROR_SIGNED_FAILED = 1;
  public static final int ERROR_RESPONSE_NOT_SUCCESS = 2;
  public static final int ERROR_RESPONSE_EMPTY = 3;
  public static final int ERROR_RESPONSE_WRONG_STATUS = 4;
  public static final int ERROR_VERIFY_SERVER_HTTP_ERROR = 5;
  public static final int SOFT_PURCHASE_ERROR = 10;

  private void logVerifyEvent(String sku, String orderId, int status, int errorCode, String reason, long startTimes) {
    Bundle bundle = new Bundle();
    if (sku != null) {
      bundle.putString(EventParams.PARAM_ITEMID, sku);
    }
    if (orderId != null) {
      bundle.putString(EventParams.PARAM_ORDERID, orderId);
    }
    bundle.putInt(EventParams.PARAM_TIMES, (int) ((System.currentTimeMillis() - startTimes) / 1000));
    if (errorCode > 0) {
      bundle.putString(EventParams.PARAM_LABEL, String.valueOf(errorCode));
    }
    if (reason != null) {
      bundle.putString(EventParams.PARAM_REASON, reason);
    }
    if (status == 1) {
      eventTracker.logEvent("iap_verified", bundle);
    } else {
      eventTracker.logEvent("iap_verified_failed", bundle);
    }
  }


  public void setPayVerifyUrl(String verifyUrl) {
    Logger.debug(TAG, "Update verify URL >>> " + verifyUrl);
    if (preferences != null) {
      preferences.verifyUrl = verifyUrl;
    }
  }

  @Override
  public void onResume(Activity activity) {
    PurchasingService.getUserData();
    PurchasingService.getPurchaseUpdates(false);

    hideProgressBar(activity);
  }
}
