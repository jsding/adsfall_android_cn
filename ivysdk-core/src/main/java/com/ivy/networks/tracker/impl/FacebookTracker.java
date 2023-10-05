package com.ivy.networks.tracker.impl;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.facebook.appevents.AppEventsConstants;
import com.facebook.appevents.AppEventsLogger;
import com.ivy.networks.tracker.EventTrackerProvider;
import com.ivy.util.Logger;

import java.math.BigDecimal;
import java.util.Currency;

public class FacebookTracker implements EventTrackerProvider {
  private static final String TAG = "FacebookTracker";
  private AppEventsLogger logger = null;
  private boolean suppress = false;

  @Override
  public void initialize(Context context) {
    try {
      logger = AppEventsLogger.newLogger(context);
    } catch (Throwable t) {
      Logger.error(TAG, "initialize facebooktracker exception", t);
    }
  }

  @Override
  public void setSuppress(boolean suppress) {
    this.suppress = suppress;
  }

  @Override
  public void setUserID(String userID) {
    try {
      if (logger != null) {
        AppEventsLogger.setUserID(userID);
      }
    } catch (Throwable t) {
      Logger.error(TAG, "setUserID exception", t);
    }
  }

  @Override
  public void setUserProperty(String key, String value) {
  }

  @Override
  public void logPurchase(String contentType, String contentId, String currency, float revenue) {
    if (logger != null) {
      try {
        Bundle parameters = new Bundle();
        if (contentId != null) {
          parameters.putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, contentId);
        }
        if (contentType != null) {
          parameters.putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, contentType);
        }
        logger.logPurchase(new BigDecimal(revenue), Currency.getInstance(currency));
      } catch (Throwable t) {
        Logger.error(TAG, "logPurchase", t);
      }
    }
  }

  @Override
  public void logEvent(String eventName, Bundle bundle) {
    if (suppress || logger == null) {
      return;
    }
    try {
      logger.logEvent(eventName, bundle);
    } catch (Throwable t) {
      Logger.error(TAG, "logEvent", t);
    }
  }
}
