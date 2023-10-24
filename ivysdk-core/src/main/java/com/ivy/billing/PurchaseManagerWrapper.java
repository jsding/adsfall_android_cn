package com.ivy.billing;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.android.client.OnSkuDetailsListener;
import com.android.client.OrderConsumeListener;
import com.android.client.SKUDetail;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class PurchaseManagerWrapper {
  private final PurchaseManager purchaseManager;

  public PurchaseManagerWrapper(PurchaseManager purchaseManager) {
    this.purchaseManager = purchaseManager;
  }

  public void setStoreItems(Map<String, JSONObject> storeItems) {
    if (this.purchaseManager != null) {
      this.purchaseManager.setStoreItems(storeItems);
    }
  }

  public SKUDetail getSKUDetail(String iapId) {
    if (this.purchaseManager != null) {
      return this.purchaseManager.getSKUDetail(iapId);
    }
    return null;
  }

  public void startLoadingStoreData(@NonNull List<String> iapIdsS, @NonNull Map<String, JSONObject> storeItems) {
    this.setStoreItems(storeItems);
    this.purchaseManager.checkBillingSupported(iapIdsS);
  }

  public void querySKUDetails(@NonNull List<String> iapIds, @NonNull OnSkuDetailsListener onSkuDetailsListener) {
    if (purchaseManager != null) {
      purchaseManager.querySKUDetails(iapIds, onSkuDetailsListener);
    }
  }

  public void setBillItemName(String itemId) {
    if (purchaseManager != null) {
      purchaseManager.setBillItemId(itemId);
    }
  }

  public void queryPurchase() {
    if (purchaseManager != null) {
      purchaseManager.queryPurchase();
    }
  }

  public void onDestroy() {
    if (purchaseManager != null) {
      purchaseManager.onDestroy();
    }
  }

  public void onResume(Activity activity) {
    if (purchaseManager != null) {
      purchaseManager.onResume(activity);
    }
  }

  public void onActivityResult(final int requestCode, int resultCode, Intent data) {
    if (purchaseManager != null) {
      purchaseManager.onActivityResult(requestCode, resultCode, data);
    }
  }

  public void startBuying(@NonNull String iapId, @Nullable String developerPayload) {
    if (this.purchaseManager != null) {
      this.purchaseManager.buy(iapId, developerPayload);
    }
  }

  public void consumePurchase(@NonNull String purchaseToken, @NonNull OrderConsumeListener orderConsumeListener) {
    if (this.purchaseManager != null) {
      this.purchaseManager.consumePurchase(purchaseToken, orderConsumeListener);
    }
  }
}
