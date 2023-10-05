package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoManualListener;
import com.ironsource.mediationsdk.sdk.RewardedVideoListener;
import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.ads.interfaces.IvyLoadStatus;
import com.ivy.util.Logger;

import org.json.JSONObject;

public class IronsourceRewardedAdapter extends FullpageAdapter<BaseAdapter.GridParams> implements LevelPlayRewardedVideoListener, LevelPlayRewardedVideoManualListener {
  private static final String TAG = "IronsourceReward";

  private boolean gotReward = false;

  @Override
  public void onAdAvailable(AdInfo adInfo) {

  }

  @Override
  public void onAdUnavailable() {

  }

  @Override
  public void onAdReady(AdInfo adInfo) {
    super.onAdLoadSuccess();
  }

  @Override
  public void onAdLoadFailed(IronSourceError ironSourceError) {
    super.onAdLoadFailed(ironSourceError.getErrorMessage());
  }

  @Override
  public void onAdOpened(AdInfo adInfo) {
    super.onAdShowSuccess();
  }

  @Override
  public void onAdShowFailed(IronSourceError ironSourceError, AdInfo adInfo) {
    super.onAdShowFail();
  }

  @Override
  public void onAdClicked(Placement placement, AdInfo adInfo) {
    super.onAdClicked();
  }

  @Override
  public void onAdRewarded(Placement placement, AdInfo adInfo) {
    gotReward = true;
  }

  @Override
  public void onAdClosed(AdInfo adInfo) {
    super.onAdClosed(gotReward);
  }


  public static class GridParams extends BaseAdapter.GridParams {
    public String appKey;
    public String placement;

    public GridParams() {
    }

    @Override
    public BaseAdapter.GridParams fromJSON(JSONObject jsonObject) {
      this.appKey = jsonObject.optString("appKey");
      this.placement = jsonObject.optString("placement");
      return this;
    }

    protected String getParams() {
      return "placement=" + this.placement + ", appKey=" + this.appKey;
    }
  }

  public IronsourceRewardedAdapter(Context context, String gridName, IvyAdType adType) {
    super(context, gridName, adType);
  }

  public void setup(Activity activity) {
    super.setup(activity);
    try {
      Logger.debug(TAG, "setup()");
      IronsourceManager.init(activity, this.getAppKey(), null);
      IronSource.setLevelPlayRewardedVideoManualListener(this);
      IronSource.setLevelPlayRewardedVideoListener(this);
    } catch (Throwable t) {
      Logger.error(TAG, "setup ironsource exception", t);
    }
  }


  public void fetch(Activity activity) {
    gotReward = false;
    if (IronSource.isRewardedVideoAvailable()) {
      super.onAdLoadSuccess();
    } else {
      IronSource.loadRewardedVideo();
    }
  }

  public void show(Activity activity) {
    gotReward = false;
    Logger.debug(TAG, "[%s]show()", getAdType());
    if (IronSource.isRewardedVideoAvailable()) {
      IronSource.showRewardedVideo();
    } else {
      onAdShowFail();
    }
  }

  public String getPlacementId() {
    return ((GridParams) getGridParams()).placement;
  }

  protected GridParams newGridParams() {
    return new GridParams();
  }

  public String getAppKey() {
    return ((GridParams) getGridParams()).appKey;
  }
}
