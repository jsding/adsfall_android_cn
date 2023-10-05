package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.ads.interfaces.IvyLoadStatus;
import com.ivy.util.Logger;
import com.my.target.ads.MyTargetView;

import org.json.JSONObject;


public final class MytargetBannerAdapter extends BannerAdapter<BannerAdapter.GridParams> {
  private static final String TAG = "Mytarget-Banner";
  private MyTargetView mAdview;

  public MytargetBannerAdapter(Context context, String gridName, IvyAdType adType) {
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
    this.mAdview = new MyTargetView(activity);
    this.mAdview.setSlotId(Integer.parseInt(placement));
    this.mAdview.setAdSize(MyTargetView.AdSize.ADSIZE_320x50);
    this.mAdview.setListener(new MyTargetView.MyTargetViewListener() {
      @Override
      public void onLoad(@NonNull MyTargetView myTargetView) {
        MytargetBannerAdapter.this.onAdLoadSuccess();
      }

      @Override
      public void onNoAd(@NonNull String s, @NonNull MyTargetView myTargetView) {
        MytargetBannerAdapter.this.onAdLoadFailed(IvyLoadStatus.NO_FILL);

      }


      @Override
      public void onShow(@NonNull MyTargetView myTargetView) {
        MytargetBannerAdapter.this.onAdShowSuccess();
      }

      @Override
      public void onClick(@NonNull MyTargetView myTargetView) {
        MytargetBannerAdapter.this.onAdClicked();
      }
    });
    this.mAdview.load();
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
