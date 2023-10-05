package com.ivy.ads.events;

import androidx.annotation.NonNull;

import com.ivy.ads.adapters.BaseAdapter;
import com.ivy.networks.tracker.EventTracker;

public class RewardedEventHandler extends BaseEventHandler {
  public RewardedEventHandler(EventTracker eventLogger) {
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
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());

    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam(EventParams.PARAM_PLACEMENT, adapter.getPlacementId());
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    logEvent(EventID.VIDEO_SHOWN, eventParams, this.eventLogger);
  }

  public void onAdShowFailCalled(@NonNull BaseAdapter adapter) {
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());
    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam(EventParams.PARAM_PLACEMENT, adapter.getPlacementId());
    eventParams.addParam(EventParams.PARAM_REASON, adapter.getShowStatus().toString());
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    logEvent(EventID.VIDEO_FAILED, eventParams, this.eventLogger);
  }

  public void onAdClickCalled(@NonNull BaseAdapter adapter) {
  }

  public void onAdClosedCalled(@NonNull BaseAdapter adapter, boolean isReward) {
    if (isReward) {
      EventParams eventParams = new EventParams();
      eventParams.putAll(adapter.getEventParams());
      eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
      eventParams.addParam(EventParams.PARAM_PLACEMENT, adapter.getPlacementId());
      eventParams.addParam(EventParams.PARAM_SHOWNTIME, adapter.getShowTimeMs());
      String displayedTag = adapter.getDisplayedTag();
      if (displayedTag != null) {
        eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
      }
      logEvent(EventID.VIDEO_COMPLETED, eventParams, this.eventLogger);
    }
  }
}
