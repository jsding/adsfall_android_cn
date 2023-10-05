package com.ivy.networks.tracker.impl;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.ivy.IvySdk;
import com.ivy.networks.tracker.EventTrackerProvider;
import com.ivy.util.DeviceInfo;
import com.ivy.util.Logger;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ParfkaTracker implements EventTrackerProvider {
  private static final String TAG = "Parfka";
  public static final MediaType JSON
    = MediaType.get("application/json; charset=utf-8");
  private OkHttpClient okHttpClient;

  private boolean suppress = false;
  private String userId = null;

  private DeviceInfo deviceInfo = null;

  @Override
  public void initialize(Context context) {
    Logger.debug(TAG, "ParfkaTracker initialized");
    Dispatcher dispatcher = new Dispatcher();
    dispatcher.setMaxRequests(2);
    dispatcher.setMaxRequestsPerHost(5);

    okHttpClient = new OkHttpClient.Builder().dispatcher(dispatcher).connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();
    deviceInfo = new DeviceInfo(context);
  }

  @Override
  public void setSuppress(boolean suppress) {
    this.suppress = suppress;
  }

  @Override
  public void setUserID(String userID) {
    this.userId = userID;
  }

  @Override
  public void setUserProperty(String key, String value) {
    Logger.debug(TAG, "setUserProperty, " + key + "," + value);
  }

  @Override
  public void logPurchase(String contentType, String contentId, String currency, float revenue) {
  }

  private String getPostDataString(String eventName, Bundle bundle) {
    try {
      JSONObject result = new JSONObject();
      if (bundle != null) {
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
          Object v = bundle.get(key);
          if (v != null) {
            result.put(key, v);
          } else {
            Logger.debug(TAG, eventName + "null key data: " + key);
          }
        }
      }

      long now = System.currentTimeMillis();
      result.put("st", Long.valueOf(now / 1000));

      if (userId != null) {
        result.put("roleId", userId);
      }
      result.put("event_token", eventName);

      if (deviceInfo != null) {
        deviceInfo.injectJson(result);
      }
      return result.toString();
    } catch (Throwable t) {
      Logger.error(TAG, "getPostDataString exception", t);
    }
    return "{}";
  }

  private HttpUrl cachedEventHttpUrl = null;

  @Override
  public void logEvent(String eventName, Bundle bundle) {
    if (suppress) {
      return;
    }

    try {
      String eventDataJson = getPostDataString(eventName, bundle);
      Logger.debug(TAG, "post event >>> " + eventName + " data >> " + eventDataJson);
      Request.Builder requestBuilder = new Request.Builder();
      RequestBody body = RequestBody.create(eventDataJson, JSON);
      if (cachedEventHttpUrl == null) {
        cachedEventHttpUrl = HttpUrl.get(ParfkaFactory.baseUrl);
      }
      Request request = requestBuilder.url(cachedEventHttpUrl).post(body).build();
      okHttpClient.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
          Logger.error(TAG, "post event onFailure " + eventName, e);
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) {
          try (ResponseBody body = response.body()) {
            if (body != null) {
              String str = body.string();
              Logger.debug(TAG, ">>> event sent result: " + str);
              if (!"".equals(str) && str.contains("success")) {
                JSONObject o = new JSONObject(str);
                Logger.debug(TAG, "post event success >>> " + eventName);
                if (o.has("success") && o.optBoolean("success", false) && o.has("data")) {
                  JSONObject attachedData = o.optJSONObject("data");
                  if (attachedData != null) {
                    IvySdk.processEventCallback(attachedData);
                  }
                }
              }
            }
          } catch (Throwable t) {
            Logger.error(TAG, "onResponse ", t);
          }
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "logEvent exception", t);
    }
  }
}
