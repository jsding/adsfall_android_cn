package com.ivy.ads.configuration;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClipConfig extends BaseConfig {
  public Ad ad = new Ad();
  public List<JSONObject> adRewardsProviders = new ArrayList<>();

  @Override
  public BaseConfig fillFromJson(@NonNull JSONObject jsonObject) {
    super.fillFromJson(jsonObject);
    this.adRewardsProviders = new ArrayList<>();

    if (jsonObject.has("video")) {
      JSONArray arr = jsonObject.optJSONArray("video");
      if (arr != null) {
        for (int i = 0; i < arr.length(); i++) {
          JSONObject o = arr.optJSONObject(i);
          this.adRewardsProviders.add(o);
        }
      }
    }
    this.ad.fillFromJson(jsonObject);
    return this;
  }

  public static class Ad extends BaseConfig.Ad {
    public long rewardedClipPreloadDelaySeconds = 3;
    public boolean rewardedClipRestartWaterfallAfterVolatileClip = true;
    public boolean rewardedClipUseVolatileClips = true;
    public long rewardedClipVolatileWaitSeconds = 5;
    public boolean useVideoClipPreloading = false;

    public void fillFromJson(@NonNull JSONObject jsonObject) {
      if (jsonObject.has("rewardedClipPreloadDelaySeconds")) {
        this.rewardedClipPreloadDelaySeconds = jsonObject.optInt("rewardedClipPreloadDelaySeconds");
      }
      if (jsonObject.has("rewardedClipRestartWaterfallAfterVolatileClip")) {
        this.rewardedClipRestartWaterfallAfterVolatileClip = jsonObject.optBoolean("rewardedClipRestartWaterfallAfterVolatileClip");
      }
      if (jsonObject.has("rewardedClipUseVolatileClips")) {
        this.rewardedClipUseVolatileClips = jsonObject.optBoolean("rewardedClipUseVolatileClips");
      }
      if (jsonObject.has("rewardedClipVolatileWaitSeconds")) {
        this.rewardedClipVolatileWaitSeconds = jsonObject.optInt("rewardedClipVolatileWaitSeconds");
      }
      if (jsonObject.has("useVideoClipPreloading")) {
        this.useVideoClipPreloading = jsonObject.optBoolean("useVideoClipPreloading");
      }
    }

  }
}
