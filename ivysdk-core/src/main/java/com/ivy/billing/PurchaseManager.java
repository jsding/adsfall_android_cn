package com.ivy.billing;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.client.OnSkuDetailsListener;
import com.android.client.OrderConsumeListener;
import com.android.client.SKUDetail;
import com.ivy.event.EventBus;
import com.ivy.networks.tracker.EventTracker;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public interface PurchaseManager {
  enum PurchaseState {
    PURCHASED,
    CANCELED,
    ERROR;
  }

  void init(@NonNull Context context, @NonNull EventBus eventBus, @NonNull EventTracker eventLogger);

  void buy(@NonNull String productId, @Nullable String developerPayload);

  void checkBillingSupported(@NonNull List<String> list);

  void setStoreItems(Map<String, JSONObject> storeItems);

  SKUDetail getSKUDetail(String iapId);

  void querySKUDetails(List<String> iapIds, @NonNull OnSkuDetailsListener onSkuDetailsListener);

  void setBillItemId(String itemId);

  void consumePurchase(@NonNull String purchaseToken, @NonNull OrderConsumeListener orderConsumeListener);

  void queryPurchase();

  void onDestroy();
}
