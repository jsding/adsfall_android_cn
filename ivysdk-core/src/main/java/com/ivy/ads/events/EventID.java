package com.ivy.ads.events;

public interface EventID {

  String BANNER_ADSFALL_DISPLAYED = "banner_af_displayed";

  String BANNER_DISPLAYED = "banner_displayed";
  String BANNER_CLICKED = "banner_clicked";

  String INTERSTITIAL_SHOW_FAILED = "interstitial_show_failed";
  String INTERSTITIAL_SHOWN = "interstitial_shown";
  String INTERSTITIAL_CLICKED = "interstitial_clicked";

  String INTERSTITIAL_ADSFALL_SHOW_FAILED = "interstitial_af_show_failed";
  String INTERSTITIAL_ADSFALL_SHOWN = "interstitial_af_shown";
  String INTERSTITIAL_ADSFALL_CLICKED = "interstitial_af_clicked";

  String VIDEO_FAILED = "video_failed";
  String VIDEO_SHOWN = "video_shown";
  String VIDEO_COMPLETED = "video_completed";

  String CLICK_SHOW_INTERSTITIAL = "click_show_interstitial";
  String CLICK_SHOW_REWARDED = "click_show_rewarded";
  String CLICK_SHOW_NATIVEAD = "click_show_nativead";
  String CLICK_SHOW_BANNER = "click_show_banner";

  String GAMEWALL_DISPLAYED = "gamewall_displayed";

  String QUIT_DISPLAYED = "quit_displayed";

  String IAP_PURCHASED = "iap_purchased";

  String FIRST_PURCHASE = "first_purchase";
  String IAP_CANCEL = "iap_cancel";

  String ROAS_LTV_PING = "gms_ad_paid_event";
  String GMS_AD_IMPRESSION_PING = "ad_impression_revenue";

  // facebook 分享成功
  String FB_SHARE = "fb_share";
}
