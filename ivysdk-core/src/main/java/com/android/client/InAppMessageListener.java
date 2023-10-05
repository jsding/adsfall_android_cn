package com.android.client;

import androidx.annotation.NonNull;

public interface InAppMessageListener {
  void sentryLog(@NonNull String message);
  void messageDisplayed(@NonNull String message);
  void messageDisplayed(String campaignId, String dataJson);
  void messageClicked(String actionUrl);
}
