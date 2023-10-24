package com.ivy.amazon;

import android.app.Activity;
import android.content.Intent;

import com.amazon.device.messaging.ADM;
import com.android.client.IProviderFacade;
import com.android.client.OnPaymentSystemReadyListener;
import com.android.client.OnSignedInListener;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.impl.PurchaseManagerImpl;
import com.ivy.util.Logger;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import kotlin.OptIn;

public class AmazonProviderFacade implements IProviderFacade {
  private static final String TAG = "Amazon";


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
    try {
      ADM adm = new ADM(activity);
      if (adm.getRegistrationId() == null) {
        adm.startRegister();
      }
    } catch(Throwable t) {
      Logger.error(TAG, "adm startRegister exception", t);
    }
  }
  @Override
  public String getChannel() {
    return "amazon";
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
