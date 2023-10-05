package com.android.client;

import androidx.annotation.NonNull;

public interface FriendFinderListener {
  void onSuccess();
  void onCancel();
  void onError(@NonNull String error);
}
