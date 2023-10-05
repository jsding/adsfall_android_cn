package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialListener;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;
import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.util.Logger;

import org.json.JSONObject;

public class IronsourceNonRewardedAdapter extends FullpageAdapter<BaseAdapter.GridParams> implements LevelPlayInterstitialListener {
  private static final String TAG = "IronsourceNonReward";

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

  }

  @Override
  public void onAdShowSucceeded(AdInfo adInfo) {
    super.onAdShowSuccess();
  }

  @Override
  public void onAdShowFailed(IronSourceError ironSourceError, AdInfo adInfo) {
    super.onAdShowFail();
  }

  @Override
  public void onAdClicked(AdInfo adInfo) {
    super.onAdClicked();
  }

  @Override
  public void onAdClosed(AdInfo adInfo) {
    super.onAdClosed(false);
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

  public IronsourceNonRewardedAdapter(Context context, String gridName, IvyAdType adType) {
    super(context, gridName, adType);
  }

  public void setup(Activity activity) {
    super.setup(activity);
    try {
      Logger.debug(TAG, "setup()");
      IronsourceManager.init(activity, this.getAppKey(), null);
      IronSource.setLevelPlayInterstitialListener(this);
    } catch (Throwable t) {
      Logger.error(TAG, "ironsource setup exception", t);
    }
  }

  public void fetch(Activity activity) {
    if (IronSource.isInterstitialReady()) {
      super.onAdLoadSuccess();
    } else {
      IronSource.loadInterstitial();
    }
  }

  public void show(Activity activity) {
    Logger.debug(TAG, "show()");
    if (IronSource.isInterstitialReady()) {
      IronSource.showInterstitial();
      return;
    }
    super.onAdShowFail();
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
