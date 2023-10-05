package com.ivy.push.local;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.adsfall.R;
import com.ivy.IvySdk;
import com.ivy.util.Logger;

public class NotificationUtil {

  private static final String TAG = "LocalNotification";
  private static final String CHANNEL_ID = "lis_push_channel";
  private static final String CHANNEL_NAME = "LisGameNotification";

  public static final int NOTIFICATION_ID = 1027;

  private static boolean channelCreated = false;

  private static int pushIconDrawable = 0;


  public static void createNotification(Context context, String title, String subtitle, boolean autoClose) {
    try {
      Logger.debug(TAG, "notification started");
      NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      if (!channelCreated && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(channel);
        channelCreated = true;
      }

      if (pushIconDrawable == 0) {
        try {
          ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
          Bundle bundle = ai.metaData;
          pushIconDrawable = bundle.getInt("com.google.firebase.messaging.default_notification_icon", 0);
        } catch(Throwable t) {
          Logger.error(TAG, "get push icon exception", t);
        }

        if (pushIconDrawable == 0) {
          Logger.warning(TAG, "No push icon set, use the default resources");
          pushIconDrawable = R.drawable.push_icon;
        }
      }

      Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
      PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE);

      Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(subtitle)
        .setWhen(System.currentTimeMillis())
        .setSmallIcon(pushIconDrawable)
        .setAutoCancel(autoClose)
        .setContentIntent(pendingIntent)
        .setChannelId(CHANNEL_ID)
        .build();
      manager.notify(NOTIFICATION_ID, notification);
    } catch(Throwable t) {
      Logger.error(TAG, "createNotification exception", t);
    }
  }
}
