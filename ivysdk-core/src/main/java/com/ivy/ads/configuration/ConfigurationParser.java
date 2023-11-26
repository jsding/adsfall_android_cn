package com.ivy.ads.configuration;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ivy.networks.grid.GridManager;
import com.ivy.util.Logger;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationParser {
  private static final Map<String, BaseConfig> configCache = new HashMap<>();

  @NonNull
  public BaseConfig getConfig(String configType) {
    BaseConfig config;
    if (configCache.containsKey(configType)) {
      return configCache.get(configType);
    }

    if ("banner".equals(configType)) {
      config = new BannerConfig();
    } else if ("full".equals(configType)) {
      config = new InterstitialConfig();
    } else {
      config = new ClipConfig();
    }
    config.fillFromJson(GridManager.getGridData());
    configCache.put(configType, config);
    return config;
  }
}
