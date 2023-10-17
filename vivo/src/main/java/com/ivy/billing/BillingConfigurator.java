package com.ivy.billing;

import androidx.annotation.NonNull;

import com.ivy.billing.impl.BillingPreferences;
import com.ivy.networks.grid.GridManager;

import org.json.JSONObject;

public class BillingConfigurator {
  @NonNull
  public static BillingPreferences setUpBillingPreferences() {

    BillingPreferences billingPreferences = new BillingPreferences();
    JSONObject gridData = GridManager.getGridData();
    if (!gridData.has("payment")) {
      return billingPreferences;
    }
    JSONObject paymentObject = gridData.optJSONObject("payment");
    if (paymentObject == null || !paymentObject.has("checkout")) {
      return billingPreferences;
    }

    JSONObject checkoutObject = paymentObject.optJSONObject("checkout");
    if (checkoutObject != null) {
      billingPreferences.appId = checkoutObject.optString("appId");
      billingPreferences.appSecret = checkoutObject.optString("appSecret");
      billingPreferences.notifiyUrl = checkoutObject.optString("notifiyUrl");
      billingPreferences.extInfo = checkoutObject.optString("extInfo");
    }
    return billingPreferences;
  }
}
