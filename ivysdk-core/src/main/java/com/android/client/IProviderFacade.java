package com.android.client;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.ivy.billing.PurchaseManager;

import org.json.JSONObject;

/**
 *
 */
public interface IProviderFacade {

  void onInitialize(@NonNull Activity activity, @NonNull JSONObject gridData);

  void signIn(@NonNull Activity activity, @NonNull OnSignedInListener onSignedInListener);

  boolean onlyUsingPlatformAccount();

  PurchaseManager getPurchaseManager();

  void initPushSystem(@NonNull Activity activity);

  String getChannel();
}
