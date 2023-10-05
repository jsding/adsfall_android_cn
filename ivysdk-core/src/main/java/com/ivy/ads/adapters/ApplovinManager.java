package com.ivy.ads.adapters;


import android.app.Activity;

import androidx.annotation.NonNull;

import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.ivy.ads.interfaces.IvyLoadStatus;
import com.ivy.util.Logger;

import javax.annotation.Nullable;


public class ApplovinManager {
  private static final String TAG = "ApplovinManager";

  private ApplovinManager() {
  }

  public static synchronized AppLovinSdk getInstance(@NonNull Activity activity, @Nullable AppLovinSdk.SdkInitializationListener sdkInitializationListener) {
    AppLovinSdk sdk = null;
    synchronized (ApplovinManager.class) {
      try {
        sdk = AppLovinSdk.getInstance(activity.getApplicationContext());
        sdk.setMediationProvider(AppLovinMediationProvider.MAX);
        if (!sdk.isInitialized()) {
          sdk.initializeSdk(appLovinSdkConfiguration -> {
            Logger.debug(TAG, "Applovin Max Initialized successfull");
            if (sdkInitializationListener != null) {
              sdkInitializationListener.onSdkInitialized(appLovinSdkConfiguration);
            }
          });
        }
      } catch (Throwable t) {
        Logger.error(TAG, "Max initialize exception", t);
      }
    }

    return sdk;
  }

  public static void muteAudio(boolean flag) {
  }

  public static String errorCodeToMessage(int errorCode) {
    String reason = "unknow";
    switch (errorCode) {
      case AppLovinErrorCodes.SDK_DISABLED:
        reason = "sdk_disabled";
        break;
      case AppLovinErrorCodes.FETCH_AD_TIMEOUT:
        reason = "fetch_ad_timeout";
        break;
      case AppLovinErrorCodes.NO_FILL:
        reason = IvyLoadStatus.NO_FILL;
        break;
      case AppLovinErrorCodes.INVALID_ZONE:
        reason = "invalid_zone";
        break;
      case AppLovinErrorCodes.NO_NETWORK:
        reason = "no_network";
        break;
      case AppLovinErrorCodes.UNABLE_TO_RENDER_AD:
        reason = "unable_to_render_ad";
        break;
      case AppLovinErrorCodes.UNSPECIFIED_ERROR:
        reason = "unspecified_error";
        break;
    }
    return reason;
  }
}
