package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.util.Logger;
import com.yandex.mobile.ads.banner.AdSize;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;

import org.json.JSONObject;

public final class YandexBannerAdapter extends BannerAdapter<BannerAdapter.GridParams> {
  private static final String TAG = "Yandex-Banner";
  private BannerAdView mAdview;

  public YandexBannerAdapter(Context context, String gridName, IvyAdType adType) {
    super(context, gridName, adType);
  }

  public void fetch(Activity activity) {
    Logger.debug(TAG, "fetch admob banner begin");
    if (this.mAdview != null) {
      this.mAdview.destroy();
      this.mAdview = null;
    }

    String placement = getPlacementId();

    if (placement == null || "".equals(placement)) {
      Logger.warning(TAG, "invalid placement");
      super.onAdLoadFailed("INVALID");
      return;
    }
    mAdview = new BannerAdView(activity);
    this.mAdview.setAdUnitId(placement);

    Display display = activity.getWindowManager().getDefaultDisplay();
    DisplayMetrics outMetrics = new DisplayMetrics();
    display.getMetrics(outMetrics);
    float widthPixels = outMetrics.widthPixels;
    float density = outMetrics.density;
    int adWidth = (int) (widthPixels / density);
    this.mAdview.setAdSize(AdSize.stickySize(adWidth));
    this.mAdview.setBannerAdEventListener(new BannerAdEventListener() {
      @Override
      public void onAdLoaded() {
        YandexBannerAdapter.this.onAdLoadSuccess();
      }

      @Override
      public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
        YandexBannerAdapter.this.onAdLoadFailed("error");
      }

      @Override
      public void onAdClicked() {
        YandexBannerAdapter.this.onAdClicked();
      }

      @Override
      public void onLeftApplication() {

      }

      @Override
      public void onReturnedToApplication() {

      }

      @Override
      public void onImpression(@Nullable ImpressionData impressionData) {
        YandexBannerAdapter.this.onAdShowSuccess();
      }
    });
    AdRequest adRequest = new AdRequest.Builder().build();
    this.mAdview.loadAd(adRequest);
  }

  @Override
  public String getPlacementId() {
    return ((GridParams) getGridParams()).placement;
  }

  @Override
  public View getView() {
    return this.mAdview;
  }

  @Override
  public int getWidth() {
    return BannerAdapter.SMART_BANNER_WIDTH;
  }

  @Override
  public int getHeight() {
    return STANDARD_BANNER_HEIGHT;
  }

  protected GridParams newGridParams() {
    return new GridParams();
  }


  public static class GridParams extends BaseAdapter.GridParams {
    public String placement;
    @Override
    public GridParams fromJSON(JSONObject jsonObject) {
      super.fromJSON(jsonObject);
      this.placement = jsonObject.optString("placement");
      return this;
    }

    protected String getParams() {
      return "placement=" + this.placement;
    }
  }
}
