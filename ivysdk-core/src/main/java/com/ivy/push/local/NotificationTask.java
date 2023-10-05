package com.ivy.push.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ivy.util.Logger;


public class NotificationTask extends Worker {
  private static final String TAG = "LocalNotification";

  public NotificationTask(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  @NonNull
  @Override
  public Result doWork() {
    try {
      Data data = getInputData();
      NotificationUtil.createNotification(getApplicationContext(),
        data.getString("title"),
        data.getString("subtitle"),
        data.getBoolean("autoClose", true));
    } catch(Throwable t) {
      Logger.error(TAG, "doPush work exception", t);
    }
    return Result.success();
  }
}
