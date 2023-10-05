package com.ivy.ads.events;

import androidx.annotation.NonNull;

import com.ivy.ads.adapters.BaseAdapter;
import com.ivy.networks.tracker.EventTracker;

public abstract class BaseEventHandler {
  public static final String ADSFALL = "adsfall";

  protected EventTracker eventLogger;

  public BaseEventHandler(@NonNull EventTracker eventLogger) {
    this.eventLogger = eventLogger;
  }

  public abstract void fetchCalled(@NonNull BaseAdapter<?> baseAdapter);

  public abstract void onAdClickCalled(@NonNull BaseAdapter<?> baseAdapter);

  public abstract void onAdClosedCalled(@NonNull BaseAdapter<?> baseAdapter, boolean z);

  public abstract void onAdLoadFailCalled(@NonNull BaseAdapter<?> baseAdapter, String loadStatus);

  public abstract void onAdLoadSuccessCalled(@NonNull BaseAdapter<?> baseAdapter);

  public abstract void onAdShowFailCalled(@NonNull BaseAdapter<?> baseAdapter);

  public abstract void onAdShowSuccessCalled(@NonNull BaseAdapter<?> baseAdapter);

  public abstract void showCalled(@NonNull BaseAdapter<?> baseAdapter);

  public abstract void timeoutCalled(@NonNull BaseAdapter<?> baseAdapter);

  protected void logEvent(String eventType, @NonNull EventParams params, @NonNull EventTracker eventLogger) {
    eventLogger.logEvent(eventType, params.getParamsBundle());
  }

  public EventTracker getEventLogger() {
    return this.eventLogger;
  }
}
