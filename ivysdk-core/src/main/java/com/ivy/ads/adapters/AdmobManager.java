package com.ivy.ads.adapters;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.ivy.ads.interfaces.IvyLoadStatus;

public class AdmobManager {
  private static boolean initialized = false;
  private static final AdmobManager INSTANCE = new AdmobManager();


  public static synchronized AdmobManager getInstance() {
    return INSTANCE;
  }

  public synchronized void initialize(@NonNull Activity activity, @NonNull OnInitializationCompleteListener initializationCompleteListener) {
    if (!initialized) {
      MobileAds.initialize(activity, initializationCompleteListener);
      initialized = true;
    }
  }

  public synchronized void makesureIntialized(Activity activity) {
    if (!initialized) {
      MobileAds.initialize(activity);
      initialized = true;
    }
  }

  public static String errorCodeToMessage(int errorCode) {
    String reason = "unknow";
    switch(errorCode) {
      case AdRequest.ERROR_CODE_INTERNAL_ERROR:
        reason= "internal_error";
        break;
      case AdRequest.ERROR_CODE_INVALID_REQUEST:
        reason = "invalid_request";
        break;
      case AdRequest.ERROR_CODE_NETWORK_ERROR:
        reason = "network_error";
        break;
      case AdRequest.ERROR_CODE_NO_FILL:
        reason = IvyLoadStatus.NO_FILL;
        break;
    }
    return reason;
  }
}
