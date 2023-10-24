package com.android.client;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.ivy.billing.PurchaseManager;

import org.json.JSONObject;

public interface IProviderFacade {

  void onInitialize(@NonNull Activity activity, @NonNull JSONObject gridData);

  void signIn(@NonNull Activity activity, @NonNull OnSignedInListener onSignedInListener);

  boolean onlyUsingPlatformAccount();

  PurchaseManager getPurchaseManager();

  void initPushSystem(@NonNull Activity activity);

  String getChannel();

  void onCreate(Activity activity);

  void onResume(Activity activity);

  void onPause(Activity activity);

  void onActivityResult(int requestCode, int resultCode, Intent data);

  void registerPaymentSystemReadyListener(@NonNull OnPaymentSystemReadyListener listener);
}
