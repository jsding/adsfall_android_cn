package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.ads.interfaces.IvyLoadStatus;
import com.ivy.util.Logger;
import com.my.target.ads.Reward;
import com.my.target.ads.RewardedAd;

import org.json.JSONObject;

public class MytargetRewardedAdapter extends FullpageAdapter<BaseAdapter.GridParams> {
  private static final String TAG = "Adapter-Mytarget-Rewarded";
  private boolean gotReward;
  private RewardedAd mRewardedAd = null;

  public MytargetRewardedAdapter(Context context, String gridName, IvyAdType adType) {
    super(context, gridName, adType);
  }

  public void fetch(Activity activity) {
    Logger.debug(TAG, "fetch()");
    try {
      String placement = getPlacementId();
      if (placement == null || "".equals(placement)) {
        super.onAdLoadFailed("INVALID");
        return;
      }

      mRewardedAd = new RewardedAd(Integer.parseInt(placement), activity);

      mRewardedAd.setListener(new RewardedAd.RewardedAdListener() {
        @Override
        public void onLoad(@NonNull RewardedAd rewardedAd) {
          Logger.debug(TAG, "onAdLoaded()");
          MytargetRewardedAdapter.this.onAdLoadSuccess();
        }

        @Override
        public void onNoAd(@NonNull String s, @NonNull RewardedAd rewardedAd) {
          Logger.debug(TAG, "onNoAd");
          MytargetRewardedAdapter.this.onAdLoadFailed(IvyLoadStatus.NO_FILL);

        }


        @Override
        public void onClick(@NonNull RewardedAd rewardedAd) {
          MytargetRewardedAdapter.this.onAdClicked();
        }

        @Override
        public void onDismiss(@NonNull RewardedAd rewardedAd) {
          MytargetRewardedAdapter.this.onAdClosed(gotReward);
        }

        @Override
        public void onReward(@NonNull Reward reward, @NonNull RewardedAd rewardedAd) {
          gotReward = true;
        }

        @Override
        public void onDisplay(@NonNull RewardedAd rewardedAd) {
          MytargetRewardedAdapter.this.onAdShowSuccess();
        }
      });

      mRewardedAd.load();
    } catch (Throwable t) {
      Logger.error(TAG, "fetchAd exception", t);
    }
  }

  public void show(Activity activity) {
    Logger.debug(TAG, "show()");
    this.gotReward = false;
    if (this.mRewardedAd != null) {
      this.mRewardedAd.show();
    } else {
      this.onAdShowFail();
    }
  }

  @Override
  public String getPlacementId() {
    return ((GridParams) getGridParams()).placement;
  }

  protected GridParams newGridParams() {
    return new GridParams();
  }

  public static class GridParams extends BaseAdapter.GridParams {
    public String placement;

    public GridParams fromJSON(JSONObject jsonObject) {
      this.placement = jsonObject.optString("placement");
      return this;
    }

    protected String getParams() {
      return "placement=" + this.placement;
    }
  }
}
