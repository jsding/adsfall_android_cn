package com.android.client;

public class BuilderListener {
  private AndroidSdk.Builder builder;

  public void setBuilder(AndroidSdk.Builder builder) {
    this.builder = builder;
  }

  public void onInitialized() {
    if (builder.sdkResultListener != null) {
      builder.sdkResultListener.onInitialized();
    }
  }
}
