package com.ivy.push.local;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.ivy.IvySdk;
import com.ivy.util.Logger;

import java.util.concurrent.TimeUnit;

public class LocalNotificationManager {
  private static final String TAG = "LocalNotification";

  /**
   * @param tag        任务标志，可根据此标志关闭任务
   * @param title      通知栏标题
   * @param subtitle   通知栏副标题
   * @param delay      延迟时间，单位分钟
   * @param autoCancel 是否自动关闭
   */
  public static void schedulePush(@NonNull Context context, @NonNull String tag, @NonNull String title, @NonNull String subtitle, long delay, boolean autoCancel) {
    try {
      WorkManager.getInstance(context).cancelAllWorkByTag(tag);

      if (delay <= 0) {
        Logger.debug(TAG, "delay wrong, cancel");
        return;
      }

      Logger.debug(TAG, "schedule local push at " + delay + ", tag " + tag);
      Data data = new Data.Builder().putString("title", title).putString("subtitle", subtitle).putBoolean("autoClose", autoCancel).build();
      OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(NotificationTask.class).setInputData(data).setInitialDelay(delay, TimeUnit.SECONDS).addTag(tag).build();
      WorkManager.getInstance(context).enqueue(oneTimeWorkRequest);
    } catch (Throwable t) {
      Logger.error(TAG, "schedulePush exception", t);
    }
  }

  /**
   * 关闭对应tag的任务，无法确保一定会关闭
   *
   * @param context
   * @param tag
   */
  public static void cancelPush(@NonNull Context context, @NonNull String tag) {
    try {
      Logger.debug(TAG, "Cancel local push " + tag);
      WorkManager.getInstance(context).cancelAllWorkByTag(tag);
    } catch (Throwable t) {
      Logger.error(TAG, "cancelPush exception", t);
    }
  }

  public static void clearLocalNotification(@NonNull Context context) {
    try {
      NotificationManagerCompat.from(context).cancel(NotificationUtil.NOTIFICATION_ID);
    } catch (Throwable t) {
      Logger.error(TAG, "clearLocalNotification exception", t);
    }
  }

  public static boolean isPermissionEnabled(@NonNull Context context) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
      } else {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
      }
    } catch (Throwable t) {
      Logger.error(TAG, "isPermissionEnabled exception", t);
      return false;
    }
  }

  public static void enablePermission(Activity activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
        boolean hasRequested = IvySdk.mmGetBoolValue("has_Requested_Notification", false);
        if (hasRequested && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
          openPermissionSetting(activity);
        } else {
          IvySdk.mmSetBoolValue("has_Requested_Notification", true);
          ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
      }
    } else {
      boolean enabled = NotificationManagerCompat.from(activity).areNotificationsEnabled();
      if (!enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          openPermissionSetting(activity);
        }
      }
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private static void openPermissionSetting(Context context) {
    try {
      Intent intent = new Intent();
      intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
      intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
      // intent.putExtra(Settings.EXTRA_CHANNEL_ID, "channel_id");
      intent.putExtra("app_package", context.getPackageName());
      intent.putExtra("app_uid", context.getApplicationInfo().uid);
      context.startActivity(intent);
    } catch (Throwable t) {
      Logger.error(TAG, "openPermissionSetting exception", t);
      Intent intent = new Intent();
      intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      intent.setData(Uri.fromParts("package", context.getPackageName(), null));
      context.startActivity(intent);
    }
  }
}
