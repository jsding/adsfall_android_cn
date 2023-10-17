package com.android.client;

import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;

import com.adsfall.R;
import com.alibaba.fastjson.JSON;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.ivy.IvySdk;
import com.ivy.IvyUtils;
import com.ivy.ads.interfaces.IvyAdCallbacks;
import com.ivy.ads.interfaces.IvyAdInfo;
import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.PurchaseStateChangeData;
import com.ivy.event.CommonEvents;
import com.ivy.event.EventBus;
import com.ivy.event.EventListener;
import com.ivy.facebook.FacebookLoginListener;
import com.ivy.facebook.FacebookUserManager;
import com.ivy.help.TiledeskActivity;
import com.ivy.internal.WebViewActivity;
import com.ivy.networks.grid.GridManager;
import com.ivy.util.CommonUtil;
import com.ivy.util.Logger;
import com.sherdle.universal.MainActivity;
import com.smarx.notchlib.NotchScreenManager;

import org.checkerframework.checker.units.qual.C;
import org.json.JSONObject;

import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AndroidSdk {
  public static final String TAG = "AndroidSdk";
  private static Builder builder;
  private static FacebookUserManager facebookUserManager = null;

  public static final int POS_LEFT_TOP = 1;
  public static final int POS_LEFT_BOTTOM = 2;
  public static final int POS_CENTER_TOP = 3;
  public static final int POS_CENTER_BOTTOM = 4;
  public static final int POS_CENTER = 5;
  public static final int POS_RIGHT_TOP = 6;
  public static final int POS_RIGHT_BOTTOM = 7;

  private static BuilderListener sdkListener;
  // public static SDKUserCenterListener sdkUserCenterListener;

  private static boolean paymentSystemValid = false;

  private static boolean sdkInitialized = false;


  interface HomeAdListener {
    void showLoading();

    void closeLoading();
  }

  public static class Builder {
    PaymentSystemListener paymentResultListener;
    UserCenterListener userCenterListener;
    UrlListener urlListener;
    SdkResultListener sdkResultListener;
    AdEventListener adEventListener;
    AdLoadedListener adLoadedListener;
    EventOccurredListener eventOccurredListener;
    DeepLinkReceivedListener deepLinkReceivedListener;
    InAppMessageListener inAppMessageListener;
    GoogleListener googleListener;

    OfferwallCreditListener offerwallCreditListener;


    OnRemoteConfigLoadListener onRemoteConfigLoadListener;

    OnGameMessageListener onGameMessageListener;

    public Builder setPaymentListener(PaymentSystemListener listener) {
      this.paymentResultListener = listener;
      return this;
    }

    public Builder setAdLoadedListener(AdLoadedListener listener) {
      this.adLoadedListener = listener;
      return this;
    }

    public Builder setGoogleListener(GoogleListener listener) {
      this.googleListener = listener;
      return this;
    }

    public Builder setDeepLinkReceivedListener(DeepLinkReceivedListener listener) {
      this.deepLinkReceivedListener = listener;
      return this;
    }

    public Builder setEventOccurredListener(EventOccurredListener listener) {
      this.eventOccurredListener = listener;
      return this;
    }

    public Builder setUserCenterListener(UserCenterListener listener) {
      this.userCenterListener = listener;
      return this;
    }

    public Builder setUrlListener(UrlListener listener) {
      this.urlListener = listener;
      return this;
    }

    public Builder setSdkResultListener(SdkResultListener listener) {
      this.sdkResultListener = listener;
      return this;
    }

    public Builder setAdEventListener(AdEventListener listener) {
      this.adEventListener = listener;
      return this;
    }

    public Builder setInAppMessageClickListener(InAppMessageListener inAppMessageListener) {
      this.inAppMessageListener = inAppMessageListener;
      return this;
    }

    public Builder setOfferwallCreditListener(OfferwallCreditListener offerwallCreditListener) {
      this.offerwallCreditListener = offerwallCreditListener;
      return this;
    }

    public Builder setOnRemoteConfigLoadListener(OnRemoteConfigLoadListener onRemoteConfigLoadListener) {
      this.onRemoteConfigLoadListener = onRemoteConfigLoadListener;
      return this;
    }

    public Builder setOnGameMessageListener(OnGameMessageListener onGameMessageListener) {
      this.onGameMessageListener = onGameMessageListener;
      return this;
    }
  }

  public synchronized static void onCreate(@NonNull Activity activity, @NonNull Builder builder) {
    if (sdkListener == null) {
      sdkListener = new BuilderListener();
    }

    AndroidSdk.builder = builder;
    sdkListener.setBuilder(builder);

    if (builder.adEventListener != null) {
      registerAdEventListener(builder.adEventListener);
    } else {
      registerAdEventListener(new AdEventListener());
    }

    IvySdk.updateCurrentActivity(activity);

    if (sdkInitialized) {
      Logger.warning(TAG, "Already initialized");
      return;
    }

    sdkInitialized = true;

    IvySdk.initialize(activity, null, new IvySdk.InitializeCallback() {
      @Override
      public void onInitialized() {

      }

      @Override
      public void onRemoteConfigUpdated() {
        if (builder.sdkResultListener != null) {
          builder.sdkResultListener.onReceiveServerExtra("{}");
        }

        if (builder.onRemoteConfigLoadListener != null) {
          builder.onRemoteConfigLoadListener.onRemoteConfigPrepared();
        }
      }
    });

    EventBus.getInstance().addListener(CommonEvents.AD_LOADED, new EventListener() {
      @Override
      public void onEvent(int i, Object obj) {
        if (i == CommonEvents.AD_LOADED) {
          if (obj instanceof Integer) {
            int adType = (Integer) obj;
            if (builder.adLoadedListener != null) {
              builder.adLoadedListener.onAdLoaded(adType);
            }
          }
        }
      }
    });

    EventBus.getInstance().addListener(CommonEvents.BILLING_PURCHASE_STATE_CHANGE, new EventListener() {
      @Override
      public void onEvent(int i, Object obj) {
        if (i != CommonEvents.BILLING_PURCHASE_STATE_CHANGE) {
          return;
        }
        Logger.debug(TAG, "purchase event called");
        if (obj instanceof PurchaseStateChangeData) {
          PurchaseStateChangeData changeData = (PurchaseStateChangeData) obj;
          Logger.debug(TAG, "Purchased: " + changeData);

          String itemId = changeData.getItemId();
          JSONObject storeItem = IvySdk.getStoreItem(itemId);
          if (storeItem == null) {
            Logger.error(TAG, "Not found billId for product: " + itemId);
            return;
          }
          int billId = storeItem.optInt("billId");
          if (changeData.getPurchaseState() == PurchaseManager.PurchaseState.PURCHASED) {
            if (builder.paymentResultListener != null) {
              String orderId = changeData.getOrderId();
              String developerPayload = changeData.getDeveloperPayload();
              String purchaseToken = changeData.getPurchaseToken();

              Logger.debug(TAG, "send paymentResult for bill: " + billId);
              Logger.debug(TAG, "orderID: " + orderId);
              Logger.debug(TAG, "purchaseToken: " + purchaseToken);

              Logger.debug(TAG, "onPaymentSuccessWithPurchase >>> " + developerPayload);
              builder.paymentResultListener.onPaymentSuccessWithPurchase(billId, orderId, purchaseToken, developerPayload);

            } else {
              Logger.error(TAG, "onPaymentSuccess failed, no payment callback");
            }
          } else if (changeData.getPurchaseState() == PurchaseManager.PurchaseState.CANCELED) {
            if (builder.paymentResultListener != null) {
              Logger.debug(TAG, "send payment cancelled result for bill: " + billId);
              builder.paymentResultListener.onPaymentCanceled(billId);
            } else {
              Logger.error(TAG, "onPaymentCanceled failed, no payment callback");
            }
          } else if (changeData.getPurchaseState() == PurchaseManager.PurchaseState.ERROR) {
            if (builder.paymentResultListener != null) {
              Logger.debug(TAG, "send payment error result for bill: " + billId);
              builder.paymentResultListener.onPaymentFail(billId);
            } else {
              Logger.error(TAG, "onPaymentFail failed, no payment callback");
            }
          }
        }
      }
    });

    EventBus.getInstance().addListener(CommonEvents.BILLING_BECOMES_AVAILABLE, new EventListener() {
      @Override
      public void onEvent(int i, Object obj) {
        if (i != CommonEvents.BILLING_BECOMES_AVAILABLE) {
          return;
        }
        Logger.debug(TAG, "BILLING_BECOMES_AVAILABLE");
        try {
          if (builder.paymentResultListener != null) {
            paymentSystemValid = true;
            builder.paymentResultListener.onPaymentSystemValid();
          }
        } catch (Throwable t) {
          // ignore
          paymentSystemValid = false;
        }
      }
    });

    boolean slientLoginGoogle = IvySdk.getGridConfigBoolean("slientLoginGoogle");
    if (slientLoginGoogle) {
      Logger.debug(TAG, "Set to slient Login");
      IvySdk.slientLoginGoogle(builder.googleListener);
    }

    try {
      sdkListener.onInitialized();
      facebookUserManager = new FacebookUserManager();
    } catch (Throwable t) {
      Logger.error(TAG, "sdk onInit exception", t);
    }

    if (activity.getIntent() != null) {
      handleIntent(activity.getIntent());
    }

    if (IvySdk.getGridConfigBoolean("enableInAppMessage", false)) {
      IvySdk.registerInAppMessageService(builder.inAppMessageListener);
    }
  }


  public static void setHomeAdListener(HomeAdListener listener) {
  }

  public static void onNewIntent(Intent intent) {
    Logger.debug(TAG, "onNewIntent");
    handleIntent(intent);
  }

  public static String encodeParams(String params) {
    return CommonUtil.encodeParams(IvySdk.CONTEXT, params);
  }

  public static String decodeParams(String params) {
    return CommonUtil.decodeParams(IvySdk.CONTEXT, params);
  }


  public static void registerAdEventListener(final AdEventListener listener) {
  }

  public static void pushLocalMessage(String key, String title, String content, long pushTime, int interval, boolean useSound, String soundName, String userInfo) {
    if (pushTime > 0 && pushTime < System.currentTimeMillis() / 1000) {
      pushTime = System.currentTimeMillis() + pushTime * 1000;
    } else {
      pushTime *= 1000;
    }
    title = IvyUtils.replaceEmojiText(title);
    content = IvyUtils.replaceEmojiText(content);
    IvySdk.push(key, title, content, pushTime, false, null, IvySdk.getUUID(), null, 0, useSound, soundName, userInfo);
  }

  public static void cancelLocalMessage(String key) {
    IvySdk.cancelPush(key);
  }

  @Deprecated
  public static void cancelMessage(String key) {
    Log.e(TAG, "cancelMessage deprecated");
  }


  public static void onPause() {
    IvySdk.onPause();
  }

  public static void onResume() {
    IvySdk.onResume();
  }

  public static void onDestroy(Activity activity) {
  }

  public static void onDestroy() {
    Logger.debug(TAG, "AndroidSdk onDestroy");
    IvySdk.onDestroy();
    sdkInitialized = false;
  }

  public static void onActivityResult(int requestCode, int resultCode, Intent data) {
    Logger.debug(TAG, "onActivityResult(), requestCode: " + requestCode);
    Activity activity = IvySdk.getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(() -> {
      IvySdk.onActivityResult(requestCode, resultCode, data);
      if (facebookUserManager != null) {
        facebookUserManager.onActivityResult(requestCode, resultCode, data);
      }
    });
  }

  public static void setUserTag(String userTag) {
//        SdkServer.setUserTag(userTag);
  }

  public static int getRemoteConfigInt(String key) {
    return IvySdk.getRemoteConfigAsInt(key);
  }

  public static long getRemoteConfigLong(String key) {
    return IvySdk.getRemoteConfigAsLong(key);
  }

  public static double getRemoteConfigDouble(String key) {
    return IvySdk.getRemoteConfigAsDouble(key);
  }

  public static boolean getRemoteConfigBoolean(String key) {
    return IvySdk.getRemoteConfigAsBoolean(key);
  }

  public static String getRemoteConfigString(String key) {
    return IvySdk.getRemoteConfigAsString(key);
  }

  public static boolean hasFull(String tag) {
    try {
      boolean got = IvySdk.haveInterstitial();
      if (!got) {
        Logger.debug(TAG, "No full, try to fetch one");
        IvySdk.fetchInterstitial();
      }
      return IvySdk.haveInterstitial();
    } catch (Throwable t) {
      //
      return false;
    }
  }

  public static final String FULL_TAG_START = "start";
  public static final String FULL_TAG_PAUSE = "pause";
  public static final String FULL_TAG_PASS_LEVEL = "passlevel";
  public static final String FULL_TAG_EXIT = "exit";
  public static final String FULL_TAG_CUSTOM = "custom";

  @Deprecated
  public static void moreGame() {
  }

  public static void showFullAd(String tag) {
    try {
      showFullAd(tag, new AdListener());
    } catch (Exception ex) {
      //
    }
  }

  public static void showFullAd(String tag, final AdListener listener) {
    Logger.debug(TAG, "showFullAd called");
    try {
      IvySdk.setAdCallback(IvyAdType.INTERSTITIAL, new IvyAdCallbacks() {
        @Override
        public void onAdClicked(IvyAdInfo adInfo) {
          if (listener != null) {
            listener.onAdClicked();
          }
        }

        @Override
        public void onAdClosed(IvyAdInfo adInfo, boolean gotReward) {
          if (listener != null) {
            listener.onAdClosed();
          }
        }

        @Override
        public void onAdLoadFail(IvyAdType adInfo) {
          if (listener != null) {
            listener.onAdLoadFails();
          }
        }

        @Override
        public void onAdLoadSuccess(IvyAdInfo adInfo) {
          if (listener != null) {
            listener.onAdLoadSuccess();
          }
        }

        @Override
        public void onAdShowFail(IvyAdType adInfo) {
          if (listener != null) {
            listener.onAdShowFails();
          }
        }

        @Override
        public void onAdShowSuccess(IvyAdInfo adInfo) {
          if (listener != null) {
            listener.onAdShow();
          }
        }
      });
      IvySdk.showInterstitialAd(tag);
    } catch (Throwable t) {
      //
    }
  }

  public static void loadFullAd(String tag, @NonNull AdListener listener) {
    try {
      IvySdk.setAdCallback(IvyAdType.INTERSTITIAL, new IvyAdCallbacks() {
        @Override
        public void onAdClicked(IvyAdInfo adInfo) {
          listener.onAdClicked();
        }

        @Override
        public void onAdClosed(IvyAdInfo adInfo, boolean gotReward) {
          listener.onAdClosed();
        }

        @Override
        public void onAdLoadFail(IvyAdType adInfo) {
          listener.onAdLoadFails();
        }

        @Override
        public void onAdLoadSuccess(IvyAdInfo adInfo) {
          listener.onAdLoadSuccess();
        }

        @Override
        public void onAdShowFail(IvyAdType adInfo) {
          listener.onAdShowFails();
        }

        @Override
        public void onAdShowSuccess(IvyAdInfo adInfo) {
          listener.onAdShow();
        }
      });
      IvySdk.fetchInterstitial();
    } catch (Throwable t) {
      //
    }
  }

  public static void showRewardAd(String tag, @NonNull AdListener listener) {
    try {
      IvySdk.setAdCallback(IvyAdType.REWARDED, new IvyAdCallbacks() {
        @Override
        public void onAdClicked(IvyAdInfo adInfo) {
          listener.onAdClicked();
        }

        @Override
        public void onAdClosed(IvyAdInfo adInfo, boolean gotReward) {
          listener.onAdReward(!gotReward);
          listener.onAdClosed();
        }

        @Override
        public void onAdLoadFail(IvyAdType adInfo) {
          listener.onAdLoadFails();
        }

        @Override
        public void onAdLoadSuccess(IvyAdInfo adInfo) {
          listener.onAdLoadSuccess();
        }

        @Override
        public void onAdShowFail(IvyAdType adInfo) {
          listener.onAdShowFails();
        }

        @Override
        public void onAdShowSuccess(IvyAdInfo adInfo) {
          listener.onAdShow();
        }
      });
      IvySdk.showRewardAd(tag);
    } catch (Throwable t) {
      //
    }
  }

  private static long lastTriggerAutoFetchTime = 0;

  public static boolean hasRewardAd(String tag) {
    try {
      boolean hasReward = IvySdk.haveRewardAd();
      if (!hasReward) {
        if (System.currentTimeMillis() - lastTriggerAutoFetchTime > 30000) {
          Logger.debug(TAG, "No reward, we trigger to fetch");
          IvySdk.fetchRewardVideo();
          lastTriggerAutoFetchTime = System.currentTimeMillis();
        }
        return false;
      }
      hasReward = IvySdk.haveRewardAd();
      Log.d(TAG, "hasRewardAd() : " + hasReward);
      return hasReward;
    } catch (Throwable t) {
      //
      return false;
    }
  }

  public static void showBanner(String tag, int pos) {
    try {
      IvySdk.showBannerAd(pos);
    } catch (Exception ex) {
      //
    }
  }

  public static void showBanner(String tag, int pos, int animate) {
    Logger.debug(TAG, "showBanner(tag, pos, animate)");
    try {
      IvySdk.showBannerAd(pos);
    } catch (Exception ex) {
      //
    }
  }

  public static void closeBanner(String tag) {
    Logger.debug(TAG, "closeBanner(tag)");
    try {
      IvySdk.closeBanners();
    } catch (Throwable t) {
      //
    }
  }


  public static void clickUrl(final String url) {
    Activity activity = IvySdk.getActivity();
    if (activity != null) {
      activity.runOnUiThread(() -> IvyUtils.openBrowser(IvySdk.getActivity(), url));
    }
  }


  public static void launchApp(String packageName) {
    Activity context = IvySdk.getActivity();
    if (context == null || packageName == null) {
      return;
    }
    try {
      context.runOnUiThread(() -> {
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        context.startActivity(launchIntent);
      });
    } catch (Throwable t) {
      Logger.error(TAG, "launchApp " + packageName + " failed", t);
    }
  }

  public static void openAppStore(String pkg) {
    Activity activity = IvySdk.getActivity();
    if (activity == null) {
      Logger.error(TAG, "Activity is finished?");
      return;
    }
    activity.runOnUiThread(() -> IvyUtils.openPlayStore(activity, pkg, "openstore", null));
  }


  public static void track(String eventName, String action, String label, int value) {
    Log.d(TAG, "trackEvent called");
    Bundle bundle = new Bundle();

    bundle.putString("action", action);
    bundle.putString("label", label);
    bundle.putInt(FirebaseAnalytics.Param.VALUE, value);

    IvySdk.logEvent(eventName, bundle);
  }

  public static void track(String event, String data) {
    Log.d(TAG, "trackEvent called");

    Bundle bundle = new Bundle();
    if (data != null) {
      String[] splits = data.split(",");
      if (splits.length > 1) {
        for (int i = 0; i < splits.length; i += 2) {
          try {
            bundle.putDouble(splits[i], Double.parseDouble(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            bundle.putFloat(splits[i], Float.parseFloat(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            bundle.putLong(splits[i], Long.parseLong(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            bundle.putInt(splits[i], Integer.parseInt(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            bundle.putString(splits[i], splits[i + 1]);
          } catch (Exception ignore) {
          }
        }
      } else {
        bundle.putString("action", data);
        bundle.putString("label", data);
        bundle.putInt(FirebaseAnalytics.Param.VALUE, 1);
      }
    }
    // supress all game event o
    IvySdk.logEvent(event, bundle);
  }

  public static void track(String event, Map<String, Object> extra) {
//        TrackApi.api().track(event, extra);
    Bundle bundle = new Bundle();
    if (extra != null) {
      Iterator<String> it = extra.keySet().iterator();
      while (it.hasNext()) {
        String k = it.next();
        Object v = extra.get(k);
        if (k != null && !"".equals(k) && v != null) {
          if (v instanceof String) {
            bundle.putString(k, String.valueOf(v));
          } else if (v instanceof Integer) {
            bundle.putInt(k, (Integer) v);
          } else if (v instanceof Long) {
            bundle.putLong(k, (Long) v);
          } else if (v instanceof Boolean) {
            bundle.putBoolean(k, (Boolean) v);
          } else if (v instanceof Float) {
            bundle.putFloat(k, (Float) v);
          } else if (v instanceof Double) {
            bundle.putDouble(k, (Double) v);
          }
        }
      }
    }

    IvySdk.logEvent(event, bundle);
  }

  public static void track(String screenName) {
    Bundle bundle = new Bundle();
    IvySdk.logEvent(screenName, bundle);
  }


  public static void trackAll(String event) {
//        TrackApi.api().trackAll(event, new HashMap<String, Object>());
    IvySdk.logEvent(event, new Bundle());
  }

  public static boolean isPaymentValid() {
    return paymentSystemValid;
  }

  public static void querySKUDetail(int billId, @NonNull OnSkuDetailsListener onSkuDetailsListener) {
    JSONObject gridData = GridManager.getGridData();
    if (!gridData.has("payment")) {
      Log.d(TAG, "Grid data not configured for payment");
      return;
    }
    JSONObject checkout = gridData.optJSONObject("payment").optJSONObject("checkout");
    JSONObject product = checkout != null && checkout.has(String.valueOf(billId)) ? checkout.optJSONObject(String.valueOf(billId)) : null;
    if (product == null) {
      return;
    }
    String productId = product.optString("feename");
    List<String> iapIds = new ArrayList<>();
    iapIds.add(productId);
    IvySdk.querySKUDetail(iapIds, onSkuDetailsListener);
  }

  public static SKUDetail getSKUDetail(int billId) {
    JSONObject gridData = GridManager.getGridData();
    if (!gridData.has("payment")) {
      Log.d(TAG, "Grid data not configured for payment");
      return null;
    }
    JSONObject checkout = gridData.optJSONObject("payment").optJSONObject("checkout");
    JSONObject product = checkout != null && checkout.has(String.valueOf(billId)) ? checkout.optJSONObject(String.valueOf(billId)) : null;
    if (product == null) {
      return null;
    }
    String productId = product.optString("feename");
    return IvySdk.getSKUDetail(productId);
  }

  public static String getDefaultSkuDetailData(int billId) {
    JSONObject gridData = GridManager.getGridData();
    if (!gridData.has("payment")) {
      Log.d(TAG, "Grid data not configured for payment");
      return "{}";
    }
    JSONObject checkout = gridData.optJSONObject("payment").optJSONObject("checkout");
    JSONObject product = checkout != null && checkout.has(String.valueOf(billId)) ? checkout.optJSONObject(String.valueOf(billId)) : null;
    if (product == null) {
      return "{}";
    }

    JSONObject payObject = checkout.optJSONObject(String.valueOf(billId));
    String productId = product.optString("feename");

    try {
      JSONObject json = new JSONObject();
      json.put("id", productId);
      json.put("type", "inapp");
      json.put("price", "US$" + payObject.optDouble("usd"));
      json.put("currency", "USD");
      json.put("title", "");
      json.put("desc", "");
      json.put("usd", payObject.optDouble("usd"));
      return json.toString();
    } catch (Throwable ignore) {
    }
    return "{}";
  }

  public static void pay(final int bill) {
    pay(bill, null, null);
  }

  public static void pay(final int bill, final String itemName, final String payload) {
    try {
      if (builder != null && builder.paymentResultListener != null) {
        Log.d(TAG, "Android pay called, id: " + bill);
        JSONObject gridData = GridManager.getGridData();
        if (!gridData.has("payment")) {
          Log.d(TAG, "Grid data not configured for payment");
          return;
        }
        JSONObject checkout = gridData.optJSONObject("payment").optJSONObject("checkout");
        JSONObject product = checkout != null && checkout.has(String.valueOf(bill)) ? checkout.optJSONObject(String.valueOf(bill)) : null;
        if (product != null) {
          try {
            product.put("billId", String.valueOf(bill));
          } catch (Exception ex) {
            ex.printStackTrace();
          }
          String productId = product.optString("feename");
          if (itemName == null) {

          }
          IvySdk.pay(productId, itemName, payload);
        } else {
          Log.e(TAG, bill + " no defined.");
        }
      } else {
        Log.e(TAG, "builder.paymentResultListener is not defined, ignore");
      }
    } catch (Throwable t) {
      // ignore
    }
  }

  public static String getPrices() {
    return IvySdk.getInventory().toString();
  }

  /**
   * 根据tag标签设置从远程配置钟获取需要分享的内容参数。
   * 如果没有设置tag,将自动fallback到包的默认分享，
   *
   * @param tag
   */
  public static void shareOnFacebook(String tag, @NonNull ShareResultListener shareResultListener) {
    JSONObject shareTags = IvySdk.getRemoteConfigAsJSONObject(IvySdk.KEY_REMOTE_CONFIG_SHARE_TAGS);
    if (!shareTags.has(tag)) {
      doShare(null, null, null, null);
      return;
    }
    JSONObject sharedInfo = shareTags.optJSONObject(tag);
    if (sharedInfo == null) {
      doShare(null, null, null, null);
      return;
    }

    String url = sharedInfo.optString("url");
    String hashtag = sharedInfo.optString("hashtag");
    doShare(url, tag, hashtag, shareResultListener);
  }

  public static void findFacebookFriends(@NonNull FriendFinderListener listener) {
    Activity activity = IvySdk.getActivity();
    if (activity == null || facebookUserManager == null) {
      return;
    }
    activity.runOnUiThread(() -> facebookUserManager.findFriends(activity, listener));
  }

  public static void shareImagesOnFacebook(@NonNull String title, @NonNull List<String> imageUrls, @NonNull ShareResultListener shareResultListener) {
    Activity activity = IvySdk.getActivity();
    if (facebookUserManager == null || activity == null) {
      return;
    }

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        facebookUserManager.shareImage(activity, title, imageUrls, shareResultListener);
      }
    });
  }

  private static void doShare(String shareUrl, String tag, String hashtag, ShareResultListener shareResultListener) {
    Activity a = IvySdk.getActivity();
    if (a == null || a.isFinishing()) {
      return;
    }


    if (shareUrl == null || "".equals(shareUrl)) {
      shareUrl = IvyUtils.GOOGLE_PLAY_URL + a.getPackageName() + "&referrer=utm_source%3D" + "ivy" +
        "%26utm_campaign%3D" + a.getPackageName() +
        "%26utm_medium%3D" + (tag != null ? tag : "share") +
        "%26utm_term%3D" + "share" +
        "%26utm_content%3D" + "share";
    }

    final String resultShareUrl = shareUrl;
    a.runOnUiThread(() -> {
      try {
        boolean useFacebookShare = IvySdk.getGridConfigBoolean("useFacebookShare", true);

        if (useFacebookShare && facebookUserManager != null && IvyUtils.isFacebookInstalled(a)) {
          facebookUserManager.loginAndShare(a, resultShareUrl, tag, hashtag, shareResultListener);
          return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, resultShareUrl);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        shareIntent.setPackage(null);
        a.startActivity(Intent.createChooser(shareIntent, "Play game with friends"));

      } catch (Throwable t) {
        Logger.error(TAG, "share exception", t);
      }
    });
  }

  public static void share() {
    try {
      doShare(null, null, null, null);
    } catch (Throwable t) {
      //
    }
  }

  public static void share(String url, String quote) {
    doShare(url, quote, null, null);
  }

  public static void share(String url, String quote, String hashtag, ShareResultListener shareResultListener) {
    doShare(url, quote, hashtag, shareResultListener);
  }

  public static void shareVideo(String url) {
    doShare(url, null, null, null);
  }

  public static boolean shareVideo_(String url) {
    return false;
  }

  public static boolean shareBitmap(Bitmap bmp) {
    return false;
  }

  public static boolean shareBitmap(final String url) {
    return false;
  }

  public static void support(String email, String extra) {
    Activity context = IvySdk.getActivity();
    if (context == null) {
      Logger.error(TAG, "support(): activity is finished?");
      return;
    }
    context.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_TEXT, extra == null ? "" : extra);

        try {
          context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  public static void support(String email, String title, String extra) {
    Activity context = IvySdk.getActivity();
    if (context == null) {
      Logger.error(TAG, "support(): activity is finished?");
      return;
    }
    context.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));
        if (title != null) {
          intent.putExtra(Intent.EXTRA_SUBJECT, title);
        }
        intent.putExtra(Intent.EXTRA_TEXT, extra == null ? "" : extra);

        try {
          context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  public static void refreshExtraData(final UrlListener listener) {
    Logger.error(TAG, "refreshExtraData not supported!");
  }

  public static void rateUs() {
    rateUs(5);
  }

  public static void rateUs(float star) {
    try {
      IvySdk.rate((int) star);
    } catch (Throwable t) {
      //
    }
  }

  public static void toast(final String msg) {
    IvySdk.showToast(msg);
  }

  public static String getExtraData() {
    JSONObject extraJson = GridManager.getGridData().optJSONObject("data");
    if (extraJson != null) {
      return extraJson.toString();
    }
    return "";
  }

  public static void onQuit() {
    try {
      IvySdk.onQuit();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public static void forceQuit() {
    android.os.Process.killProcess(android.os.Process.myPid());
  }

  public static void logoutFacebook() {
    try {
      facebookUserManager.logout();
    } catch (Throwable t) {
      Logger.error(TAG, "logoutFacebook exception", t);
    }
  }

  public static void loginFacebook(@NonNull FacebookLoginListener facebookLoginListener) {
    try {
      facebookUserManager.login(IvySdk.getActivity(), facebookLoginListener);
    } catch (Throwable t) {
      //
    }
  }


  public static void login() {
    try {
      facebookUserManager.login(IvySdk.getActivity(), new FacebookLoginListener() {
        @Override
        public void onReceiveLoginResult(boolean success) {
          Logger.debug(TAG, "Facebook login success");
          if (builder != null && builder.userCenterListener != null) {
            builder.userCenterListener.onReceiveLoginResult(success);
          }
        }

        @Override
        public void onReceiveFriends(String friends) {
          Logger.debug(TAG, "Get Facebook friends" + friends);
          if (builder != null && builder.userCenterListener != null) {
            builder.userCenterListener.onReceiveFriends(friends);
          }
        }
      });
    } catch (Throwable t) {
      //
    }
  }

  public static void logout() {
    Logger.debug(TAG, "logout()");
    if (facebookUserManager != null) {
      facebookUserManager.logout();
    }
  }

  public static boolean isLogin() {
    if (facebookUserManager != null) {
      return facebookUserManager.isLogin();
    }
    return false;
  }

  public static void invite() {
//        UserMaster.master().invite();
  }

  public static void challenge(String title, String message) {
//        UserMaster.master().challenge(title, message);
  }


  public static String me() {
    if (facebookUserManager == null) {
      Logger.error(TAG, "Facebook SDK not initialized?");
      return "{}";
    }
    String me = facebookUserManager.me();
    Logger.debug(TAG, "I am " + me);
    return me;
  }

  public static String getFacebookUserId() {
    if (facebookUserManager != null) {
      return facebookUserManager.getUserId();
    }
    return "";
  }

  public static String friends() {
    try {
      if (facebookUserManager == null) {
        Logger.error(TAG, "Facebook SDK not initialized?");
        return null;
      }
      return facebookUserManager.friends(new FacebookLoginListener() {
        @Override
        public void onReceiveLoginResult(boolean success) {
          Logger.debug(TAG, "Facebook login success");
          if (builder != null && builder.userCenterListener != null) {
            builder.userCenterListener.onReceiveLoginResult(success);
          }
        }

        @Override
        public void onReceiveFriends(String friends) {
          Logger.debug(TAG, "Facebook login success");
          if (builder != null && builder.userCenterListener != null) {
            builder.userCenterListener.onReceiveFriends(friends);
          }
        }
      });
    } catch (Throwable t) {
      //
      return null;
    }
  }

  public static boolean isNetworkConnected() {
    return IvyUtils.isOnline(IvySdk.getActivity());
  }


  public static final int CONFIG_KEY_APP_ID = 1;
  public static final int CONFIG_KEY_LEADER_BOARD_URL = 2;
  public static final int CONFIG_KEY_API_VERSION = 3;
  public static final int CONFIG_KEY_SCREEN_WIDTH = 4;
  public static final int CONFIG_KEY_SCREEN_HEIGHT = 5;
  public static final int CONFIG_KEY_LANGUAGE = 6;
  public static final int CONFIG_KEY_COUNTRY = 7;
  public static final int CONFIG_KEY_VERSION_CODE = 8;
  public static final int CONFIG_KEY_VERSION_NAME = 9;
  public static final int CONFIG_KEY_PACKAGE_NAME = 10;
  public static final int CONFIG_KEY_UUID = 11;

  public static final int CONFIG_KEY_HELP_ENGAGEMENT_PROJECTID = 51;

  public static final int SDK_CONFIG_KEY_JSON_VERSION = 21;

  public static final int SDK_CONFIG_KEY_FIREBASE_USERID = 22;


  private static String recordedVersionCode = null;
  private static String recordedVersionName = null;

  public static String getConfig(int configKey) {
    try {
      switch (configKey) {
        case CONFIG_KEY_APP_ID:
          return GridManager.getGridData().optString("appid");
        case CONFIG_KEY_LEADER_BOARD_URL:
          JSONObject sns = GridManager.getGridData().optJSONObject("sns");
          if (sns != null && sns.has("leader_board_url")) {
            return sns.optString("leader_board_url");
          } else {
            return "";
          }
        case CONFIG_KEY_API_VERSION:
          return GridManager.getGridData().optString("v_api", "26");
        case CONFIG_KEY_SCREEN_WIDTH: {
          Activity activity = IvySdk.getActivity();
          if (activity == null) {
            return "0";
          }
          DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
          if (displayMetrics != null) {
            return String.valueOf(displayMetrics.widthPixels);
          } else {
            return "0";
          }
        }
        case CONFIG_KEY_SCREEN_HEIGHT: {
          Activity activity = IvySdk.getActivity();
          if (activity == null) {
            return "0";
          }
          DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
          if (displayMetrics != null) {
            return String.valueOf(displayMetrics.heightPixels);
          }
          return "0";
        }
        case CONFIG_KEY_LANGUAGE:
          return Locale.getDefault().getLanguage();
        case CONFIG_KEY_COUNTRY:
          return IvySdk.getCountryCode();
        case CONFIG_KEY_VERSION_CODE:
          try {
            if (recordedVersionCode != null) {
              return recordedVersionCode;
            }
            Context context = IvySdk.CONTEXT;
            PackageManager packageManager = context.getPackageManager();
            String name = context.getPackageName();
            PackageInfo info = packageManager.getPackageInfo(name, 0);
            recordedVersionCode = String.valueOf(info.versionCode);
          } catch (Exception var5) {
            return "0";
          }
        case CONFIG_KEY_VERSION_NAME:
          try {
            if (recordedVersionName != null) {
              return recordedVersionName;
            }
            Context context = IvySdk.CONTEXT;
            PackageManager packageManager = context.getPackageManager();
            String name = context.getPackageName();
            PackageInfo info = packageManager.getPackageInfo(name, 0);
            recordedVersionName = info.versionName;
            return recordedVersionName;
          } catch (Exception var5) {
            return "0";
          }
        case CONFIG_KEY_PACKAGE_NAME:
          return IvySdk.CONTEXT.getPackageName();
        case CONFIG_KEY_UUID:
          return IvySdk.getUUID();
        case SDK_CONFIG_KEY_JSON_VERSION:
          String domain = IvySdk.getGridConfigString("domain");
          if (domain == null || "".equals(domain)) {
            return "";
          }
          try {
            Uri uri = Uri.parse(domain);
            if (uri != null) {
              return uri.getQueryParameter("v_api");
            }
          } catch (Throwable t) {
            // ignore
            t.printStackTrace();
          }
          return "";
        case SDK_CONFIG_KEY_FIREBASE_USERID:
          return "";
        case CONFIG_KEY_HELP_ENGAGEMENT_PROJECTID:
          return IvySdk.getGridConfigString("helpengage.appId", "");
        default:
          Log.e(TAG, "ATTENTION, Unknow config key for " + configKey);
          return "";
      }
    } catch (Throwable t) {
      return "";
    }
  }


  public static String getConfig(String packageName, int configKey) {
    switch (configKey) {
      case CONFIG_KEY_VERSION_CODE:
        try {
          return String.valueOf(IvySdk.CONTEXT.getPackageManager().getPackageInfo(packageName, 0).versionCode);
        } catch (Exception e) {
          e.printStackTrace();
          return "0";
        }

      case CONFIG_KEY_VERSION_NAME:
        try {
          return String.valueOf(IvySdk.CONTEXT.getPackageManager().getPackageInfo(packageName, 0).versionName);
        } catch (Exception e) {
          e.printStackTrace();
          return "0.0";
        }
      default:
        Log.e(TAG, "ATTENTION, Unknow config key for " + configKey);
        return "";
    }
  }

  public static void alert(final String title, final String message) {
    Activity activity = IvySdk.getActivity();

    if (activity != null) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .create().show();
        }
      });
    }
  }


  public static void setUserProperty(String key, String value) {
    IvySdk.setUserProperty(key, value);
  }

  public static void setUserID(String userID) {

  }

  public static void silentLoginGoogle(@NonNull final GoogleListener listener) {
    try {
      IvySdk.slientLoginGoogle(listener);
    } catch (Throwable t) {
      listener.onFails();
    }
  }

  public static void loginGoogle(@NonNull final GoogleListener listener) {
    try {
      IvySdk.slientLoginGoogle(listener);
    } catch (Throwable t) {
      listener.onFails();
    }
  }

  public static void updateGoogleAchievement(String id, int step) {
    try {
      IvySdk.updateGoogleAchievement(id, step);
    } catch (Throwable t) {

    }
  }

  public static void updateGoogleLeaderBoard(String id, long value) {
    try {
      IvySdk.updateGoogleLeaderBoard(id, value);
    } catch (Throwable t) {

    }
  }

  public static void showGoogleAchievements() {
    try {
      IvySdk.showGoogleAchievement();
    } catch (Throwable t) {
      //
    }
  }

  public static void showGoogleLeaderBoards(String... ids) {
    try {
      IvySdk.displayGameLeaderboards();
    } catch (Throwable t) {
      //
    }
  }

  public static boolean isGoogleSupport() {
    try {
      return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(IvySdk.CONTEXT) == 0;
    } catch (Throwable t) {
      //
    }
    return false;
  }

  public static void showGoogleLeaderBoards() {
    Log.d(TAG, "showGoogleLeaderBoards: ");
    try {
      IvySdk.displayGameLeaderboards();
    } catch (Throwable t) {
      //
    }
  }

  public static boolean hasNotch() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      return false;
    }
    Activity activity = IvySdk.getActivity();
    if (activity == null) {
      return false;
    }

    try {
      boolean flag = NotchScreenManager.getInstance().hasNotch(activity);
      JSONObject notchConfig = IvySdk.getGridConfigJson("notch");
      if (notchConfig != null) {
        String deviceModel = Build.MODEL != null ? Build.MODEL.toLowerCase(Locale.ENGLISH) : "unknown";
        if (notchConfig.has(deviceModel)) {
          int notchHeight = notchConfig.optInt("deviceModel", 0);
          return notchHeight > 0;
        }
      }
      return flag;
    } catch (Throwable t) {
      // ignore
    }
    return false;
  }

  public static int getNotchHeight() {
    Activity activity = IvySdk.getActivity();
    if (activity == null) {
      return 0;
    }

    try {
      int notchHeight = NotchScreenManager.getInstance().getNotchHeight(activity);

      JSONObject notchConfig = IvySdk.getGridConfigJson("notch");
      if (notchConfig != null) {
        String deviceModel = Build.MODEL != null ? Build.MODEL.toLowerCase(Locale.ENGLISH) : "unknown";
        if (notchConfig.has(deviceModel)) {
          return notchConfig.optInt(deviceModel);
        }
      }

      if (notchHeight > 0) {
        return notchHeight;
      }

      int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
      if (resourceId > 0) {
        notchHeight = activity.getResources().getDimensionPixelSize(resourceId);
      }

      return notchHeight;
    } catch (Throwable t) {
      // ignore
    }
    return 0;
  }

  public static void onKill() {
    android.os.Process.killProcess(android.os.Process.myPid());
  }


  @Deprecated
  public static void like() {
    Log.e(TAG, "Like deprecated");
  }

  @Deprecated
  public static boolean isCachingUrl(String url) {
    Log.e(TAG, "isCachingUrl deprecated");
    return false;
  }

  @Deprecated
  public static void cacheUrl(int tag, String url) {
    Log.e(TAG, "cacheUrl deprecated");
  }

  @Deprecated
  public static void cacheUrl(String url, boolean external, final UrlListener listener) {
    Log.e(TAG, "cacheUrl deprecated");
  }


  public static void verifyIdCard() {
  }

  public static void resetIdCheck() {
  }

  public static int getIdCardVerifyedAge() {
    return 0;
  }


  public static void setDisplayInNotch(Activity activity) {
    try {
      boolean displayedInNotch = true;
      ApplicationInfo ai = null;
      try {
        ai = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
      } catch (PackageManager.NameNotFoundException e) {
        e.printStackTrace();
      }

      if (ai != null && ai.metaData != null) {
        Object o = ai.metaData.get("din");
        if (o instanceof Boolean) {
          displayedInNotch = (Boolean) o;
        }
      }
      if (!displayedInNotch) {
        NotchScreenManager.getInstance().setEnableDisplayInNotch(false);
      }
      NotchScreenManager.getInstance().setDisplayInNotch(activity);
    } catch (Throwable ex) {
      ex.printStackTrace();
    }
  }

  public static void copyText(final String str) {
    try {
      Activity activity = IvySdk.getActivity();
      if (activity == null) {
        return;
      }
      activity.runOnUiThread(() -> {
        try {
          android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clip = ClipData.newPlainText("clip_text", str);
          clipboard.setPrimaryClip(clip);
        } catch (Throwable t) {
          //
        }
      });
    } catch (Throwable e) {
      Logger.error(TAG, "Copytext exception", e);
    }
  }

  public static long getFreeMem() {
    try {
      Activity activity = IvySdk.getActivity();
      if (activity == null) {
        Logger.warning(TAG, "activity is null, getFreeMem is impossible");
        return -1;
      }

      ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
      ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
      manager.getMemoryInfo(info);

      Logger.debug(TAG, "Memory Info >>> avail: " + info.availMem + ", total: " + info.totalMem + ", isLowMemory: " + info.lowMemory);
      return info.availMem;
    } catch (Throwable t) {
      Logger.error(TAG, "getFreeMem exception", t);
    }
    return -1L;
  }

  public static String getKeyHash() {
    Activity activity = IvySdk.getActivity();
    if (activity == null) {
      Logger.error(TAG, "Activity is not initialized, forgot onCreate?");
      return "";
    }
    return CommonUtil.getKeyStoreHash(activity);
  }


  public static void setIdCardVerified(int age) {

  }

  // handle app link intent
  public static void handleIntent(@NonNull Intent intent) {

    Bundle bundle = intent.getExtras();
    if (bundle != null) {
      String payload = bundle.getString("inapp_message_action");
      if (payload != null && !"".equals(payload)) {
        Logger.debug(TAG, "message payload >>> " + payload);
        handleInAppMessagePayload(bundle);
      }
    }
  }

  private static void handleInAppMessagePayload(Bundle bundle) {
    if (builder.onGameMessageListener == null) {
      Logger.debug(TAG, "handleInAppMessagePayload ignore ");
      return;
    }

    try {
      String type = bundle.getString("inapp_message_type", "");
      String body = bundle.getString("inapp_message_body", "");

      Logger.debug(TAG, "handleInAppMessagePayload >>> " + type + ", body: " + body);
      builder.onGameMessageListener.onMessage(type, body);
    } catch (Throwable t) {
      Logger.error(TAG, "handleInAppMessagePayload exception", t);
    }
  }

  @NonNull
  public static String getFirebaseUserId() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user != null) {
      return user.getUid();
    }
    return "";
  }

  @NonNull
  public static String getFirebaseUserName() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      return "";
    }
    String displayName = user.getDisplayName();
    if (displayName != null && !"".equals(displayName)) {
      return displayName;
    }
    List<? extends UserInfo> userInfoList = user.getProviderData();
    for (UserInfo info : userInfoList) {
      if (info != null) {
        displayName = info.getDisplayName();
        if (displayName != null && !"".equals(displayName)) {
          return displayName;
        }
      }
    }
    return "";
  }

  public static void onGameMessage(String type, String data) {
    if (builder != null && builder.onGameMessageListener != null) {
      builder.onGameMessageListener.onMessage(type, data);
    }
  }

  public static void checkHelpEngagement() {
    Logger.debug(TAG, "checkHelpEngagement");
    if (builder == null || builder.onGameMessageListener == null) {
      Logger.warning(TAG, "OnGameMessage not registered, ignore");
      return;
    }

    String projectId = IvySdk.getGridConfigString("helpengage.appId", "62e72ac738480f797ef40eb7");
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    if (currentUser == null) {
      Logger.warning(TAG, "user not signed in");
      return;
    }
    String f_uid = currentUser.getUid();
    if ("".equals(f_uid)) {
      Logger.warning(TAG, "userId is empty");
      return;
    }

    String checkUrl = "https://eii2wnwjeukuf2oj6vxjustpve0paafz.lambda-url.ap-southeast-1.on.aws/?projectId=" + projectId + "&customerId=" + f_uid;

    Logger.debug(TAG, "check URL: " + checkUrl);
    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder.url(HttpUrl.get(checkUrl)).get().build();

    IvySdk.getOkHttpClient().newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(@NonNull Call call, @NonNull IOException e) {
        Logger.error(TAG, "checkHelpEngagement exception", e);
      }

      @Override
      public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        try (ResponseBody body = response.body()) {
          if (body != null) {
            String str = body.string();
            Logger.debug(TAG, ">>> event sent result: " + str);
            if (!"".equals(str) && str.contains("success")) {
              JSONObject o = new JSONObject(str);
              if (o.has("success") && o.optBoolean("success")) {
                boolean hasnew = o.optBoolean("hasnew");
                if (hasnew) {
                  String content = o.optString("content", "");
                  if (!"".equals(content)) {
                    builder.onGameMessageListener.onMessage("support", content);

                    // set the message read
                    setHelpEngagementMessageRead(projectId, f_uid);
                  }
                }
              }
            }
          }
        } catch (Throwable t) {
          Logger.error(TAG, "checkHelpEngagement exception", t);
        }
      }
    });
  }

  private static void setHelpEngagementMessageRead(@NonNull String projectId, @NonNull String fUid) {
    String checkUrl = "https://eii2wnwjeukuf2oj6vxjustpve0paafz.lambda-url.ap-southeast-1.on.aws/?projectId=" + projectId + "&customerId=" + fUid;

    Logger.debug(TAG, "setHelpEngagementMessageRead URL: " + checkUrl);
    Request.Builder requestBuilder = new Request.Builder();
    Request request = requestBuilder.url(HttpUrl.get(checkUrl)).get().build();

    IvySdk.getOkHttpClient().newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(@NonNull Call call, @NonNull IOException e) {
        Logger.error(TAG, "setHelpEngagementMessageRead exception", e);
      }

      @Override
      public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        try (ResponseBody body = response.body()) {
          if (body != null) {
            String str = body.string();
            Logger.debug(TAG, ">>> setHelpEngagementMessageRead: " + str);
          }
        } catch (Throwable t) {
          Logger.error(TAG, "setHelpEngagementMessageRead exception", t);
        }
      }
    });
  }

  public static void helpshift(final String customerName, String info) {
    String projectId = IvySdk.getGridConfigString("helpengage.appId", "62e72ac738480f797ef40eb7");
    Activity activity = IvySdk.getActivity();
    if (activity == null) {
      return;
    }

    if (TextUtils.isEmpty(projectId)) {
      Logger.error(TAG, "help engagement error, projectId empty");
      return;
    }

    JSONObject jsonObject = null;
    try {
      jsonObject = new JSONObject(info);
      jsonObject.put("app_version", getConfig(CONFIG_KEY_VERSION_NAME));
      jsonObject.put("device", Build.MODEL != null ? Build.MODEL.toLowerCase(Locale.ENGLISH) : "unknown");
      jsonObject.put("os", Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + ")");
      // append system info
      FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
      if (currentUser != null) {
        jsonObject.put("f_uid", currentUser.getUid());
      }
    } catch (Throwable ex) {
      Logger.error(TAG, "helpshift error", ex);
    }

    final String resultInfo = jsonObject != null ? jsonObject.toString() : "";
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("projectId", projectId);
        intent.putExtra("customerName", customerName);
        intent.putExtra("customInfo", resultInfo);
        activity.startActivity(intent);
      }
    });
  }


  public static void showWebView(final String title, final String url) {
    final Activity activity = IvySdk.getActivity();
    if (activity == null || activity.isFinishing()) {
      return;
    }
    activity.runOnUiThread(() -> {
      Intent intent = new Intent(activity, WebViewActivity.class);
      intent.putExtra("title", title);
      intent.putExtra("url", url);
      activity.startActivityForResult(intent, IvySdk.RC_WEBVIEW);
    });
  }

  public static void saveUserAttribute(JSONObject dataJson) {
    try {
      IvySdk.saveUserAttribute(dataJson, builder.inAppMessageListener);
    } catch (Throwable t) {
      // ignore
    }
  }

  public static void recordEventConversion(String conversionUrl, String eventName, Bundle bundle) {
    try {
      IvySdk.recordEventConversion(conversionUrl, eventName, bundle, builder.inAppMessageListener);
    } catch (Throwable t) {
      // ignore
    }
  }

  public static void setTargetForChild() {
    try {
      RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration()
        .toBuilder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .build();
      MobileAds.setRequestConfiguration(requestConfiguration);
    } catch (Throwable t) {
      Logger.error(TAG, "setTargetForChild exception", t);
    }
  }

  public static void recordVirtualCurrency(String name, int value) {
    // save this main line
    try {
      String remaingCurrencyString = IvySdk.mmGetStringValue(IvySdk.KEY_VIRUTAL_CURRENCY, "{}");
      JSONObject o = new JSONObject(remaingCurrencyString);
      o.put(name, value);
      IvySdk.mmSetStringValue(IvySdk.KEY_VIRUTAL_CURRENCY, o.toString());
    } catch (Throwable ignored) {
    }
  }

  /**
   * 更新玩家的主线数据
   *
   * @param name  属性名
   * @param value 新值
   */
  public static void trackMainLine(String name, int value) {
    Bundle bundle = new Bundle();

    bundle.putString("label", name);
    bundle.putInt(FirebaseAnalytics.Param.VALUE, value);
    bundle.putInt("times", IvySdk.appStartTimes);
    IvySdk.logIvyEvent("track_main_line", bundle);

    // save this main line
    try {
      String savedMainLine = IvySdk.mmGetStringValue(IvySdk.KEY_GAME_MAIN_LINE, "{}");
      JSONObject o = new JSONObject(savedMainLine);
      o.put(name, value);
      IvySdk.mmSetStringValue(IvySdk.KEY_GAME_MAIN_LINE, o.toString());
    } catch (Throwable ignored) {
    }
  }

  public static void trackRetentionStep(int stepId, String stepName) {
    Bundle bundle = new Bundle();
    bundle.putString("label", stepName);
    bundle.putInt("value", stepId);
    bundle.putInt("times", IvySdk.appStartTimes);
    IvySdk.logEvent("track_retention_step_" + stepId, bundle);
  }

  /**
   * 记录玩家的游戏核心玩家动作
   *
   * @param name
   * @param inc
   */
  public static void recordCoreAction(String name, int inc) {
    if (name == null || "".equals(name)) {
      return;
    }
    int actionNums = IvySdk.mmGetIntValue(name, 0);
    IvySdk.mmSetIntValue(name, actionNums + inc);
  }

  public static void commitCoreAction(String name) {
    if (name == null || "".equals(name)) {
      return;
    }
    int actionNums = IvySdk.mmGetIntValue(name, 0);

    Bundle bundle = new Bundle();

    bundle.putString("label", name);
    bundle.putInt("times", IvySdk.appStartTimes);
    bundle.putInt("value", actionNums);

    IvySdk.logIvyEvent("track_core_action", bundle);

    IvySdk.mmSetIntValue(name, 0);
  }

  /**
   * 记录活动开启
   */
  public static void trackActivityStart(String name, String catalog) {
    Bundle bundle = new Bundle();

    bundle.putString("label", name);
    if (catalog != null) {
      bundle.putString("catalog", catalog);
    }
    IvySdk.logIvyEvent("track_activity_start", bundle);
  }

  /**
   * 记录玩家的活动的进程
   *
   * @param name
   * @param step
   */
  public static void trackActivityStep(String name, int step) {
    Bundle bundle = new Bundle();

    bundle.putString("label", name);
    bundle.putInt("value", step);

    IvySdk.logIvyEvent("track_activity_step", bundle);
  }

  /**
   * 记录玩家的特定活动结束
   *
   * @param name
   */
  public static void trackActivityEnd(String name) {
    Bundle bundle = new Bundle();

    bundle.putString("label", name);
    IvySdk.logIvyEvent("track_activity_end", bundle);
  }

  /**
   * 记录通用的活动内动作
   *
   * @param name
   * @param catalog
   * @param value
   */
  public static void trackActivityEvent(String name, String catalog, float value, boolean iap) {
    Bundle bundle = new Bundle();

    bundle.putString("label", name);
    bundle.putString("catalog", catalog);
    bundle.putFloat("value", value);
    if (iap) {
      bundle.putFloat("revenue", value);
    }

    IvySdk.logIvyEvent("track_activity_event", bundle);
  }

  public static void spendVirtualCurrency(String virtualCurrencyName, String itemid, int value,
                                          int currentValue, String catalog) {
    Bundle bundle = new Bundle();

    bundle.putString("label", virtualCurrencyName);
    bundle.putString("itemid", itemid);
    bundle.putInt("value", value);

    if (catalog != null) {
      bundle.putString("catalog", catalog);
    }

    IvySdk.logIvyEvent("spend_virtual_currency", bundle);

    if (currentValue >= 0) {
      recordVirtualCurrency(virtualCurrencyName, currentValue);
    }
  }

  public static void earnVirtualCurrency(String virtualCurrencyName, String itemid, int value,
                                         int currentValue) {
    Bundle bundle = new Bundle();

    bundle.putString("label", virtualCurrencyName);
    bundle.putString("itemid", itemid);
    bundle.putInt("value", value);

    IvySdk.logIvyEvent("earn_virtual_currency", bundle);

    if (currentValue >= 0) {
      recordVirtualCurrency(virtualCurrencyName, currentValue);
    }
  }

  public static void trackScreenStart(String screenName) {
    Bundle bundle = new Bundle();

    bundle.putString("label", screenName);
    IvySdk.logIvyEvent("track_screen_start", bundle);
  }

  public static void trackScreenEnd(String screenName) {
    Bundle bundle = new Bundle();

    bundle.putString("label", screenName);
    IvySdk.logIvyEvent("track_screen_end", bundle);
  }

  public static OfferwallCreditListener getOfferwallCreditListener() {
    return builder.offerwallCreditListener;
  }
}
