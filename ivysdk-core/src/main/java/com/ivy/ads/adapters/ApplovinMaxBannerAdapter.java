package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.util.Logger;

import org.json.JSONObject;

public class ApplovinMaxBannerAdapter extends BannerAdapter<BannerAdapter.GridParams> implements MaxAdViewAdListener, MaxAdRevenueListener {
  private static final String TAG = "Applovin-Max-Banner";
  private MaxAdView bannerAdView;

  public ApplovinMaxBannerAdapter(Context context, String gridName, IvyAdType adType) {
    super(context, gridName, adType);
  }

  @Override
  public String getPlacementId() {
    return ((GridParams) getGridParams()).zoneId;
  }

  @Override
  public View getView() {
    return this.bannerAdView;
  }

  @Override
  public void fetch(Activity activity) {
    if (isBannerHide) {
      onAdLoadFailed("banner hide");
      return;
    }
    AppLovinSdk sdk = ApplovinManager.getInstance(activity, null);
    if (sdk == null) {
      Logger.error(TAG, "Applovin SDK initialize failed");
      super.onAdLoadFailed("not_init");
      return;
    }

    this.bannerAdView = new MaxAdView(getPlacementId(), activity);
    this.bannerAdView.setListener(this);
    this.bannerAdView.setRevenueListener(this);

    int width = ViewGroup.LayoutParams.MATCH_PARENT;

    // Get the adaptive banner height.
    int heightDp = MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight();
    int heightPx = AppLovinSdkUtils.dpToPx(activity, heightDp);

    this.bannerAdView.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx));
    this.bannerAdView.setExtraParameter("adaptive_banner", "true");

    this.bannerAdView.loadAd();
  }

  protected GridParams newGridParams() {
    return new GridParams();
  }


  @Override
  public void onAdExpanded(MaxAd ad) {
  }

  @Override
  public void onAdCollapsed(MaxAd ad) {
  }


  @Override
  public void onAdLoaded(MaxAd ad) {
    Logger.debug(TAG, "onAdLoaded()");
    super.onAdLoadSuccess();
  }

  @Override
  public void onAdDisplayed(final MaxAd maxAd) { /* DO NOT USE - THIS IS RESERVED FOR FULLSCREEN ADS ONLY AND WILL BE REMOVED IN A FUTURE SDK RELEASE */ }

  @Override
  public void onAdHidden(final MaxAd maxAd) { /* DO NOT USE - THIS IS RESERVED FOR FULLSCREEN ADS ONLY AND WILL BE REMOVED IN A FUTURE SDK RELEASE */ }


  @Override
  public void onAdRevenuePaid(MaxAd ad) {
    try {
      if (ad == null) {
        return;
      }
      double revenue = ad.getRevenue(); // In USD
      if (revenue <= 0) {
        return;
      }
      String networkName = ad.getNetworkName(); // Display name of the network that showed the ad (e.g. "AdColony")
      String adUnitId = ad.getAdUnitId(); // The MAX Ad Unit ID
      String placement = ad.getPlacement(); // The placement this ad's postbacks are tied to
      String networkPlacement = ad.getNetworkPlacement(); // The placement ID from the network that showed the ad
      super.onGmsPaidEvent(networkName, "banner", placement, adUnitId, "USD", 0, (long) (revenue * 1000000.0f));
    } catch (Throwable t) {
      Logger.error(TAG, "onAdRevenuePaid exception", t);
    }
  }

  @Override
  public void show(Activity activity, AdOpenCloseCallback adOpenCloseCallback) {
    super.show(activity, adOpenCloseCallback);
    if (this.bannerAdView != null) {
      this.bannerAdView.startAutoRefresh();
    }
  }

  @Override
  public void show(Activity activity) {
    super.show(activity);
    Log.e("AppLovin", "call to start AutoRefresh ");
    if (this.bannerAdView != null) {
      this.bannerAdView.startAutoRefresh();
    }
  }

  @Override
  public void hide() {
    super.hide();
    Log.e("AppLovin", "call to stop AutoRefresh ");
    if (this.bannerAdView != null) {
      this.bannerAdView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
      this.bannerAdView.stopAutoRefresh();
    }
  }

  @Override
  public void onAdClicked(MaxAd ad) {
    Logger.debug(TAG, "onAdClicked");
    super.onAdClicked();
  }

  @Override
  public void onAdLoadFailed(String adUnitId, MaxError error) {
    Logger.warning(TAG, "onAdLOadFailed " + adUnitId + ", " + error.getMessage());
    super.onAdLoadFailed(String.valueOf(error.getCode()));
  }

  @Override
  public void onAdDisplayFailed(MaxAd ad, MaxError error) {
    Logger.warning(TAG, "onAdDisplayFailed");
    super.onAdShowFail();
  }

  public static class GridParams extends com.ivy.ads.adapters.BaseAdapter.GridParams {
    public String zoneId = "";

    @Override
    public GridParams fromJSON(JSONObject jsonObject) {
      super.fromJSON(jsonObject);
      this.zoneId = jsonObject.optString("placement");
      return this;
    }

    protected String getParams() {
      return "placement=" + (this.zoneId != null ? ", zoneId=" + this.zoneId : "");
    }
  }
}
