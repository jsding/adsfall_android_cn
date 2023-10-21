package com.ivy.samsung;

import android.app.Activity;

import com.android.client.IProviderFacade;
import com.android.client.OnSignedInListener;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.impl.PurchaseManagerImpl;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public class SamsungProviderFacade implements IProviderFacade {
  private static final String TAG = "Samsung";


  @Override
  public void onInitialize(@NonNull Activity activity, @NonNull JSONObject gridData) {
  }

  @Override
  public void signIn(@NonNull Activity activity, @NonNull OnSignedInListener onSignedInListener) {
  }

  public boolean onlyUsingPlatformAccount() {
    return false;
  }

  @Override
  public PurchaseManager getPurchaseManager() {
    return new PurchaseManagerImpl();
  }

  @Override
  public void initPushSystem(@NonNull Activity activity) {
  }

  @Override
  public String getChannel() {
    return "samsung";
  }
}
