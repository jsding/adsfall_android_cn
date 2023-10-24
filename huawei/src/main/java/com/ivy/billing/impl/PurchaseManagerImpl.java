package com.ivy.billing.impl;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.metrics.Event;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.android.client.AndroidSdk;
import com.android.client.OnSkuDetailsListener;
import com.android.client.OrderConsumeListener;
import com.android.client.SKUDetail;
import com.example.iapdemo.callback.IsEnvReadyCallback;
import com.example.iapdemo.callback.ProductInfoCallback;
import com.example.iapdemo.callback.PurchaseIntentResultCallback;
import com.example.iapdemo.callback.QueryPurchasesCallback;
import com.example.iapdemo.common.CipherUtil;
import com.example.iapdemo.common.Constants;
import com.example.iapdemo.common.ExceptionHandle;
import com.example.iapdemo.common.IapRequestHelper;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.IsSandboxActivatedReq;
import com.huawei.hms.iap.entity.IsSandboxActivatedResult;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.support.api.client.Status;
import com.ivy.IvySdk;
import com.ivy.IvyUtils;
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
import com.ivy.networks.util.Util;
import com.ivy.util.Logger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PurchaseManagerImpl implements PurchaseManager, EventListener {
  private static final String TAG = "Purchase";

  private EventBus eventBus;
  private EventTracker eventTracker;

  private BillingPreferences preferences;

  private SharedPreferences sp;

  private final Map<String, SKUDetail> skuDetailMap = new HashMap<>();

  private Map<String, JSONObject> storeItems = new HashMap<>();

  private IapClient mIapClient;


  private void deliverProduct(final String inAppPurchaseDataStr, final String inAppPurchaseDataSignature, boolean autoconsume) {
    try {
      InAppPurchaseData receipt = new InAppPurchaseData(inAppPurchaseDataStr);

      Logger.debug(TAG, "deliverProduct >>> " + receipt);

      if (receipt.getPurchaseState() != InAppPurchaseData.PurchaseState.PURCHASED) {
        Logger.warning(TAG, "purchase not in purchased status");
        return;
      }

      String publicKey = preferences.publicKey;
      if (!"".equals(publicKey) && !CipherUtil.doCheck(inAppPurchaseDataStr, inAppPurchaseDataSignature, publicKey)) {
        Logger.warning(TAG, "receipt signature error");
        return;
      }

      purchaseStateChange(PurchaseState.PURCHASED, receipt, false);

      if (autoconsume) {
        consumePurchase(receipt.getPurchaseToken(), new OrderConsumeListener() {
          @Override
          public void onConsumeSuccess(@NonNull String purchaseToken) {
            String developPayloadKey = receipt.getDeveloperPayload();
            Logger.debug(TAG, "consumePurchase success, remove payload: " + developPayloadKey);
            if (sp.contains(developPayloadKey)) {
              sp.edit().remove(developPayloadKey).apply();
            }
          }

          @Override
          public void onConsumeError(@NonNull String purchaseToken, String errorCode, String errorMessage) {
            Logger.error(TAG, "consumePurchase exception" + errorCode + errorMessage);
          }
        });
      }

    } catch (final Throwable e) {
      Logger.error(TAG, "deliverProduct exception", e);
    }
  }

  public void handleConsumablePurchase(final @NonNull PurchaseResultInfo purchaseResultInfo) {
    String inAppPurchaseDataStr = purchaseResultInfo.getInAppPurchaseData();
    String inAppPurchaseDataSignature = purchaseResultInfo.getInAppDataSignature();
    deliverProduct(inAppPurchaseDataStr, inAppPurchaseDataSignature, false);
  }

  private void purchaseStateChange(PurchaseState state, @NonNull InAppPurchaseData receipt, boolean justRestore) {
    purchaseStateChange(state, receipt, justRestore, false, false);
  }

  private void purchaseStateChange(PurchaseState state, @NonNull InAppPurchaseData receipt, boolean justRestore, boolean verified, boolean force) {
    String sku = receipt.getProductId();
    long purchaseTime = receipt.getPurchaseTime();
    String orderID = receipt.getOrderID();
    String developerPayloadKey = receipt.getDeveloperPayload();
    String developerPayload = "";
    if (sp.contains(developerPayloadKey)) {
      developerPayload = sp.getString(developerPayloadKey, "");
    }

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

    PurchaseStateChangeData changeData = new PurchaseStateChangeData(orderID, state, sku, quantity, purchaseTime, developerPayload, justRestore, receipt.getPurchaseToken(), isAutoRenewing);
    changeData.setSignature(receipt.getOrderID());
    changeData.setReceipt(receipt.getOrderID());

    changeData.setPackageName(AndroidSdk.getConfig(AndroidSdk.CONFIG_KEY_PACKAGE_NAME));

    fireEvent(CommonEvents.BILLING_PURCHASE_STATE_CHANGE, changeData);

    if (sku != null && isConsumable(sku) && !"".equals(orderID)) {
      if (PurchaseState.PURCHASED.equals(state)) {
        sp.edit().putString(orderID + "_r_send", "send").apply();
      }
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

    eventBus.addListener(CommonEvents.BILLING_PURCHASE_STATE_CHANGE, this);

    mIapClient = Iap.getIapClient(context);
    IapRequestHelper.isEnvReady(mIapClient, new IsEnvReadyCallback() {
      @Override
      public void onSuccess() {
        Logger.debug(TAG, "Iap client ready");
        Task<IsSandboxActivatedResult> task = mIapClient.isSandboxActivated(new IsSandboxActivatedReq());
        task.addOnSuccessListener(new OnSuccessListener<IsSandboxActivatedResult>() {
          @Override
          public void onSuccess(IsSandboxActivatedResult result) {
            Logger.debug(TAG, "isSandboxActivated success");
          }
        }).addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(Exception e) {
            Logger.error("IAP", "isSandboxActivated fail", e);
            if (e instanceof IapApiException) {
              IapApiException apiException = (IapApiException) e;
              int returnCode = apiException.getStatusCode();
            } else {
              // 其他外部错误
            }
          }
        });

        querySKUDetails(autoLoadIaps, new OnSkuDetailsListener() {
          @Override
          public void onReceived() {
            Logger.debug(TAG, "receive product Info");
            queryUnconsumedPurchases();
          }
        });

        Logger.debug(TAG, "Fire Payment System Ready");
        fireEvent(CommonEvents.BILLING_BECOMES_AVAILABLE, new ArrayList<>());
      }

      @Override
      public void onFail(Exception e) {
        Logger.error(TAG, "Iap client exception", e);
      }
    });
  }


  private void querySkuAndPurchase(@NonNull String sku) {
    Set<String> skuSets = new HashSet<>();
    skuSets.add(sku);

    // 触发PurchasingListener.onProductDataResponse()
//    PurchasingService.getProductData(skuSets);
  }

  private void queryInventoryAsync(List<String> iaps, List<String> subs) {
  }

  private List<String> autoLoadIaps = null;

  public void checkBillingSupported(@NonNull final List<String> skus) {
    this.autoLoadIaps = new ArrayList<>();

    // 触发PurchasingListener.onProductDataResponse()
    for (String sku : storeItems.keySet()) {
      JSONObject skuInfo = storeItems.get(sku);
      if (skuInfo != null && skuInfo.optInt("autoload") == 1) {
        autoLoadIaps.add(sku);
      }
    }
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
            bundle.putString(EventParams.PARAM_PROVIDER, "appgallery");
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
    IapRequestHelper.obtainOwnedPurchases(mIapClient, IapClient.PriceType.IN_APP_CONSUMABLE, new QueryPurchasesCallback() {
      @Override
      public void onSuccess(OwnedPurchasesResult result) {
        if (result == null) {
          Logger.debug(TAG, "obtainOwnedPurchases result is null");
          return;
        }
        Logger.debug(TAG, "obtainOwnedPurchases, success");
        if (result.getInAppPurchaseDataList() != null) {
          List<String> inAppPurchaseDataList = result.getInAppPurchaseDataList();
          List<String> inAppSignature = result.getInAppSignature();
          for (int i = 0; i < inAppPurchaseDataList.size(); i++) {
            final String inAppPurchaseData = inAppPurchaseDataList.get(i);
            final String inAppPurchaseDataSignature = inAppSignature.get(i);

            deliverProduct(inAppPurchaseData, inAppPurchaseDataSignature, true);
          }
        }
      }

      @Override
      public void onFail(Exception e) {
        Logger.error(TAG, "obtainOwnedPurchases, type=" + IapClient.PriceType.IN_APP_CONSUMABLE + ", " + e.getMessage());
      }
    });
  }

  @Override
  public void buy(@NonNull String iapId, String developerPayload) {
    try {
      Activity activity = IvySdk.getActivity();
      showProgressBar(activity);

      String saveKey = iapId+"_"+ (developerPayload != null ? IvyUtils.md5(developerPayload) : "");
      if (developerPayload != null) {
        sp.edit().putString(saveKey, developerPayload).apply();
      } else {
        sp.edit().remove(saveKey).apply();
      }

      IapRequestHelper.createPurchaseIntent(mIapClient, iapId, IapClient.PriceType.IN_APP_CONSUMABLE, saveKey, new PurchaseIntentResultCallback() {
        @Override
        public void onSuccess(PurchaseIntentResult result) {
          hideProgressBar(activity);
          if (result == null) {
            Logger.debug(TAG, "result is null");
            return;
          }
          Status status = result.getStatus();
          if (status == null) {
            Logger.debug(TAG, "status is null");
            return;
          }
          Logger.debug(TAG, "createPurchaseIntent success >> " + status.getStatusMessage());

          IvySdk.runOnUiThreadCustom(new Runnable() {
            @Override
            public void run() {
              Logger.debug(TAG, "pull up the page to complete the payment process >> ");

              // you should pull up the page to complete the payment process.
              IapRequestHelper.startResolutionForResult(activity, status, Constants.REQ_CODE_BUY);
            }
          });
        }

        @Override
        public void onFail(Exception e) {
          hideProgressBar(activity);
          int errorCode = ExceptionHandle.handle(activity, e);
          if (errorCode != ExceptionHandle.SOLVED) {
            Log.i(TAG, "createPurchaseIntent, returnCode: " + errorCode);
            if (errorCode == OrderStatusCode.ORDER_PRODUCT_OWNED) {
              queryUnconsumedPurchases();
            } else {
              Logger.error(TAG, "Payment failed: Error Code: " + errorCode);
            }
          } else {
            Logger.error("Payment failed, Error Code: " + errorCode);
          }
        }
      });
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

  private void onProductDataResponse(ProductInfoResult productInfoResult, @NonNull OnSkuDetailsListener listener) {

    List<ProductInfo> productInfos = productInfoResult.getProductInfoList();
    Logger.debug(TAG, "onProductDataResponse received " + productInfos.size());

    for (ProductInfo productInfo : productInfos) {
      String sku = productInfo.getProductId();
      double usd = 0.0f;
      JSONObject storeItem = storeItems.get(sku);
      if (storeItem != null) {
        usd = storeItem.optDouble("usd", 0.0f);

      }

      String type = "inapp";

      String price = productInfo.getPrice();
      long priceAmountMicros = 0L;
      String priceCurrencyCode = "USD";
      String title = productInfo.getProductName();
      String description = productInfo.getProductDesc();

      SKUDetail skuDetail = new SKUDetail(type, sku, price, priceAmountMicros, priceCurrencyCode, title, description, usd);
      skuDetailMap.put(sku, skuDetail);

      String jsonData = skuDetail.toJson().toString();
      IvySdk.mmSetStringValue("_sku_cache_" + storeItem.optInt("billId"), jsonData);

      EventBus.getInstance().fireEvent(CommonEvents.BILLING_PAYMENT_DATA, skuDetail);

      Logger.debug(TAG, String.format("产品：%s\n 类型：%s\n SKU：%s\n 价格：%s\n 描述：%s\n", productInfo.getProductName(), productInfo.getPriceType(), productInfo.getProductId(), productInfo.getPrice(), productInfo.getProductDesc()));
    }

    listener.onReceived();
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
      Logger.warning(TAG, "all products are ready");
      onSkuDetailsListener.onReceived();
      return;
    }

    Logger.warning(TAG, "fire query products...");

    IapRequestHelper.obtainProductInfo(mIapClient, unQueriedIaps, IapClient.PriceType.IN_APP_CONSUMABLE, new ProductInfoCallback() {
      @Override
      public void onSuccess(ProductInfoResult result) {
        Logger.debug(TAG, "obtainProductInfo success");
        onProductDataResponse(result, onSkuDetailsListener);
      }

      @Override
      public void onFail(Exception e) {
        Logger.error(TAG, "obtainProductInfo exception", e);
        onSkuDetailsListener.onReceived();
      }
    });
  }

  private String currentBuyingBillingItem = null;

  @Override
  public void setBillItemId(String itemId) {
    currentBuyingBillingItem = itemId;
  }

  @Override
  public void consumePurchase(@NonNull String purchaseToken, @NonNull OrderConsumeListener orderConsumeListener) {
    Logger.debug(TAG, "consumePurchase : " + purchaseToken);
    IapRequestHelper.consumeOwnedPurchase(mIapClient, purchaseToken);
  }

  @Override
  public void queryPurchase() {
    queryUnconsumedPurchases();
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

  @Override
  public void onActivityResult(final int requestCode, int resultCode, Intent data) {
    Logger.debug(TAG, "Purchase onActivityResult called");
    if (requestCode == Constants.REQ_CODE_BUY || requestCode == Constants.REQ_CODE_BUYWITHPRICE) {
      if (data == null) {
        Logger.error(TAG, "data is null");
        return;
      }
      Activity activity = IvySdk.getActivity();
      if (activity == null) {
        Logger.error(TAG, "Activity disposed");
        return;
      }
      PurchaseResultInfo purchaseResultInfo = Iap.getIapClient(activity).parsePurchaseResultInfoFromIntent(data);
      int returnCode = purchaseResultInfo.getReturnCode();
      Logger.debug(TAG, "purchase Result: code: " + returnCode + ", msg: " + purchaseResultInfo.getErrMsg());
      switch (returnCode) {
        case OrderStatusCode.ORDER_STATE_CANCEL:
          Logger.debug(TAG, "Order Cancelled");
          break;
        case OrderStatusCode.ORDER_STATE_FAILED:
        case OrderStatusCode.ORDER_STATE_NET_ERROR:
          Logger.debug(TAG, "Order failed: ");
          queryUnconsumedPurchases();
          break;
        case OrderStatusCode.ORDER_PRODUCT_OWNED:
        case OrderStatusCode.ORDER_STATE_SUCCESS:
          Logger.debug(TAG, "Order Success");
          handleConsumablePurchase(purchaseResultInfo);
          break;
        default:
          break;
      }
    }
  }

  @Override
  public void onResume(Activity activity) {
    hideProgressBar(activity);
  }
}
