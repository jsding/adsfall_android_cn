package com.ivy.vivo;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.annotation.Keep;

import com.android.client.IAppFacade;
import com.sherdle.universal.util.Log;
import com.vivo.unionpay.sdk.open.VivoUnionSDK;

@Keep
public class VivoApplicationFacade implements IAppFacade {
  @Override
  public void attachBaseContext(Context base) {
  }

  @Override
  public void onCreate(Context context) {
    try {
      ApplicationInfo ai = context.getApplicationInfo();
      if (ai != null && ai.metaData != null) {
        String vivoAppId = ai.metaData.getString("adsfall.vivoAppId", "");
        VivoUnionSDK.initSdk(context, vivoAppId);
      }
    } catch (Exception e) {
      Log.e("VivoApplication", "onCreate exception");
    }
  }
}
