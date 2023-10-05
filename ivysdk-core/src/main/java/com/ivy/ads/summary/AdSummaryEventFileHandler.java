package com.ivy.ads.summary;

import android.app.Activity;

import com.ivy.ads.interfaces.IvyAdType;
import com.ivy.networks.tracker.EventTracker;

public class AdSummaryEventFileHandler implements AdSummaryEventHandler {
  private EventTracker eventTracker;

  @Override
  public void init(Activity context, EventTracker eventTracker) {
    this.eventTracker = eventTracker;
  }

  @Override
  public long[] getSummaryData(IvyAdType adType, String provider) {
    return null;
  }

  @Override
  public void onRequest(IvyAdType adType, String partner) {
  }

  @Override
  public void onLoad(IvyAdType adType, String partner) {
  }

  @Override
  public void onAdPaid(IvyAdType adType, String str, float revenue) {
    if (eventTracker != null) {
      eventTracker.pingROAS(revenue, "ad");
    }
  }

  @Override
  public void onImpression(IvyAdType adType, String partner, float revenue) {
    if (eventTracker != null) {
      eventTracker.pingROAS(revenue, "ad");
    }
  }

  @Override
  public void onAction(IvyAdType adType, String partner) {
  }

  public void saveAdSummaryData() {

  }
}
