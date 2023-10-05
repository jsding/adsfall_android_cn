package com.android.client;

public interface SdkResultListener {
  void onInitialized();

  void onReceiveServerExtra(String data);

  void onReceiveNotificationData(String data);
}
