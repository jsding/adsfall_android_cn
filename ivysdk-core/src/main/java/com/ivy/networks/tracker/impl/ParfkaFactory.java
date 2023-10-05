package com.ivy.networks.tracker.impl;

import com.ivy.util.Logger;

public class ParfkaFactory {
  public static String apptoken;
  public static String baseUrl = "http://gateway-3.hierugo.com/event";

  public static void setApptoken(String apptoken) {
    ParfkaFactory.apptoken = apptoken;
  }

  public static void setBaseUrl(String baseUrl) {
    Logger.debug("Parfka", "set baseUrl " + baseUrl);
    ParfkaFactory.baseUrl = baseUrl;
  }
}
