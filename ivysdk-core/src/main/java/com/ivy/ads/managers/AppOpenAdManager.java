package com.ivy.ads.managers;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.ivy.IvySdk;
import com.ivy.networks.tracker.EventTracker;
import com.ivy.util.Logger;

import java.util.Date;
import java.util.Locale;

public class AppOpenAdManager implements DefaultLifecycleObserver, OnPaidEventListener {
  private static final String TAG = "AppOpenAdManager";

  private String placement = "ca-app-pub-3940256099942544/3419835294";
  private AppOpenAd appOpenAd = null;
  private boolean isLoadingAd = false;
  private boolean isShowingAd = false;

  /**
   * Keep track of the time an app open ad is loaded to ensure you don't show an expired ad.
   */
  private long loadTime = 0;

  private Activity currentActivity;

  private EventTracker eventTracker;

  private int apaMinDisplayGapsSecs = 300;

  private long lastAdDisplayTime = 0L;

  /**
   * Constructor.
   */
  public AppOpenAdManager(@NonNull final Activity activity, @NonNull EventTracker eventTracker, @NonNull String placement) {
    currentActivity = activity;
    this.placement = placement;
    this.eventTracker = eventTracker;

    this.apaMinDisplayGapsSecs = IvySdk.getRemoteConfigAsInt("apa_min_display_gaps_secs");
    if (this.apaMinDisplayGapsSecs == 0) {
      this.apaMinDisplayGapsSecs = 300;
    }

    lastAdDisplayTime = System.currentTimeMillis();
    try {
      ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    } catch (Throwable ignored) {

    }
  }

  public void destroy() {
    try {
      ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
    } catch (Throwable ignored) {

    }
  }

  /**
   * DefaultLifecycleObserver method that shows the app open ad when the app moves to foreground.
   */
  @Override
  public void onStart(@NonNull LifecycleOwner owner) {
    DefaultLifecycleObserver.super.onStart(owner);
    if (System.currentTimeMillis() - lastAdDisplayTime < apaMinDisplayGapsSecs) {
      return;
    }

    // Show the ad (if available) when the app moves to foreground.
    showAdIfAvailable(currentActivity);
  }

  /**
   * Load an ad.
   *
   * @param context the context of the activity that loads the ad
   */
  private void loadAd(Context context) {
    // Do not load ad if there is an unused ad or one is already loading.
    if (isLoadingAd || isAdAvailable()) {
      return;
    }

    isLoadingAd = true;
    AdRequest request = new AdRequest.Builder().build();
    AppOpenAd.load(
      context,
      placement,
      request,
      new AppOpenAd.AppOpenAdLoadCallback() {
        /**
         * Called when an app open ad has loaded.
         *
         * @param ad the loaded app open ad.
         */
        @Override
        public void onAdLoaded(AppOpenAd ad) {
          appOpenAd = ad;
          appOpenAd.setOnPaidEventListener(AppOpenAdManager.this);
          isLoadingAd = false;
          loadTime = (new Date()).getTime();

          Logger.debug(TAG, "onAdLoaded.");
        }

        /**
         * Called when an app open ad has failed to load.
         *
         * @param loadAdError the error.
         */
        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
          isLoadingAd = false;
          Logger.debug(TAG, "onAdFailedToLoad: " + loadAdError.getMessage());
        }
      });
  }

  /**
   * Check if ad was loaded more than n hours ago.
   */
  private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
    long dateDifference = (new Date()).getTime() - loadTime;
    long numMilliSecondsPerHour = 3600000;
    return (dateDifference < (numMilliSecondsPerHour * numHours));
  }

  /**
   * Check if ad exists and can be shown.
   */
  private boolean isAdAvailable() {
    // Ad references in the app open beta will time out after four hours, but this time limit
    // may change in future beta versions. For details, see:
    // https://support.google.com/admob/answer/9341964?hl=en
    return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
  }


  /**
   * Show the ad if one isn't already showing.
   *
   * @param activity the activity that shows the app open ad
   */
  public void showAdIfAvailable(@NonNull final Activity activity) {
    // If the app open ad is already showing, do not show the ad again.
    if (isShowingAd) {
      Logger.debug(TAG, "The app open ad is already showing.");
      return;
    }

    // If the app open ad is not available yet, invoke the callback then load the ad.
    if (!isAdAvailable()) {
      Logger.debug(TAG, "The app open ad is not ready yet.");
      loadAd(activity);
      return;
    }

    Logger.debug(TAG, "Will show ad.");

    appOpenAd.setFullScreenContentCallback(
      new FullScreenContentCallback() {
        /** Called when full screen content is dismissed. */
        @Override
        public void onAdDismissedFullScreenContent() {
          // Set the reference to null so isAdAvailable() returns false.
          appOpenAd = null;
          isShowingAd = false;

          Logger.debug(TAG, "onAdDismissedFullScreenContent.");

          loadAd(activity);
        }

        /** Called when fullscreen content failed to show. */
        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
          appOpenAd = null;
          isShowingAd = false;

          Logger.debug(TAG, "onAdFailedToShowFullScreenContent: " + adError.getMessage());

          loadAd(activity);
        }

        /** Called when fullscreen content is shown. */
        @Override
        public void onAdShowedFullScreenContent() {
          Logger.debug(TAG, "onAdShowedFullScreenContent.");

          lastAdDisplayTime = System.currentTimeMillis();
        }
      });


    isShowingAd = true;
    appOpenAd.show(activity);
  }


  @Override
  public void onPaidEvent(@NonNull AdValue adValue) {
    Logger.debug(TAG, String.format(Locale.US,
      "Paid event of value %d in currency %s of precision %s%n.",
      adValue.getValueMicros(),
      adValue.getCurrencyCode(),
      adValue.getPrecisionType())
    );

    String currencyCode = adValue.getCurrencyCode();
    int precisionType = adValue.getPrecisionType();
    long valueMicros = adValue.getValueMicros();

    double revenue = valueMicros / 1000000.0f;
    // 如果单次展示<=0,或大于20，觉得是平台传过来的脏数据，忽略掉
    if (revenue >= 20.0 || revenue <= 0) {
      return;
    }

    if (eventTracker != null) {
      eventTracker.pingROAS((float) revenue, "ad");
      Bundle bundle = new Bundle();
      bundle.putDouble(FirebaseAnalytics.Param.VALUE, revenue);
      bundle.putString(FirebaseAnalytics.Param.CURRENCY, currencyCode);

      eventTracker.afAdImpressionPing(bundle, revenue);
    }
  }
}

