package com.ivy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.UrlQuerySanitizer;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.ivy.util.CommonUtil;
import com.ivy.util.Logger;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class IvyUtils {
  public static final String GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=";

  public static boolean isOnline(Context c) {
    if (c == null) {
      return true;
    }
    try {
      if (ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
        Object o = c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (o != null) {
          NetworkInfo netInfo = ((ConnectivityManager) o).getActiveNetworkInfo();
          return netInfo != null && netInfo.isConnectedOrConnecting();
        }
      }
    } catch (Throwable t) {
      // ignore
    }
    return true;
  }


  public static boolean isFacebookInstalled(@NonNull Context context) {
    PackageManager pm = context.getPackageManager();
    boolean flag = false;
    try {
      pm.getPackageInfo("com.facebook.katana", PackageManager.GET_ACTIVITIES);
      flag = true;
    } catch (Throwable ignored) {
    }
    return flag;
  }

  public static String readSecureText(Context context, String fileName) {
    try {
      InputStream is = openAsset(context, fileName);
      return decode(is);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private static String decode(InputStream is) throws Exception {
    DataInputStream in = new DataInputStream(is);
    int seed = in.readByte();
    int len = in.readInt();
    int fileLen = in.readInt();
    byte[] data = new byte[fileLen];
    in.read(data);
    for (int i = 0, n = data.length; i < n; i++) {
      int tmp = data[i];
      if (len-- > 0) {
        int t = tmp + seed;
        tmp = t > 255 && tmp >= 128 ? tmp : tmp - seed;
      }
      data[i] = (byte) tmp;
    }
    in.close();
    return new String(data).trim();
  }

  public static String readSecureTextByKeystore(Context context, String content) {
    try {
      if (content != null) {
        return CommonUtil.decodeParams(context, content.getBytes());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return content;
  }

  public static String md5(String content) {
    try {
      byte[] bytes = MessageDigest.getInstance("md5").digest(content.getBytes());
      StringBuilder ret = new StringBuilder(bytes.length << 1);
      for (byte aByte : bytes) {
        ret.append(Character.forDigit((aByte >> 4) & 0xf, 16));
        ret.append(Character.forDigit(aByte & 0xf, 16));
      }
      return ret.toString();
    } catch (Exception exp) {
      return content;
    }
  }

  public static InputStream openAsset(Context context, String fileName) {
    try {
      return context.getAssets().open(fileName);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static String streamToString(InputStream is) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024 * 4];
      int len = 0;
      while ((len = is.read(buffer)) != -1) {
        baos.write(buffer, 0, len);
      }
      baos.flush();
      baos.close();
      is.close();
      return baos.toString();
    } catch (Exception e) {
      return null;
    }
  }

  private static String fixUrl(Context context, String url, String tag) {
    url = appendReferrer(context, url, tag);
    if (url.startsWith("http")) {
      return url;
    } else {
      return GOOGLE_PLAY_URL + url;
    }
  }

  private static boolean isPlayStoreUrl(String url) {
    return url.startsWith(GOOGLE_PLAY_URL);
  }

  private static String appendReferrer(Context context, String url, String tag) {
    if (!url.contains("&referrer")) {
      return url + "&referrer=utm_source%3D" + "ivy" +
        "%26utm_campaign%3D" + context.getPackageName() +
        "%26utm_medium%3D" + tag +
        "%26utm_term%3D" + tag +
        "%26utm_content%3D" + tag;
    } else {
      return url;
    }
  }

  public static boolean hasApp(Context context, String packageName) {
    try {
      int playStoreEnabled = context.getPackageManager().getApplicationEnabledSetting(packageName);
      return playStoreEnabled == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT || playStoreEnabled == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    } catch (Exception ignore) {
      return false;
    }
  }

  public static boolean hasPlayStore(Context context) {
    return hasApp(context, "com.android.vending");
  }

  public static void openBrowser(@NonNull Context context, String url) {
    Intent i = new Intent(Intent.ACTION_VIEW);
    launchBrowser(context, url, i);
  }

  public static void openPlayStore(Context context, String pkg, String referrer, JSONObject app) {
    if (context == null) {
      return;
    }

    Intent i = new Intent(Intent.ACTION_VIEW);
    if (app != null && app.has("url")) {
      String linkUrl = app.optString("url");
      if (!"".equals(linkUrl)) {
        Logger.debug("ADSFALL", "Open link: " + linkUrl);
        try {
          i.setData(Uri.parse(linkUrl));
          context.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT));
          return;
        } catch (Throwable t) {
          // ignore
        }
      }
    }

    String url = fixUrl(context, pkg, referrer);
    boolean isGooglePlay = isPlayStoreUrl(url);

    if (isGooglePlay) {
      if (hasPlayStore(context)) {
        launchPlayStore(context, url, i);
      } else {
        launchBrowser(context, url, i);
      }
    } else {
      launchBrowser(context, url, i);
    }
  }

  private static void launchPlayStore(Context context, String url, Intent i) {
    final String marketUrl = "market://details?id=";
    url = url.replace(GOOGLE_PLAY_URL, marketUrl);
    i.setPackage("com.android.vending");
    i.setData(Uri.parse(url));
    // launchApp(context, i);
    try {
      context.startActivity(i);
    } catch (Exception e) {

    }
  }

  private static void launchApp(Context context, Intent i) {
    try {
      context.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void launchBrowser(Context context, String url, Intent i) {
    i.setData(Uri.parse(url));
    String browserPackageName = getDefaultBrowserPackageName(context, i);
    if (browserPackageName != null) {
      i.setPackage(browserPackageName);
    }
    launchApp(context, i);
  }

  private static String getDefaultBrowserPackageName(Context context, Intent intent) {
    PackageManager packageManager = context.getPackageManager();
    @SuppressLint("QueryPermissionsNeeded") List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
    if (resolveInfos.size() > 0) {
      ResolveInfo info = resolveInfos.get(0);
      return info.activityInfo.packageName;
    } else {
      return null;
    }
  }

  public static JSONObject getUrlParametersWithJson(String url) {
    try {
      JSONObject object = new JSONObject();
      UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
      sanitizer.setAllowUnregisteredParamaters(true);
      sanitizer.parseUrl(url);
      for (UrlQuerySanitizer.ParameterValuePair pair : sanitizer.getParameterList()) {
        object.put(pair.mParameter, pair.mValue);
      }
      return object;
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return null;
  }

  private static Map<String, String> EMOJI_MAP = null;

  public static String replaceEmojiText(@NonNull String text) {
    if (EMOJI_MAP == null) {
      EMOJI_MAP = new HashMap<>(74);
      EMOJI_MAP.put(":e1:", "\ud83d\ude03");
      EMOJI_MAP.put(":e2:", "\ud83d\ude02");
      EMOJI_MAP.put(":e3:", "\ud83d\ude05");
      EMOJI_MAP.put(":e4:", "\ud83d\ude06");
      EMOJI_MAP.put(":e5:", "\ud83d\ude08");
      EMOJI_MAP.put(":e6:", "\ud83d\ude09");
      EMOJI_MAP.put(":e7:", "\ud83d\ude0a");
      EMOJI_MAP.put(":e8:", "\ud83d\ude0b");
      EMOJI_MAP.put(":e9:", "\ud83d\ude0c");
      EMOJI_MAP.put(":e10:", "\ud83d\ude0d");

      EMOJI_MAP.put(":e11:", "\ud83d\ude0e");
      EMOJI_MAP.put(":e12:", "\ud83d\ude0f");
      EMOJI_MAP.put(":e13:", "\ud83d\ude13");
      EMOJI_MAP.put(":e14:", "\ud83d\ude18");
      EMOJI_MAP.put(":e15:", "\ud83d\ude1b");
      EMOJI_MAP.put(":e16:", "\ud83d\ude1c");
      EMOJI_MAP.put(":e17:", "\ud83d\ude1d");
      EMOJI_MAP.put(":e18:", "\ud83d\ude21");
      EMOJI_MAP.put(":e19:", "\ud83d\ude22");
      EMOJI_MAP.put(":e20:", "\ud83d\ude24");

      EMOJI_MAP.put(":e21:", "\ud83d\ude28");
      EMOJI_MAP.put(":e22:", "\ud83d\ude29");
      EMOJI_MAP.put(":e23:", "\ud83d\ude2a");
      EMOJI_MAP.put(":e24:", "\ud83d\ude2d");
      EMOJI_MAP.put(":e25:", "\ud83d\ude31");
      EMOJI_MAP.put(":e26:", "\ud83d\ude30");
      EMOJI_MAP.put(":e27:", "\ud83d\ude33");
      EMOJI_MAP.put(":e28:", "\ud83d\ude34");
      EMOJI_MAP.put(":e29:", "\ud83d\ude37");
      EMOJI_MAP.put(":e30:", "\ud83d\ude38");

      EMOJI_MAP.put(":e31:", "\ud83d\ude39");
      EMOJI_MAP.put(":e32:", "\ud83d\ude3b");
      EMOJI_MAP.put(":e33:", "\ud83d\ude3d");
      EMOJI_MAP.put(":e34:", "\ud83d\ude3f");
      EMOJI_MAP.put(":e35:", "\ud83d\ude40");
      EMOJI_MAP.put(":e36:", "\ud83d\ude48");
      EMOJI_MAP.put(":e37:", "\ud83d\ude4f");
      EMOJI_MAP.put(":e38:", "\ud83d\udc3d");
      EMOJI_MAP.put(":e39:", "\ud83d\udc3b");
      EMOJI_MAP.put(":e40:", "\ud83d\udc40");

      EMOJI_MAP.put(":e41:", "\ud83d\udc3e");
      EMOJI_MAP.put(":e42:", "\ud83d\udc51");
      EMOJI_MAP.put(":e43:", "\ud83d\udc93");
      EMOJI_MAP.put(":e44:", "\ud83d\udc94");
      EMOJI_MAP.put(":e45:", "\ud83d\udc95");
      EMOJI_MAP.put(":e46:", "\ud83d\udc96");
      EMOJI_MAP.put(":e47:", "\ud83d\udc98");
      EMOJI_MAP.put(":e48:", "\ud83d\udc9d");
      EMOJI_MAP.put(":e49:", "\ud83d\udc9e");
      EMOJI_MAP.put(":e50:", "\ud83d\udcb0");

      EMOJI_MAP.put(":e51:", "\ud83d\udce2");
      EMOJI_MAP.put(":e52:", "\ud83d\udce3");
      EMOJI_MAP.put(":e53:", "\ud83d\udcea");
      EMOJI_MAP.put(":e54:", "\ud83d\udcec");
      EMOJI_MAP.put(":e55:", "\ud83d\udcaa");
      EMOJI_MAP.put(":e56:", "\ud83d\udca6");
      EMOJI_MAP.put(":e57:", "\ud83d\udc7b");
      EMOJI_MAP.put(":e58:", "\ud83d\udc37");
      EMOJI_MAP.put(":e59:", "\ud83d\udc36");
      EMOJI_MAP.put(":e60:", "\ud83d\udc38");
      EMOJI_MAP.put(":e61:", "\ud83d\udc19");
      EMOJI_MAP.put(":e62:", "\ud83d\udc14");
      EMOJI_MAP.put(":e63:", "\ud83c\udfc6");
      EMOJI_MAP.put(":e64:", "\ud83c\udf85");
      EMOJI_MAP.put(":e65:", "\ud83c\udf83");
      EMOJI_MAP.put(":e66:", "\ud83c\udf81");
      EMOJI_MAP.put(":e67:", "\ud83c\udf84");
      EMOJI_MAP.put(":e68:", "\ud83c\udf39");
      EMOJI_MAP.put(":e69:", "\ud83c\udf3b");
      EMOJI_MAP.put(":e70:", "\ud83c\udf44");
      EMOJI_MAP.put(":e71:", "\ud83c\udf4a");
      EMOJI_MAP.put(":e72:", "\ud83c\udf4e");
      EMOJI_MAP.put(":e73:", "\ud83c\udf31");
      EMOJI_MAP.put(":e74:", "\ud83c\udf3e");
    }
    for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
      text = text.replaceAll(entry.getKey(), entry.getValue());
    }
    return text;
  }
}
