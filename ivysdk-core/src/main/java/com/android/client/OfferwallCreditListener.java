package com.android.client;

import androidx.annotation.NonNull;

public interface OfferwallCreditListener {
  void onOfferwallAdCredited(int credits, int totalCredits);
  void onGetOfferwallCreditsFailed(@NonNull String message);
}
