package com.ivy.networks.tracker.impl;


import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ivy.networks.tracker.EventTrackerProvider;

public class FirebaseTracker implements EventTrackerProvider {
  private FirebaseAnalytics mFirebaseAnalytics;

  private boolean suppress = false;

  public void initialize(Context context) {
    mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
  }

  @Override
  public void setSuppress(boolean suppress) {
    this.suppress = suppress;
  }

  @Override
  public void setUserID(String userID) {
    mFirebaseAnalytics.setUserId(userID);
  }

  @Override
  public void logPurchase(String contentType, String contentId, String currency, float revenue) {

  }

  @Override
  public void logEvent(String eventName, Bundle bundle) {
    if (suppress) {
      return;
    }
    eventName = eventName.replaceAll("-", "_");
    mFirebaseAnalytics.logEvent(eventName, bundle);
  }

  public void setUserProperty(String key, String value) {
    mFirebaseAnalytics.setUserProperty(key, value);
  }

  public void setDefaultEventParams(Bundle bundle) {
    mFirebaseAnalytics.setDefaultEventParameters(bundle);
  }
}
