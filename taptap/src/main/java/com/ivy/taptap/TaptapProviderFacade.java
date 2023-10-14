package com.ivy.taptap;

import android.app.Activity;

import com.android.client.IProviderFacade;
import com.ivy.IvySdk;
import com.tapsdk.bootstrap.TapBootstrap;
import com.tds.common.entities.TapConfig;
import com.tds.common.models.TapRegionType;

import androidx.annotation.NonNull;


public class TaptapProviderFacade implements IProviderFacade {
  @Override
  public void onInitialize(@NonNull Activity activity) {
    String clientId = IvySdk.getGridConfigString("taptap.clientId");
    String clientToken = IvySdk.getGridConfigString("taptap.clientToken");
    String serverUrl = IvySdk.getGridConfigString("taptap.serverUrl");
    TapConfig config = new TapConfig.Builder()
      .withClientId(clientId) // 必须，开发者中心对应 Client ID
      .withClientToken(clientToken) // 必须，开发者中心对应 Client Token
      .withAppContext(activity) // Android app context
      .withServerUrl(serverUrl) // 必须，开发者中心 > 你的游戏 > 游戏服务 > 基本信息 > 域名配置 > API
      .withRegionType(TapRegionType.IO) // 必须，CN 表示中国大陆，IO 表示其他国家或地区
      .build();

    TapBootstrap.init(activity, config);
  }
}
