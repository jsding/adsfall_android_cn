package com.ivy.ads.events;

import androidx.annotation.NonNull;

import com.ivy.ads.adapters.BaseAdapter;
import com.ivy.networks.tracker.EventTracker;

public class NonRewardedEventHandler extends BaseEventHandler {
  public NonRewardedEventHandler(EventTracker eventLogger) {
    super(eventLogger);
  }

  public void fetchCalled(@NonNull BaseAdapter adapter) {
  }

  public void showCalled(@NonNull BaseAdapter adapter) {
  }

  public void timeoutCalled(@NonNull BaseAdapter adapter) {
  }

  public void onAdLoadSuccessCalled(@NonNull BaseAdapter adapter) {
  }

  public void onAdLoadFailCalled(@NonNull BaseAdapter adapter, String failReason) {
  }

  public void onAdShowSuccessCalled(@NonNull BaseAdapter adapter) {
    String eventName = adapter != null && ADSFALL.equals(adapter.getName()) ? EventID.INTERSTITIAL_ADSFALL_SHOWN : EventID.INTERSTITIAL_SHOWN;
    EventParams params = new EventParams();
    params.putAll(adapter.getEventParams());
    params.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    params.addParam(EventParams.PARAM_PLACEMENT, adapter.getPlacementId());
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      params.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    logEvent(eventName, params, this.eventLogger);
  }

  public void onAdShowFailCalled(@NonNull BaseAdapter adapter) {
    String eventName = adapter != null && ADSFALL.equals(adapter.getName()) ? EventID.INTERSTITIAL_ADSFALL_SHOW_FAILED : EventID.INTERSTITIAL_SHOW_FAILED;

    EventParams params = new EventParams();
    params.putAll(adapter.getEventParams());
    params.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    params.addParam(EventParams.PARAM_PLACEMENT, adapter.getPlacementId());
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      params.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    logEvent(eventName, params, this.eventLogger);
  }

  public void onAdClickCalled(@NonNull BaseAdapter adapter) {
    String eventName = adapter != null && ADSFALL.equals(adapter.getName()) ? EventID.INTERSTITIAL_ADSFALL_CLICKED : EventID.INTERSTITIAL_CLICKED;

    EventParams params = new EventParams();
    params.putAll(adapter.getEventParams());
    params.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    params.addParam(EventParams.PARAM_PLACEMENT, adapter.getPlacementId());
    params.addParam(EventParams.PARAM_SHOWNTIME, adapter.getShowTimeMs());
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      params.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    logEvent(eventName, params, this.eventLogger);
  }

  public void onAdClosedCalled(@NonNull BaseAdapter adapter, boolean isReward) {

  }
}
