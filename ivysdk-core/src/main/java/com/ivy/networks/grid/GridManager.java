package com.ivy.networks.grid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.ivy.IvySdk;
import com.ivy.IvyUtils;
import com.ivy.networks.tracker.EventTracker;
import com.ivy.networks.util.Util;
import com.ivy.util.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Locale;

public final class GridManager {
  public static final String TAG = "GridManager";

  public static final String FILE_JSON_RESPONSE = "jsonResponse";
  private static final String PREFS = "prefs";
  private static int vc;
  private static Long gts = null;
  public final Activity activity;

  private static JSONObject gridData = new JSONObject();

  public GridManager(Activity activity, EventTracker eventTracker, long startupTime, boolean forceUpdate) {
    this.activity = activity;

    vc = Util.getVersionCode(activity);

    // 如果当前的游戏版本超过保存的grid版本， 需要清除缓存数据，使用assets目录下的资源
    SharedPreferences sp = this.activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

    // load the grid data from file cache, if not found, load the default grid data from assets
    String gridString = IvySdk.loadGridData();

    int localCacheVersion = sp.getInt("gridDataVersion", vc);
    if (localCacheVersion != vc) {
      Logger.debug(TAG, "user upgrade the game, we should local the grid from assets");
      gridString = null;
    }

    // read grid data from assets directory
    if (gridString == null) {
      final String suffix = activity.getPackageName();
      String fileName = IvyUtils.md5("config_" + suffix);

      Logger.debug(TAG, "try load security config file:" + fileName);

      gridString = IvyUtils.readSecureText(activity, fileName);

      if (gridString == null) {
        IvySdk.showToast("config file not found!!");
        Log.d(TAG, "Security file is empty, try to load default.json directly");
        InputStream in = IvyUtils.openAsset(activity, "default.json");
        if (in != null) {
          Logger.debug(TAG, "Plain config file is OK, use it");
          gridString = IvyUtils.streamToString(in);
        }
      }

      if (gridString != null) {
        sp.edit().putInt("gridDataVersion", vc).apply();
        Util.storeData(activity, GridManager.FILE_JSON_RESPONSE, gridString);
      }
    }

    try {
      if (gridString != null) {
        GridManager.gridData = new JSONObject(gridString);
        mergeGridWithRemoteConfig();
      }
    } catch (Throwable ex) {
      Logger.error(TAG, "parse grid data exception", ex);
    }
  }

  /**
   * 根据video配置的provider检测当前的mediation provider
   * @return
   */
  public String getMediationProvider() {
    JSONArray ads = gridData.optJSONArray("video");
    if (ads != null && ads.length() > 0) {
      JSONObject firstVideoConfig = ads.optJSONObject(0);
      String firstProvider = firstVideoConfig.optString("provider");
      if ("applovinmax".equals(firstProvider) || "max".equals(firstProvider)) {
        return "max";
      } else if ("admob".equals(firstProvider)) {
        return "admob";
      } else if ("ironsource".equals(firstProvider)) {
        return "ironsource";
      }
    }
    return gridData.optString("mediation_provider", "admob");
  }

  public String getClientCountryCode() {
    if (this.activity == null) {
      return Locale.getDefault().getCountry().toLowerCase(Locale.ENGLISH);
    }

    SharedPreferences sp = this.activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    return sp.getString("clientCountryCode", Locale.getDefault().getCountry().toLowerCase(Locale.ENGLISH));
  }

  public interface AdProvidersCallback {
    void setupAdProviders(String str, boolean z);
  }

  @NonNull
  public static JSONObject getGridData() {
    return gridData;
  }

  /**
   * 从remote config中读取部分配置并覆盖
   */
  private void mergeGridWithRemoteConfig() {
    FirebaseRemoteConfig firebaseRemoteConfig = IvySdk.getFirebaseRemoteConfig();
    if (firebaseRemoteConfig == null) {
      return;
    }

    String remoteGridData = firebaseRemoteConfig.getString("config_grid_data_android");
    if (!FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING.equals(remoteGridData)) {
      try {
        JSONObject jsonObject = new JSONObject(remoteGridData);
        if (jsonObject.has("v_api")) {
          gridData = jsonObject;
        }
      } catch (Throwable t) {
        Logger.error(TAG, "remote grid data exception", t);
      }
    }

    // banner配置 ad_config_banner
    String bannerConfig = firebaseRemoteConfig.getString("ad_config_banner");
    if (!FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING.equals(bannerConfig)) {
      try {
        JSONObject jsonObject = new JSONObject(bannerConfig);
        if (jsonObject.has("ads")) {
          JSONArray banner = jsonObject.optJSONArray("ads");
          if (banner != null && banner.length() > 0) {
            gridData.put("banner", banner);
            if (jsonObject.has("bannerLoadTimeoutSeconds")) {
              int bannerLoadTimeoutSeconds = jsonObject.optInt("bannerLoadTimeoutSeconds", 0);
              if (bannerLoadTimeoutSeconds > 0) {
                gridData.put("bannerLoadTimeoutSeconds", bannerLoadTimeoutSeconds);
              }
            }
            if (jsonObject.has("adRefreshInterval")) {
              int adRefreshInterval = jsonObject.optInt("adRefreshInterval", 0);
              if (adRefreshInterval > 0) {
                gridData.put("adRefreshInterval", adRefreshInterval);
              }
            }
          }
        }
      } catch (Throwable t) {
        Logger.error(TAG, "ad_config_banner data error");
      }
    }

    // 插屏配置 ad_config_full
    String fullConfig = firebaseRemoteConfig.getString("ad_config_full");
    if (!FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING.equals(fullConfig)) {
      try {
        JSONObject jsonObject = new JSONObject(fullConfig);
        if (jsonObject.has("ads")) {
          JSONArray full = jsonObject.optJSONArray("ads");
          if (full != null && full.length() > 0) {
            gridData.put("full", full);
          }
        }
      } catch (Throwable t) {
        Logger.error(TAG, "ad_config_full data error");
      }
    }

    // 激励视频配置
    String rewardedConfig = firebaseRemoteConfig.getString("ad_config_video");
    if (!FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING.equals(rewardedConfig)) {
      try {
        JSONObject jsonObject = new JSONObject(rewardedConfig);
        if (jsonObject.has("ads")) {
          JSONArray video = jsonObject.optJSONArray("ads");
          if (video != null && video.length() > 0) {
            gridData.put("video", video);
          }
        }
      } catch (Throwable t) {
        Logger.error(TAG, "ad_config_video data error");
      }
    }

    String remoteMediationProvider = firebaseRemoteConfig.getString("mediation_provider");
    if (!FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING.equals(rewardedConfig)) {
      try {
        gridData.put("mediation_provider", remoteMediationProvider);
      } catch(Throwable t) {
        Logger.error(TAG, "mediation_provider put error");
      }
    }
  }
}