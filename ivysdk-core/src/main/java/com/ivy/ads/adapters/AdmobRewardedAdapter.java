package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.util.Logger;

import org.json.JSONObject;

import java.util.Locale;

public class AdmobRewardedAdapter extends FullpageAdapter<BaseAdapter.GridParams> {
  private static final String TAG = "Adapter-Admob-Rewarded";
  private boolean gotReward;
  private RewardedAd mRewardedAd = null;

  private final FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
      Logger.debug(TAG, "onAdFailedToShowFullScreenContent()");
      onAdShowFail();
    }

    public void onAdShowedFullScreenContent() {
      Logger.debug(TAG, "onAdShowSuccess()");
      onAdShowSuccess();
    }

    public void onAdDismissedFullScreenContent() {
      Logger.debug(TAG, "onRewardedAdClosed()");
      onAdClosed(gotReward);
    }

    public void onAdImpression() {
    }

    public void onAdClicked() {
    }
  };

  private final OnUserEarnedRewardListener onUserEarnedRewardListener = new OnUserEarnedRewardListener() {
    @Override
    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
      gotReward = true;
    }
  };

  private final OnPaidEventListener onPaidEventListener = adValue -> {
    try {
      Logger.debug(TAG, String.format(Locale.US,
        "Paid event of value %d in currency %s of precision %s%n.",
        adValue.getValueMicros(),
        adValue.getCurrencyCode(),
        adValue.getPrecisionType())
      );

      String currencyCode = adValue.getCurrencyCode();
      int precisionType = adValue.getPrecisionType();
      long valueMicros = adValue.getValueMicros();
      String adNetwork = "admob";
      if (mRewardedAd != null) {
        adNetwork = mRewardedAd.getResponseInfo().getMediationAdapterClassName();
      }
      onGmsPaidEvent(adNetwork, "rewarded", "rewarded", getPlacementId(), currencyCode, precisionType, valueMicros);
    } catch(Throwable t) {
      Logger.error(TAG, "onPaidEvent exception", t);
    }
  };

  public AdmobRewardedAdapter(Context context, String gridName, IvyAdType adType) {
    super(context, gridName, adType);
  }

  public void fetch(Activity activity) {
    Logger.debug(TAG, "fetch()");

    AdmobManager.getInstance().makesureIntialized(activity);

    try {
      String placement = getPlacementId();
      if (placement == null || "".equals(placement)) {
        super.onAdLoadFailed("INVALID");
        return;
      }
      AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
      AdRequest adRequest = adRequestBuilder.build();

      RewardedAd.load(activity, placement, adRequest, new RewardedAdLoadCallback() {
        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
          AdmobRewardedAdapter.this.mRewardedAd = rewardedAd;
          AdmobRewardedAdapter.this.mRewardedAd.setFullScreenContentCallback(fullScreenContentCallback);
          AdmobRewardedAdapter.this.mRewardedAd.setOnPaidEventListener(onPaidEventListener);
          AdmobRewardedAdapter.this.onAdLoadSuccess();
        }

        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
          int errorCode = loadAdError.getCode();
          Logger.warning(TAG, "onAdFailedToLoad : " + errorCode);
          AdmobRewardedAdapter.this.mRewardedAd = null;
          AdmobRewardedAdapter.this.onAdLoadFailed(String.valueOf(errorCode));
        }
      });
    } catch(Throwable t) {
      t.printStackTrace();
    }
  }

  public void show(Activity activity) {
    Logger.debug(TAG, "show()");
    this.gotReward = false;
    if (this.mRewardedAd != null) {
      this.mRewardedAd.show(activity, onUserEarnedRewardListener);
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
