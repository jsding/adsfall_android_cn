package com.ivy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.adsfall.BuildConfig;
import com.adsfall.R;
import com.alibaba.fastjson.JSON;
import com.android.client.AndroidSdk;
import com.android.client.GoogleListener;
import com.android.client.IProviderFacade;
import com.android.client.InAppMessageListener;
import com.android.client.OfferwallCreditListener;
import com.android.client.OnCloudFunctionResult;
import com.android.client.OnDataListener;
import com.android.client.OnSkuDetailsListener;
import com.android.client.OrderConsumeListener;
import com.android.client.SKUDetail;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay;
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks;
import com.google.firebase.inappmessaging.model.Action;
import com.google.firebase.inappmessaging.model.BannerMessage;
import com.google.firebase.inappmessaging.model.CampaignMetadata;
import com.google.firebase.inappmessaging.model.ImageData;
import com.google.firebase.inappmessaging.model.ImageOnlyMessage;
import com.google.firebase.inappmessaging.model.InAppMessage;
import com.google.firebase.inappmessaging.model.MessageType;
import com.google.firebase.inappmessaging.model.ModalMessage;
import com.google.firebase.inappmessaging.model.Text;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.ConfigUpdate;
import com.google.firebase.remoteconfig.ConfigUpdateListener;
import com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException;
import com.ivy.ads.IvyAds;
import com.ivy.ads.IvyAdsManager;
import com.ivy.ads.adapters.AdmobManager;
import com.ivy.ads.adapters.ApplovinManager;
import com.ivy.ads.adapters.IronsourceManager;
import com.ivy.ads.events.DefaultEventLogger;
import com.ivy.ads.interfaces.IvyAdCallbacks;
import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.ads.managers.AppOpenAdManager;
import com.ivy.ads.managers.BannerAdManager;
import com.ivy.billing.PurchaseManager;
import com.ivy.billing.PurchaseManagerWrapper;
import com.ivy.event.EventBus;
import com.ivy.internal.Banner;
import com.ivy.networks.grid.GridManager;
import com.ivy.networks.tracker.impl.ParfkaFactory;
import com.ivy.networks.ui.dialog.ImmersiveDialog;
import com.ivy.networks.util.Util;
import com.ivy.push.local.LocalNotificationManager;
import com.ivy.util.Logger;
import com.tencent.mmkv.MMKV;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class IvySdk {
  private static final String TAG = "IVYSDK";

  private static final int RC_ACHIEVEMENT_UI = 9003;
  private static final int RC_REQUEST_UPDATE = 8001;
  public static final int RC_WEBVIEW = 880;

  public static final int POS_LEFT_TOP = 1;
  public static final int POS_LEFT_BOTTOM = 2;
  public static final int POS_CENTER_TOP = 3;
  public static final int POS_CENTER_BOTTOM = 4;
  public static final int POS_CENTER = 5;
  public static final int POS_RIGHT_TOP = 6;
  public static final int POS_RIGHT_BOTTOM = 7;

  public static final String KEY_NO_AD_FLAG = "internal_no_ad";
  public static final String KEY_CORE_ACTIONS = "internal_core_action";
  public static final String KEY_GAME_MAIN_LINE = "internal_main_line";
  public static final String KEY_VIRUTAL_CURRENCY = "internal_virtual_currency";
  private static final String KEY_CUSTOM_REMOTE_CONFIG = "_custom_remote_config";
  public static final String KEY_LOCAL_USER_PROPERIES = "_pf_user_properties";

  private static final String KEY_AUTO_INAPP_MESSAGE_EVENT = "auto_inapp_message_event";

  public static final String KEY_REMOTE_CONFIG_SHARE_TAGS = "remote_share_tags";

  public static final String KEY_LAST_SIGNIN_PROVIDER = "_last_signin_provider";

  private static final String ID_MMKV_GAMEDATA = "_gamedata_";

  public static boolean skipPauseOnce = false;
  public static boolean skipResumeOnce = false;

  public static Context CONTEXT;

  private static final Boolean sdkInitialized = false;
  private static IvyAdsManager adManager;
  private static GridManager gridManager = null;
  private static DefaultEventLogger eventTracker;

  private static PurchaseManagerWrapper purchaseManagerWrapper;

  private static WeakReference<Activity> main = null;
  private static boolean alreadyResumed = false;
  private static boolean alreadyPaused = false;

  private static Map<String, JSONObject> storeItems = new HashMap<>();

  private static JSONObject googleAchievements = null;
  private static JSONObject googleLeaderBoards = null;


  private static MMKV gameDataMMKV = null;

  private static String userAttributeUrl = null;

  private static boolean afConversionTracked = false;

  // 全局记录google play service是否可用，默认认为可用
  private static boolean isGooglePlayServiceAvailable = true;

  @SuppressLint("StaticFieldLeak")
  private static AppOpenAdManager appOpenAdManager;

  private static boolean useWorkerManagerPush = false;

  public static synchronized boolean isInitialized() {
    boolean booleanValue;
    synchronized (IvySdk.class) {
      booleanValue = sdkInitialized;
    }
    return booleanValue;
  }

  /**
   * this method will called by ContentProvider, we fetch basic application information from xml
   *
   * @param applicationContext application context
   */
  public static synchronized void sdkInitialize(Context applicationContext) {
    Log.d(TAG, "IvySDK initialized");
    CONTEXT = applicationContext;
  }

  @SuppressLint("StaticFieldLeak")
  private static FirebaseRemoteConfig mFirebaseRemoteConfig;

  private static ConfigUpdateListenerRegistration configUpdateListenerRegistration;


  public static void updateCurrentActivity(Activity activity) {
    if (activity != null) {
      main = new WeakReference<>(activity);
    } else {
      main = null;
    }
  }

  /**
   * The sdk real initialize function.
   */
  public static synchronized void initialize(@NonNull final Activity activity, final Bundle savedInstanceState, @NonNull final InitializeCallback callback) {
    try {
      FirebaseApp.initializeApp(activity);
    } catch (Throwable ignored) {

    }

    main = new WeakReference<>(activity);

    long startupTime = System.currentTimeMillis();

    String appToken = "parfka";
    ApplicationInfo ai = null;
    try {
      ai = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    if (ai != null && ai.metaData != null) {
      Object o = ai.metaData.get("parfka.appToken");
      if (o instanceof String) {
        appToken = String.valueOf(o);
      }
    }
    ParfkaFactory.setApptoken(appToken);

    final SharedPreferences sp = main.get().getSharedPreferences("prefs", Context.MODE_PRIVATE);

    boolean debugMode = false;
    if (ai != null && ai.metaData != null) {
      debugMode = ai.metaData.getBoolean("ivy.debug", false);
      if (debugMode) {
        Logger.enableLogging();
      } else {
        Logger.disableLogging();
      }
    }

    try {
      MMKV.initialize(activity);
      gameDataMMKV = MMKV.mmkvWithID(ID_MMKV_GAMEDATA);
      mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    } catch (Throwable t) {
      Logger.error(TAG, "initialize MMKV exception", t);
    }

    eventTracker = new DefaultEventLogger(activity);

    gridManager = new GridManager(activity, eventTracker, startupTime, false);

    // read store data
    JSONObject gridData = GridManager.getGridData();

    eventTracker.setGridData(gridData);
    if (gridData.has("gen_events")) {
      eventTracker.setEventHooks(gridData.optJSONObject("gen_events"));
    }

    if (gridData.has("event_targets")) {
      eventTracker.setEventTargets(gridData.optJSONObject("event_targets"));
    }

    if (gridData.has("parfkaUrl")) {
      String newParfkaUrl = gridData.optString("parfkaUrl");
      if (!"".equals(newParfkaUrl)) {
        ParfkaFactory.setBaseUrl(newParfkaUrl);
      }
    }

    providerFacade.onInitialize(activity, gridData);

    // 内购
    try {
      PurchaseManager purchaseManager = providerFacade.getPurchaseManager();
      purchaseManager.init(activity, EventBus.getInstance(), eventTracker);
      purchaseManagerWrapper = new PurchaseManagerWrapper(purchaseManager);
    } catch (Throwable t) {
      showToast("Purchase System Initialized Failed!");
      Logger.error(TAG, "PurchaseManager created exception", t);
    }

    storeItems = new HashMap<>();

    List<String> iapIds = new ArrayList<>();
    if (gridData.has("payment")) {
      try {
        JSONObject checkout = gridData.optJSONObject("payment").optJSONObject("checkout");
        Iterator<String> iapItems = checkout.keys();
        while (iapItems.hasNext()) {
          String billId = iapItems.next();
          JSONObject iapItem = checkout.optJSONObject(billId);
          if (iapItem != null) {
            String feeName = iapItem.optString("feename");
            if (!"".equals(feeName)) {
              iapIds.add(feeName);
              iapItem.put("billId", billId);
              iapItem.put("usd", iapItem.optDouble("usd", 0));
              if (!iapItem.has("autoload")) {
                iapItem.put("autoload", 1);
              }
              storeItems.put(iapItem.optString("feename"), iapItem);
            }
          }
        }
      } catch (Exception ex) {
        Logger.error(TAG, "config payment exception", ex);
      }
    }

    if (storeItems != null && storeItems.size() > 0) {
      try {
        purchaseManagerWrapper.startLoadingStoreData(iapIds, storeItems);
      } catch (Throwable t) {
        Logger.error(TAG, "startLoadingStoreData exception", t);
      }
    }

    // read google achievements data in grid data
    JSONObject googleEntry = gridData.optJSONObject("google");
    if (googleEntry != null && googleEntry.has("achievement")) {
      googleAchievements = googleEntry.optJSONObject("achievement");
    }

    if (googleEntry != null && googleEntry.has("leaderboard")) {
      googleLeaderBoards = googleEntry.optJSONObject("leaderboard");
      if (googleLeaderBoards != null) {
        Logger.debug(TAG, "enable google leaderboards: " + googleLeaderBoards);
      }
    }

    adManager = new IvyAdsManager();
    adManager.onCreate(activity, eventTracker, gridManager);

    if (debugMode) {
      Toast.makeText(activity, "DO NOT PUBLISH THIS!", Toast.LENGTH_LONG).show();
    }

    setDebug(debugMode);

    try {
      mFirebaseRemoteConfig.fetch().addOnCompleteListener(new OnCompleteListener<Void>() {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
          if (task.isSuccessful()) {
            mFirebaseRemoteConfig.activate();
            Logger.debug(TAG, "Remote config fetched");
            if (eventTracker != null) {
              eventTracker.overrideConfigByRemoteConfig(mFirebaseRemoteConfig);
              eventTracker.checkRemoteConfigEvents(mFirebaseRemoteConfig);
            }

            String autoInAppMessageEvent = mFirebaseRemoteConfig.getString(KEY_AUTO_INAPP_MESSAGE_EVENT);
            if (!"".equals(autoInAppMessageEvent)) {
              triggerInAppMessage(autoInAppMessageEvent);
            }

            callback.onRemoteConfigUpdated();

          } else {
            Exception ex = task.getException();
            if (ex != null) {
              Logger.error(TAG, "fetch remote config failed: ", ex);
            }
          }
        }
      });

      configUpdateListenerRegistration = mFirebaseRemoteConfig.addOnConfigUpdateListener(new ConfigUpdateListener() {
        @Override
        public void onUpdate(@NonNull ConfigUpdate configUpdate) {
          Set<String> keys = configUpdate.getUpdatedKeys();
          if (keys.size() > 0) {
            Logger.debug(TAG, "Updated keys: " + keys);
            mFirebaseRemoteConfig.activate().addOnCompleteListener((OnCompleteListener<Boolean>) task -> {
              Logger.debug(TAG, "RemoteConfig Updated");
              callback.onRemoteConfigUpdated();
            });
          }
        }

        @Override
        public void onError(@NonNull FirebaseRemoteConfigException error) {
          Logger.error(TAG, "FirebaseRemoteConfigException ", error);
        }
      });
    } catch (Throwable ex) {
      Logger.error(TAG, "Remote Config failed", ex);
    }

    try {
      String oldFirebaseToken = sp.getString("firebase_token", null);

      JSONObject pushEntity = getPushEntity();
      if (pushEntity != null) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
          if (!task.isSuccessful()) {
            Logger.warning(TAG, "Fetching FCM registration token failed", task.getException());
            useWorkerManagerPush = true;
            return;
          }

          // Get new FCM registration token
          String token = task.getResult();
          if (token == null) {
            return;
          }
          Logger.debug(TAG, "token>>> " + token);

          if (oldFirebaseToken != null && oldFirebaseToken.equals(token)) {
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            initFirebaseToken(token, options.getProjectId());
            return;
          }

          sp.edit().putString("firebase_token", token).apply();

          Logger.debug(TAG, "receive token: " + token);
          FirebaseOptions options = FirebaseApp.getInstance().getOptions();
          initFirebaseToken(token, options.getProjectId());
          Logger.debug(TAG, "mf is created " + token + "\n projectId " + options.getProjectId());
          initLocalPush();
        });
      }
    } catch (Throwable t) {
      Logger.error(TAG, "Push Settings failed", t);
    }


    // load custom remote config object
    customRemoteConfig = mmGetJsonValue(KEY_CUSTOM_REMOTE_CONFIG);

    if (gridData.has("appflyers.devkey")) {
      String appflyersDevKey = gridData.optString("appflyers.devkey");
      if (!"".equals(appflyersDevKey)) {
        AppsFlyerConversionListener conversionListener = new AppsFlyerConversionListener() {
          @Override
          public void onConversionDataSuccess(Map<String, Object> conversionData) {
            Logger.debug(TAG, "onConversionDataSuccess");
            if (afConversionTracked) {
              return;
            }
            if (eventTracker != null && conversionData != null) {
              Bundle bundle = new Bundle();
              for (String attrName : conversionData.keySet()) {
                bundle.putString(attrName, String.valueOf(conversionData.get(attrName)));
              }
              eventTracker.trackConversion("af_conversion", bundle);
              eventTracker.parfkaLog("af_conversion", bundle);
              Object status = conversionData.get("af_status");
              if (status != null && !status.equals("Organic")) {
                if (conversionData.containsKey("media_source")) {
                  String mediaSource = String.valueOf(conversionData.get("media_source"));
                  eventTracker.setUserProperty("af_media_source", mediaSource);
                }
                if (conversionData.containsKey("campaign")) {
                  String campaign = String.valueOf(conversionData.get("campaign"));
                  eventTracker.setUserProperty("af_campaign", campaign);
                }
                if (conversionData.containsKey("af_adset")) {
                  String adset = String.valueOf(conversionData.get("af_adset"));
                  eventTracker.setUserProperty("af_adset", adset);
                }
              } else {
                eventTracker.setUserProperty("af_campaign", "Organic");
              }
              afConversionTracked = true;
            }
          }

          @Override
          public void onConversionDataFail(String s) {
          }

          @Override
          public void onAppOpenAttribution(Map<String, String> conversionData) {
            if (eventTracker != null && conversionData != null) {
              Bundle bundle = new Bundle();
              for (String attrName : conversionData.keySet()) {
                bundle.putString(attrName, String.valueOf(conversionData.get(attrName)));
              }
              eventTracker.parfkaLog("af_app_open_attribution", bundle);
            }
          }

          @Override
          public void onAttributionFailure(String s) {
          }
        };
        AppsFlyerLib.getInstance().init(appflyersDevKey, conversionListener, activity.getApplicationContext());
        AppsFlyerLib.getInstance().start(activity);
      }
    }

    trackAppOpen(activity);

    userAttributeUrl = gridData.optString("user.attribute.url", "");

    try {
      String mediationProvider = gridManager != null ? gridManager.getMediationProvider() : "admob";
      if ("max".equals(mediationProvider)) {
        ApplovinManager.getInstance(activity, appLovinSdkConfiguration -> {
          try {
            Logger.debug(TAG, "Applovin Max Initialized");
            adManager.preload(gridData);
          } catch (Throwable t) {
            Logger.error(TAG, "adManager preload exception", t);
          }
        });
      } else if ("ironsource".equals(mediationProvider)) {
        String appkey = gridData.optString("ironsource_appkey");
        if (!"".equals(appkey)) {
          IronsourceManager.init(activity, appkey, () -> {
            try {
              Logger.debug(TAG, "Ironsource initialized");
              adManager.preload(gridData);
            } catch (Throwable t) {
              Logger.error(TAG, "Ironsource exception", t);
            }
          });
        }
      } else {
        AdmobManager.getInstance().initialize(activity, initializationStatus -> {
          try {
            Logger.debug(TAG, "Admob initialized");
            Map<String, AdapterStatus> adapterStatusMap = initializationStatus.getAdapterStatusMap();
            for (Map.Entry<String, AdapterStatus> entry : adapterStatusMap.entrySet()) {
              Logger.debug(TAG, "adapter status: " + entry.getKey() + " >>> " + entry.getValue().getInitializationState());
            }
            adManager.preload(gridData);
          } catch (Throwable t) {
            Logger.error(TAG, "adManager preload exception", t);
          }
        });

      }
    } catch (Throwable ex) {
      Logger.error(TAG, "Admob initialize exception", ex);
    }

    isGooglePlayServiceAvailable = checkGooglePlayService();
    try {
      if (isGooglePlayServiceAvailable) {
        PlayGamesSdk.initialize(activity);
      }
    } catch (Throwable t) {
      Logger.error(TAG, "PlayGamesSdk initialize exception", t);
    }

    String appopenadId = gridData.optString("admob-appopenad");
    if (!"".equals(appopenadId)) {
      boolean apaEnable = getRemoteConfigAsInt("apa_enable") == 1;
      if (apaEnable) {
        boolean noAdFlag = IvySdk.mmGetBoolValue(KEY_NO_AD_FLAG, false);
        if (!noAdFlag) {
          appOpenAdManager = new AppOpenAdManager(activity, eventTracker, appopenadId);
        }
      }
    }

    LocalNotificationManager.clearLocalNotification(activity);

    if (providerFacade != null) {
      providerFacade.initPushSystem(activity);
    }
  }

  private static JSONObject customRemoteConfig = null;

  public static String firebaseToken = null;
  public static String firebaseProjectId = null;

  private static void initFirebaseToken(String token, String fcmProjectId) {
    firebaseToken = token;
    firebaseProjectId = fcmProjectId;
  }

  private static OkHttpClient okHttpClient = null;

  public static OkHttpClient getOkHttpClient() {
    if (okHttpClient == null) {
      okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build();
    }
    return okHttpClient;
  }

  public static void cancelPush(String key) {
    try {
      // cancel lcoal push worker
      LocalNotificationManager.cancelPush(IvySdk.getActivity(), key);

      JSONObject pushEntity = getPushEntity();

      if (pushEntity != null) {
        String url = pushEntity.optString("push-server-url");
        if ("".equals(url)) {
          Logger.error(TAG, "push server url not config");
          return;
        }
        Request.Builder requestBuilder = new Request.Builder();
        HttpUrl.Builder httpBuilder = HttpUrl.parse(url).newBuilder();
        httpBuilder.addQueryParameter("data", getCancelPushParams(key));
        Request request = requestBuilder.url(httpBuilder.build()).build();
        getOkHttpClient().newCall(request).enqueue(new Callback() {
          @Override
          public void onFailure(@NonNull Call call, @NonNull IOException e) {
          }

          @Override
          public void onResponse(@NonNull Call call, @NonNull Response response) {
            try {
              response.close();
            } catch (Throwable t) {
              Logger.error(TAG, "cancelPush exception", t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Logger.error(TAG, "Cancel push failed", t);
    }
  }

  private static void pushByWorkerMananger(String key, String title, String content, long pushTime) {
    Activity activity = getActivity();
    if (activity != null) {
      LocalNotificationManager.schedulePush(activity, key, title, content, pushTime, true);
    }
  }

  public static void push(String key, String title, String content, long pushTime, boolean localTimeZone, String fbIds, String uuids, String topics, int iosBadge, boolean useSound, String soundName, String extraData) {
    try {
      if (firebaseProjectId == null || firebaseToken == null || useWorkerManagerPush) {
        Logger.debug(TAG, "use local worker push");
        pushByWorkerMananger(key, title, content, (pushTime - System.currentTimeMillis()) / 1000);
        return;
      }

      if (pushTime - System.currentTimeMillis() <= 30 * 60 * 1000) {
        pushByWorkerMananger(key, title, content, (pushTime - System.currentTimeMillis()) / 1000);
        return;
      }

      JSONObject gridData = GridManager.getGridData();
      Logger.debug(TAG, "push start called");
      JSONObject pushEntity = getPushEntity();
      if (pushEntity != null) {
        String url = pushEntity.optString("push-server-url");
        if ("".equals(url)) {
          Logger.error(TAG, "push server url not config");
          return;
        }

        JSONObject map = new JSONObject();
        try {
          JSONObject message = new JSONObject();

          String appId = gridData.optString("appid");
          String versionName = "unknown";
          String packageName = "";
          try {
            Context context = IvySdk.CONTEXT;
            PackageManager packageManager = context.getPackageManager();
            packageName = context.getPackageName();
            PackageInfo info = packageManager.getPackageInfo(packageName, 0);
            versionName = info.versionName;
          } catch (Exception ex) {
            ex.printStackTrace();
          }

          if (key != null) {
            message.put("key", IvySdk.getUUID() + "_" + key);
          }
          message.put("uuid", IvySdk.getUUID());
          message.put("facebookIds", fbIds == null ? "" : fbIds);
          message.put("uuids", uuids == null ? "" : uuids);
          message.put("receive_appid", appId);
          message.put("receive_pkg", packageName);
          message.put("receive_topic", topics == null ? "" : topics);
          message.put("data", extraData == null ? "" : extraData);
          message.put("iosBadge", iosBadge);
          message.put("sound", useSound);
          message.put("soundName", soundName);
          message.put("project", IvySdk.firebaseProjectId);
          message.put("title", title);
          message.put("content", content);
          message.put("pushTime", pushTime);
          message.put("delayTime", pushTime - System.currentTimeMillis());
          message.put("useLocalTimeZone", localTimeZone);
          int timeZone = TimeZone.getDefault().getRawOffset() / 3600000;
          if (timeZone > 0) {
            message.put("sendTimeZone", "+" + timeZone);
          } else {
            message.put("sendTimeZone", String.valueOf(timeZone));
          }

          message.put("languageCode", Locale.getDefault().getLanguage());
          message.put("countryCode", Locale.getDefault().getCountry());
          message.put("versionName", versionName);

          map.put("method", "push");
          map.put("message", message);

          Request.Builder requestBuilder = new Request.Builder();

          RequestBody requestBody = new FormBody.Builder().add("data", encryptPushParams(map.toString())).build();
          Request request = requestBuilder.url(url).post(requestBody).build();
          getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
              Logger.debug(TAG, "Register Push Failed");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
              Logger.debug(TAG, "Register Push success");
              try {
                response.close();
              } catch (Throwable ignored) {

              }
            }
          });
        } catch (JSONException e) {
          Logger.error(TAG, "push exception", e);
        }
      }
    } catch (Throwable t) {
      Logger.error(TAG, "register push failed", t);
    }
  }

  private static String encryptPushParams(String s) {
    if (s == null) {
      return "";
    }
    JSONObject gridData = GridManager.getGridData();
    boolean encodePushParams = gridData.optBoolean("encodePushParams", true);
    if (encodePushParams) {
      byte[] bytes = s.getBytes();
      int len = bytes.length;
      byte[] buffer = new byte[len + 5];
      byte seed = (byte) ((Math.random() * 63) + 1);
      buffer[0] = seed;
      buffer[1] = (byte) ((len >> 24) & 0xFF);
      buffer[2] = (byte) ((len >> 16) & 0xFF);
      buffer[3] = (byte) ((len >> 8) & 0xFF);
      buffer[4] = (byte) (len & 0xFF);

      for (int i = 0; i < len; i++) {
        int tmp = bytes[i];
        tmp = (tmp + seed) < 256 ? (tmp + seed) : tmp;
        buffer[i + 5] = (byte) tmp;
      }

      return Base64.encodeToString(buffer, Base64.NO_WRAP).trim();
    } else {
      try {
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
      } catch (Throwable t) {
        return "";
      }
    }
  }

  private static String getCancelPushParams(String key) {
    JSONObject map = new JSONObject();
    try {
      map.put("method", "cancel");
      if (key != null) {
        map.put("key", getUUID() + "_" + key);
      }
      map.put("uuid", getUUID());
      return encryptPushParams(map.toString());
    } catch (JSONException e) {
      e.printStackTrace();
      return "";
    }
  }

  private static String getPushParams() {
    JSONObject map = new JSONObject();
    try {
      JSONObject gridData = GridManager.getGridData();

      String appId = gridData.optString("appid");
      String versionName = "unknown";
      String packageName = "";
      try {
        Context context = IvySdk.CONTEXT;
        PackageManager packageManager = context.getPackageManager();
        packageName = context.getPackageName();
        PackageInfo info = packageManager.getPackageInfo(packageName, 0);
        versionName = info.versionName;
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      map.put("method", "saveToken");
      map.put("uuid", getUUID());
      map.put("appid", appId);
      map.put("pkg", packageName);
      map.put("token", firebaseToken);
      map.put("facebookId", "");
      map.put("fcmProjectId", firebaseProjectId);
      map.put("languageCode", Locale.getDefault().getLanguage());
      map.put("countryCode", Locale.getDefault().getCountry());
      map.put("versionName", versionName);
      return encryptPushParams(map.toString());
    } catch (JSONException e) {
      return "";
    }
  }

  private static JSONObject getPushEntity() {
    JSONObject gridData = GridManager.getGridData();
    if (!gridData.has("data")) {
      return null;
    }
    JSONObject dataObject = gridData.optJSONObject("data");
    if (dataObject == null || !dataObject.has("push")) {
      return null;
    }
    JSONArray pushArray = dataObject.optJSONArray("push");
    if (pushArray != null && pushArray.length() > 0) {
      return pushArray.optJSONObject(0);
    }
    return null;
  }

  private static void initLocalPush() {
    Logger.debug(TAG, "initLocalPush called ");
    try {
      JSONObject pushEntity = getPushEntity();
      if (pushEntity != null) {
        String url = pushEntity.optString("push-server-url");

        Request.Builder requestBuilder = new Request.Builder();
        HttpUrl.Builder httpBuilder = HttpUrl.parse(url).newBuilder();
        httpBuilder.addQueryParameter("data", getPushParams());
        Request request = requestBuilder.url(httpBuilder.build()).build();
        getOkHttpClient().newCall(request).enqueue(new Callback() {
          @Override
          public void onFailure(@NonNull Call call, @NonNull IOException e) {
            Logger.error(TAG, "Register Push Failed", e);
          }

          @Override
          public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            try (ResponseBody responseBody = response.body()) {
              if (responseBody != null) {
                Logger.debug(TAG, "Register Push success " + responseBody.string());
              }
            }
          }
        });
      }
    } catch (Throwable t) {
      Logger.error(TAG, "initLocalPush exception", t);
    }
  }

  public static JSONObject getStoreItem(String productId) {
    if (storeItems != null && storeItems.containsKey(productId)) {
      return storeItems.get(productId);
    }
    return null;
  }

  public static void querySKUDetail(List<String> iapIds, @NonNull OnSkuDetailsListener skuDetailsListener) {
    if (purchaseManagerWrapper != null) {
      purchaseManagerWrapper.querySKUDetails(iapIds, skuDetailsListener);
    }
  }

  public static SKUDetail getSKUDetail(String productId) {
    if (purchaseManagerWrapper != null) {
      return purchaseManagerWrapper.getSKUDetail(productId);
    }

    return null;
  }

  public static void setAdCallback(IvyAdType adType, IvyAdCallbacks callback) {
    if (adManager != null) {
      adManager.setAdCallback(adType, callback);
    } else {
      Logger.error(TAG, "Ad not configured? ");
    }
  }

  // 应用启动计数
  public static int appStartTimes = 1;
  // 应用的首次启动时间戳
  private static long firstAppStartTime = 0L;
  // 应用最后启动时间
  private static long lastAppStartTime = 0L;
  private static int totalOrders = 0;
  private static float totalRevenues = 0.0f;

  /**
   * 应用启动逻辑
   * <p>
   * appOpen: 用户打开时,触发此事件,
   * 1. 触发启动次数summary统计逻辑
   * 2. 触发留存summary统计逻辑
   * 3. 触发连续活跃统计逻辑
   */
  private static void trackAppOpen(@NonNull Activity activity) {
    try {
      SharedPreferences sharedPreferences = activity.getSharedPreferences("prefs", Context.MODE_PRIVATE);
      int appStartTimes = sharedPreferences.getInt("_app_start_times", 0) + 1;
      sharedPreferences.edit().putInt("_app_start_times", appStartTimes).apply();

      IvySdk.appStartTimes = appStartTimes;
      IvySdk.lastAppStartTime = System.currentTimeMillis();

      long currentTimeStamp = System.currentTimeMillis();
      // 第一次启动日期
      long firstStartTimestamp = sharedPreferences.getLong("_first_start_timestamp", 0);
      if (firstStartTimestamp == 0) {
        firstStartTimestamp = currentTimeStamp;
        sharedPreferences.edit().putLong("_first_start_timestamp", firstStartTimestamp).apply();
      }
      IvySdk.firstAppStartTime = firstStartTimestamp;

      SharedPreferences paySp = activity.getSharedPreferences("pays", Context.MODE_PRIVATE);
      totalOrders = 0;
      totalRevenues = 0.0f;
      if (paySp != null) {
        totalOrders = paySp.getInt("total_orders", 0);
        totalRevenues = paySp.getFloat("total_revenue", 0.0f);
      }

      Bundle apbundle = new Bundle();
      apbundle.putInt("times", appStartTimes);
      if (totalOrders > 0 && totalRevenues > 0) {
        apbundle.putInt("value", totalOrders);
        apbundle.putFloat("revenue", totalRevenues);
      }
      eventTracker.logEvent("app_open", apbundle);

      if (appStartTimes == 1) {
        eventTracker.parfkaLog("first_open", null);
      }

      // check summary events settings
      JSONObject gridData = GridManager.getGridData();
      if (!gridData.has("summary_events")) {
        return;
      }

      JSONObject eventSettings = gridData.optJSONObject("summary_events");
      if (eventSettings == null) {
        return;
      }

      eventTracker.setSummaryEventSettings(eventSettings);
    } catch (Throwable ex) {
      Logger.error(TAG, "trackApp Open failed", ex);
    }
  }


  public static void pause() {
    if (skipPauseOnce) {
      skipPauseOnce = false;
      return;
    }
    if (!alreadyPaused) {
      alreadyPaused = true;
      alreadyResumed = false;
      Activity activity = getActivity();
      if (adManager != null && activity != null) {
        adManager.onPause(activity);
      }
    }
  }

  public static void onPause() {
    try {
      pause();
      if (eventTracker != null) {
        eventTracker.onPause();
      }

    } catch (Throwable t) {
      Logger.error(TAG, "onPause exception", t);
    }
  }

  public static void queryPurchase() {
    if (purchaseManagerWrapper != null) {
      purchaseManagerWrapper.queryPurchase();
    }
  }

  public static void onResume() {
    try {
      resume();
      if (eventTracker != null) {
        eventTracker.onResume();
      }
    } catch (Throwable t) {
      Logger.error(TAG, "onResume exception", t);
    }
  }

  public static void runOnUiThreadCustom(final Runnable r) {
    Activity activity = getActivity();
    if (activity != null) {
      activity.runOnUiThread(r);
    } else {
      Logger.error(TAG, "Activity is null, thread not able to start");
    }
  }


  private static void resume() {
    if (skipResumeOnce) {
      skipResumeOnce = false;
      return;
    }
    if (!alreadyResumed) {
      alreadyResumed = true;
      alreadyPaused = false;
      Activity activity = getActivity();
      if (adManager != null && activity != null) {
        adManager.onResume(activity);
      }
    }

    Activity activity = getActivity();
    if (activity != null && purchaseManagerWrapper != null) {
      purchaseManagerWrapper.onResume(getActivity());
    }
  }


  public static void onDestroy() {
    Logger.debug(TAG, "OnDestroy called");
    try {
      Activity activity = getActivity();
      if (activity == null) {
        return;
      }
      if (appOpenAdManager != null) {
        appOpenAdManager.destroy();
      }

      if (configUpdateListenerRegistration != null) {
        configUpdateListenerRegistration.remove();
        configUpdateListenerRegistration = null;
      }

      adManager.onDestroy(activity);
      EventBus.getInstance().removeAllListeners();

      if (eventTracker != null) {
        eventTracker.destroy();
      }

      purchaseManagerWrapper.onDestroy();
      updateCurrentActivity(null);
    } catch (Throwable ex) {
      Logger.error(TAG, "onDestroy exception", ex);
    }
  }

  /**
   * @param requestCode
   * @param resultCode
   * @param data
   */
  public static void onActivityResult(int requestCode, int resultCode, Intent data) {
    final Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    if (requestCode == RC_WEBVIEW) {
      return;
    }

    if (requestCode == RC_REQUEST_UPDATE) {
      if (resultCode == Activity.RESULT_CANCELED) {
        // user cancelled the app update
        SharedPreferences sp = activity.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        sp.edit().putLong("last_check_update_time", System.currentTimeMillis()).apply();
      }
    }
  }


  public static void showToast(final String message) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(() -> {
      if (!activity.isFinishing()) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private static final String TAG_BANNER_VIEW = "banner_view";

  private static long lastBannerShowCalledTimes = 0;

  //## 广告相关接口
  // if banner already present, and displayed less than 10s, just ignore the new call
  public static void showBannerAd(final int adPos) {
    final Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    if (adManager == null) {
      return;
    }
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        FrameLayout rootView = (FrameLayout) activity.getWindow().getDecorView().getRootView();

        View bannerView = rootView.findViewWithTag(TAG_BANNER_VIEW);
        if (bannerView != null) {
          if (System.currentTimeMillis() - lastBannerShowCalledTimes < 10000) {
            return;
          }
          rootView.removeView(bannerView);
        }

        FrameLayout rl = new FrameLayout(activity);
        int gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        int internalBannerPosition = -1;
        switch (adPos) {
          case POS_CENTER:
            gravity = Gravity.CENTER;
            break;

          case POS_CENTER_BOTTOM:
            gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            internalBannerPosition = BannerAdManager.BANNER_POSITION_BOTTOM;
            break;

          case POS_CENTER_TOP:
            gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            internalBannerPosition = BannerAdManager.BANNER_POSITION_TOP;
            break;

          case POS_LEFT_BOTTOM:
            gravity = Gravity.START | Gravity.BOTTOM;
            internalBannerPosition = BannerAdManager.BANNER_POSITION_BOTTOM;
            break;

          case POS_LEFT_TOP:
            gravity = Gravity.START | Gravity.TOP;
            internalBannerPosition = BannerAdManager.BANNER_POSITION_TOP;
            break;

          case POS_RIGHT_BOTTOM:
            gravity = Gravity.END | Gravity.BOTTOM;
            internalBannerPosition = BannerAdManager.BANNER_POSITION_BOTTOM;
            break;

          case POS_RIGHT_TOP:
            gravity = Gravity.END | Gravity.TOP;
            internalBannerPosition = BannerAdManager.BANNER_POSITION_TOP;
            break;
        }

        try {
          FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.dpToPx(main.get(), 60), gravity);
          rl.setTag(TAG_BANNER_VIEW);
          rootView.addView(rl, lp);

          adManager.showBanners(activity, rl);
          adManager.setBannerPosition(internalBannerPosition, activity);

          lastBannerShowCalledTimes = System.currentTimeMillis();
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
    });
  }

  public static void closeBanners() {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        lastBannerShowCalledTimes = 0;
        Logger.debug(TAG, "Close Banner");
        if (adManager != null) {
          try {
            adManager.hideBanners();
          } catch (Throwable t) {
            // ignore
          }
        }
      }
    });
  }

  public static void fetchInterstitial() {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(() -> {
      if (adManager != null) {
        adManager.fetchInterstitial(activity);
      }
    });
  }


  public static void showInterstitialAd() {
    showInterstitialAd("default");
  }

  public static void showInterstitialAd(final String tag) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    boolean interstitial_disabled = getRemoteConfigAsBoolean("interstitial_disabled");
    if (interstitial_disabled) {
      Logger.warning(TAG, "Interstitial disabled by config. ignore this display");
      return;
    }
    activity.runOnUiThread(() -> {
      if (adManager != null) {
        adManager.showInterstitial(activity, tag);
      }
    });
  }


  public static void fetchRewardVideoIfNotLoaded() {
    if (haveRewardAd()) {
      return;
    }
    fetchRewardVideo();
  }

  public static void fetchRewardVideo() {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(() -> {
      if (adManager != null) {
        adManager.fetchRewarded(activity);
      }
    });
  }

  public static void showRewardAd(String tag) {
    Activity activity = getActivity();
    if (activity == null) {
      Logger.error(TAG, "Activity is null, showRewardAd not possible");
      return;
    }
    activity.runOnUiThread(() -> {
      if (adManager != null) {
        adManager.showRewarded(activity, tag);
      }
    });
  }

  public static String loadGridData() {
    Activity activity = getActivity();
    if (activity == null) {
      return "{}";
    }
    return Util.retrieveData(activity, GridManager.FILE_JSON_RESPONSE);
  }

  /**
   * 检查是否有退出广告位，如果有，退出广告位完毕,退出游戏
   */
  public static void onQuit() {
    if (adManager != null) {
      adManager.onQuit();
    }
  }


  /**
   * IVY内部交叉推广的形式。 交叉推广内容从grid数据的promote中读取
   *
   * 1. 全屏广告，可以滑动的从promote中提取的左右滑动的全屏广告  showPromoteAd
   * 2. GIF ICON, 客户设定大小，及屏幕绝对位置的GIF动画，点击GIF动画进入一个新的Activity,播放视频广告动画 showDeliciousIcon
   * 3. GIF BANNER 显示所有BANNER动画推广 showDeliciousBanner
   * 4. 全屏包含GIF BANNER的视频广告 showDeliciousAd
   * 5. 游戏退出广告位, 显示自定义的退出插屏及更多游戏链接 onQuit
   * 6. 游戏墙 moreGame
   */
  /**
   * Callback passed to the sdkInitialize function.
   */
  public interface InitializeCallback {
    /**
     * Called when the sdk has been initialized.
     */
    void onInitialized();

    void onRemoteConfigUpdated();
  }

  private static boolean isDebugMode = false;

  public static void setDebug(final boolean flag) {
    isDebugMode = false;
    IvyAds.setDebugMode(flag);
  }

  public static boolean isDebugMode() {
    return isDebugMode;
  }

  public static void tryStartInAppReview() {
    final Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    String inAppReviewUrl = GridManager.getGridData().optString("inapp.review.url", "");
    if (!"".equals(inAppReviewUrl)) {
      AndroidSdk.showWebView(activity.getString(R.string.user_survey), inAppReviewUrl);
    }
  }

  private static void openPlaystoreInBrowser(Activity activity) {
    activity.runOnUiThread(() -> IvyUtils.openPlayStore(activity, activity.getPackageName(), "rate", null));
  }

  public static void rate(int rateStar) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    openPlaystoreInBrowser(activity);
  }

  public static void pay(String productId, String productName, String payLoad) {
    Logger.debug(TAG, "start buying: " + productId + ", payload: " + payLoad);
    Activity activity = getActivity();
    if (activity == null) {
      Logger.error(TAG, "Activity is null, pay impossible");
      return;
    }
    if (purchaseManagerWrapper != null) {
      purchaseManagerWrapper.setBillItemName(productName);
      purchaseManagerWrapper.startBuying(productId, payLoad);
    }
  }

  public static boolean checkGooglePlayService() {
    Activity activity = getActivity();
    if (activity == null) {
      return false;
    }
    int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);
    return resultCode == ConnectionResult.SUCCESS;
  }


  private static void _assignPlayers(Activity activity, @NonNull final GoogleListener listener) {
    PlayGames.getPlayersClient(activity).getCurrentPlayer().addOnCompleteListener(mTask -> {
        // Get PlayerID with mTask.getResult().getPlayerId()
        if (mTask.isSuccessful()) {
          String playerId = mTask.getResult().getPlayerId();
          listener.onSuccess(playerId, "");
        } else {
          listener.onFails();
        }
      }
    );
  }

  public static void loginPlayGames(@NonNull GoogleListener listener) {
    try {
      Logger.debug(TAG, "loginPlayGames");
      Activity activity = getActivity();
      if (activity == null || !isGooglePlayServiceAvailable) {
        Logger.error(TAG, "Activity not initialized? google Signin is not possible.");
        listener.onFails();
        return;
      }

      final GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(activity);
      gamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask -> {
        boolean isAuthenticated = (isAuthenticatedTask.isSuccessful() && isAuthenticatedTask.getResult().isAuthenticated());
        if (isAuthenticated) {
          _assignPlayers(activity, listener);
        } else {
          listener.onFails();
        }
      }).addOnCanceledListener(listener::onFails);
    } catch (Throwable t) {
      Logger.error(TAG, "loginPlayGames exception", t);
      listener.onFails();
    }
  }

  public static void slientLoginGoogle(@NonNull GoogleListener listener) {
    Logger.debug(TAG, "Configure login google");
    Activity activity = getActivity();
    if (activity == null) {
      Logger.error(TAG, "Activity not initialized? google Signin is not possible.");
      listener.onFails();
      return;
    }
    if (!isGooglePlayServiceAvailable) {
      Logger.error(TAG, "Google Play Service Not available.");
      listener.onFails();
      return;
    }
    try {

      final GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(activity);
      gamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask -> {
        boolean isAuthenticated = (isAuthenticatedTask.isSuccessful() && isAuthenticatedTask.getResult().isAuthenticated());
        if (isAuthenticated) {
          _assignPlayers(activity, listener);
        } else {
          // Disable your integration with Play Games Services or show a
          // login button to ask  players to sign-in. Clicking it should
          // call GamesSignInClient.signIn().
          gamesSignInClient.signIn().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
              boolean isAuthenticated1 = task.getResult().isAuthenticated();
              if (isAuthenticated1) {
                _assignPlayers(activity, listener);
              } else {
                listener.onFails();
              }
            } else {
              listener.onFails();
            }
          }).addOnCanceledListener(listener::onFails);
        }
      }).addOnCanceledListener(listener::onFails);
    } catch (Throwable t) {
      Logger.error(TAG, "slientLoginGoogle exception", t);
      listener.onFails();
    }
  }

  public static Activity getActivity() {
    if (main == null) {
      return null;
    }
    return main.get();
  }

  public static void showGoogleAchievement() {
    if (!isGooglePlayServiceAvailable) {
      return;
    }
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    AchievementsClient achievementsClient = PlayGames.getAchievementsClient(activity);
    achievementsClient
      .getAchievementsIntent()
      .addOnSuccessListener(intent -> {
        try {
          activity.startActivityForResult(intent, RC_ACHIEVEMENT_UI);
        } catch (Throwable t) {
          t.printStackTrace();
        }
      });
  }

  public static void updateGoogleAchievement(final String id, int step) {
    if (!isGooglePlayServiceAvailable) {
      return;
    }

    if (googleAchievements == null || !googleAchievements.has(id)) {
      Logger.debug(TAG, "No google achievement, id: " + id);
      return;
    }

    Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    JSONObject achievement = googleAchievements.optJSONObject(id);
    if (achievement != null) {
      int lastUpdateStep = mmGetIntValue("gc_a_" + id, 0);
      if (lastUpdateStep == step) {
        return;
      }
      AchievementsClient achievementsClient = PlayGames.getAchievementsClient(activity);
      boolean incremental = achievement.optBoolean("incremental", false);
      String achievementId = achievement.optString("id");
      if (incremental) {
        achievementsClient.increment(achievementId, step);
      } else {
        if (step == 100) {
          achievementsClient.unlock(achievementId);
        }
      }
      mmSetIntValue("gc_a_" + id, step);
    }
  }

  public static void updateGoogleLeaderBoard(String id, final long value) {
    if (!isGooglePlayServiceAvailable) {
      Logger.debug(TAG, "Google Play service not available");
      return;
    }
    if (googleLeaderBoards == null || !googleLeaderBoards.has(id)) {
      Logger.debug(TAG, "no leaderboard found for: " + id);
      return;
    }
    String googleLeaderboardId = googleLeaderBoards.optJSONObject(id).optString("id");
    if ("".equals(googleLeaderboardId)) {
      return;
    }
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    long lastUpdateScore = mmGetLongValue("gc_l_" + id, 0);
    if (lastUpdateScore == value) {
      return;
    }

    Logger.debug(TAG, "updateGoogleLeaderBoard called, id: " + googleLeaderboardId);
    PlayGames.getLeaderboardsClient(activity).submitScore(googleLeaderboardId, value);

    mmSetLongValue("gc_l_" + id, value);
  }

  private static final int RC_LEADERBOARD_UI = 9004;

  public static void displayGameLeaderboards() {
    final Activity activity = getActivity();
    if (!isGooglePlayServiceAvailable || activity == null) {
      return;
    }

    PlayGames.getLeaderboardsClient(activity).getAllLeaderboardsIntent().addOnSuccessListener(intent -> activity.startActivityForResult(intent, RC_LEADERBOARD_UI));
  }


  public static boolean haveRewardAd() {
    if (adManager != null) {
      return adManager.haveRewardAd();
    }
    return false;
  }

  public static boolean haveInterstitial() {
    if (adManager != null) {
      return adManager.haveInterstitial();
    }
    return false;
  }


  /**
   * SDK自定义事件接口. 根据grid的配置，自定义事件将保存到firebase, facebook 和 ivy.
   *
   * @param eventName
   * @param bundle
   */
  public static void logEvent(String eventName, Bundle bundle) {
    if (eventTracker != null) {
      eventTracker.logEvent(eventName, bundle);
    }
  }

  public static void logEventToConversionPlatforms(String eventName, Bundle bundle) {
    if (eventTracker != null) {
      eventTracker.logEventToConversionPlatforms(eventName, bundle);
    }
  }

  public static void logToFirebase(String eventName, Bundle bundle) {
    if (eventTracker != null) {
      eventTracker.logToFirebase(eventName, bundle);
    }
  }

  public static void logIvyEvent(String eventName, Bundle bundle) {
    if (eventTracker != null) {
      eventTracker.parfkaLog(eventName, bundle);
    }
  }

  public static void trackEngagement(long seconds) {
    if (eventTracker != null) {
      eventTracker.trackEngagement(seconds);
    }
  }

  public static int getRemoteConfigAsInt(String key) {
    if (customRemoteConfig != null && customRemoteConfig.has(key)) {
      return customRemoteConfig.optInt(key);
    }

    if (mFirebaseRemoteConfig != null) {
      return (int) (mFirebaseRemoteConfig.getLong(key));
    }
    return 0;
  }

  public static long getRemoteConfigAsLong(String key) {
    if (customRemoteConfig != null && customRemoteConfig.has(key)) {
      return customRemoteConfig.optLong(key);
    }

    if (mFirebaseRemoteConfig != null) {
      return mFirebaseRemoteConfig.getLong(key);
    }
    return 0L;
  }

  public static String getFirebaseRemoteConfigAsString(String key, String defaultValue) {
    try {
      if (mFirebaseRemoteConfig == null) {
        Logger.error(TAG, "Remote Config not initialized");
        return defaultValue;
      }

      String valueInFirebase = mFirebaseRemoteConfig.getString(key);
      if (!FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING.equals(valueInFirebase)) {
        return valueInFirebase;
      }
      return defaultValue;
    } catch (Throwable t) {
      Logger.error(TAG, "getFirebaseRemoteConfigAsString exception", defaultValue);
      return defaultValue;
    }
  }

  public static double getRemoteConfigAsDouble(String key) {
    if (customRemoteConfig != null && customRemoteConfig.has(key)) {
      return customRemoteConfig.optDouble(key);
    }
    if (mFirebaseRemoteConfig != null) {
      return mFirebaseRemoteConfig.getDouble(key);
    }
    return 0;
  }

  public static boolean getRemoteConfigAsBoolean(String key) {
    if (customRemoteConfig != null && customRemoteConfig.has(key)) {
      return customRemoteConfig.optBoolean(key);
    }
    if (mFirebaseRemoteConfig != null) {
      return mFirebaseRemoteConfig.getBoolean(key);
    }

    return false;
  }

  private static final String EMPTY = "";

  @NonNull
  public static String getRemoteConfigAsString(String key) {
    if (customRemoteConfig != null && customRemoteConfig.has(key)) {
      return customRemoteConfig.optString(key);
    }

    if (mFirebaseRemoteConfig != null) {
      return mFirebaseRemoteConfig.getString(key);
    }

    return EMPTY;
  }

  @NonNull
  public static JSONObject getRemoteConfigAsJSONObject(String key) {
    try {
      if (customRemoteConfig != null && customRemoteConfig.has(key)) {
        return new JSONObject(customRemoteConfig.optString(key));
      }

      if (mFirebaseRemoteConfig != null) {
        String data = mFirebaseRemoteConfig.getString(key);
        if (!"".equals(data)) {
          return new JSONObject(data);
        }
      }
    } catch (Throwable t) {
      Logger.error(TAG, "getRemoteConfigAsJSONObject exception", t);
    }
    return new JSONObject();
  }

  public static boolean hasGridConfig(String key) {
    return GridManager.getGridData().has(key);
  }

  public static JSONObject getGridConfigJson(String key) {
    return GridManager.getGridData().optJSONObject(key);
  }

  public static JSONArray getGridConfigJsonArray(String key) {
    return GridManager.getGridData().optJSONArray(key);
  }

  public static boolean getGridConfigBoolean(String key, boolean defaultValue) {
    return GridManager.getGridData().optBoolean(key, defaultValue);
  }

  public static int getGridConfigInt(String key, int defaultValue) {
    return GridManager.getGridData().optInt(key, defaultValue);
  }

  public static boolean getGridConfigBoolean(String key) {
    return GridManager.getGridData().optBoolean(key);
  }

  @NonNull
  public static String getGridConfigString(String key) {
    return GridManager.getGridData().optString(key);
  }

  @NonNull
  public static String getGridConfigString(String key, @NonNull String defaultValue) {
    return GridManager.getGridData().optString(key, defaultValue);
  }

  public static void setUserProperty(@NonNull String key, @NonNull String value) {
    Logger.debug(TAG, "setUserProperty : " + key + " >>> " + value);
    if (eventTracker != null) {
      eventTracker.setUserProperty(key, value);
    }
  }

  public static void setUserID(@NonNull String userID) {
    if (eventTracker != null) {
      eventTracker.setUserProperty("af_customer_user_id", userID);
    }
  }

  private static String uuid = "";

  public static String getUUID() {
    try {
      if (uuid == null || "".equals(uuid)) {
        String uuidKey = "_ANDROID_*****_UUID_";
        Activity activity = getActivity();
        if (activity == null) {
          return "";
        }
        SharedPreferences sp = activity.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        uuid = sp.getString(uuidKey, null);
        if (uuid == null || uuid.length() == 0) {
          uuid = UUID.randomUUID().toString().toUpperCase();
          sp.edit().putString(uuidKey, uuid).apply();
        }
      }
      return uuid;
    } catch (Throwable t) {
      //
    }
    return "";
  }

  public static JSONObject getInventory() {
    JSONObject inventory = new JSONObject();

    if (storeItems == null || purchaseManagerWrapper == null) {
      return inventory;
    }

    try {
      for (JSONObject iap : storeItems.values()) {
        String billId = iap.optString("billId");
        String iapId = iap.optString("feename");
        SKUDetail skuDetail = purchaseManagerWrapper.getSKUDetail(iapId);
        if (skuDetail != null) {
          inventory.put(billId, skuDetail.toJson());
        }
      }
    } catch (Exception ex) {
      Logger.error(TAG, "getInventory failed", ex);
    }
    return inventory;
  }


  public static String getCountryCode() {
    try {
      String mobileCountryCode = Locale.getDefault().getCountry().toLowerCase(Locale.ENGLISH);
      if (mmContainsKey(KEY_MM_COUNTRY)) {
        return mmGetStringValue(KEY_MM_COUNTRY, mobileCountryCode);
      }
      if (gridManager == null) {
        return mobileCountryCode;
      }
      String clientCountryCode = gridManager.getClientCountryCode();
      if (clientCountryCode == null || "".equals(clientCountryCode)) {
        return mobileCountryCode;
      }
      return clientCountryCode;
    } catch (Throwable t) {
      Logger.error(TAG, "getCountryCode exception", t);
      return Locale.ENGLISH.getCountry();
    }
  }

  public static boolean isNotificationChannelEnabled(Activity activity) {
    try {
      return NotificationManagerCompat.from(activity).areNotificationsEnabled();
    } catch (Throwable t) {
      Logger.error(TAG, "isNotificationChannelEnabled exception", t);
    }
    return true;
  }

  public static void openNotificationSettings(Activity activity) {
    if (activity == null || activity.isFinishing()) {
      Logger.warning(TAG, "activity not valid, openNotificationSettings ignore");
      return;
    }
    try {
      Intent intent = new Intent();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("app_package", activity.getPackageName());
        intent.putExtra("app_uid", activity.getApplicationInfo().uid);
      } else {
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
      }
      activity.startActivity(intent);
    } catch (Throwable t) {
      Logger.error(TAG, "openNotificationSettings exception", t);
    }
  }

  private static Dialog progress = null;

  public static void hideProgressBar(Activity activity) {
    if (activity == null || activity.isFinishing()) {
      return;
    }
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (progress != null) {
          try {
            progress.dismiss();
            progress = null;
          } catch (Exception e) {
            // ignore
          }
        }
      }
    });
  }

  public static void showProgressBar(final Activity activity) {
    if (activity == null || activity.isFinishing()) {
      return;
    }
    activity.runOnUiThread(() -> {
      try {
        if (progress != null) {
          progress.dismiss();
          progress = null;
        }
        progress = new ImmersiveDialog(activity);
        progress.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        ProgressBar progressBar = new ProgressBar(activity, null, 16842874);
        progress.requestWindowFeature(1);
        progress.setContentView(progressBar);
        progress.setOwnerActivity(activity);
        progress.show();
      } catch (Throwable e) {
        // ignore
      }
    });
  }

  public static void executeCloudFunction(@NonNull String funcName, JSONObject parameters, @NonNull OnCloudFunctionResult onCloudFunctionResult) {
    try {
      String functionLocation = IvySdk.getGridConfigString("cf.location", "us-central1");
      FirebaseFunctions mFunctions = FirebaseFunctions.getInstance(functionLocation);
      mFunctions.getHttpsCallable(funcName).call(parameters).addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
          try {
            String resultString = JSON.toJSONString(task.getResult().getData());
            Logger.debug(TAG, " success <<< " + resultString);
            onCloudFunctionResult.onResult(resultString);
          } catch (Throwable t) {
            t.printStackTrace();
            onCloudFunctionResult.onFail("result error");
          }
        } else {
          Logger.debug(TAG, " fail <<< " + task.getException());

          onCloudFunctionResult.onFail("fail");
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "executeCloudFunction exception", t);
      onCloudFunctionResult.onFail(t.getLocalizedMessage());
    }
  }

  public static void trackScreen(@NonNull String screenClass, @NonNull String screenName) {
    if (eventTracker == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
    bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);

    eventTracker.logToFirebase(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
  }

  public static String getPushToken() {
    if (main == null || main.get() == null) {
      return "";
    }
    final SharedPreferences sp = main.get().getSharedPreferences("prefs", Context.MODE_PRIVATE);
    return sp.getString("firebase_token", "");
  }

  public static void supressInAppMessage(boolean suppressMessage) {
    Logger.debug(TAG, "supressInAppMessage >>> " + suppressMessage);

    FirebaseInAppMessaging.getInstance().setMessagesSuppressed(suppressMessage);
  }

  public static void triggerInAppMessage(String eventName) {
    try {
      FirebaseInAppMessaging.getInstance().triggerEvent(eventName);
    } catch (Throwable t) {
      Logger.debug(TAG, "triggerInAppMessage exception", t);
    }
  }

  private static final Map<String, FirebaseInAppMessagingDisplayCallbacks> currentFirebaseInAppMessagingDisplayCallbacksMap = new HashMap<>();
  private static final Map<String, Action> currentFirebaseInAppMessageActionsMap = new HashMap<>();

  public static void inAppMessageClicked(String campaignId) {
    Logger.debug(TAG, "inAppMessageClicked");
    try {
      FirebaseInAppMessagingDisplayCallbacks callbacks = currentFirebaseInAppMessagingDisplayCallbacksMap.get(campaignId);
      if (callbacks == null) {
        return;
      }
      Action action = currentFirebaseInAppMessageActionsMap.get(campaignId);
      if (action == null) {
        return;
      }
      callbacks.messageClicked(action).addOnCompleteListener(task -> Logger.debug(TAG, "inAppMessageClicked reported >>> " + task.isSuccessful()));
    } catch (Throwable ignored) {
    }
  }

  private static String inAppMessageInstallationId = null;

  public static void registerInAppMessageService(final InAppMessageListener listener) {
    try {
      FirebaseInAppMessaging firebaseInAppMessaging = FirebaseInAppMessaging.getInstance();

      FirebaseInstallations.getInstance().getId().addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
          inAppMessageInstallationId = task.getResult();
          Logger.debug(TAG, "installationID >>> " + task.getResult());
        } else {
          Logger.error(TAG, "InAppMessageService exception");
        }
      });

      firebaseInAppMessaging.setMessagesSuppressed(false);
      firebaseInAppMessaging.setMessageDisplayComponent(new FirebaseInAppMessagingDisplay() {
        @Override
        public void displayMessage(@NonNull InAppMessage inAppMessage, @NonNull FirebaseInAppMessagingDisplayCallbacks firebaseInAppMessagingDisplayCallbacks) {
          try {
            Logger.debug(TAG, "display In App Messenger");
            MessageType messageType = inAppMessage.getMessageType();
            if (messageType == MessageType.BANNER && inAppMessage instanceof BannerMessage) {
              return;
            }

            CampaignMetadata campaignMetadata = inAppMessage.getCampaignMetadata();

            String campaignId = campaignMetadata != null ? campaignMetadata.getCampaignName() : "";

            // record this callback
            currentFirebaseInAppMessagingDisplayCallbacksMap.put(campaignId, firebaseInAppMessagingDisplayCallbacks);

            JSONObject inAppMessageJson = new JSONObject();
            inAppMessageJson.put("id", campaignId);
            if (messageType == MessageType.MODAL && inAppMessage instanceof ModalMessage) {
              ModalMessage modalMessage = (ModalMessage) inAppMessage;
              Text titleText = modalMessage.getTitle();
              inAppMessageJson.put("title", titleText.getText());

              Text bodyText = modalMessage.getBody();
              if (bodyText != null) {
                inAppMessageJson.put("body", bodyText.getText());
              }

              ImageData imageData = modalMessage.getImageData();
              if (imageData != null) {
                String imageUrl = imageData.getImageUrl();
                if (!"".equals(imageUrl)) {
                  inAppMessageJson.put("image", imageUrl);
                }
              }

              Action action = modalMessage.getAction();
              if (action != null) {
                String actionUrl = action.getActionUrl();
                if (actionUrl != null && actionUrl.contains("dummy")) {
                  return;
                } else {
                  inAppMessageJson.put("action", actionUrl);
                  JSONObject actionParams = IvyUtils.getUrlParametersWithJson(actionUrl);
                  inAppMessageJson.put("action_params", actionParams);

                  currentFirebaseInAppMessageActionsMap.put(campaignId, action);
                }
              }
            } else if (messageType == MessageType.IMAGE_ONLY && inAppMessage instanceof ImageOnlyMessage) {
              ImageOnlyMessage imageOnlyMessage = (ImageOnlyMessage) inAppMessage;

              ImageData imageData = imageOnlyMessage.getImageData();
              String imageUrl = imageData.getImageUrl();
              if (!"".equals(imageUrl)) {
                inAppMessageJson.put("image", imageUrl);
              }

              Action action = imageOnlyMessage.getAction();
              if (action != null) {
                String actionUrl = action.getActionUrl();
                if (actionUrl == null || actionUrl.contains("dummy")) {
                  return;
                } else {
                  inAppMessageJson.put("action", actionUrl);
                  JSONObject actionParams = IvyUtils.getUrlParametersWithJson(actionUrl);
                  inAppMessageJson.put("action_params", actionParams);
                  currentFirebaseInAppMessageActionsMap.put(campaignId, action);
                }
              }
            } else {
              Logger.error(TAG, "unknown inapp message type");
              return;
            }

            Logger.debug(TAG, "Inappmessage>>> " + inAppMessageJson);
            if (listener != null) {
              listener.messageDisplayed(campaignId, inAppMessageJson.toString());
            }

            // impression
            firebaseInAppMessagingDisplayCallbacks.impressionDetected().addOnCompleteListener(new OnCompleteListener<Void>() {
              @Override
              public void onComplete(@NonNull Task<Void> task) {
              }
            });
          } catch (Throwable t) {
            Logger.error(TAG, "display in app message exception", t);
          }
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "registerInAppMessageService exception", t);
    }
  }

  private static long lastErrorLoggedTime = 0L;

  public static void logError(String message) {
    if (System.currentTimeMillis() - lastErrorLoggedTime < 5000L) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putString("stack", message);
    eventTracker.parfkaLog("unity_error", bundle);

    lastErrorLoggedTime = System.currentTimeMillis();
  }

  public static FirebaseRemoteConfig getFirebaseRemoteConfig() {
    return mFirebaseRemoteConfig;
  }

  public static long mmActualSize() {
    return gameDataMMKV.actualSize();
  }

  public static void mmSetIntValue(String key, int value) {
    if (gameDataMMKV == null) {
      return;
    }
    gameDataMMKV.encode(key, value);
  }

  public static int mmGetIntValue(String key, int defaultValue) {
    if (gameDataMMKV == null) {
      return defaultValue;
    }
    return gameDataMMKV.decodeInt(key, defaultValue);
  }

  public static void mmSetLongValue(String key, long value) {
    if (gameDataMMKV == null) {
      return;
    }
    gameDataMMKV.encode(key, value);
  }

  public static long mmGetLongValue(String key, long defaultValue) {
    if (gameDataMMKV == null) {
      return defaultValue;
    }
    return gameDataMMKV.decodeLong(key, defaultValue);
  }

  public static void mmSetBoolValue(String key, boolean value) {
    if (gameDataMMKV == null) {
      return;
    }
    gameDataMMKV.encode(key, value);
  }

  public static boolean mmGetBoolValue(String key, boolean defaultValue) {
    if (gameDataMMKV == null) {
      return defaultValue;
    }
    return gameDataMMKV.decodeBool(key, defaultValue);
  }

  public static void mmSetFloatValue(String key, float value) {
    if (gameDataMMKV == null) {
      return;
    }
    gameDataMMKV.encode(key, value);
  }

  public static float mmGetFloatValue(String key, float defaultValue) {
    if (gameDataMMKV == null) {
      return defaultValue;
    }
    return gameDataMMKV.decodeFloat(key, defaultValue);
  }

  public static void mmSetStringValue(String key, String value) {
    if (gameDataMMKV == null) {
      return;
    }
    gameDataMMKV.encode(key, value);
  }

  public static void mmSetStringValueWithExpired(String key, String value, int expireDurationInSecond) {
    if (gameDataMMKV != null) {
      gameDataMMKV.encode(key, value, expireDurationInSecond);
    }
  }

  public static void mmSetJsonKeyStringValue(String key, String jsonKey, String value) {
    if (gameDataMMKV == null) {
      return;
    }
    try {
      String savedJsonString = IvySdk.mmGetStringValue(key, "{}");
      JSONObject o = new JSONObject(savedJsonString);
      o.put(jsonKey, value);
      IvySdk.mmSetStringValue(key, o.toString());
    } catch (Throwable t) {
      Logger.error(TAG, "mmSetJsonKeyStringValue exception", t);
    }
  }

  public static JSONObject mmGetJsonValue(String key) {
    if (gameDataMMKV == null) {
      return null;
    }

    try {
      String savedJsonString = IvySdk.mmGetStringValue(key, "");
      if ("".equals(savedJsonString)) {
        return null;
      }
      return new JSONObject(savedJsonString);
    } catch (Throwable t) {
      Logger.error(TAG, "mmGetJsonValue exception", t);
    }
    return null;
  }

  public static String mmGetStringValue(String key, String defaultValue) {
    if (gameDataMMKV == null) {
      Log.e(TAG, "gameDataMMKV invalid");
      return defaultValue;
    }
    return gameDataMMKV.decodeString(key, defaultValue);
  }

  public static boolean mmContainsKey(String key) {
    if (gameDataMMKV == null) {
      Log.e(TAG, "gameDataMMKV invalid");
      return false;
    }

    return gameDataMMKV.containsKey(key);
  }

  public static void mmRemoveKey(String key) {
    if (gameDataMMKV == null) {
      Log.e(TAG, "gameDataMMKV invalid");
      return;
    }
    gameDataMMKV.removeValueForKey(key);
  }

  public static void mmRemoveKeys(String keys) {
    if (gameDataMMKV == null) {
      Log.e(TAG, "gameDataMMKV invalid");
      return;
    }
    if (keys == null) {
      return;
    }
    String[] newKeys = keys.split(",");
    if (newKeys.length > 0) {
      gameDataMMKV.removeValuesForKeys(newKeys);
    }
  }

  public static void mmClearAll() {
    if (gameDataMMKV == null) {
      Log.e(TAG, "gameDataMMKV invalid");
      return;
    }
    gameDataMMKV.clearAll();
  }

  public static void recordEventConversion(@NonNull String conversionUrl, String eventName, Bundle bundle, final @NonNull InAppMessageListener listener) {
    Logger.debug(TAG, "recordEventConversion >>> " + eventName);
    try {
      FormBody.Builder formBuilder = new FormBody.Builder();

      // uid
      FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
      if (firebaseUser != null) {
        formBuilder.add("uid", firebaseUser.getUid());
      }

      formBuilder.add("country", getCountryCode());

      formBuilder.add("event_token", eventName);
      // 基本启动指标
      formBuilder.add("app_first_start", String.valueOf(IvySdk.firstAppStartTime));
      formBuilder.add("app_last_start", String.valueOf(IvySdk.lastAppStartTime));
      formBuilder.add("app_start_times", String.valueOf(IvySdk.appStartTimes));
      formBuilder.add("total_orders", String.valueOf(IvySdk.totalOrders));
      formBuilder.add("total_revenue", String.valueOf(IvySdk.totalRevenues));

      if (inAppMessageInstallationId != null && !"".equals(inAppMessageInstallationId)) {
        formBuilder.add("inapp_installation", inAppMessageInstallationId);
      }


      if (eventTracker != null) {
        long engagement = eventTracker.getEngagementTimestamp();
        formBuilder.add("engagement", String.valueOf(engagement));
      }

      if (bundle != null) {
        for (String key : bundle.keySet()) {
          String value = bundle.getString(key, null);
          if (value != null) {
            formBuilder.add(key, value);
          }
        }
      }

      // collect all pa_ remote configs
      if (mFirebaseRemoteConfig != null) {
        for (String key : mFirebaseRemoteConfig.getKeysByPrefix(KEY_REMOTE_USER_ATTRIBUTE)) {
          String value = mFirebaseRemoteConfig.getString(key);
          if (!"".equals(key) && !"".equals(value)) {
            formBuilder.add(key, value);
          }
        }
      }

      try {
        String savedMainLine = IvySdk.mmGetStringValue(IvySdk.KEY_GAME_MAIN_LINE, "");
        if (!"".equals(savedMainLine)) {
          JSONObject mainLineData = new JSONObject(savedMainLine);
          Iterator<String> keys = mainLineData.keys();
          while (keys.hasNext()) {
            String key = keys.next();
            int value = mainLineData.optInt(key);
            if (value != 0) {
              formBuilder.add(key, String.valueOf(value));
            }
          }
        }

        String remaingCurrency = IvySdk.mmGetStringValue(IvySdk.KEY_VIRUTAL_CURRENCY, "");
        if (!"".equals(remaingCurrency)) {
          JSONObject currencyData = new JSONObject(remaingCurrency);
          Iterator<String> keys = currencyData.keys();
          while (keys.hasNext()) {
            String key = keys.next();
            int value = currencyData.optInt(key);
            if (value != 0) {
              formBuilder.add(key, String.valueOf(value));
            }
          }
        }

        JSONObject localUserProperties = mmGetJsonValue(KEY_LOCAL_USER_PROPERIES);
        if (localUserProperties != null) {
          Iterator<String> keys = localUserProperties.keys();
          while (keys.hasNext()) {
            String key = keys.next();
            String value = localUserProperties.optString(key);
            if (!"".equals(value)) {
              formBuilder.add(key, value);
            }
          }
        }

        formBuilder.add("total_ad_revenue", String.valueOf(eventTracker.getTotalAdRevenue()));
      } catch (Throwable ignored) {

      }

      Request request = new Request.Builder()
        .url(conversionUrl)
        .post(formBuilder.build())
        .build();

      getOkHttpClient().newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
          Logger.warning(TAG, "event conversion report failed");
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) {
          Logger.debug(TAG, "event conversion reported");
          try (ResponseBody body = response.body()) {
            if (body == null) {
              return;
            }
            String data = body.string();
            JSONObject result = new JSONObject(data);
            if (result.has("inapp_event")) {
              String inAppEvent = result.optString("inapp_event");
              if (!"".equals(inAppEvent)) {
                triggerInAppMessage(inAppEvent);
              }
            }

            if (result.has("event")) {
              String event = result.optString("event");
              if (!"".equals(event)) {
                logEventToConversionPlatforms(event, null);
              }
            }

            if (result.has("event_once")) {
              String event = result.optString("event_once");
              if (!"".equals(event)) {
                boolean alreadySent = mmGetBoolValue("sent_" + event, false);
                if (!alreadySent) {
                  logEventToConversionPlatforms(event, null);
                  mmSetBoolValue("sent_" + event, true);
                }
              }
            }

            if (result.has("user_properties")) {
              JSONObject userProperties = result.optJSONObject("user_properties");
              if (userProperties != null && userProperties.length() > 0) {
                Iterator<String> keys = userProperties.keys();
                while (keys.hasNext()) {
                  String key = keys.next();
                  String value = userProperties.optString(key);
                  eventTracker.setUserProperty(key, value);
                }
              }
            }

            if (result.has("remote_config")) {
              checkCustomRemoteConfig(result.optJSONObject("remote_config"));
            }

            if (result.has("inapp_message")) {
              JSONObject inappMessage = result.optJSONObject("inapp_message");
              if (inappMessage != null) {
                listener.messageDisplayed(inappMessage.optString("id"), inappMessage.toString());
              }
            }
          } catch (Throwable t) {
            Logger.error(TAG, "parse response exception " + conversionUrl, t);
          }
        }
      });
    } catch (Throwable t) {
      // crash protected
    }
  }

  private static void checkCustomRemoteConfig(JSONObject data) {
    if (data == null) {
      return;
    }

    try {
      if (customRemoteConfig == null) {
        customRemoteConfig = new JSONObject();
      }

      Iterator<String> keys = data.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        Object value = data.opt(key);
        if (key != null && value != null) {
          if ("_remove".equals(value)) {
            customRemoteConfig.remove(key);
          } else {
            customRemoteConfig.put(key, value);
          }
        }
      }
      // series the custome remote config to local
      mmSetStringValue(KEY_CUSTOM_REMOTE_CONFIG, customRemoteConfig.toString());
    } catch (Throwable t) {
      Logger.error(TAG, "checkCustomRemoteConfig exception", t);
    }
  }


  private static final String KEY_REMOTE_USER_ATTRIBUTE = "pa_";

  private static final String KEY_MM_COUNTRY = "__country";

  /**
   * 如果配置了用户属性地址，调用此接口，同步用户属性数据，
   * 用户属性数据是用户基本数据的快照，运营可以根据此接口的数据根据条件组合进行push或其他有趣的操作。
   */
  public static void saveUserAttribute(@Nullable JSONObject dataMap, final @NonNull InAppMessageListener listener) {
    if (userAttributeUrl == null || "".equals(userAttributeUrl)) {
      return;
    }

    try {
      FormBody.Builder formBuilder = new FormBody.Builder();

      // uid
      FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
      if (firebaseUser != null) {
        formBuilder.add("uid", firebaseUser.getUid());
        String displayName = firebaseUser.getDisplayName();
        if (displayName != null) {
          formBuilder.add("username", displayName);
        }
        String email = firebaseUser.getEmail();
        if (email != null && !"".equals(email)) {
          formBuilder.add("email", email);
        }
      }

      formBuilder.add("country", getCountryCode());
      // 基本启动指标
      formBuilder.add("app_first_start", String.valueOf(IvySdk.firstAppStartTime));
      formBuilder.add("app_last_start", String.valueOf(IvySdk.lastAppStartTime));
      formBuilder.add("app_start_times", String.valueOf(IvySdk.appStartTimes));
      if (IvySdk.totalOrders > 0) {
        formBuilder.add("total_orders", String.valueOf(IvySdk.totalOrders));
      }
      if (IvySdk.totalRevenues > 0) {
        formBuilder.add("total_revenue", String.valueOf(IvySdk.totalRevenues));
      }

      if (inAppMessageInstallationId != null && !"".equals(inAppMessageInstallationId)) {
        formBuilder.add("inapp_installation", inAppMessageInstallationId);
      }

      if (eventTracker != null) {
        long engagement = eventTracker.getEngagementTimestamp();
        formBuilder.add("engagement", String.valueOf(engagement));
      }

      // push_token
      String push_token = getPushToken();
      if (push_token != null && !"".equals(push_token)) {
        formBuilder.add("push_token", push_token);
      }

      // dataMap
      if (dataMap != null) {
        Iterator<String> it = dataMap.keys();
        while (it.hasNext()) {
          String key = it.next();
          String value = dataMap.optString(key, "");
          if (!"".equals(value)) {
            formBuilder.add(key, value);
          }
        }
      }

      // remote config里以pa_开头的所有变量
      if (mFirebaseRemoteConfig != null) {
        for (String key : mFirebaseRemoteConfig.getKeysByPrefix(KEY_REMOTE_USER_ATTRIBUTE)) {
          String value = mFirebaseRemoteConfig.getString(key);
          if (!"".equals(key) && !"".equals(value)) {
            formBuilder.add(key, value);
          }
        }
      }

      try {
        String savedMainLine = IvySdk.mmGetStringValue(IvySdk.KEY_GAME_MAIN_LINE, "");
        if (!"".equals(savedMainLine)) {
          JSONObject mainLineData = new JSONObject(savedMainLine);
          Iterator<String> keys = mainLineData.keys();
          while (keys.hasNext()) {
            String key = keys.next();
            int value = mainLineData.getInt(key);
            formBuilder.add(key, String.valueOf(value));
          }
        }

        String remaingCurrency = IvySdk.mmGetStringValue(IvySdk.KEY_VIRUTAL_CURRENCY, "");
        if (!"".equals(remaingCurrency)) {
          JSONObject currencyData = new JSONObject(remaingCurrency);
          Iterator<String> keys = currencyData.keys();
          while (keys.hasNext()) {
            String key = keys.next();
            int value = currencyData.getInt(key);
            formBuilder.add(key, String.valueOf(value));
          }
        }

        JSONObject localUserProperties = mmGetJsonValue(KEY_LOCAL_USER_PROPERIES);
        if (localUserProperties != null) {
          Iterator<String> keys = localUserProperties.keys();
          while (keys.hasNext()) {
            String key = keys.next();
            String value = localUserProperties.getString(key);
            formBuilder.add(key, value);
          }
        }
        formBuilder.add("total_ad_revenue", String.valueOf(eventTracker.getTotalAdRevenue()));
      } catch (Throwable ignored) {
      }

      Request request = new Request.Builder()
        .url(userAttributeUrl)
        .post(formBuilder.build())
        .build();

      getOkHttpClient().newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
          Logger.warning(TAG, "user attribute report failed");
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
          Logger.debug(TAG, "user attribute reported");
          try (ResponseBody body = response.body()) {
            if (body == null) {
              return;
            }
            String data = body.string();
            JSONObject result = new JSONObject(data);
            if (result.has("inapp_event")) {
              String inAppEvent = result.optString("inapp_event");
              if (!"".equals(inAppEvent)) {
                triggerInAppMessage(inAppEvent);
              }
            }
            if (result.has("event")) {
              String event = result.optString("event");
              if (!"".equals(event)) {
                logEventToConversionPlatforms(event, null);
              }
            }

            if (result.has("event_once")) {
              String event = result.optString("event_once");
              if (!"".equals(event)) {
                boolean alreadySent = mmGetBoolValue("sent_" + event, false);
                if (!alreadySent) {
                  logEventToConversionPlatforms(event, null);
                  mmSetBoolValue("sent_" + event, true);
                }
              }
            }

            if (result.has("remote_config")) {
              checkCustomRemoteConfig(result.optJSONObject("remote_config"));
            }

            if (result.has("user_properties")) {
              JSONObject userProperties = result.optJSONObject("user_properties");
              if (userProperties != null && userProperties.length() > 0) {
                Iterator<String> keys = userProperties.keys();
                while (keys.hasNext()) {
                  String key = keys.next();
                  String value = userProperties.optString(key);
                  eventTracker.setUserProperty(key, value);
                }
              }
            }

            if (result.has("inapp_message")) {
              JSONObject inAppMessage = result.optJSONObject("inapp_message");
              if (inAppMessage != null) {
                listener.messageDisplayed(inAppMessage.optString("id"), inAppMessage.toString());
              }
            }

            if (result.has(KEY_MM_COUNTRY)) {
              mmSetStringValue(KEY_MM_COUNTRY, result.optString(KEY_MM_COUNTRY));
            }

          } catch (Throwable t) {
            Logger.error(TAG, "parse response exception " + userAttributeUrl, t);
          }
        }
      });
    } catch (Throwable t) {
      // crash protected
    }
  }

  public static void processEventCallback(@NonNull JSONObject json) {
    if (json.has("inapp_event")) {
      String inAppEvent = json.optString("inapp_event");
      if (!"".equals(inAppEvent)) {
        triggerInAppMessage(inAppEvent);
      }
    }

    if (json.has("event")) {
      String event = json.optString("event");
      if (!"".equals(event)) {
        logEventToConversionPlatforms(event, null);
      }
    }

    if (json.has("event_once")) {
      String event = json.optString("event_once");
      if (!"".equals(event)) {
        boolean alreadySent = mmGetBoolValue("sent_" + event, false);
        if (!alreadySent) {
          logEventToConversionPlatforms(event, null);
          mmSetBoolValue("sent_" + event, true);
        }
      }
    }

    if (json.has("user_properties")) {
      JSONObject userProperties = json.optJSONObject("user_properties");
      if (userProperties != null && userProperties.length() > 0) {
        Iterator<String> keys = userProperties.keys();
        while (keys.hasNext()) {
          String key = keys.next();
          String value = userProperties.optString(key);
          eventTracker.setUserProperty(key, value);
        }
      }
    }

    if (json.has("remote_config")) {
      checkCustomRemoteConfig(json.optJSONObject("remote_config"));
    }

    if (json.has("gamemessage")) {
      JSONObject gamemessage = json.optJSONObject("gamemessage");
      if (gamemessage != null) {
        String type = gamemessage.optString("type");
        String data = gamemessage.optString("data");
        if (!"".equals(type) && !"".equals(data)) {
          AndroidSdk.onGameMessage(type, data);
        }
      }
    }
  }

  public static void showInAppMessage(@NonNull String message) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(() -> {
      FrameLayout rootview = (FrameLayout) activity.getWindow().getDecorView().getRootView();
      Banner.make(rootview, activity, message, Banner.TOP, 5000).show();
    });
  }

  public static void consumePurchase(@NonNull String purchaseToken, @NonNull OrderConsumeListener orderConsumeListener) {
    if (purchaseManagerWrapper != null) {
      purchaseManagerWrapper.consumePurchase(purchaseToken, orderConsumeListener);
    }
  }

  public static void displayConsentForm(@NonNull Activity activity) {
    // Set tag for under age of consent. false means users are not under
    // age.
    ConsentRequestParameters params = new ConsentRequestParameters
      .Builder()
      .setTagForUnderAgeOfConsent(false)
      .build();

    ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(activity);
    consentInformation.requestConsentInfoUpdate(
      activity,
      params,
      new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
        @Override
        public void onConsentInfoUpdateSuccess() {
          // The consent information state was updated.
          // You are now ready to check if a form is available.
          if (consentInformation.isConsentFormAvailable()) {
            // Loads a consent form. Must be called on the main thread.
            UserMessagingPlatform.loadConsentForm(
              activity,
              new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
                @Override
                public void onConsentFormLoadSuccess(ConsentForm consentForm) {
                  if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                    consentForm.show(
                      activity,
                      new ConsentForm.OnConsentFormDismissedListener() {
                        @Override
                        public void onConsentFormDismissed(@Nullable FormError formError) {
                          if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED) {
                            // App can start requesting ads.

                          }

                          // Handle dismissal by reloading form.
                          // loadForm();
                        }
                      });
                  }
                }
              },
              new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
                @Override
                public void onConsentFormLoadFailure(FormError formError) {
                  // Handle the error.
                }
              }
            );
          }
        }
      },
      new ConsentInformation.OnConsentInfoUpdateFailureListener() {
        @Override
        public void onConsentInfoUpdateFailure(FormError formError) {
          // Handle the error.
        }
      });
  }

  private static final String CHNANEL_NOTIFICATION = "ivy_notification_channel";

  public static void displayNotification(int notificationId, String title, String message) {
    try {
      Activity activity = getActivity();
      if (activity == null) {
        return;
      }
      NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, CHNANEL_NOTIFICATION)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(activity);

      // notificationId is a unique int for each notification that you must define
      if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        Logger.warning(TAG, "No permission granted, notification ignored");
        return;
      }
      notificationManager.notify(notificationId, builder.build());
    } catch (Throwable t) {
      Logger.error(TAG, "displayNotification exception", t);
    }
  }

  public static void showMessageDialog(String title, String message) {
    Activity activity = getActivity();
    if (activity == null || activity.isFinishing()) {
      return;
    }
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        AlertDialog alertDialog = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle(title).setMessage(message).setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {

          }
        }).setCancelable(true).create();
        alertDialog.show();
      }
    });
  }

  public static void onAccountSignedIn() {
    Logger.debug(TAG, "onAccount Signed In");
    try {
      checkOnlineStatus();
      startCheckRealtimeInAppMessage();
    } catch (Throwable t) {
      Logger.error(TAG, "onAccountSignedIn exception", t);
    }
  }

  private static boolean realTimeMessageCheckerStarted = false;

  private static void startCheckRealtimeInAppMessage() {
    boolean enableRealtimeMessage = getGridConfigBoolean("realtime.appmessage.check");
    if (!enableRealtimeMessage || realTimeMessageCheckerStarted) {
      return;
    }

    String userId = FirebaseAuth.getInstance().getUid();
    if (userId == null) {
      return;
    }

    Logger.debug(TAG, "startCheckRealtimeInAppMessage");
    realTimeMessageCheckerStarted = true;
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    final DatabaseReference msgRef = database.getReference("message/" + userId);
    msgRef.addChildEventListener(new ChildEventListener() {
      @Override
      public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
        Logger.debug(TAG, "onChildAdded");
        Object o = snapshot.getValue();
        if (o instanceof Map) {
          try {
            String id = snapshot.getKey();
            if (id != null) {
              try {
                JSONObject resultObject = new JSONObject((Map<String, Object>) o);
                Logger.debug(TAG, "Receiving in app message >>> " + resultObject);
                AndroidSdk.onGameMessage(id, resultObject.toString());
              } catch (Throwable t) {
                Logger.error(TAG, "parse realtime message exception", t);
              }
              msgRef.child(id).removeValue();
            }
          } catch (Throwable e) {
            Logger.error(TAG, "startCheckRealtimeInAppMessage exception", e);
          }
        } else {
          Logger.warning(TAG, "Not map data ignore");
        }
      }

      @Override
      public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
        Logger.debug(TAG, "onChildChanged");
      }

      @Override
      public void onChildRemoved(@NonNull DataSnapshot snapshot) {
        Logger.debug(TAG, "onChildRemoved");
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
  }

  private static boolean onlineStatusChecker = false;

  private static void checkOnlineStatus() {
    try {
      boolean enableUserOnlineCheck = getGridConfigBoolean("online.status.check");
      if (!enableUserOnlineCheck || onlineStatusChecker) {
        return;
      }
      String userId = FirebaseAuth.getInstance().getUid();
      if (userId == null) {
        return;
      }
      Logger.debug(TAG, "start online status monitor ： " + userId);

      onlineStatusChecker = true;
      // Since I can connect from multiple devices, we store each connection instance separately
      // any time that connectionsRef's value is null (i.e. has no children) I am offline
      final FirebaseDatabase database = FirebaseDatabase.getInstance();
      final DatabaseReference myConnectionsRef = database.getReference("users/" + userId + "/connections");

      // Stores the timestamp of my last disconnect (the last time I was seen online)
      final DatabaseReference lastOnlineRef = database.getReference("users/" + userId + "/lastOnline");
      final DatabaseReference connectedRef = database.getReference(".info/connected");
      connectedRef.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
          boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
          if (connected) {
            Logger.debug(TAG, "User connected");
            DatabaseReference con = myConnectionsRef.push();

            // When this device disconnects, remove it
            con.onDisconnect().removeValue();

            // When I disconnect, update the last time I was seen online
            lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP);

            // Add this device to my connections list
            // this value could contain info about the device or a timestamp too
            con.setValue(Boolean.TRUE);
          } else {
            Logger.debug(TAG, "user disconnected");
          }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
          Logger.debug(TAG, "Listener was cancelled at .info/connected");
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "checkOnlineStatus exception", t);
    }
  }

  private static final String URL_LOADING_PROMOTION = "https://loading-promotion-bq4v4uzyia-uc.a.run.app";
  private static final String URL_MORE_GAMES = "https://cdn.lisgame.com/promote/crosspromotion.json";

  /**
   * 检查闪屏开启的交叉推广。闪屏交叉推广是特殊的展示推广位，向应用提供一个应用图标。
   */
  public static void checkLoadingPromotion() {
  }

  /**
   *
   */
  public static void checkMoreGames(@NonNull OnDataListener listener) {
    Activity a = IvySdk.getActivity();
    if (a == null) {
      return;
    }

    String moreGameData = Util.retrieveData(a, "moregame");
    if (moreGameData != null) {
      long lastGetMoreGameTimestamp = IvySdk.mmGetLongValue("last_more_game_timestamp", 0L);
      if (lastGetMoreGameTimestamp > 0 && System.currentTimeMillis() - lastGetMoreGameTimestamp < 24 * 3600 * 1000L) {
        listener.onData(moreGameData);
        return;
      }
    }

    Request request = new Request.Builder().url(URL_MORE_GAMES).get().build();
    getOkHttpClient().newCall(request).enqueue(new Callback() {
      @Override
      public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        try (ResponseBody responseBody = response.body()) {
          if (responseBody != null) {
            try {
              String body = responseBody.string();
              JSONObject o = new JSONObject(body);
              if (o.has("result") && o.optBoolean("result")) {
                listener.onData(body);
                // write the body to
                Activity a = IvySdk.getActivity();
                if (a != null) {
                  Util.storeData(a, "moregame", body);
                  IvySdk.mmSetLongValue("last_more_game_timestamp", System.currentTimeMillis());
                  Logger.debug(TAG, "new more game config saved!");
                }
              }
            } catch (Throwable t) {
              Logger.error(TAG, "checkMoreGames exception", t);
            }
          }
        }
      }

      @Override
      public void onFailure(@NonNull Call call, @NonNull IOException e) {
        Logger.error(TAG, "checkMoreGames exception", e);
      }
    });
  }

  private static IProviderFacade providerFacade = null;
  public static void setProviderFacade(@NonNull IProviderFacade providerFacade) {
    IvySdk.providerFacade = providerFacade;
  }
}

