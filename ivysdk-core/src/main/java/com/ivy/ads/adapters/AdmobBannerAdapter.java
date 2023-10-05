package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.util.Logger;

import org.json.JSONObject;

import java.util.Locale;

public final class AdmobBannerAdapter extends BannerAdapter<BannerAdapter.GridParams> {
  private static final String TAG = "Admob-Banner";
  private AdView mAdview;
  private AdSize mAdSize;
  private boolean isSmartBanner = false;

  public AdmobBannerAdapter(Context context, String gridName, IvyAdType adType) {
    super(context, gridName, adType);
  }


  public void fetch(Activity activity) {
    Logger.debug(TAG, "fetch admob banner begin");
    AdmobManager.getInstance().makesureIntialized(activity);
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
    this.mAdview = new AdView(activity);
    this.mAdview.setAdUnitId(placement);

    String setSize = getSize();
    if (setSize != null) {
      if ("banner".equals(setSize)) {
        this.mAdSize = AdSize.BANNER;
        this.mAdview.setAdSize(this.mAdSize);
      } else {
        this.mAdSize = AdSize.SMART_BANNER;
        this.mAdview.setAdSize(this.mAdSize);
      }
    } else {
      if (isAdaptive()) {
        this.mAdSize = getAdSize(activity);
        // Step 4 - Set the adaptive ad size on the ad view.
        this.mAdview.setAdSize(this.mAdSize);
      } else {
        boolean z = isHeightLowerThan720DpAngHigherThan400Dp(activity) && isSmartBannerEnabled();
        this.isSmartBanner = z;
        AdSize adSize = isTablet() ? AdSize.LEADERBOARD : this.isSmartBanner ? AdSize.SMART_BANNER : AdSize.BANNER;
        this.mAdview.setAdSize(adSize);
      }
    }


    this.mAdview.setAdListener(new AdListener() {
      @Override
      public void onAdClosed() {
        Logger.debug(TAG, "onAdClosed()");
        AdmobBannerAdapter.this.onAdClosed(false);
      }

      @Override
      public void onAdClicked() {
        Logger.debug(TAG, "onAdClicked()");
        AdmobBannerAdapter.this.onAdClicked();
      }

      @Override
      public void onAdFailedToLoad(LoadAdError loadAdError) {
        int errorCode = loadAdError.getCode();
        Logger.debug(TAG, "errorCode: %s", errorCode);
        String reason = AdmobManager.errorCodeToMessage(errorCode);
        AdmobBannerAdapter.this.onAdLoadFailed(reason);

      }

      @Override
      public void onAdOpened() {
        Logger.debug(TAG, "onAdOpened()");
        AdmobBannerAdapter.this.onAdShowSuccess();
      }

      @Override
      public void onAdLoaded() {
        Logger.debug(TAG, "onAdLoaded()");

        AdmobBannerAdapter.this.onAdLoadSuccess();
      }

      @Override
      public void onAdImpression() {
      }
    });

    this.mAdview.setOnPaidEventListener(new OnPaidEventListener() {
      @Override
      public void onPaidEvent(AdValue adValue) {
        if (adValue == null) {
          return;
        }

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


          onGmsPaidEvent("admob", "banner", "banner", getPlacementId(), currencyCode, precisionType, valueMicros);
        } catch (Throwable t) {
          Logger.error(TAG, "onPaidEvent exception", t);
        }
      }
    });


    AdRequest.Builder adRequestBuilder = new AdRequest.Builder();

    String collapsible = ((GridParams) getGridParams()).collapsible;
    if (collapsible != null && !"none".equals(collapsible)) {
      Bundle extras = new Bundle();
      extras.putString("collapsible", collapsible);
      adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
    }
    this.mAdview.loadAd(adRequestBuilder.build());
  }

  @Override
  public void show(Activity activity) {
    super.show(activity);
    try {
      if (mAdview != null)
        mAdview.resume();
    } catch (Exception e) {
    }
  }

  @Override
  public void hide() {
    super.hide();
    try {
      if (mAdview != null) {
        mAdview.pause();
      }
    } catch (Exception e) {
    }
  }

  @Override
  public String getPlacementId() {
    if (isDebugMode()) {
      Logger.debug(TAG, "in debug mode, will use test id ");
      return "ca-app-pub-3940256099942544/6300978111";
    }
    return ((GridParams) getGridParams()).placement;
  }

  public boolean isAdaptive() {
    return ((GridParams) getGridParams()).adaptive;
  }

  public String getSize() {
    return ((GridParams) getGridParams()).size;
  }

  public View getView() {
    return this.mAdview;
  }

  private boolean isHeightLowerThan720DpAngHigherThan400Dp(Activity activity) {
    DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
    float dpHeight = ((float) displayMetrics.heightPixels) / displayMetrics.density;
    Logger.debug(TAG, "screen height in dp = %s", Float.valueOf(dpHeight));
    return dpHeight < 720.0f && dpHeight > 400.0f;
  }


  private AdSize getAdSize(Activity activity) {
    // Step 2 - Determine the screen width (less decorations) to use for the ad width.
    Display display = activity.getWindowManager().getDefaultDisplay();
    DisplayMetrics outMetrics = new DisplayMetrics();
    display.getMetrics(outMetrics);

    float widthPixels = outMetrics.widthPixels;
    float density = outMetrics.density;

    int adWidth = (int) (widthPixels / density);

    // Step 3 - Get adaptive ad size and return for setting on the ad view.
    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
  }

  @Override
  public int getWidth() {
    // Determine the screen width (less decorations) to use for the ad width.
    if (isAdaptive() && this.mAdSize != null) {
      return this.mAdSize.getWidth();
    }

    return BannerAdapter.SMART_BANNER_WIDTH;
  }

  @Override
  public int getHeight() {
    if (isAdaptive() && this.mAdSize != null) {
      return this.mAdSize.getHeight();
    }
    return STANDARD_BANNER_HEIGHT;
  }

  protected GridParams newGridParams() {
    return new GridParams();
  }


  public static class GridParams extends BaseAdapter.GridParams {
    public String placement;
    public boolean adaptive = true;
    public String size = null;
    public String collapsible = null;

    @Override
    public GridParams fromJSON(JSONObject jsonObject) {
      super.fromJSON(jsonObject);
      this.placement = jsonObject.optString("placement");

      this.adaptive = jsonObject.optBoolean("adaptive", true);
      this.size = jsonObject.optString("size", null);
      this.collapsible = jsonObject.optString("collapsible", "none");
      return this;
    }

    protected String getParams() {
      return "placement=" + this.placement;
    }
  }
}
