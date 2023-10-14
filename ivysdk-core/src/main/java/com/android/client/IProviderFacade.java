package com.android.client;

import android.app.Activity;

import androidx.annotation.NonNull;

/**
 *
 */
public interface IProviderFacade {

  void onInitialize(@NonNull Activity activity);
}
