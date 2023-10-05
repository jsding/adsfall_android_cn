package com.ivy.ads;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.adsfall.R;
import com.ivy.IvySdk;
import com.ivy.ads.events.EventID;
import com.ivy.ads.events.EventParams;
import com.ivy.ads.interfaces.IvyAdCallbacks;
import com.ivy.ads.interfaces.IvyAdInfo;
import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.ads.interfaces.IvyBannerAd;
import com.ivy.ads.interfaces.IvyFullpageAd;
import com.ivy.ads.interfaces.IvySoftCallbacks;
import com.ivy.ads.utils.HandlerFactory;
import com.ivy.event.CommonEvents;
import com.ivy.event.EventBus;
import com.ivy.networks.grid.GridManager;
import com.ivy.networks.tracker.EventTracker;
import com.ivy.networks.util.Util;
import com.ivy.util.Logger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class IvyAdsManager implements IvyAdCallbacks, IvySoftCallbacks {
  private static final String TAG = IvyAdsManager.class.getCanonicalName();
  private FrameLayout mBannerContainer;

  private IvyBannerAd mBannerAds;

  private IvyFullpageAd mInterstitialAds;
  private IvyFullpageAd mRewardedAds;

  private Activity main;

  private final Map<IvyAdType, IvyAdCallbacks> adCallbacks = new HashMap<>();

  private boolean hasHomeAD = false;
  private static long lastHomeAdPauseTime = 0;
  private static long lastHomeAdShowTime= 0;

  private static int homeAdStaySecs = 10;
  private static int minHomeAdShowDurationSecs = 10;

  private volatile  boolean willDisplayingAd = false;

  public void onCreate(Activity main, EventTracker eventLogger, GridManager gridManager) {
    this.main = main;

    IvyAds.init(main, eventLogger, gridManager);

    this.mBannerAds = IvyAds.getBanners();

    this.mInterstitialAds = IvyAds.getInterstitials();
    this.mRewardedAds = IvyAds.getRewardedVideos();

    this.mBannerAds.setCallback(this);
    this.mInterstitialAds.setCallback(this);
    this.mRewardedAds.setCallback(this);

    this.mInterstitialAds.setSoftCallback(this);
    this.mRewardedAds.setSoftCallback(this);


    this.hasHomeAD = GridManager.getGridData().has("homeAd");
    if (hasHomeAD) {
      Logger.debug(TAG, "Home Ad Enabled");
      JSONObject homeAdSettings = GridManager.getGridData().optJSONObject("homeAd");
      if (homeAdSettings != null) {
        homeAdStaySecs = homeAdSettings.optInt("backgroundStayTime", 10);
        minHomeAdShowDurationSecs = homeAdSettings.optInt("minShowDuration", 10);
      }
    }
  }

  public void onPause(Activity activity) {
    IvyAds.onPause(activity);

    if (!willDisplayingAd) {
      lastHomeAdPauseTime = System.currentTimeMillis();
    } else {
      lastHomeAdPauseTime = 0L;
    }
  }

  public void onResume(Activity activity) {
    IvyAds.onResume(activity);

    if (hasHomeAD) {
      if (System.currentTimeMillis() - lastHomeAdShowTime < minHomeAdShowDurationSecs * 1000L) {
        return;
      }

      if (lastHomeAdPauseTime != 0 && System.currentTimeMillis() - lastHomeAdPauseTime > homeAdStaySecs * 1000L) {
        setAdCallback(IvyAdType.INTERSTITIAL, new IvyAdCallbacks() {
          @Override
          public void onAdClicked(IvyAdInfo adInfo) {
          }

          @Override
          public void onAdClosed(IvyAdInfo adInfo, boolean gotReward) {
            lastHomeAdShowTime = System.currentTimeMillis();
          }

          @Override
          public void onAdLoadFail(IvyAdType adInfo) {
          }

          @Override
          public void onAdLoadSuccess(IvyAdInfo adInfo) {
          }

          @Override
          public void onAdShowFail(IvyAdType adInfo) {
          }

          @Override
          public void onAdShowSuccess(IvyAdInfo adInfo) {
          }
        });
        showInterstitial(main, "homead");
      }
    }
  }

  public void onDestroy(Activity activity) {
    IvyAds.onDestroy(activity);
  }

  public void showBanners(Activity activity, FrameLayout container) {
    IvySdk.logEvent(EventID.CLICK_SHOW_BANNER, new Bundle());

    this.mBannerContainer = container;

    this.mBannerAds.show(activity, container);

  }

  public void setAdCallback(IvyAdType adType, IvyAdCallbacks callback) {
    this.adCallbacks.put(adType, callback);
  }

  public void hideBanners() {
    Logger.debug(TAG, "Hide Banner");

    if (this.mBannerAds != null) {
      this.mBannerAds.hide();
    }
  }

  public void disableBanners() {
    Logger.debug(TAG, "Disable Banner");

    hideBanners();
    if (this.mBannerContainer != null) {
      this.mBannerContainer.setVisibility(View.GONE);
    }
  }

  public void setBannerPosition(int position, Activity activity) {
    this.mBannerAds.setBannerPosition(position, activity);
  }

  public void fetchInterstitial(Activity activity) {
    this.mInterstitialAds.fetch(activity);
  }


  public void showInterstitial(Activity activity, String tag) {
    try {
      Bundle bundle = new Bundle();
      bundle.putString(EventParams.PARAM_LABEL, tag);
      IvySdk.logEvent(EventID.CLICK_SHOW_INTERSTITIAL, bundle);

      if (this.mInterstitialAds.isLoaded()) {
        willDisplayingAd = true;
        this.mInterstitialAds.show(activity, tag);
      } else {
          fetchInterstitial(activity);
          Log.d(TAG, "mInterstitialAds not ready");
          boolean isLoading = this.mInterstitialAds.isLoading();
          if (isLoading) {
            bundle.putString(EventParams.PARAM_LABEL, "1");
          }
          IvySdk.logEvent("ad_show_interstitial_skip", bundle);


      }
    } catch(Throwable ex) {
      ex.printStackTrace();
    }
  }

  public boolean haveInterstitial() {
    return this.mInterstitialAds.isLoaded();
  }


  public void fetchRewarded(Activity activity) {
    Logger.debug(TAG, "Fetch Rewarded called");
    this.mRewardedAds.fetch(activity);
  }

  public boolean haveRewardAd() {
    if (this.mRewardedAds == null) {
      Log.e(TAG, "Reward ad module not loaded");
      return false;
    }
    return this.mRewardedAds.isLoaded();
  }

  private boolean displayRewardVideoWhenLoaded = false;

  public void showRewarded(final Activity activity, final String tag) {
    try {
      if (this.mRewardedAds == null) {
        Logger.error(TAG, "Reward ad module not loaded, showRewarded is impossible");
        return;
      }
      Bundle bundle = new Bundle();
      bundle.putString(EventParams.PARAM_LABEL, tag);
      IvySdk.logEvent(EventID.CLICK_SHOW_REWARDED, bundle);
      if (this.mRewardedAds.isLoaded()) {
        willDisplayingAd = true;
        this.mRewardedAds.show(activity, tag);
      } else {

          boolean autoDisplayRewarded = GridManager.getGridData().optBoolean("auto_display_rewarded", false);
          if (autoDisplayRewarded) {
            showLoadingDialog(IvyAdType.REWARDED);
            displayRewardVideoWhenLoaded = true;
          }
          this.fetchRewarded(activity);

          Logger.debug(TAG, "Reward ad not ready");
          boolean isLoading = this.mRewardedAds.isLoading();
          if (isLoading) {
            bundle.putString("label", "1");
          }
          IvySdk.logEvent("ad_show_video_skip", bundle);

      }
    } catch(Exception ex) {
     // ignore
    }
  }

  public void onAdShowSuccess(IvyAdInfo adInfo) {
    Logger.debug(TAG, "onAdShowSuccess %s, %s", adInfo.getAdType(), adInfo.getAdapter().getName());
    if (this.adCallbacks.containsKey(adInfo.getAdType())) {
      this.adCallbacks.get(adInfo.getAdType()).onAdShowSuccess(adInfo);
    }
  }

  public void onAdShowFail(IvyAdType adType) {
    Logger.debug(TAG, "onAdShowFail %s", adType);
    willDisplayingAd = false;

    if (adCallbacks.containsKey(adType)) {
      adCallbacks.get(adType).onAdShowFail(adType);
    }

    if (adType == IvyAdType.REWARDED) {
      fetchRewarded(main);
    } else if (adType == IvyAdType.INTERSTITIAL) {
      fetchInterstitial(main);
    }
  }

  public void onAdLoadSuccess(IvyAdInfo adInfo) {
    if (adInfo == null) {
      return;
    }
    try {
      if (adCallbacks.containsKey(adInfo.getAdType())) {
        adCallbacks.get(adInfo.getAdType()).onAdLoadSuccess(adInfo);
      }

      EventBus.getInstance().fireEvent(CommonEvents.AD_LOADED, adInfo.getAdType().ordinal());

      switch (adInfo.getAdType()) {
        case REWARDED:
          if (displayRewardVideoWhenLoaded) {
            dismissLoadingDialog();
            displayRewardVideoWhenLoaded = false;
            this.mRewardedAds.show(main, "default");
          }
          return;
        case INTERSTITIAL:
          return;
        case NATIVE_AD:
          return;
        default:
          return;
      }
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public void onAdLoadFail(IvyAdType adType) {
    Logger.debug(TAG, "onAdLoadFail %s", adType);
    if (adCallbacks.containsKey(adType)) {
      adCallbacks.get(adType).onAdLoadFail(adType);
    }
    if (adType == IvyAdType.REWARDED) {
      dismissLoadingDialog();
      if (displayRewardVideoWhenLoaded) {
        displayRewardVideoWhenLoaded = false;
        Toast.makeText(main, main.getString(R.string.reward_video_load_failed), Toast.LENGTH_SHORT).show();
      }
      return;
    }
  }

  @Override
  public void onAdClosed(IvyAdInfo adInfo, boolean isReward) {
    Logger.debug(TAG, "onAdClosed %s", adInfo);

    // reset the home Ad show and pause time
    willDisplayingAd = false;
    lastHomeAdShowTime = System.currentTimeMillis();
    lastHomeAdPauseTime = System.currentTimeMillis();

    if (adInfo == null) {
      return;
    }

    try {
      if (adCallbacks != null) {
        if (adCallbacks.containsKey(adInfo.getAdType())) {
          adCallbacks.get(adInfo.getAdType()).onAdClosed(adInfo, isReward);
        }
      }

      IvyAdType adType = adInfo.getAdType();
      if (adType == IvyAdType.REWARDED) {
        displayRewardVideoWhenLoaded = false;
        fetchRewarded(main);
      } else if (adType == IvyAdType.INTERSTITIAL) {
        fetchInterstitial(main);
      }
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public void onAdClicked(IvyAdInfo adInfo) {
    Logger.debug(TAG, "onAdClicked %s", adInfo);
    if (adCallbacks.containsKey(adInfo.getAdType())) {
      adCallbacks.get(adInfo.getAdType()).onAdClicked(adInfo);
    }
  }

  public void softPause(IvyAdInfo o7AdInfo) {
  }

  public void softResume(IvyAdInfo o7AdInfo) {
  }

  public void preload(JSONObject config) {
    if (config == null) {
      return;
    }

    int preFetchRewardDelaySecs = config.optInt("preFetchRewardDelaySecs", 3);
    int preFetchInterstitialDelaySecs = config.optInt("preFetchInterstitialDelaySecs", 6);
    try {
      // fetchReward ad when ad providers setup, if no present
      Handler handler = HandlerFactory.createUiHandler();
      if (!mRewardedAds.isLoaded()) {
        if (preFetchRewardDelaySecs > 0) {
          handler.postDelayed(() -> fetchRewarded(main), preFetchRewardDelaySecs * 1000L);
        }
      }

      if (!mInterstitialAds.isLoaded()) {
        if (preFetchInterstitialDelaySecs > 0) {
          handler.postDelayed(() -> fetchInterstitial(main), preFetchInterstitialDelaySecs * 1000L);
        }
      }

    } catch(Throwable t) {
      Logger.error(TAG, "Error preload ads", t);
    }
  }


  public int getBannerHeightInPx(Context context) {
    int height = 0;
    if (this.mBannerContainer != null) {
      height = this.mBannerContainer.getHeight();
    }
    if (height == 0) {
      return (int) (51.0f * context.getResources().getDisplayMetrics().density);
    }
    return height;
  }

  public boolean hasEnoughTimePassedToFetchInterstitial(Context context) {
    return true;
  }



  /**
   * 检查是否有退出广告位，如果有，退出广告位完毕,退出游戏
   */
  public void onQuit() {

  }


  private AlertDialog loadingDialog;

  private void showLoadingDialog(IvyAdType adType) {
    try {
      if (this.loadingDialog == null) {
        this.loadingDialog = new AlertDialog.Builder(main).create();
      }
      this.loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable());
      this.loadingDialog.setCancelable(false);
      this.loadingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
        @Override
        public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
          if (i == KeyEvent.KEYCODE_SEARCH || i == KeyEvent.KEYCODE_BACK) {
            displayRewardVideoWhenLoaded = false;
            dismissLoadingDialog();
            onAdLoadFail(adType);
            return false;
          }
          return false;
        }
      });
      this.loadingDialog.show();

      View inflate = LayoutInflater.from(main).inflate(R.layout.loading_alert, null);
      if (inflate != null) {
        this.loadingDialog.setContentView(inflate);
        this.loadingDialog.setCanceledOnTouchOutside(false);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void dismissLoadingDialog() {
    if (this.loadingDialog != null && this.loadingDialog.isShowing()) {
      this.loadingDialog.dismiss();
      this.loadingDialog = null;
    }
  }
}
