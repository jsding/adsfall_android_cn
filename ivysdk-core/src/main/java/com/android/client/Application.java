package com.android.client;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.multidex.MultiDex;

import com.ivy.util.Logger;

public class Application extends androidx.multidex.MultiDexApplication {
  private IAppFacade appFacade;
  @Override
  public void onCreate() {
    super.onCreate();
    try {
      ApplicationInfo ai = getApplicationInfo();
      if (ai != null && ai.metaData != null) {
        Object o = ai.metaData.get("adsfall.application");
        if (o instanceof String) {
          String providerClass = String.valueOf(o);
          appFacade = (IAppFacade) Class.forName(providerClass).newInstance();
          appFacade.onCreate(this);
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    if (appFacade != null) {
      appFacade.attachBaseContext(base);
    }
  }
}
