package com.ivy.ads.configuration;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BaseConfig {
  static long AD_REFRESH_INTERVAL = 30000;
  public Ad ad = new Ad();
  public int adLoadTimeout = 10;
  public int adRefreshInterval = ((int) (AD_REFRESH_INTERVAL / 1000));
  public String clientCountryCode = "";
  public boolean hideAdLabel = false;

  /**
   * 从JSON数据中获取配置
   *
   * @param jsonObject
   */
  public BaseConfig fillFromJson(@NonNull JSONObject jsonObject) {
    if (jsonObject.has("adLoadTimeout")) {
      this.adLoadTimeout = jsonObject.optInt("adLoadTimeout");
    }
    if (jsonObject.has("adRefreshInterval")) {
      this.adRefreshInterval = jsonObject.optInt("adRefreshInterval");
    }
    if (jsonObject.has("clientCountryCode")) {
      this.clientCountryCode = jsonObject.optString("clientCountryCode");
    }

    if (jsonObject.has("hideAdLabel")) {
      this.hideAdLabel = jsonObject.optBoolean("hideAdLabel");
    }
    return this;
  }

  public static class Ad {
    public long rewardedClipLoadTimeoutSeconds = 8;
  }
}
