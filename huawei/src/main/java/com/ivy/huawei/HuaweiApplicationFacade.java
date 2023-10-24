package com.ivy.huawei;

import android.app.Application;
import android.content.Context;

import com.android.client.IAppFacade;
import com.huawei.hms.api.HuaweiMobileServicesUtil;

public class HuaweiApplicationFacade implements IAppFacade {
  @Override
  public void attachBaseContext(Context base) {

  }

  @Override
  public void onCreate(Application app) {
    HuaweiMobileServicesUtil.setApplication(app);
  }
}

