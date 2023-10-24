package com.android.client;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adsfall.R;
import com.alibaba.fastjson.JSON;
import com.applovin.sdk.AppLovinSdk;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.auth.PlayGamesAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.ivy.IvySdk;
import com.ivy.IvyUtils;
import com.ivy.ads.adapters.ApplovinManager;
import com.ivy.ads.configuration.InterstitialConfig;
import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.facebook.FacebookLoginListener;
import com.ivy.firestore.FirestoreAdapter;
import com.ivy.push.local.LocalNotificationManager;
import com.ivy.util.Logger;
import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Unity {
  private static final String TAG = "Unity";
  public static final String TRUE = "0";
  public static final String FALSE = "1";
  static WeakReference<Activity> activityWeakReference;

  public static final int AD_FULL = 1;
  public static final int AD_VIDEO = 2;
  public static final int AD_BANNER = 3;
  public static final int AD_NATIVE = 5;
  public static final int AD_GIF_ICON = 6;

  private static IProviderFacade providerFacade;
  private static final String MSG_FIRESTORE_CONNECTED = "onFirestoreConnected";

  public static void onCreate(@NonNull Activity activity) {
    try {
      activityWeakReference = new WeakReference<>(activity);
      activity.runOnUiThread(() -> {
        if (activityWeakReference.get() != null) {
          AndroidSdk.Builder builder = new AndroidSdk.Builder();

          builder.setSdkResultListener(new SdkResultListener() {
            @Override
            public void onInitialized() {
              sendMessage("onInitialized", "");
            }

            @Override
            public void onReceiveServerExtra(String data) {
              sendMessage("onReceiveServerExtra", data);
            }

            @Override
            public void onReceiveNotificationData(String data) {
              sendMessage("onReceiveNotificationData", data);
            }
          }).setPaymentListener(new PaymentSystemListener() {
            @Override
            public void onPaymentSuccessWithPurchase(int bill, String orderId, String purchaseToken, String payload) {
              Logger.debug(TAG, "Unity#onPaymentSuccessWithPurchase >>>>>> " + bill + "|" + orderId + "|" + purchaseToken + "|" + payload);
              sendMessage("onPaymentSuccessWithPurchase", bill + "|" + orderId + "|" + purchaseToken + "|" + payload);
            }

            @Override
            public void onPaymentFail(int billId) {
              Logger.debug(TAG, "Unity#onPaymentFail ");

              sendMessage("onPaymentFail", String.valueOf(billId));
            }

            @Override
            public void onPaymentSystemValid() {
              Logger.debug(TAG, "Unity#onPaymentSystemValid ");
              sendMessage("onPaymentSystemValid", "");
            }

            @Override
            public void onPaymentCanceled(int bill) {
              sendMessage("onPaymentCanceled", String.valueOf(bill));
            }

            @Override
            public void onPaymentSystemError(int code, String message) {
              Logger.debug(TAG, "Unity#onPaymentSystemError: " + code + "|" + message);

              sendMessage("onPaymentSystemError", code + "|" + message);
            }

            @Override
            public void onSkuDetailData(int bill, String skuDetail) {
              sendMessage("onPaymentData", bill + "|" + skuDetail);
            }
          }).setUrlListener(new UrlListener() {
            @Override
            public void onSuccess(int tag, String path) {
              sendMessage("onCacheUrlResult", tag + "|0|" + path);
            }

            @Override
            public void onFailure(int tag) {
              sendMessage("onCacheUrlResult", tag + "|1");
            }
          }).setUserCenterListener(new UserCenterListener() {
            @Override
            public void onReceiveLoginResult(boolean success) {
              sendMessage("onReceiveLoginResult", success ? TRUE : FALSE);
            }

            @Override
            public void onReceiveFriends(String friends) {
              sendMessage("onReceiveFriends", friends);
            }

            @Override
            public void onReceiveInviteResult(boolean success) {
              sendMessage("onReceiveInviteResult", success ? TRUE : FALSE);
            }

            @Override
            public void onReceiveChallengeResult(int count) {
              sendMessage("onReceiveChallengeResult", String.valueOf(count));
            }

            @Override
            public void onReceiveLikeResult(boolean success) {
              sendMessage("onReceiveLikeResult", success ? TRUE : FALSE);
            }
          }).setAdLoadedListener(new AdLoadedListener() {
            @Override
            public void onAdLoaded(int adType) {
              try {
                int newAdType = 0;
                if (adType == IvyAdType.INTERSTITIAL.ordinal()) {
                  newAdType = AD_FULL;
                } else if (adType == IvyAdType.REWARDED.ordinal()) {
                  newAdType = AD_VIDEO;
                } else if (adType == IvyAdType.BANNER.ordinal()) {
                  newAdType = AD_BANNER;
                } else if (adType == IvyAdType.NATIVE_AD.ordinal()) {
                  newAdType = AD_NATIVE;
                }
                Logger.debug(TAG, "Notify AdLoaded: " + newAdType);

                sendMessage("onAdLoaded", String.valueOf(newAdType));

              } catch (Throwable t) {
                Logger.error(TAG, "ad loaded not defined", t);
              }
            }
          }).setEventOccurredListener(new EventOccurredListener() {
            @Override
            public void onEventOccurred(String eventName) {
              sendMessage("onEventOccurred", eventName);
            }
          }).setOnRemoteConfigLoadListener(new OnRemoteConfigLoadListener() {
            @Override
            public void onRemoteConfigPrepared() {
              sendMessage("onRemoteConfigPrepared", "");
            }
          }).setOnGameMessageListener(new OnGameMessageListener() {
            @Override
            public void onMessage(String type, String data) {
              sendMessage("onGameMessage", type + "|" + data);
            }
          }).setDeepLinkReceivedListener(uri -> sendMessage("onDeepLinkReceived", uri)).setInAppMessageClickListener(new InAppMessageListener() {
            @Override
            public void sentryLog(@NonNull String message) {
              sendMessage("onSentryLog", message);
            }

            @Override
            public void messageDisplayed(@NonNull String message) {
              sendMessage("onInAppMessage", message);
            }

            @Override
            public void messageDisplayed(String campaignId, String dataJson) {
              sendMessage("onInAppMessageDisplayed", dataJson);
            }

            @Override
            public void messageClicked(String actionUrl) {
              sendMessage("onInAppMessageClicked", actionUrl);
            }
          }).setOfferwallCreditListener(new OfferwallCreditListener() {
            @Override
            public void onOfferwallAdCredited(int credits, int totalCredits) {
              sendMessage("onOfferwallAdCredited", credits + "|" + totalCredits);
            }

            @Override
            public void onGetOfferwallCreditsFailed(@NonNull String message) {
              sendMessage("onGetOfferwallCreditsFailed", message);
            }
          });

          ApplicationInfo ai = null;
          try {
            ai = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);

            if (ai != null && ai.metaData != null) {
              Object o = ai.metaData.get("adsfall.provider");
              if (o instanceof String) {
                String providerClass = String.valueOf(o);
                providerFacade = (IProviderFacade) Class.forName(providerClass).newInstance();
              }
            }
          } catch (Throwable t) {
            Logger.error(TAG, "initialize exception", t);
          }

          IvySdk.setProviderFacade(providerFacade);
          AndroidSdk.onCreate(activity, builder);
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "onCreate exception", t);
    }
  }

  public static void cancelLocalMessage(String key) {
    AndroidSdk.cancelLocalMessage(key);
  }

  public static void cancelMessage(String key) {
    AndroidSdk.cancelMessage(key);
  }

  public static void pushMessage(String key, String title, String content, int pushTime, boolean localTimeZone, String fbIds, String uuids, String topics, int iosBadge, boolean useSound, String soundName, String extraData) {
  }

  public static void pushLocalMessage(String key, String title, String content, int pushTime, int interval, boolean useSound, String soundName, String userInfo) {
    AndroidSdk.pushLocalMessage(key, title, content, pushTime, interval, useSound, soundName, userInfo);
  }


  public static void sendMessage(String method, String data) {
    try {
      UnityPlayer.UnitySendMessage("RiseSdkListener", method, data);
    } catch (Throwable t) {
      Logger.error(TAG, "sendMessage exception", t);
    }
  }

  public static void onPause() {
    AndroidSdk.onPause();
  }

  public static void onResume() {
    AndroidSdk.onResume();
  }

  public static void onDestroy() {
    AndroidSdk.onDestroy();
  }

  public static void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    Logger.debug(TAG, "Unity#onActivityResult, requestCode: " + requestCode);
    AndroidSdk.onActivityResult(requestCode, resultCode, data);
  }

  public static void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
  }

  public static void showFullAd(final String tag) {
    AndroidSdk.showFullAd(tag, new AdListener() {
      @Override
      public void onAdClosed() {
        sendMessage("onFullAdClosed", tag);
      }

      @Override
      public void onAdShow() {
        sendMessage("onFullAdShown", tag);
      }

      @Override
      public void onAdClicked() {
        sendMessage("onFullAdClicked", tag);
      }
    });
  }

  public static void showRewardAd(int id) {
    showRewardAd("default", id);
  }

  public static boolean hasRewardAd() {
    return AndroidSdk.hasRewardAd("default");
  }

  public static void showRewardAd(final String tag, final int id) {
    Logger.debug(TAG, "showRewardAd: " + tag + ", " + id);
    AndroidSdk.showRewardAd(tag, new AdListener() {
      @Override
      public void onAdReward(boolean skip) {
        Logger.debug(TAG, "onAdReward, skip: " + skip);
        sendMessage("onReceiveReward", (skip ? FALSE : TRUE) + "|" + id + "|" + tag + "|" + (skip ? TRUE : FALSE));
      }

      @Override
      public void onAdLoadFails() {
        Logger.debug(TAG, "onAdLoadFails");
        sendMessage("onAdLoadFailed", "" + id);
      }

      @Override
      public void onAdLoadSuccess() {
        Logger.debug(TAG, "onAdLoadSuccess");
        sendMessage("onAdLoadSuccess", "" + id);
      }

      @Override
      public void onAdShowFails() {
        Logger.debug(TAG, "onAdShowFails, ");
        sendMessage("onReceiveReward", FALSE + "|" + id + "|" + tag + "|" + FALSE);
      }

      @Override
      public void onAdClosed() {
        Logger.debug(TAG, "onAdClosed, ");
        sendMessage("onVideoAdClosed", tag);
      }
    });
  }


  public static boolean hasFull(String tag) {
    return AndroidSdk.hasFull(tag);
  }

  public static boolean hasRewardAd(String tag) {
    return AndroidSdk.hasRewardAd(tag);
  }

  public static void checkRewardAd() {
    IvySdk.fetchRewardVideoIfNotLoaded();
  }

  public static void pay(int id) {
    AndroidSdk.pay(id);
  }

  public static void pay(int id, String payload) {
    AndroidSdk.pay(id, null, payload);
  }

  public static void pay(int id, String itemName, String payload) {
    AndroidSdk.pay(id, itemName, payload);
  }

  public static void query(int id) {
    IvySdk.queryPurchase();
  }

  public static void getPaymentDataAsyn(int bill) {
    AndroidSdk.querySKUDetail(bill, () -> sendMessage("onPaymentData", bill + "|" + getPaymentData(bill)));
  }

  public static String getPaymentData(int bill) {
    Logger.debug(TAG, "getPaymentData for bill " + bill);
    SKUDetail skuDetail = AndroidSdk.getSKUDetail(bill);
    if (skuDetail == null) {
      // check cached data
      String cacheData = IvySdk.mmGetStringValue("_sku_cache_" + bill, "");
      if ("".equals(cacheData)) {
        return AndroidSdk.getDefaultSkuDetailData(bill);
      }
      return cacheData;
    }

    String jsonData = skuDetail.toJson().toString();
    IvySdk.mmSetStringValue("_sku_cache_" + bill, jsonData);

    Logger.debug(TAG, ">>> receive billing price  " + jsonData);

    return jsonData;
  }


  public static void share(final String shareId, String url, String quote, String hashtag) {
    AndroidSdk.share(url, quote, hashtag, new ShareResultListener() {
      @Override
      public void onSuccess(String postId) {
        sendMessage("onShareSuccess", shareId + "|" + postId);
      }

      @Override
      public void onCancel() {
        sendMessage("onShareCancel", shareId);
      }

      @Override
      public void onError(String message) {
        sendMessage("onShareError", shareId + "|" + message);
      }
    });
  }

  public static void findFriendsOnFacebook() {
    AndroidSdk.findFacebookFriends(new FriendFinderListener() {
      @Override
      public void onSuccess() {
        sendMessage("onFindFriendsSuccess", "");

      }

      @Override
      public void onCancel() {
        sendMessage("onFindFriendsCancel", "");

      }

      @Override
      public void onError(@NonNull String error) {
        sendMessage("onFindFriendsError", error);
      }
    });
  }

  public static void shareOnFacebook(String tag) {
    AndroidSdk.shareOnFacebook(tag, new ShareResultListener() {
      @Override
      public void onSuccess(String postId) {
        sendMessage("onShareSuccess", tag + "|" + postId);
      }

      @Override
      public void onCancel() {
        sendMessage("onShareCancel", tag);
      }

      @Override
      public void onError(String message) {
        sendMessage("onShareError", tag + "|" + message);
      }
    });
  }

  public static void share() {
    AndroidSdk.share();
  }

  public static void share(String url) {
    AndroidSdk.share(url, null);
  }

  public static void shareMessage(String msg, boolean sysOnly) {
    AndroidSdk.share();
  }

  public static void shareVideo(String url) {
    AndroidSdk.shareVideo(url);
  }

  public static void rate() {
    AndroidSdk.rateUs();
  }

  public static void inAppReview() {
    IvySdk.tryStartInAppReview();
  }

  public static void rate(float star) {
    AndroidSdk.rateUs(star);
  }

  @Deprecated
  public static void moreGame() {
    AndroidSdk.moreGame();
  }

  /**
   * mark the user entered to the main game play
   */
  public static void loginCompleted() {
    // delayed send the game message

  }

  public static void trackEvent(String category, String action, String label, int value) {
    AndroidSdk.track(category, action, label, value);
  }

  public static void trackScreen(String screenClass, String screenName) {
    IvySdk.trackScreen(screenClass, screenName);
  }

  public static void trackIvyEvent(String event, String data) {
    Bundle extra = new Bundle();
    if (data != null) {
      String[] splits = data.split(",");
      if (splits.length > 1) {
        for (int i = 0; i < splits.length; i += 2) {
          try {
            extra.putDouble(splits[i], Double.parseDouble(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            extra.putFloat(splits[i], Float.parseFloat(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            extra.putLong(splits[i], Long.parseLong(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            extra.putInt(splits[i], Integer.parseInt(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            extra.putString(splits[i], splits[i + 1]);
          } catch (Exception ignore) {
          }
        }
      }
    }

    IvySdk.logIvyEvent(event, extra);
  }

  public static void trackEvent(String event, String data) {
    Map<String, Object> extra = new HashMap<>();
    if (data != null) {
      String[] splits = data.split(",");
      if (splits.length > 1) {
        for (int i = 0; i < splits.length; i += 2) {
          try {
            extra.put(splits[i], Double.parseDouble(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            extra.put(splits[i], Float.parseFloat(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            extra.put(splits[i], Long.parseLong(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            extra.put(splits[i], Integer.parseInt(splits[i + 1]));
            continue;
          } catch (Exception ignore) {
          }

          try {
            extra.put(splits[i], splits[i + 1]);
          } catch (Exception ignore) {
          }
        }
      }
    }
    AndroidSdk.track(event, extra);
  }

  @Deprecated
  public static void showNative(String tag, int yPercent) {
    Log.e(TAG, "showNative(tag, yPercent) deprecated");
  }

  @Deprecated
  public static boolean showNativeBanner(String tag, int xPercent, int yPercent, String configFile) {
    return false;
  }

  @Deprecated
  public static boolean showNativeBanner(final String tag, final int x, final int y, final int w, final int h, final String configFile) {
    return false;
  }

  @Deprecated
  public static boolean showNativeBanner(final String tag, final float x, final float y, final float w, final float h) {
    return false;
  }

  @Deprecated
  public static boolean showNativeBanner(final String tag, final int x, final int y, final int w, final int h, final int sw, final int sh) {
    return false;
  }

  @Deprecated
  public static void closeNativeBanner(String tag) {

  }

  public static String getExtraData() {
    return AndroidSdk.getExtraData();
  }

  public static void onQuit() {
    AndroidSdk.forceQuit();
  }

  public static void login() {
    AndroidSdk.login();
  }

  public static void logout() {
    AndroidSdk.logout();
  }

  public static boolean isLogin() {
    return AndroidSdk.isLogin();
  }

  @Deprecated
  public static void invite() {
  }

  @Deprecated
  public static void challenge(String title, String message) {
  }

  public static String me() {
    return AndroidSdk.me();
  }

  public static String friends() {
    return AndroidSdk.friends();
  }

  public static void loadFullAd(final String tag) {
    AndroidSdk.loadFullAd(tag, new AdListener() {
      @Override
      public void onAdLoadSuccess() {
        sendMessage("onFullAdLoadSuccess", tag);
      }

      @Override
      public void onAdLoadFails() {
        sendMessage("onFullAdLoadFails", tag);
      }
    });
  }

  public static boolean hasNotch() {
    return AndroidSdk.hasNotch();
  }

  public static int getNotchHeight() {
    return AndroidSdk.getNotchHeight();
  }

  public static boolean isNetworkConnected() {
    return AndroidSdk.isNetworkConnected();
  }

  @Deprecated
  public static String cacheUrl(String url) {
    return "";
  }

  @Deprecated
  public static void cacheUrl(int tag, String url) {
    AndroidSdk.cacheUrl(tag, url);
  }

  public static String getConfig(int configKey) {
    return AndroidSdk.getConfig(configKey);
  }

  public static boolean hasApp(String packageName) {
    Activity activity = IvySdk.getActivity();
    return IvyUtils.hasApp(activity, packageName);
  }

  public static void launchApp(String packageName) {
    // fix package for amazon
    if ("amazon".equals(getAppstore())) {
      if ("com.merge.elves".equals(packageName)) {
        packageName = "com.merge.elves.amazon";
      } else if ("com.merge.farmtown".equals(packageName)) {
        packageName = "com.merge.farmtown.amazon";
      } else if ("com.merge.farmharvest".equals(packageName)) {
        packageName = "com.merge.farmharvest.amazon";
      } else if ("com.merge.inn".equals(packageName)) {
        packageName = "com.merge.inn.amazon";
      } else if ("com.merge.romance".equals(packageName)) {
        packageName = "com.merge.romance.amazon";
      } else if ("com.lisgame.animalstown".equals(packageName)) {
        packageName = "com.lisgame.animalstown.amazon";
      }
    }
    AndroidSdk.launchApp(packageName);
  }

  public static void getApp(String packageName) {
    // fix package for amazon
    if ("amazon".equals(getAppstore())) {
      if ("com.merge.elves".equals(packageName)) {
        packageName = "com.merge.elves.amazon";
      } else if ("com.merge.farmtown".equals(packageName)) {
        packageName = "com.merge.farmtown.amazon";
      } else if ("com.merge.farmharvest".equals(packageName)) {
        packageName = "com.merge.farmharvest.amazon";
      } else if ("com.merge.inn".equals(packageName)) {
        packageName = "com.merge.inn.amazon";
      } else if ("com.merge.romance".equals(packageName)) {
        packageName = "com.merge.romance.amazon";
      } else if ("com.lisgame.animalstown".equals(packageName)) {
        packageName = "com.lisgame.animalstown.amazon";
      }
    }
    AndroidSdk.openAppStore(packageName);
  }

  public static boolean checkAppInstalled(String packageName) {
    Activity activity = IvySdk.getActivity();
    return IvyUtils.hasApp(activity, packageName);
  }

  public static void openPromoteUrl(String packageName, String url) {
    boolean isAppInstalled = checkAppInstalled(packageName);
    if (isAppInstalled) {
      launchApp(packageName);
      return;
    }
    Activity activity = IvySdk.getActivity();
    if (activity != null) {
      activity.runOnUiThread(() -> IvyUtils.openBrowser(activity, url));
    }
  }

  public static void alert(String title, String message) {
    AndroidSdk.alert(title, message);
  }

  public static void toast(String message) {
    AndroidSdk.toast(message);
  }

  public static String getConfig(String packageName, int configKey) {
    return AndroidSdk.getConfig(packageName, configKey);
  }

  public static boolean isPaymentValid() {
    return AndroidSdk.isPaymentValid();
  }

  public static String getPaymentDatas() {
    return AndroidSdk.getPrices();
  }

  public static String getPrices() {
    return AndroidSdk.getPrices();
  }

  public static void onKill() {
    AndroidSdk.onKill();
  }

  public static void setUserTag(String userTag) {
    AndroidSdk.setUserTag(userTag);
  }

  public static void support(String email, String extra) {
    AndroidSdk.support(email, extra);
  }

  public static void support(String email, String title, String extra) {
    AndroidSdk.support(email, title, extra);
  }

  public static int getRemoteConfigInt(String key) {
    return AndroidSdk.getRemoteConfigInt(key);
  }

  public static long getRemoteConfigLong(String key) {
    return AndroidSdk.getRemoteConfigLong(key);
  }

  public static double getRemoteConfigDouble(String key) {
    return AndroidSdk.getRemoteConfigDouble(key);
  }

  public static boolean getRemoteConfigBoolean(String key) {
    return AndroidSdk.getRemoteConfigBoolean(key);
  }

  public static String getRemoteConfigString(String key) {
    return AndroidSdk.getRemoteConfigString(key);
  }

  public static void setUserProperty(String key, String value) {
    AndroidSdk.setUserProperty(key, value);
  }

  public static String encodeParams(String params) {
    return AndroidSdk.encodeParams(params);
  }

  public static String decodeParams(String params) {
    return AndroidSdk.decodeParams(params);
  }

  public static void silentLoginGoogle() {
    AndroidSdk.silentLoginGoogle(new GoogleListener() {
      @Override
      public void onSuccess(String googleId, String googleEmail) {
        sendMessage("onSilentLoginGoogle", TRUE);
      }

      @Override
      public void onFails() {
        sendMessage("onSilentLoginGoogleFailed", FALSE);
      }
    });
  }

  public static void loginGoogle() {
    AndroidSdk.loginGoogle(new GoogleListener() {
      @Override
      public void onSuccess(String googleId, String googleEmail) {
        Logger.debug(TAG, "Google onSuccess: " + googleId);
        sendMessage("onLoginGoogleSuccess", googleId);
      }

      @Override
      public void onFails() {
        Logger.debug(TAG, "Google onFails: ");
        sendMessage("onLoginGoogleFailure", FALSE);
      }
    });
  }

  @Deprecated
  public static void logoutGoogle() {
  }

  public static void displayLeaderboards() {
    AndroidSdk.showGoogleLeaderBoards();
  }

  public static void displayLeaderboard(String id) {
    AndroidSdk.showGoogleLeaderBoards(id);
  }

  public static void displayArchievements() {
    AndroidSdk.showGoogleAchievements();
  }

  public static void updateArchievement(final String id, int step) {
    try {
      IvySdk.updateGoogleAchievement(id, step);
    } catch (Throwable ignore) {
      // crash protected
    }
  }

  public static void updateLeaderboard(final String id, long value) {
    try {
      IvySdk.updateGoogleLeaderBoard(id, value);
    } catch (Throwable ignore) {
      // crash protected
    }
  }

  public static void updateLeaderboard(final String id, int value) {
    AndroidSdk.updateGoogleLeaderBoard(id, value);
  }

  public static boolean isGoogleSupport() {
    return AndroidSdk.isGoogleSupport();
  }


  public static void setDisplayInNotch(Activity activity) {
    AndroidSdk.setDisplayInNotch(activity);
  }

  @Deprecated
  public static void setPayVerifyUrl(String verifyUrl) {
  }

  public static void forceQuit() {
    AndroidSdk.forceQuit();
  }

  public static void copyText(final String str) {
    AndroidSdk.copyText(str);
  }

  public static void verifyIdCard() {
    AndroidSdk.verifyIdCard();
  }

  public static void resetIdCheck() {
    AndroidSdk.resetIdCheck();
  }

  public static int getIdCardVerifyedAge() {
    return AndroidSdk.getIdCardVerifyedAge();
  }

  public static void setIdCardVerified(int age) {
    AndroidSdk.setIdCardVerified(age);
  }

  public static long getFreeMem() {
    return AndroidSdk.getFreeMem();
  }

  public static long mmActualSize() {
    return IvySdk.mmActualSize();
  }

  public static void mmSetIntValue(String key, int value) {
    IvySdk.mmSetIntValue(key, value);
  }

  public static int mmGetIntValue(String key, int defaultValue) {
    return IvySdk.mmGetIntValue(key, defaultValue);
  }

  public static void mmSetLongValue(String key, long value) {
    IvySdk.mmSetLongValue(key, value);
  }

  public static long mmGetLongValue(String key, long defaultValue) {
    return IvySdk.mmGetLongValue(key, defaultValue);
  }

  public static void mmSetBoolValue(String key, boolean value) {
    IvySdk.mmSetBoolValue(key, value);
  }

  public static boolean mmGetBoolValue(String key, boolean defaultValue) {
    return IvySdk.mmGetBoolValue(key, defaultValue);
  }

  public static void mmSetFloatValue(String key, float value) {
    IvySdk.mmSetFloatValue(key, value);
  }

  public static float mmGetFloatValue(String key, float defaultValue) {
    return IvySdk.mmGetFloatValue(key, defaultValue);
  }

  public static void mmSetStringValue(String key, String value) {
    IvySdk.mmSetStringValue(key, value);
  }

  public static void mmSetStringValueWithExpiredSeconds(String key, String value, int expireDurationInSecond) {
    IvySdk.mmSetStringValueWithExpired(key, value, expireDurationInSecond);
  }

  public static String mmGetStringValue(String key, String defaultValue) {
    return IvySdk.mmGetStringValue(key, defaultValue);
  }

  public static boolean mmContainsKey(String key) {
    return IvySdk.mmContainsKey(key);
  }

  public static void mmRemoveKey(String key) {
    IvySdk.mmRemoveKey(key);
  }

  public static void mmRemoveKeys(String keys) {
    IvySdk.mmRemoveKeys(keys);
  }

  public static void mmClearAll() {
    IvySdk.mmClearAll();
  }

  public static boolean isNotificationEnabled() {
    return IvySdk.isNotificationChannelEnabled(IvySdk.getActivity());
  }

  public static void openNotificationSettings() {
    IvySdk.openNotificationSettings(IvySdk.getActivity());
  }

  public static String getKeyHashSha1() {
    return AndroidSdk.getKeyHash();
  }

  public static void clickUrl(String url) {
    AndroidSdk.clickUrl(url);
  }


  public static void readFirestore(String collection, String documentId) {
    FirestoreAdapter.getInstance().read(collection, documentId, new DatabaseListener() {
      @Override
      public void onData(String collection, String data) {
        sendMessage("onFirestoreReadData", collection + "|" + documentId + "|" + data);
      }

      @Override
      public void onSuccess(String collection) {
        // not use
      }

      @Override
      public void onFail(String collection) {
        sendMessage("onFirestoreReadFail", collection + "|" + documentId);
      }
    });
  }

  public static void readFirestore(String collection) {
    FirestoreAdapter.getInstance().read(collection, new DatabaseListener() {
      @Override
      public void onData(String collection, String data) {
        sendMessage("onFirestoreReadData", collection + "|" + data);
      }

      @Override
      public void onSuccess(String collection) {
        // not use
      }

      @Override
      public void onFail(String collection) {
        sendMessage("onFirestoreReadFail", collection);
      }
    });
  }

  public static void mergeFirestore(String collection, String jsonData) {
    try {
      if (jsonData == null || "".equals(jsonData)) {
        Logger.warning(TAG, "Empty " + jsonData);
        return;
      }
      FirestoreAdapter.getInstance().merge(collection, jsonData, new DatabaseListener() {
        @Override
        public void onData(String collection, String data) {
          // not use
        }

        @Override
        public void onSuccess(String collection) {
          sendMessage("onFirestoreMergeSuccess", collection);
        }

        @Override
        public void onFail(String collection) {
          sendMessage("onFirestoreMergeFail", collection);
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "mergeFirestore exception " + jsonData, t);
    }
  }

  public static void setFirestore(String collection, String jsonData) {
    try {
      if (jsonData == null || "".equals(jsonData)) {
        Logger.warning(TAG, "Empty " + jsonData);
        return;
      }
      FirestoreAdapter.getInstance().set(collection, jsonData, new DatabaseListener() {
        @Override
        public void onData(String collection, String data) {
          // not use
        }

        @Override
        public void onSuccess(String collection) {
          sendMessage("onFirestoreSetSuccess", collection);
        }

        @Override
        public void onFail(String collection) {
          sendMessage("onFirestoreSetData", collection);
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "setFirestore exception: " + jsonData, t);
    }
  }

  public static void updateFirestore(String collection, String transactionId, String jsonData) {
    try {
      if (jsonData == null || "".equals(jsonData)) {
        Logger.warning(TAG, "Empty " + jsonData);
        return;
      }
      String id = "";
      FirestoreAdapter.getInstance().update(collection, jsonData, new DatabaseListener() {
        @Override
        public void onData(String collection, String data) {
          // not use
        }

        @Override
        public void onSuccess(String collection) {
          sendMessage("onFirestoreUpdateSuccess", collection + "|" + transactionId);
        }

        @Override
        public void onFail(String collection) {
          sendMessage("onFirestoreUpdateFail", collection + "|" + transactionId);
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "updateFirestore exception: " + jsonData, t);
    }
  }

  public static void deleteFirestore(String collection) {
    FirestoreAdapter.getInstance().delete(collection, new DatabaseListener() {
      @Override
      public void onData(String collection, String data) {
        // not use
      }

      @Override
      public void onSuccess(String collection) {
        sendMessage("onFirestoreDeleteSuccess", collection);
      }

      @Override
      public void onFail(String collection) {
        sendMessage("onFirestoreDeleteFail", collection);
      }
    });
  }

  public static void snapshotFirestore(String collection) {
    FirestoreAdapter.getInstance().snapshot(collection, new DatabaseChangedListener() {
      @Override
      public void onData(String collection, String jsonData) {
        sendMessage("onFirestoreSnapshot", collection + "|" + jsonData);
      }
    });
  }

  public static void snapshotFirestore(String collection, String documentId) {
    FirestoreAdapter.getInstance().snapshot(collection, documentId, new DatabaseChangedListener() {
      @Override
      public void onData(String collection, String jsonData) {
        sendMessage("onFirestoreSnapshot", collection + "|" + documentId + "|" + jsonData);
      }
    });
  }

  public static void queryFirestore(String collection) {
    FirestoreAdapter.getInstance().query(collection, new DatabaseListener() {
      @Override
      public void onData(String collection, String data) {
        sendMessage("onFirestoreQueryData", collection + "|" + data);
      }

      @Override
      public void onSuccess(String collection) {
        sendMessage("onFirestoreQuerySuccess", collection);
      }

      @Override
      public void onFail(String collection) {
        sendMessage("onFirestoreQueryFail", collection);
      }
    });
  }

  @Deprecated
  public static void playerFinder() {
  }

  public static void cloudfunction(String name, String jsonString) {
    Logger.debug(TAG, ">>> " + name + ", " + jsonString);
    JSONObject jsonObject = null;
    try {
      jsonObject = new JSONObject(jsonString);
    } catch (Exception ex) {
      Logger.error(TAG, "Error parse to JSON, " + jsonString);
    }
    IvySdk.executeCloudFunction(name, jsonObject, new OnCloudFunctionResult() {
      @Override
      public void onResult(String data) {
        sendMessage("onCloudFunctionResult", name + "|" + data);
      }

      @Override
      public void onFail(String errorMessage) {
        sendMessage("onCloudFunctionFailed", name + "|" + errorMessage);
      }
    });
  }

  public static void cloudfunction(String name) {
    Logger.debug(TAG, ">>> " + name);

    IvySdk.executeCloudFunction(name, null, new OnCloudFunctionResult() {
      @Override
      public void onResult(String data) {
        Logger.debug(TAG, "OnCloudFunctionResult >>> " + data);
        sendMessage("onCloudFunctionResult", name + "|" + data);
      }

      @Override
      public void onFail(String errorMessage) {
        Logger.debug(TAG, "OnCloudFunctionFailed >>> " + errorMessage);
        sendMessage("onCloudFunctionFailed", name + "|" + errorMessage);
      }
    });
  }

  public static void helpEngagement(String customerName, String systemInfo) {
    try {
      AndroidSdk.helpshift(customerName, systemInfo);
    } catch (Throwable t) {
      // ignore
    }
  }

  @Deprecated
  public static void helpshift(String customerName, String systemInfo) {
    try {
      AndroidSdk.helpshift(customerName, systemInfo);
    } catch (Throwable t) {
      // ignore
    }
  }

  @Deprecated
  public static void appFeedback(String sectionUrl, String systemInfo) {
    String firebaseUserId = getFirebaseUserId();
    AndroidSdk.helpshift(firebaseUserId, systemInfo);
  }

  @Deprecated
  public static void appFeedback(String sectionUrl, String ticketUrl, String userSurveyUrl, String systemInfo) {
    String firebaseUserId = getFirebaseUserId();
    AndroidSdk.helpshift(firebaseUserId, systemInfo);
  }

  public static String getPushToken() {
    return IvySdk.getPushToken();
  }

  public static void logoutFacebook() {
    AndroidSdk.logoutFacebook();
  }

  public static void logError(String message) {
    IvySdk.logError(message);
  }

  public static void showProgressBar() {
    IvySdk.showProgressBar(IvySdk.getActivity());
  }

  public static void hideProgressBar() {
    IvySdk.hideProgressBar(IvySdk.getActivity());
  }

  public static void displayUrl(String title, String url) {
    AndroidSdk.showWebView(title, url);
  }

  public static void triggerInAppMessage(String eventName) {
    IvySdk.triggerInAppMessage(eventName);
  }

  public static void suppressInAppMessage(boolean suppress) {
    IvySdk.supressInAppMessage(suppress);
  }

  public static void inAppMessageClicked(String campaignId) {
    IvySdk.inAppMessageClicked(campaignId);
  }

  public static void sendChat(@NonNull String database, @NonNull String path, @NonNull String data) {
    try {
      Logger.debug(TAG, "Send Chat: " + data);
      com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(data);
      if (jsonObject != null) {
        FirebaseDatabase.getInstance(database).getReference(path).push().setValue(jsonObject);
      }
    } catch (Throwable t) {
      Logger.error(TAG, "write socket message exception", t);
    }
  }

  @Deprecated
  public static void listen(String database, String path) {
    listenFirebaseDatabase(database, path);
  }

  public static void deleteFirebaseDatabasePath(@NonNull String database, @NonNull String path) {
    FirebaseDatabase.getInstance(database).getReference(path).removeValue();
  }

  public static void listenFirebaseDatabase(@NonNull String database, @NonNull String path) {
    try {

      FirebaseDatabase.getInstance(database).getReference(path).addChildEventListener(new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
          Object o = snapshot.getValue();
          if (o instanceof Map) {
            try {
              String id = snapshot.getKey();
              JSONObject resultObject = new JSONObject((Map<String, Object>) o);
              resultObject.put("_id", id);
              resultObject.put("path", path);
              sendMessage("onChatMessage", resultObject.toString());
            } catch (Throwable e) {
              Logger.error(TAG, "OnChatMessage exception", e);
            }
          }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
          Logger.debug(TAG, "onChildChanged");
          Object o = snapshot.getValue();
          if (o instanceof Map) {
            try {
              String id = snapshot.getKey();
              JSONObject resultObject = new JSONObject((Map<String, Object>) o);
              resultObject.put("_id", id);
              resultObject.put("path", path);
              resultObject.put("_previous", previousChildName);
              sendMessage("onChatMessageChanged", resultObject.toString());
            } catch (Throwable e) {
              Logger.error(TAG, "onChatMessageChanged exception", e);
            }
          }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot snapshot) {
          Logger.debug(TAG, "onChildRemoved");
          Object o = snapshot.getValue();
          if (o instanceof Map) {
            try {
              String id = snapshot.getKey();
              JSONObject resultObject = new JSONObject((Map<String, Object>) o);
              resultObject.put("_id", id);
              resultObject.put("path", path);
              sendMessage("onChatMessageRemoved", resultObject.toString());
            } catch (Throwable e) {
              Logger.error(TAG, "onChatMessageChanged exception", e);
            }
          }
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
          Logger.debug(TAG, "onChildMoved");
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
          Logger.debug(TAG, "onCancelled");
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "listen socket message exception", t);
    }
  }

  public static void setTargetForChild() {
    AndroidSdk.setTargetForChild();
  }

  @Deprecated
  public static void writeSavedGame(String name, String data) {
  }

  @Deprecated
  public static void showSavedGamesUI() {
  }

  @Deprecated
  public static void checkUpdate() {
  }

  @Deprecated
  public static boolean hasAppOpenAd() {
    return false;
  }

  @Deprecated
  public static void showAppOpenAd(final String tag, final int id) {

  }

  @Deprecated
  public static void like() {
  }

  public static void joinQQGroup(String uid, String ukey) {

  }

  public static String getLocation() {
    return "";
  }

  public static boolean isAdSystemAvailable() {
    return true;
  }

  public static void saveUserAttribute(String dataJsonString) {
    JSONObject dataJson = null;
    try {
      if (!"".equals(dataJsonString)) {
        dataJson = new JSONObject(dataJsonString);
      }
      AndroidSdk.saveUserAttribute(dataJson);
    } catch (Throwable t) {
      // ignore
      Logger.error(TAG, "saveUserAttribute exception", t);
    }
  }

  public static void trackMainLine(String name, int value) {
    AndroidSdk.trackMainLine(name, value);
  }

  public static void trackRetentionStep(int stepId, String stepName) {
    AndroidSdk.trackRetentionStep(stepId, stepName);
  }

  public static void recordCoreAction(String name, int inc) {
    AndroidSdk.recordCoreAction(name, inc);
  }

  public static void commitCoreAction(String name) {
    AndroidSdk.commitCoreAction(name);
  }

  /**
   * 记录玩家当前的虚拟货币数量，在虚拟货币发生变化的时候调用。
   *
   * @param name  虚拟货币名
   * @param value 存留数量
   */
  public static void recordVirtualCurrency(String name, int value) {
    AndroidSdk.recordVirtualCurrency(name, value);
  }

  public static void trackActivityStart(String name) {
    AndroidSdk.trackActivityStart(name, null);
  }

  public static void trackActivityStart(String name, String catalog) {
    AndroidSdk.trackActivityStart(name, catalog);
  }

  public static void trackActivityStep(String name, int step) {
    AndroidSdk.trackActivityStep(name, step);
  }

  public static void trackActivityEnd(String name) {
    AndroidSdk.trackActivityEnd(name);
  }

  public static void trackActivityEvent(String name, String catalog, float value) {
    AndroidSdk.trackActivityEvent(name, catalog, value, false);
  }

  public static void trackActivityEvent(String name, String catalog, float value, boolean iap) {
    AndroidSdk.trackActivityEvent(name, catalog, value, iap);
  }

  public static void spendVirtualCurrency(String virtualCurrencyName, String itemid, int value) {
    AndroidSdk.spendVirtualCurrency(virtualCurrencyName, itemid, value, 0, null);
  }

  public static void spendVirtualCurrency(String virtualCurrencyName, String itemid, int value, int currentValue) {
    AndroidSdk.spendVirtualCurrency(virtualCurrencyName, itemid, value, currentValue, null);

  }

  public static void spendVirtualCurrency(String virtualCurrencyName, String itemid, int value, int currentValue, String catalog) {
    AndroidSdk.spendVirtualCurrency(virtualCurrencyName, itemid, value, currentValue, catalog);
  }

  @Deprecated
  public static void earnVirtualCurrency(String virtualCurrencyName, String itemid, int value) {
    AndroidSdk.earnVirtualCurrency(virtualCurrencyName, itemid, value, 0);
  }

  public static void earnVirtualCurrency(String virtualCurrencyName, String itemid, int value, int currentValue) {
    AndroidSdk.earnVirtualCurrency(virtualCurrencyName, itemid, value, currentValue);
  }

  public static void trackScreenStart(String screenName) {
    AndroidSdk.trackScreenStart(screenName);
  }

  public static void trackScreenEnd(String screenName) {
    AndroidSdk.trackScreenEnd(screenName);
  }

  public static void trackEngagement(long seconds) {
    if (seconds > 0) {
      IvySdk.trackEngagement(seconds);
    }
  }

  public static void showInAppMessage(String message) {
    IvySdk.showInAppMessage(message);
  }

  @NonNull
  public static String getFirebaseUserId() {
    return AndroidSdk.getFirebaseUserId();
  }

  @NonNull
  public static String getFirebaseUserName() {
    return AndroidSdk.getFirebaseUserName();
  }

  public static boolean isAnonymous() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      return true;
    }
    return user.isAnonymous();
  }


  /**
   * 检查当前账号是否链接指定提供商
   */
  public static boolean isProviderLinked(@NonNull String providerId) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      return false;
    }

    List<? extends UserInfo> userInfoList = user.getProviderData();
    for (UserInfo info : userInfoList) {
      if (providerId.equals(info.getProviderId())) {
        return true;
      }
    }
    return false;
  }

  public static String getProviderLinkedDisplayName(@NonNull String providerId) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      return "";
    }
    List<? extends UserInfo> userInfoList = user.getProviderData();
    for (UserInfo info : userInfoList) {
      if (providerId.equals(info.getProviderId())) {
        if (EmailAuthProvider.PROVIDER_ID.equals(providerId)) {
          String email = info.getEmail();
          if (email != null && !"".equals(email)) {
            return email;
          }
          return "";
        }

        String displayName = info.getDisplayName();
        if (displayName != null && !"".equals(displayName)) {
          return displayName;
        }
        return "";
      }
    }
    return "";
  }

  /**
   * 更新玩家最后登录方式。检查当前账号，以Play Games, Email, Facebook的方式以此更新玩家最后以此登入方式。
   */
  private static void updateLastSignedProvider(@Nullable String email, @Nullable String password) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      return;
    }
    try {
      List<? extends UserInfo> userInfoList = user.getProviderData();
      List<String> validProviderIds = new ArrayList<>();
      for (UserInfo info : userInfoList) {
        validProviderIds.add(info.getProviderId());
      }

      if (validProviderIds.contains(PlayGamesAuthProvider.PROVIDER_ID)) {
        JSONObject o = new JSONObject();
        o.put("provider", PlayGamesAuthProvider.PROVIDER_ID);
        IvySdk.mmSetStringValue(IvySdk.KEY_LAST_SIGNIN_PROVIDER, o.toString());
        return;
      }

      if (validProviderIds.contains(EmailAuthProvider.PROVIDER_ID)) {
        if (email != null && password != null) {
          JSONObject o = new JSONObject();
          o.put("provider", EmailAuthProvider.PROVIDER_ID);
          o.put("email", email);
          o.put("password", password);
          IvySdk.mmSetStringValue(IvySdk.KEY_LAST_SIGNIN_PROVIDER, o.toString());
          return;
        }
      }

      if (validProviderIds.contains(FacebookAuthProvider.PROVIDER_ID)) {
        JSONObject o = new JSONObject();
        o.put("provider", FacebookAuthProvider.PROVIDER_ID);
        IvySdk.mmSetStringValue(IvySdk.KEY_LAST_SIGNIN_PROVIDER, o.toString());
      }
    } catch (Throwable t) {
      Logger.error(TAG, "updateLastSignedProvider exception", t);
    }
  }

  /**
   * TODO: 玩家如果没有登入Play Games账号，将自动获取Facebook, Email的token用户自动登录。
   * <p>
   * 在绑定的账号状态丢失时，需要主动掉起由玩家处理
   * <p>
   * 游戏自动登录功能。
   * 检查当前的登录账号，
   * 1. 如果当前用户没有登入用户（一般是新安装)，尝试以PlayGames静默登录（静默登录失败),将匿名登录
   * 2. 如果当前用户已经是登入状态，直接使用当前账号回调
   */
  public static void signIn() {
    if (providerFacade != null && providerFacade.onlyUsingPlatformAccount()) {
      providerFacade.signIn(IvySdk.getActivity(), new OnSignedInListener() {
        @Override
        public void onSignedInSuccess(SignInProfile signInProfile) {
          Logger.debug(TAG, "signIn success");
        }

        @Override
        public void onSignedInError(String code, String message) {
          Logger.debug(TAG, "onSignedInError success");

        }

        @Override
        public void onSignedInCancel() {
          Logger.debug(TAG, "onSignedInCancel success");
        }
      });
      return;
    }

    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser currentUser = auth.getCurrentUser();
    if (currentUser != null) {
      currentUser.reload().addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
          FirebaseUser currentUser1 = auth.getCurrentUser();
          if (currentUser1 != null) {
            sendMessage(MSG_FIRESTORE_CONNECTED, currentUser1.getUid());
            IvySdk.setUserID(currentUser1.getUid());
            IvySdk.onAccountSignedIn();
          }
        } else {
          Exception e = task.getException();
          if (e != null) {
            FirebaseAuthError authError = FirebaseAuthError.fromException(e);
            sendMessage("onFirestoreConnectError", authError.name() + "|" + authError.getDescription());
          }
        }
      });
      return;
    }

    // 查询最后一次登录的信息，如果没有记录启动Play games的，启动系统的静默登录
    JSONObject lastSignInProvider = IvySdk.mmGetJsonValue(IvySdk.KEY_LAST_SIGNIN_PROVIDER);
    if (lastSignInProvider == null) {
      IvySdk.loginPlayGames(new GoogleListener() {
        @Override
        public void onSuccess(String googleId, String googleEmail) {
          FirestoreAdapter.getInstance().initializeAfterSignInPlayGames(new DatabaseConnectListener() {
            @Override
            public void onSuccess() {
              String uid = auth.getUid();
              if (uid != null) {
                sendMessage(MSG_FIRESTORE_CONNECTED, uid);
                updateLastSignedProvider(null, null);
                IvySdk.onAccountSignedIn();

              } else {
                signInAnonymously();
              }
            }

            @Override
            public void onFail(String errorCode, String errorMessage) {
              signInAnonymously();
            }
          });
        }

        @Override
        public void onFails() {
          signInAnonymously();
        }
      });
      return;
    }

    String provider = lastSignInProvider.optString("provider");
    Logger.debug(TAG, "Use sign in provider " + provider);
    switch (provider) {
      case PlayGamesAuthProvider.PROVIDER_ID:
        signInWithPlayGames();
        break;
      case FacebookAuthProvider.PROVIDER_ID:
        signInWithFacebook();
        break;
      case EmailAuthProvider.PROVIDER_ID:
        String email = lastSignInProvider.optString("email");
        String password = lastSignInProvider.optString("password");
        if (!"".equals(email) && !"".equals(password)) {
          signInWithEmailAndPassword(email, password);
        }
        break;
    }
  }

  /**
   * 用户主动登入Google Play Games, 不能fallback到静默，
   */
  public static void signInWithPlayGames() {
    Logger.debug(TAG, "signInWithPlayGames");
    IvySdk.slientLoginGoogle(new GoogleListener() {
      @Override
      public void onSuccess(String googleId, String googleEmail) {
        FirestoreAdapter.getInstance().initializeAfterSignInPlayGames(new DatabaseConnectListener() {
          @Override
          public void onSuccess() {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
              sendMessage(MSG_FIRESTORE_CONNECTED, uid);
              IvySdk.setUserID(uid);
              IvySdk.onAccountSignedIn();

              updateLastSignedProvider(null, null);
            }
          }

          @Override
          public void onFail(String errorCode, String errorMessage) {
            sendMessage("onFirestoreConnectError", errorCode + "|" + errorMessage);
          }
        });
      }

      @Override
      public void onFails() {
        sendMessage("onFirestoreConnectError", "ERROR_SIGNIN_GOOGLE" + "|" + "Sign In PlayGames failed");
      }
    });
  }

  public static void invitePlayers(String tagStr) {
    Logger.debug(TAG, "invitePlayers");
    String url = IvySdk.getGridConfigString("share_link_url", "");
    if (tagStr != null) {
      url = url.replaceAll("#tag#", tagStr);
    }
    Logger.debug(TAG, "Invite players url: " + url);
    try {
      Activity activity = IvySdk.getActivity();
      if (activity == null) {
        return;
      }

      Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
      shareIntent.setType("text/plain");
      shareIntent.putExtra(Intent.EXTRA_SUBJECT, tagStr);
      shareIntent.putExtra(Intent.EXTRA_TEXT, url);
      shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
      activity.startActivity(Intent.createChooser(shareIntent, activity.getString(R.string.share_game_title)));
    } catch (Throwable t) {
      Logger.error(TAG, "fallbackToSystemShare exception", t);
    }
  }

  /**
   * 用户主动登录Facebook
   */
  public static void signInWithFacebook() {
    Logger.debug(TAG, "signInWithFacebook");

    AndroidSdk.loginFacebook(new FacebookLoginListener() {
      @Override
      public void onReceiveLoginResult(boolean success) {
        if (success) {
          FirestoreAdapter.getInstance().initializeAfterSignInFacebook(new DatabaseConnectListener() {
            @Override
            public void onSuccess() {
              String uid = FirebaseAuth.getInstance().getUid();
              if (uid != null) {
                sendMessage(MSG_FIRESTORE_CONNECTED, uid);
                IvySdk.setUserID(uid);
                IvySdk.onAccountSignedIn();

                updateLastSignedProvider(null, null);
              } else {
                sendMessage("onFirestoreConnectError", FirebaseAuthError.ERROR_UNKNOWN.name() + "|" + FirebaseAuthError.ERROR_UNKNOWN.getDescription());
              }
            }

            @Override
            public void onFail(String errorCode, String errorMessage) {
              sendMessage("onFirestoreConnectError", errorCode + "|" + errorMessage);
            }
          });
        } else {
          sendMessage("onFirestoreConnectError", "ERROR_SIGNIN_FACEBOOK" + "|" + "Sign In Facebook failed");
        }
      }

      @Override
      public void onReceiveFriends(String friends) {
      }
    });

  }

  /**
   * 非静默的Email/Password登录。
   *
   * @param email
   * @param password
   */
  public static void signInWithEmailAndPassword(String email, String password) {
    Logger.debug(TAG, "signInWithEmailAndPassword : ");
    FirestoreAdapter.getInstance().signInWithEmailAndPassword(email, password, new DatabaseConnectListener() {
      @Override
      public void onSuccess() {
        Logger.debug(TAG, "signInWithEmailAndPassword success");
        String uid = FirebaseAuth.getInstance().getUid();
        IvySdk.setUserID(uid);
        sendMessage(MSG_FIRESTORE_CONNECTED, uid);
        IvySdk.onAccountSignedIn();

        updateLastSignedProvider(email, password);

        // save the email and password for later using
        IvySdk.mmSetStringValue("__saved_email", email);
        IvySdk.mmSetStringValue("__saved_password", password);
      }

      @Override
      public void onFail(String errorCode, String errorMessage) {
        Logger.debug(TAG, "signInWithEmailAndPassword exception" + errorCode + ", " + errorMessage);
        sendMessage("onFirestoreConnectError", errorCode + "|" + errorMessage);
      }
    });
  }

  private static void signInAnonymously() {
    FirebaseAuth auth = FirebaseAuth.getInstance();
    auth.signInAnonymously().addOnCompleteListener(task -> {
      if (task.isSuccessful()) {
        String uid = auth.getUid();
        if (uid != null) {
          sendMessage(MSG_FIRESTORE_CONNECTED, uid);
          IvySdk.setUserID(uid);
          IvySdk.onAccountSignedIn();

        } else {
          sendMessage("onFirestoreConnectError", "");
        }
      }
    });
  }

  /**
   * 系统级的账号无法解绑playgames. 当前仅有一个ID无法解绑
   */
  public static boolean canUnlink(@NonNull String providerId) {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    if (currentUser == null) {
      return false;
    }

    if (PlayGamesAuthProvider.PROVIDER_ID.equals(providerId)) {
      return false;
    }

    boolean contains = false;
    List<? extends UserInfo> userInfoList = currentUser.getProviderData();
    int providerNum = 0;
    for (UserInfo info : userInfoList) {
      if (info != null) {
        String p = info.getProviderId();
        if (providerId.equals(p)) {
          contains = true;
        }
        if (PlayGamesAuthProvider.PROVIDER_ID.equals(p) || FacebookAuthProvider.PROVIDER_ID.equals(p) || EmailAuthProvider.PROVIDER_ID.equals(p)) {
          providerNum++;
        }
      }
    }
    return contains && providerNum >= 2;
  }

  public static void setSignInProvider(@NonNull String providerId, @NonNull String email, @NonNull String password) {
    try {
      JSONObject o = new JSONObject();
      o.put("provider", providerId);
      if (EmailAuthProvider.PROVIDER_ID.equals(providerId)) {
        o.put("email", email);
        o.put("password", password);
      }
      IvySdk.mmSetStringValue(IvySdk.KEY_LAST_SIGNIN_PROVIDER, o.toString());
    } catch (Throwable t) {
      Logger.error(TAG, "setSignInProvider exception", t);
    }
  }

  public static void unlinkProvider(@NonNull String providerId) {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    if (currentUser != null) {
      currentUser.unlink(providerId).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
          if (task.isSuccessful()) {
            sendMessage("onFirestoreUnlinked", providerId);
            updateLastSignedProvider(null, null);
          } else {
            sendMessage("onFirestoreUnlinkedError", providerId);
          }
        }
      });
    }
  }

  public static void updatePassword(@NonNull String password) {
    FirestoreAdapter.getInstance().updatePassword(password, new OnPasswordChangedListener() {
      @Override
      public void onSuccess() {
        String uid = FirebaseAuth.getInstance().getUid();
        sendMessage("onPasswordChangedSuccess", uid);
      }

      @Override
      public void onError(@NonNull String errorCode, @NonNull String errorMessage) {
        sendMessage("onPasswordChangedError", errorCode + "|" + errorMessage);
      }
    });
  }

  public static void signOut() {
    FirestoreAdapter.getInstance().signOutFirestore();
  }

  /**
   * 由游戏主动消耗用户购买
   */
  public static void consumePurchase(@NonNull String purchaseToken) {
    Logger.debug(TAG, "consumePurchase >>> " + purchaseToken);
    IvySdk.consumePurchase(purchaseToken, new OrderConsumeListener() {
      @Override
      public void onConsumeSuccess(@NonNull String purchaseToken) {
        sendMessage("onConsumeSuccess", purchaseToken);
      }

      @Override
      public void onConsumeError(@NonNull String purchaseToken, String errorCode, String errorMessage) {
        sendMessage("onConsumeError", purchaseToken + "|" + errorCode + "|" + errorMessage);
      }
    });
  }

  @Deprecated
  public static void displayOfferwall() {
  }

  @Deprecated
  public static void checkOfferwallCredits() {
  }

  /**
   * 是否移除Ad, flag: true 移除ad,移除ad, ad preload将不会执行
   * false
   *
   * @param flag
   */
  public static void removeAds(boolean flag) {

  }

  public static void muteAudio(boolean flag) {
    try {
      MobileAds.setAppMuted(flag);
      ApplovinManager.muteAudio(flag);
    } catch (Throwable t) {
      Logger.error(TAG, "Mute audio exception: " + flag, t);
    }
  }

  @Deprecated
  public static void onStart() {
  }

  @Deprecated
  public static void onStop() {
  }

  @Deprecated
  public static void signOutFirestore() {
    signOut();
  }

  @Deprecated
  public static void showPromoteAd(final String tag) {
  }

  /**
   * 在由产品设计的交叉推广任务完成后, 游戏调用此函数向服务器报告任务完成。
   * Android系统将通过进程间通信，实时通知发起方APP。如果发起方APP，未启动，在打开时，的回调里也将会触发
   */
  public static void reportCrossPromotionTaskFinished() {

  }

  public static void checkCrossPromotion(String pkg) {

  }

  public static void showBanner(String tag, int pos, int animate) {
    AndroidSdk.showBanner(tag, pos, animate);
  }

  public static void showBanner(String tag, int pos) {
    AndroidSdk.showBanner(tag, pos);
  }

  public static void showBanner(int pos) {
    AndroidSdk.showBanner("default", pos);
  }

  public static void closeBanner() {
    AndroidSdk.closeBanner("default");
  }

  public static void closeBanner(String tag) {
    AndroidSdk.closeBanner(tag);
  }

  public static void shareImageOnFacebook(String localUrl) {

  }

  /**
   * 设置应用不使用ad, 关闭开屏广告等
   */
  public static void noAd() {
    IvySdk.mmSetBoolValue(IvySdk.KEY_NO_AD_FLAG, true);
  }

  public static void openFacebookPage(String pageId) {
    String facebookUrl = "www.facebook.com/" + pageId;

    Activity activity = IvySdk.getActivity();
    if (activity == null) {
      return;
    }

    activity.runOnUiThread(() -> {
      try {
        if (!pageId.isEmpty()) {
          // open the Facebook app using facebookID (fb://profile/facebookID or fb://page/facebookID)
          Uri uri = Uri.parse("fb://page/" + pageId);
          activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
      } catch (Throwable e) {
        Logger.error(TAG, "openFacebookPage native exception", e);
        // Facebook is not installed. Open the browser
        // Facebook is not installed. Open the browser
        try {
          Uri uri = Uri.parse(facebookUrl);
          activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Throwable t) {
          Logger.error(TAG, "openFacebookPage exception", t);
        }
      }
    });
  }

  @Keep
  public static boolean isPostNotificationPermissionGranted() {
    Activity activity = IvySdk.getActivity();
    if (activity != null) {
      return LocalNotificationManager.isPermissionEnabled(activity);
    }
    return false;
  }

  @Keep
  public static void requestPostNotificationPermission() {
    long lastRequestNoficationTime = IvySdk.mmGetLongValue("_last_request_notification_permission_time", 0L);
    if (lastRequestNoficationTime > 0) {
      return;
    }

    Activity activity = IvySdk.getActivity();
    if (activity != null) {
      LocalNotificationManager.enablePermission(activity);
      IvySdk.mmSetLongValue("_last_request_notification_permission_time", System.currentTimeMillis());
    }
  }

  public static void checkHelpEngagement() {
    AndroidSdk.checkHelpEngagement();
  }

  public static String getAppstore() {
    Context context = IvySdk.CONTEXT;
    if (context == null) {
      return "google";
    }

    try {
      ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
      if (ai != null && ai.metaData != null) {
        return ai.metaData.getString("adsfall.appstore", "google");
      }
    } catch (Throwable t) {
      Logger.error(TAG, "getAppstore exception", t);
    }
    return "google";
  }

  public static void openUrl(String url) {
    AndroidSdk.clickUrl(url);
  }

  public static void setUserConfigString(String key, String value) {
    FirestoreAdapter.getInstance().updateUserConfig(key, value);
  }

  public static void setUserConfigInt(String key, int value) {
    FirestoreAdapter.getInstance().updateUserConfig(key, value);
  }
}