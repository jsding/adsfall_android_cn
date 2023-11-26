package com.ivy.oppo;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.android.client.IProviderFacade;
import com.android.client.OnPaymentSystemReadyListener;
import com.android.client.OnSignedInListener;
import com.android.client.SignInProfile;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.impl.PurchaseManagerImpl;
import com.ivy.util.Logger;
import com.nearme.game.sdk.GameCenterSDK;
import com.nearme.game.sdk.callback.ApiCallback;
import com.nearme.game.sdk.common.model.biz.ReqUserInfoParam;

import org.json.JSONObject;

public class OppoProviderFacade implements IProviderFacade {
  private static final String TAG = "Vivo";

  @Override
  public void onInitialize(@NonNull Activity activity, @NonNull JSONObject gridData) {
  }

  @Override
  public void signIn(@NonNull Activity activity, @NonNull OnSignedInListener onSignedInListener) {
    GameCenterSDK.getInstance().doLogin(activity, new ApiCallback() {
      @Override
      public void onSuccess(String s) {
        GameCenterSDK.getInstance().doGetTokenAndSsoid(new ApiCallback() {
          @Override
          public void onSuccess(String resultMsg) {
            try {
              JSONObject json = new JSONObject(resultMsg);
              String token = json.getString("token");
              String ssoid = json.getString("ssoid");

              GameCenterSDK.getInstance().doGetUserInfo(new ReqUserInfoParam(token, ssoid), new ApiCallback() {
                @Override
                public void onSuccess(String s) {
                  try {
                    SignInProfile signInProfile = new SignInProfile();
                    signInProfile.setPlatform("oppo");

                    JSONObject userInfo = new JSONObject(s);
                    signInProfile.setOpenid(userInfo.optString("ssoid"));
                    signInProfile.setName(userInfo.optString("userName"));

                    onSignedInListener.onSignedInSuccess(signInProfile);
                  } catch (Exception ex) {
                    Logger.error(TAG, "doGetUserInfo exception", ex);
                  }
                }

                @Override
                public void onFailure(String message, int code) {
                  Logger.error(TAG, "signIn failure, code =" + code + ", message =" + message);
                  onSignedInListener.onSignedInError(String.valueOf(code), message);
                }
              });
            } catch (Throwable e) {
              Logger.error(TAG, "SignIn exception", e);
            }
          }

          @Override
          public void onFailure(String message, int code) {
            Logger.error(TAG, "signIn failure, code =" + code + ", message =" + message);
            onSignedInListener.onSignedInError(String.valueOf(code), message);
          }
        });
      }

      @Override
      public void onFailure(String message, int code) {
        Logger.error(TAG, "signIn failure, code =" + code + ", message =" + message);
        onSignedInListener.onSignedInError(String.valueOf(code), message);
      }
    });
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
    return "oppo";
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
