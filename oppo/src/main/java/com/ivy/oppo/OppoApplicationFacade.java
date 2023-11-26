package com.ivy.oppo;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import androidx.annotation.Keep;

import com.android.client.IAppFacade;
import com.nearme.game.sdk.GameCenterSDK;
import com.overseas.gamesdk.demo.ProcessUtil;

@Keep
public class OppoApplicationFacade implements IAppFacade {
  private static final String TAG = "OppoApplicationFacade";
  @Override
  public void attachBaseContext(Context base) {
  }

  @Override
  public void onCreate(android.app.Application app) {
    Log.d(TAG, getClass().getSimpleName() + ": onCreate = ");
    if (ProcessUtil.isMainProcess(app)) {
      onMainCreate(app);
    }
  }

  private final void onMainCreate(Application app) {
    Log.d(TAG, getClass().getSimpleName() + ": onMainCreate = ");
    //init sdk：sdk跑在独立进程，这里只需要在主进程做一次初始化操作。
    String appSecret = "16141caa0bed46a6afe6a6e09818d577";
    try {
      ApplicationInfo ai = app.getApplicationInfo();
      if (ai != null && ai.metaData != null) {
        Object o = ai.metaData.get("app_secret");
        if (o instanceof String) {
          appSecret = String.valueOf(o);
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    GameCenterSDK.init(appSecret, app);
  }
}
