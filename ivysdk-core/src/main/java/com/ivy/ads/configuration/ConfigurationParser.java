package com.ivy.ads.configuration;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ivy.networks.grid.GridManager;
import com.ivy.util.Logger;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationParser {
  private static final String TAG = ConfigurationParser.class.getCanonicalName();

  private static final Map<Class, BaseConfig> configCache = new HashMap<>();

  public ConfigurationParser(Context applicationContext) {
  }

  public static void clearCache() {
    configCache.clear();
  }


  @NonNull
  public <T extends BaseConfig> T getConfig(Class<T> configClass) {
    T config = null;
    try {
      if (configCache.containsKey(configClass)) {
        return (T) (configCache.get(configClass));
      }

      config = configClass.newInstance();
      config.fillFromJson(GridManager.getGridData());

      configCache.put(configClass, config);
    } catch (InstantiationException | IllegalAccessException ex) {
      Logger.error(TAG, "Error occurred while trying to get grid JSON ", ex);
    }
    return config;
  }
}
