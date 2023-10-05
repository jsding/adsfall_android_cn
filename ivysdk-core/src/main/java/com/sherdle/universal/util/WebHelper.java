package com.sherdle.universal.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.view.View;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class WebHelper {

  public static String docToBetterHTML(Document doc, Context c) {
    return docToBetterHTML(doc, c, true);
  }

  @SuppressLint("NewApi")
  public static String docToBetterHTML(Document doc, Context c, boolean noMargins) {
    try {
      doc.select("img[src]").removeAttr("width");
    } catch (Exception e) {
      Log.printStackTrace(e);
    }
    try {
      doc.select("a[href]").removeAttr("style");
    } catch (Exception e) {
      Log.printStackTrace(e);
    }
    try {
      doc.select("div").removeAttr("style");
    } catch (Exception e) {
      Log.printStackTrace(e);
    }
    try {
      doc.select("iframe").attr("width", "100%");
    } catch (Exception e) {
      Log.printStackTrace(e);
    }
    try {
      doc.select("iframe").removeAttr("height");
    } catch (Exception e) {
      Log.printStackTrace(e);
    }

    String rtl;

    int currentapiVersion = android.os.Build.VERSION.SDK_INT;
    Configuration config = c.getResources().getConfiguration();
    if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
      rtl = "direction:RTL; unicode-bidi:embed;";
    } else {
      rtl = "";
    }

    String style = "<style>" +
      "img{" +
      "max-width: 100%; " +
      "width: auto; height: auto;" +
      "} p{" +
      "max-width: 100%; " +
      "width:auto; " +
      "height: auto;" +
      "}" +
      " body p {  " +
      ".list-inline {" +
      "display: inline;" +
      "list-style: none;" +
      "} body {  " +
      "max-width: 100% !important;" +
      (noMargins ?
        ("margin: 0;" +
          "padding: 0;") :
        "") +
      rtl +
      "}" +
      "</style>";

    Element header = doc.head();
    header.append(style);

    String html = doc.toString();
    return html;
  }

  public static int getWebViewFontSize(Context c) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
    String prefList = sharedPreferences.getString("fontSize", "16");
    return Integer.parseInt(prefList);
  }

  public static int getTextViewFontSize(Context c) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
    String prefList = sharedPreferences.getString("fontSize", "16");
    int number = Integer.parseInt(prefList);
    if (number >= 16)
      number = number - 2;
    else if (number == 7) {
      number = number + 1;
    } else if (number < 16) {
      number = number - 1;
    }
    return number;
  }
}