package com.ivy.ads.configuration;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InterstitialConfig extends BaseConfig {
  public Ad ad = new Ad();
  public long adFullScreenTimespan = 300;
  public boolean dontShowFullPageAdsOnSlowConnection = true;
  public List<JSONObject> fullPageAdProviders = new ArrayList<>();

  @Override
  public BaseConfig fillFromJson(@NonNull JSONObject jsonObject) {
    super.fillFromJson(jsonObject);

    if (jsonObject.has("adFullScreenTimespan")) {
      this.adFullScreenTimespan = jsonObject.optInt("adFullScreenTimespan");
    }
    if (jsonObject.has("dontShowFullPageAdsOnSlowConnection")) {
      this.dontShowFullPageAdsOnSlowConnection = jsonObject.optBoolean("dontShowFullPageAdsOnSlowConnection");
    }
    if (jsonObject.has("ad")) {
      this.ad.fillFromJson(jsonObject.optJSONObject("ad"));
    }
    this.fullPageAdProviders = new ArrayList<>();
    if (jsonObject.has("full")) {
      JSONArray arr = jsonObject.optJSONArray("full");
      for (int i = 0; i < arr.length(); i++) {
        Object o = arr.opt(i);
        if (o instanceof String) {
          try {
            this.fullPageAdProviders.add(new JSONObject(String.valueOf(o)));
          } catch (JSONException ex) {
            ex.printStackTrace();
          }
        } else {
          this.fullPageAdProviders.add(arr.optJSONObject(i));
        }
      }
    }
    return this;
  }


  public static class Ad extends BaseConfig.Ad {
    // @JsonProperty("iLTS")
    public int interstitialLoadTimeoutSeconds = 10;
    public long maxInterstitialCachingTimeSeconds = 120;

    public void fillFromJson(@NonNull JSONObject jsonObject) {
      if (jsonObject.has("interstitialLoadTimeoutSeconds")) {
        this.interstitialLoadTimeoutSeconds = jsonObject.optInt("interstitialLoadTimeoutSeconds");
      }
      if (jsonObject.has("maxInterstitialCachingTimeSeconds")) {
        this.maxInterstitialCachingTimeSeconds = jsonObject.optInt("maxInterstitialCachingTimeSeconds");
      }
    }
  }
}
