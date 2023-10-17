package com.android.client;

public interface OnSignedInListener {
  void onSignedInSuccess(SignInProfile signInProfile);
  void onSignedInError(String code, String message);
  void onSignedInCancel();
}
