package com.ivy.ads.events;

import androidx.annotation.NonNull;

import com.ivy.ads.adapters.BannerAdapter;
import com.ivy.ads.adapters.BaseAdapter;
import com.ivy.networks.tracker.EventTracker;

public class BannerEventHandler extends BaseEventHandler {
  public BannerEventHandler(EventTracker eventLogger) {
    super(eventLogger);
  }

  public void fetchCalled(BaseAdapter adapter) {
  }

  public void showCalled(BaseAdapter adapter) {

  }

  public void timeoutCalled(BaseAdapter adapter) {
  }

  public void onAdLoadSuccessCalled(BaseAdapter adapter) {
  }

  public void onAdLoadFailCalled(BaseAdapter adapter, String failReason) {
  }

  public void onAdShowSuccessCalled(@NonNull BaseAdapter adapter) {
    String eventName = EventID.BANNER_DISPLAYED;//adapter != null && ADSFALL.equals(adapter.getName()) ? EventID.BANNER_ADSFALL_DISPLAYED : EventID.BANNER_DISPLAYED;
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());
    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam(EventParams.PARAM_LOADTIME, adapter.getLoadTimeMs());
    eventParams.addParam(EventParams.PARAM_PLACEMENT, adapter.getPlacementId());
    logEvent(eventName, eventParams, eventLogger);
  }

  public void onAdShowFailCalled(BaseAdapter adapter) {
  }

  public void onAdClickCalled(BaseAdapter adapter) {
    String eventName = EventID.BANNER_CLICKED;//adapter != null && ADSFALL.equals(adapter.getName()) ? EventID.BANNER_ADSFALL_CLICKED : EventID.BANNER_CLICKED;
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());
    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam(EventParams.PARAM_LOADTIME, adapter.getLoadTimeMs());
    eventParams.addParam(EventParams.PARAM_PLACEMENT, adapter.getPlacementId());
    logEvent(eventName, eventParams, eventLogger);
  }

  public void onAdClosedCalled(BaseAdapter adapter, boolean isReward) {
  }

  public void onBannerDisplayed(BannerAdapter adapter) {
  }
}
