package com.ivy.taptap;

import android.app.Activity;

import com.android.client.IProviderFacade;
import com.android.client.OnSignedInListener;
import com.android.client.SignInProfile;
import com.ivy.IvySdk;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.impl.PurchaseManagerImpl;
import com.ivy.util.Logger;
import com.tapsdk.tapconnect.TapConnect;
import com.taptap.sdk.AccessToken;
import com.taptap.sdk.AccountGlobalError;
import com.taptap.sdk.LoginSdkConfig;
import com.taptap.sdk.Profile;
import com.taptap.sdk.RegionType;
import com.taptap.sdk.TapLoginHelper;
import com.tds.common.TapCommon;
import com.tds.common.entities.TapConfig;
import com.tds.common.models.TapRegionType;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public class TaptapProviderFacade implements IProviderFacade {
  private static final String TAG = "Taptap";

  private static final String PLATFORM = "Taptap";

  @Override
  public void onInitialize(@NonNull Activity activity, @NonNull JSONObject gridData) {
    String clientId = gridData.optString("taptap.clientId");
    String clientToken = gridData.optString("taptap.clientToken");

    TapConfig config = new TapConfig.Builder()
      .withClientId(clientId) // 必须，开发者中心对应 Client ID
      .withClientToken(clientToken) // 必须，开发者中心对应 Client Token
      .withAppContext(activity.getApplicationContext()) // Application Context
      .withRegionType(TapRegionType.IO) // 必须为 IO, 表示其他国家或地区
      .build();

    TapCommon.init(config);

    LoginSdkConfig loginConfig = new LoginSdkConfig();
    loginConfig.regionType = RegionType.IO;
    TapLoginHelper.init(activity.getApplicationContext(), config.clientId, loginConfig);

    TapConnect.init(activity, clientId, clientToken, false);
  }


  private void signInSuccess(@NonNull Profile profile, @NonNull OnSignedInListener onSignedInListener) {
    SignInProfile signInProfile = new SignInProfile();
    signInProfile.setPlatform(PLATFORM);
    signInProfile.setOpenid(profile.getOpenid());
    signInProfile.setName(profile.getName());
    signInProfile.setAvatar(profile.getAvatar());
    signInProfile.setEmail(profile.getEmail());
    signInProfile.setEmailVerified(profile.isEmailVerified());


    onSignedInListener.onSignedInSuccess(signInProfile);
  }

  @Override
  public void signIn(@NonNull Activity activity, @NonNull OnSignedInListener onSignedInListener) {
    AccessToken accessToken = TapLoginHelper.getCurrentAccessToken();
    if (accessToken != null) {
      // already signed in
      Profile profile = TapLoginHelper.getCurrentProfile();
      signInSuccess(profile, onSignedInListener);
      return;
    }

    TapLoginHelper.TapLoginResultCallback loginCallback = new TapLoginHelper.TapLoginResultCallback() {
      @Override
      public void onLoginSuccess(AccessToken token) {
        Logger.debug(TAG, "TapTap authorization succeed");
        // 开发者调用 TapLoginHelper.getCurrentProfile() 可以获得当前用户的一些基本信息，例如名称、头像。
        Profile profile = TapLoginHelper.getCurrentProfile();
        signInSuccess(profile, onSignedInListener);
      }

      @Override
      public void onLoginCancel() {
        Logger.debug(TAG, "TapTap authorization cancelled");
      }

      @Override
      public void onLoginError(AccountGlobalError globalError) {
        Logger.debug(TAG, "TapTap authorization failed. cause: " + globalError.getMessage());
      }
    };
    // 注册监听
    TapLoginHelper.registerLoginCallback(loginCallback);
    // 登录
    TapLoginHelper.startTapLogin(activity, TapLoginHelper.SCOPE_PUBLIC_PROFILE);
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
    return "taptap";
  }
}
