package com.ivy.adm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.amazon.device.messaging.ADMMessageHandlerJobBase;
import com.ivy.IvySdk;
import com.ivy.networks.tracker.impl.ParfkaFactory;
import com.ivy.util.Logger;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LisADMMessageHandler extends ADMMessageHandlerJobBase {
  private static final String TAG = "LisADMMessageHandler";

  private static final String URL_ADM = "https://adm-bq4v4uzyia-uc.a.run.app";

  // 通过在主要活动中调用startRegister()来启动注册
  // 过程。当注册ID就绪时，ADM将对您的应用
  // 调用onRegistered()。将传入的注册ID传输到您的服务器，以便
  // 服务器可以将消息发送到此应用实例。如果
  // 您的注册ID因任何原因发生轮换或更改，ADM也会调用onRegistered()；
  // 如果出现这种情况，您的应用应将新的注册ID传递到您的服务器。
  // 您的服务器需要能够处理长达1536个字符的
  // 注册ID。
  @Override
  protected void onRegistered(final Context context, final String newRegistrationId) {
    Logger.debug(TAG, "register adm token: " + newRegistrationId);
    try {
      String versionName = "unknown";
      String packageName = "";
      try {
        PackageManager packageManager = context.getPackageManager();
        packageName = context.getPackageName();
        PackageInfo info = packageManager.getPackageInfo(packageName, 0);
        versionName = info.versionName;
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      JSONObject result = new JSONObject();
      result.put("action", "register");
      result.put("uid", IvySdk.getUUID());
      result.put("RegistrationId", newRegistrationId);
      result.put("country", IvySdk.getCountryCode());
      result.put("package", packageName);
      result.put("version", versionName);
      result.put("provider", "amazon");
      result.put("languageCode", Locale.getDefault().getLanguage());

      String eventDataJson = result.toString();
      Request.Builder requestBuilder = new Request.Builder();
      RequestBody body = RequestBody.create(eventDataJson, MediaType.get("application/json; charset=utf-8"));

      HttpUrl url = HttpUrl.get(URL_ADM);

      Request request = requestBuilder.url(url).post(body).build();

      IvySdk.getOkHttpClient().newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) {
          try (ResponseBody body = response.body()) {
            if (body != null) {
              Logger.debug(TAG, "receive >>> " + body);
            }
          }
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "onRegistered exception", t);
    }
  }

  @Override
  protected void onUnregistered(final Context context, final String registrationId) {
    Logger.debug(TAG, "unregister adm token: " + registrationId);

    //如果您的应用在此台设备上已注销，请通知服务器
    // 此应用实例不再是有效的消息发送目标。
    try {
      JSONObject result = new JSONObject();
      result.put("action", "unregister");
      result.put("uid", IvySdk.getUUID());
      result.put("RegistrationId", registrationId);
      String eventDataJson = result.toString();
      Request.Builder requestBuilder = new Request.Builder();
      RequestBody body = RequestBody.create(eventDataJson, MediaType.get("application/json; charset=utf-8"));

      HttpUrl url = HttpUrl.get(URL_ADM);

      Request request = requestBuilder.url(url).post(body).build();

      IvySdk.getOkHttpClient().newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) {
          try (ResponseBody body = response.body()) {
            if (body != null) {
              Logger.debug(TAG, "receive >>> " + body);
            }
          }
        }
      });
    } catch (Throwable t) {
      Logger.error(TAG, "onRegistered exception", t);
    }
  }

  @Override
  protected void onRegistrationError(final Context context, final String errorId) {
    Logger.error(TAG, "adm onRegistrationError: " + errorId);
  }

  @Override
  protected void onMessage(final Context context, final Intent intent) {
    Logger.debug(TAG, "adm onMessage: ");

    // 从附加到com.amazon.device.messaging.intent.RECEIVE意图
    // 的额外信息集中提取消息内容。

    // 创建字符串以访问JSON数据中的message和timeStamp字段。
//    final String msgKey = getString(R.string.json_data_msg_key);
//    final String timeKey = getString(R.string.json_data_time_key);
//
//    // 获取将在onMessage()回调中触发的意图操作。
//    final String intentAction = getString(R.string.intent_msg_action);
//
//    // 获取意图中包含的额外信息。
//    final Bundle extras = intent.getExtras();
//
//    // 从意图中的额外信息中提取消息和时间。
//    // ADM既不会保证消息能够送达，也不会保证消息能够按顺序送达。
//    // 由于网络条件的变化，消息可能会被多次传送。
//    // 您的应用必须能够处理重复消息的实例。
//    final String msg = extras.getString(msgKey);
//    final String time = extras.getString(timeKey);
  }
}