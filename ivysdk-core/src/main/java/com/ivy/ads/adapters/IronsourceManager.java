package com.ivy.ads.adapters;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.sdk.InitializationListener;
import com.ivy.ads.interfaces.Adapter;
import com.ivy.util.Logger;

import org.json.JSONObject;

import javax.annotation.Nullable;

public class IronsourceManager {
  private static final String TAG = "Ironsource";

  private static boolean mInitialized = false;

  public static synchronized void init(Activity activity, String appKey, @Nullable InitializationListener initializationListener) {
    if (mInitialized) {
      return;
    }

    IronSource.init(activity, appKey, () -> {
       Logger.debug(TAG, "Ironsource initialized");
       if (initializationListener != null) {
         initializationListener.onInitializationComplete();
       }
    });

    IronSource.addImpressionDataListener(impressionData -> {
      Double revenue = impressionData.getRevenue();
      String adNetwork = impressionData.getAdNetwork();
      JSONObject allData = impressionData.getAllData();
      
    });
    mInitialized = true;
  }

  public static void onResume(Activity activity) {
    if (mInitialized) {
      IronSource.onResume(activity);
    }
  }

  public static void onPause(Activity activity) {
    if (mInitialized) {
      IronSource.onPause(activity);
    }
  }
}
