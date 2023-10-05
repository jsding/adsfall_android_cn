package com.sherdle.universal;

import com.adsfall.R;
import com.sherdle.universal.drawer.SimpleMenu;
import com.sherdle.universal.util.ViewModeUtils;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2020
 */

public class Config {

  /**
   * The Config.json file that defines your app's content.
   * Point url to JSON or leave empty to use config.json from assets.
   */

  public static final String CONFIG_URL = "https://cdn.lisgame.com/app/config.json";

  /**
   * Options regarding the link behaviour in your app
   */

  //Explicit links, like 'open' buttons, should be opened outside the app
  public static final boolean OPEN_EXPLICIT_EXTERNAL = true;
  //Inline links, like links in descriptions, should be opened outside the app
  public static final boolean OPEN_INLINE_EXTERNAL = false;

  /**
   * Options related to how your app looks
   */
  //Whether users can edit the ViewMode of a list
  public static final boolean EDITABLE_VIEWMODE = false;
  //If a white background and black foreground for toolbar should be used
  public static boolean LIGHT_TOOLBAR_THEME = true;

  //Hide the navigation drawer
  public static final boolean HIDE_DRAWER = false;
  //If a tablet layout should be used on tablets, or just a regular layout
  public static final boolean TABLET_LAYOUT = true;
  //Force show menu on app start
  public static final boolean DRAWER_OPEN_START = false;
  //Hide the toolbar (will disable access to toolbar items)
  public static final boolean HIDE_TOOLBAR = false;
  //Whether the toolbar should hide on scroll
  public static final boolean HIDING_TOOLBAR = true;

  //Whether tabs should be shown at the top or bottom
  public static boolean BOTTOM_TABS = false;

  /**
   * Options related to the content providers
   */
  //Show category chips in WordPress
  public static final boolean WP_CHIPS = true;
  //Show a attachments fab for WordPress posts (requires header image)
  public static final boolean WP_ATTACHMENTS_BUTTON = false;
  //Row layout to use with Wordpress
  public static final int WP_ROW_MODE = ViewModeUtils.NORMAL;
  //Wordpress REST API url / site ID for push notifications
  public static final String NOTIFICATION_BASEURL = "";

  //Use large row layout
  public static final int RSS_ROW_MODE = ViewModeUtils.NORMAL;

  //Show category chips in WooCommerce
  public static final boolean WC_CHIPS = true;
  //The currency to use for WooCommerce
  public static final String WC_CURRENCY = "$";
  //Use large row layout
  public static final int WC_ROW_MODE = ViewModeUtils.NORMAL;

  //Row layout to use for Videos, such as Youtube and Vimeo
  public static final int VIDEOS_ROW_MODE = ViewModeUtils.NORMAL;

  //If the WebView navigation buttons should be hidden
  public static final boolean FORCE_HIDE_NAVIGATION = false;
  //If the WebView should support Geolocation
  public static final boolean WEBVIEW_GEOLOCATION = false;
  //All urls that should always be opened outside the WebView and in the browser, download manager, or their respective app
  public static final String[] OPEN_OUTSIDE_WEBVIEW = new String[]{"market://", "play.google.com", "onelink.me", "plus.google.com", "mailto:", "tel:", "vid:", "geo:", "whatsapp:", "sms:", "intent://"};
  //Defines a set of urls/patterns that should exlusively load inside the webview. All other urls are loaded outside the WebView. Ignored if no urls are defined.
  public static final String[] OPEN_ALL_OUTSIDE_EXCEPT = new String[]{};


  /**
   * Performance options
   */

  //For WP RESTAPI with video / audio, Universal will attempt to extract att. url from body first
  //For WP RESTAPI with regular posts, Universal will postpone att. requests until post is selected
  public static final boolean AVOID_SEPERATE_ATTACHMENT_REQUESTS = true;

  /**
   * Options regarding the use of a Hardcoded Configuration
   */

  //Will load configuration from hardcoded Config class instead of JSON.
  public static boolean USE_HARDCODED_CONFIG = false;

  //If you use a hardcoded config, initialise it below
  public static void configureMenu(SimpleMenu menu, ConfigParser.CallBack callback) {

        /*
        List<NavItem> tabs = new ArrayList<NavItem>();
        tabs.add(new NavItem("First Name", FirstFragment.class,
                new String[]{"parameter 1", "parameter 2"}));
         tabs.add(new NavItem("Second Name", SecondFragment.class,
                new String[]{"parameter 1", "parameter 2"}));
        menu.add("Menu Item", R.drawable.ic_details, tabs);
         **/

    //Return the configuration
    callback.configLoaded(false);
  }

}
