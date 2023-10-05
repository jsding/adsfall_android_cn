package com.android.client;

import androidx.annotation.NonNull;

public interface OrderConsumeListener {
  void onConsumeSuccess(@NonNull String purchaseToken);
  void onConsumeError(@NonNull String purchaseToken, String errorCode, String errorMessage);
}
