package com.ivy.vivo;

import android.accounts.Account;
import android.app.Activity;

import androidx.annotation.NonNull;

import com.android.client.IProviderFacade;
import com.android.client.OnSignedInListener;
import com.android.client.SignInProfile;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.impl.PurchaseManagerImpl;
import com.vivo.unionpay.sdk.open.VivoAccountCallback;
import com.vivo.unionpay.sdk.open.VivoConstants;
import com.vivo.unionpay.sdk.open.VivoUnionSDK;

import org.json.JSONObject;

public class VivoProviderFacade implements IProviderFacade {
  private static final String TAG = "Vivo";
  private static final String PLATFORM = "Vivo";
  @Override
  public void onInitialize(@NonNull Activity activity, @NonNull JSONObject gridData) {
  }

  @Override
  public void signIn(@NonNull Activity activity, @NonNull OnSignedInListener onSignedInListener) {
    VivoUnionSDK.login(activity, new VivoAccountCallback() {
      @Override
      public void onVivoAccountLogin(int statusCode, String token, String uid) {
        if (statusCode == VivoConstants.VIVO_LOGIN_SUCCESS) {
          SignInProfile signInProfile = new SignInProfile();
          signInProfile.setPlatform(PLATFORM);
          signInProfile.setOpenid(uid);
          onSignedInListener.onSignedInSuccess(signInProfile);
        } else if (statusCode == VivoConstants.VIVO_LOGIN_FAILED) {

        } else {

        }
      }
    });
  }

  public boolean onlyUsingPlatformAccount() {
    return true;
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
    return "vivo";
  }
}
