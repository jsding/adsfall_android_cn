package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.ads.interfaces.IvyLoadStatus;
import com.ivy.util.Logger;
import com.my.target.ads.InterstitialAd;

import org.json.JSONObject;

public class MytargetNonRewardedAdapter extends FullpageAdapter<FullpageAdapter.GridParams> {
  private static final String TAG = "Adapter-MyTarget-Interstitial";
  private InterstitialAd mInterstitial = null;

  public MytargetNonRewardedAdapter(Context context, String gridName, IvyAdType adType) {
    super(context, gridName, adType);
  }

  public void fetch(Activity activity) {
    try {
      String placement = getPlacementId();
      if (placement == null || "".equals(placement)) {
        super.onAdLoadFailed("INVALID");
        return;
      }

      mInterstitial = new InterstitialAd(Integer.parseInt(placement), activity);
      mInterstitial.setListener(new InterstitialAd.InterstitialAdListener() {
        @Override
        public void onLoad(@NonNull InterstitialAd interstitialAd) {
          Logger.debug(TAG, "onAdLoaded()");
          MytargetNonRewardedAdapter.this.onAdLoadSuccess();
        }

        @Override
        public void onNoAd(@NonNull String s, @NonNull InterstitialAd interstitialAd) {
          Logger.debug(TAG, "onNoAd()");
          MytargetNonRewardedAdapter.this.onAdLoadFailed(IvyLoadStatus.NO_FILL);

        }


        @Override
        public void onClick(@NonNull InterstitialAd interstitialAd) {
          Logger.debug(TAG, "onNoAd()");
          MytargetNonRewardedAdapter.this.onAdClicked();
        }

        @Override
        public void onDismiss(@NonNull InterstitialAd interstitialAd) {
          Logger.debug(TAG, "onNoAd()");
          MytargetNonRewardedAdapter.this.onAdClosed(false);
        }

        @Override
        public void onVideoCompleted(@NonNull InterstitialAd interstitialAd) {
        }

        @Override
        public void onDisplay(@NonNull InterstitialAd interstitialAd) {
          Logger.debug(TAG, "onNoAd()");
          MytargetNonRewardedAdapter.this.onAdShowSuccess();
        }
      });
      mInterstitial.load();
    } catch (Throwable t) {
      Logger.error(TAG, "fetch exception", t);
    }
  }

  public void show(Activity activity) {
    if (this.mInterstitial != null) {
      this.mInterstitial.show();
    } else {
      super.onAdShowFail();
    }
  }

  @Override
  public String getPlacementId() {
    return ((GridParams) getGridParams()).placement;
  }

  protected GridParams newGridParams() {
    return new GridParams();
  }

  public static class GridParams extends com.ivy.ads.adapters.BaseAdapter.GridParams {
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
