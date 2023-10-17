package com.ivy.amazon;

import android.app.Activity;

import com.amazon.device.messaging.ADM;
import com.android.client.IProviderFacade;
import com.android.client.OnSignedInListener;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.impl.PurchaseManagerImpl;
import com.ivy.util.Logger;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public class AmazonProviderFacade implements IProviderFacade {
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
    try {
      ADM adm = new ADM(activity);
      if (adm.getRegistrationId() == null) {
        // startRegister()是异步的；当注册ID可用时，
        // 将通过onRegistered()回调通知您的应用。
        adm.startRegister();
      }
    } catch(Throwable t) {
      Logger.error(TAG, "adm startRegister exception", t);
    }
  }
}
