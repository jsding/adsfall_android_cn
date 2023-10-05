package com.android.client;

import androidx.annotation.NonNull;

public interface OnPasswordChangedListener {

  void onSuccess();
  void onError(@NonNull String errorCode, @NonNull String errorMessage);
}
