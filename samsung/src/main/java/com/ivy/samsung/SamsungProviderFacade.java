package com.ivy.samsung;

import android.app.Activity;
import android.content.Intent;

import com.android.client.IProviderFacade;
import com.android.client.OnPaymentSystemReadyListener;
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

  @Override
  public void onCreate(Activity activity) {

  }

  @Override
  public void onResume(Activity activity) {

  }

  @Override
  public void onPause(Activity activity) {

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

  }

  @Override
  public void registerPaymentSystemReadyListener(@NonNull OnPaymentSystemReadyListener listener) {
    listener.onReady();
  }
}
