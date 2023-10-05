package com.ivy.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.LocaleList;
import android.util.DisplayMetrics;

import com.ivy.IvySdk;
import com.ivy.networks.tracker.impl.ParfkaFactory;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceInfo {
  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.US);

  public String playAdId;
  public String playAdIdSource;
  public Boolean isTrackingEnabled;
  private boolean nonGoogleIdsReadOnce = false;
  public String macSha1;
  public String macShortMd5;
  public String androidId;
  public String fbAttributionId;
  public String clientSdk;
  public String packageName;
  public String appVersion;
  public String deviceType;
  public String deviceName;
  public String deviceManufacturer;
  public String osName;
  public String osVersion;
  public int apiLevel;
  public String language;
  public String country;
  public String displayWidth;
  public String displayHeight;
  public String hardwareName;
  public String appInstallTime;
  public String appUpdateTime;

  public long appInstallTimestamp;
  public long appUpdateTimestamp;


  public DeviceInfo(Context context) {
    try {
      Resources resources = context.getResources();
      DisplayMetrics displayMetrics = resources.getDisplayMetrics();
      Configuration configuration = resources.getConfiguration();
      Locale locale = getLocale(configuration);
      int screenLayout = configuration.screenLayout;

      packageName = getPackageName(context);
      appVersion = getAppVersion(context);
      deviceType = getDeviceType(screenLayout);
      deviceName = getDeviceName();
      deviceManufacturer = getDeviceManufacturer();
      osName = getOsName();
      osVersion = getOsVersion();
      apiLevel = getApiLevel();
      if (locale != null) {
        language = getLanguage(locale);
        country = getCountry(locale);
      }
      displayWidth = getDisplayWidth(displayMetrics);
      displayHeight = getDisplayHeight(displayMetrics);
      clientSdk = getClientSdk();
      fbAttributionId = getFacebookAttributionId(context);
      hardwareName = getHardwareName();
      appInstallTime = getAppInstallTime(context);
      appUpdateTime = getAppUpdateTime(context);

      appInstallTimestamp = getAppInstallTimestamp(context) / 1000;
      appUpdateTimestamp = getAppUpdateTimestamp(context) / 1000;
    } catch(Throwable t) {
      Logger.error("Parfka", "Device Info created exception", t);
    }
  }

  private String getPackageName(Context context) {
    return context.getPackageName();
  }

  private String getAppVersion(Context context) {
    try {
      PackageManager packageManager = context.getPackageManager();
      String name = context.getPackageName();
      PackageInfo info = packageManager.getPackageInfo(name, 0);
      return info.versionName;
    } catch (Exception e) {
      return null;
    }
  }

  private String getDeviceType(int screenLayout) {
    int screenSize = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

    switch (screenSize) {
      case Configuration.SCREENLAYOUT_SIZE_SMALL:
      case Configuration.SCREENLAYOUT_SIZE_NORMAL:
        return "phone";
      case Configuration.SCREENLAYOUT_SIZE_LARGE:
      case 4:
        return "tablet";
      default:
        return null;
    }
  }

  private String getDeviceName() {
    return Build.MODEL;
  }

  private String getDeviceManufacturer() {
    return Build.MANUFACTURER;
  }

  private String getOsName() {
    return "android";
  }

  private String getOsVersion() {
    return Build.VERSION.RELEASE;
  }

  private int getApiLevel() {
    return Build.VERSION.SDK_INT;
  }

  private String getLanguage(Locale locale) {
    return locale.getLanguage();
  }

  private String getCountry(Locale locale) {
    return locale.getCountry();
  }

  private String getBuildName() {
    return Build.ID;
  }

  private String getHardwareName() {
    return Build.DISPLAY;
  }


  private String getDisplayWidth(DisplayMetrics displayMetrics) {
    return String.valueOf(displayMetrics.widthPixels);
  }

  private String getDisplayHeight(DisplayMetrics displayMetrics) {
    return String.valueOf(displayMetrics.heightPixels);
  }

  private String getClientSdk() {
    return "adsfall";
  }

  private String getFacebookAttributionId(final Context context) {
    try {
      final ContentResolver contentResolver = context.getContentResolver();
      final Uri uri = Uri.parse("content://com.facebook.katana.provider.AttributionIdProvider");
      final String columnName = "aid";
      final String[] projection = {columnName};
      final Cursor cursor = contentResolver.query(uri, projection, null, null, null);

      if (cursor == null) {
        return null;
      }
      if (!cursor.moveToFirst()) {
        cursor.close();
        return null;
      }

      int columnIndex = cursor.getColumnIndex(columnName);
      String attributionId = null;
      if (columnIndex >= 0) {
        attributionId = cursor.getString(columnIndex);
      }
      cursor.close();
      return attributionId;
    } catch (Exception e) {
      return null;
    }
  }


  private long getAppInstallTimestamp(Context context) {
    try {
      PackageManager packageManager = context.getPackageManager();
      PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);

      return packageInfo.firstInstallTime;
    } catch (Exception ex) {
      return 0L;
    }
  }

  private long getAppUpdateTimestamp(Context context) {
    try {
      PackageManager packageManager = context.getPackageManager();
      PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
      return packageInfo.lastUpdateTime;
    } catch (Exception ex) {
      return 0L;
    }
  }


  private String getAppInstallTime(Context context) {
    try {
      PackageManager packageManager = context.getPackageManager();
      PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);

      String appInstallTime = dateFormatter.format(new Date(packageInfo.firstInstallTime));

      return appInstallTime;
    } catch (Exception ex) {
      return null;
    }
  }

  public static Locale getLocale(Configuration configuration) {
    // Configuration.getLocales() added as of API 24.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      LocaleList localesList = configuration.getLocales();
      if (localesList != null && !localesList.isEmpty()) {
        return localesList.get(0);
      }
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      return configuration.locale;
    }
    return null;
  }

  private String getAppUpdateTime(Context context) {
    try {
      PackageManager packageManager = context.getPackageManager();
      PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);

      return dateFormatter.format(new Date(packageInfo.lastUpdateTime));
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * inject the deviceInfo into json object
   * @param json
   */
  public void injectJson(JSONObject json) {
    try {
      json.put("package_name", packageName);
      json.put("android_uuid", IvySdk.getUUID());
      json.put("app_token", ParfkaFactory.apptoken);
      json.put("api_level", apiLevel);
      if (country != null) {
        json.put("country", country);
      }
      json.put("device_manufacturer", deviceManufacturer);
      json.put("device_name", deviceName);
      json.put("device_type", deviceType);
      json.put("os_name", osName);
      json.put("os_version", osVersion);
      json.put("language", language);
      json.put("app_version", appVersion);
      if (fbAttributionId != null) {
        json.put("fb_id", fbAttributionId);
      }
      json.put("installed_at", appInstallTime);
      json.put("it", appInstallTimestamp);
      json.put("ut", appUpdateTimestamp);
    } catch(Throwable t) {
      Logger.error("Parfka", "injectJson exception", t);
    }
  }
}
