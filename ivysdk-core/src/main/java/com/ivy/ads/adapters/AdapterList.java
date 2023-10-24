package com.ivy.ads.adapters;

import android.content.Context;
import android.util.Log;

import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.networks.grid.GridManager;
import com.ivy.util.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public final class AdapterList {
  private static String getNetworkByProvider(String provider) {
    if (provider == null) {
      return null;
    }
    if (!provider.contains("_")) {
      return provider;
    }
    String[] data = provider.split("_");
    if (data.length > 1) {
      return data[0];
    }
    return null;
  }

  public static Set<BaseAdapter> getAdapters(Context context) {
    Set<BaseAdapter> registeredProviders = new HashSet<>();
    try {
      JSONObject gridData = GridManager.getGridData();

      //1. initialize banners
      if (gridData.has("banner")) {
        JSONArray ads = gridData.optJSONArray("banner");
        int size = ads.length();
        for (int i = 0; i < size; i++) {
          JSONObject adapterSetting = ads.optJSONObject(i);
          JSONObject placementSettings = adapterSetting.optJSONObject("p");
          String gridName = adapterSetting.optString("provider");

          String network = null;
          if (!placementSettings.has("network")) {
            network = getNetworkByProvider(gridName);
          } else {
            network = placementSettings.optString("network");
          }
          if (network == null) {
            Logger.error("ADSFALL", "No network found for provider: " + gridName);
            continue;
          }
          if (AdNetworkName.ADMOB.equals(network)) {
            registeredProviders.add(new AdmobBannerAdapter(context, gridName, IvyAdType.BANNER).setNetworkName(AdNetworkName.ADMOB));
          } else if (AdNetworkName.APPLOVIN_MAX.equals(network) || AdNetworkName.MAX.equals(network)) {
            registeredProviders.add(new ApplovinMaxBannerAdapter(context, gridName, IvyAdType.BANNER).setNetworkName(AdNetworkName.APPLOVIN_MAX));
          } else if (AdNetworkName.YANDEX.equals(network)) {
            registeredProviders.add(new YandexBannerAdapter(context, gridName, IvyAdType.BANNER).setNetworkName(AdNetworkName.YANDEX));
          } else if (AdNetworkName.MYTARGET.equals(network)) {
          } else {
            Logger.error("ADSFALL", "No banner adapter for network " + network);
          }
        }
      }

      // 2. initialize interstitial
      if (gridData.has("full")) {
        JSONArray ads = gridData.optJSONArray("full");
        int size = ads.length();
        for (int i = 0; i < size; i++) {
          JSONObject adapterSetting = ads.optJSONObject(i);

          JSONObject placementSettings = adapterSetting.optJSONObject("p");
          String gridName = adapterSetting.optString("provider");
          String network;
          if (!placementSettings.has("network")) {
            network = getNetworkByProvider(gridName);
          } else {
            network = placementSettings.optString("network");
          }
          if (network == null) {
            Logger.error("ADSFALL", "No network found for provider: " + gridName);
            continue;
          }

          if (AdNetworkName.ADMOB.equals(network)) {
            registeredProviders.add(new AdmobNonRewardedAdapter(context, gridName, IvyAdType.INTERSTITIAL).setNetworkName(AdNetworkName.ADMOB));
          }  else if (AdNetworkName.APPLOVIN.equals(network)) {
            registeredProviders.add(new ApplovinNonRewardedAdapter(context, gridName, IvyAdType.INTERSTITIAL).setNetworkName(AdNetworkName.APPLOVIN));
          } else if (AdNetworkName.IRONSOURCE.equals(network)) {
            registeredProviders.add(new IronsourceNonRewardedAdapter(context, gridName, IvyAdType.INTERSTITIAL).setNetworkName(AdNetworkName.IRONSOURCE));
          } else if (AdNetworkName.YANDEX.equals(network)) {
            registeredProviders.add(new YandexNonRewardedAdapter(context, gridName, IvyAdType.INTERSTITIAL).setNetworkName(AdNetworkName.YANDEX));
          } else if (AdNetworkName.MYTARGET.equals(network)) {
          } else if (AdNetworkName.APPLOVIN_MAX.equals(network) || AdNetworkName.MAX.equals(network)) {
            registeredProviders.add(new ApplovinMaxNonRewardedAdapter(context, gridName, IvyAdType.INTERSTITIAL).setNetworkName(AdNetworkName.APPLOVIN_MAX));
          } else {
            Logger.error("ADSFALL", "No interstitial adapter for network " + network);
          }
        }
      }

      // 3. video
      if (gridData.has("video")) {
        JSONArray ads = gridData.optJSONArray("video");
        int size = ads.length();
        for (int i = 0; i < size; i++) {
          JSONObject adapterSetting = ads.optJSONObject(i);
          String gridName = adapterSetting.optString("provider");
          JSONObject placementSettings = adapterSetting.optJSONObject("p");
          String network = null;
          if (!placementSettings.has("network")) {
            network = getNetworkByProvider(gridName);
          } else {
            network = placementSettings.optString("network");
          }
          if (network == null) {
            Logger.error("ADSFALL", "No network found for provider: " + gridName);
            continue;
          }
          if (AdNetworkName.ADMOB.equals(network)) {
            registeredProviders.add(new AdmobRewardedAdapter(context, gridName, IvyAdType.REWARDED).setNetworkName(AdNetworkName.ADMOB));
          } else if (AdNetworkName.APPLOVIN.equals(network)) {
            registeredProviders.add(new ApplovinRewardedAdapter(context, gridName, IvyAdType.REWARDED).setNetworkName(AdNetworkName.APPLOVIN));
          } else if (AdNetworkName.IRONSOURCE.equals(network)) {
            registeredProviders.add(new IronsourceRewardedAdapter(context, gridName, IvyAdType.REWARDED).setNetworkName(AdNetworkName.IRONSOURCE));
          } else if (AdNetworkName.YANDEX.equals(network)) {
            registeredProviders.add(new YandexRewardedAdapter(context, gridName, IvyAdType.REWARDED).setNetworkName(AdNetworkName.YANDEX));
          } else if (AdNetworkName.MYTARGET.equals(network)) {
          } else if (AdNetworkName.APPLOVIN_MAX.equals(network) || AdNetworkName.MAX.equals(network)) {
            registeredProviders.add(new ApplovinMaxRewardedAdapter(context, gridName, IvyAdType.REWARDED).setNetworkName(AdNetworkName.APPLOVIN_MAX));
          } else {
            Logger.error("ADSFALL", "No video adapter for network " + network);
          }
        }
      }
    } catch (Exception ex) {
      Logger.error("ADSFALL", "wrong adapter settings", ex);
    }
    return registeredProviders;
  }
}
