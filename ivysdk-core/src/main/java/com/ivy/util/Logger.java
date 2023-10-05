package com.ivy.util;

import android.util.Log;

public class Logger {
  public static final int VERBOSE = 2;
  public static final int DEBUG = 3;
  public static final int INFO = 4;
  public static final int WARNING = 5;
  public static final int ERROR = 6;
  public static final int NONE = 8;
  private static int logLevel = VERBOSE;

  public static void disableLogging() {
    logLevel = NONE;
  }

  public static void enableLogging() {
    logLevel = VERBOSE;
  }

  public static void debug(String message) {
    if (logLevel == NONE) {
      return;
    }
    doLog(DEBUG, null, message, null);
  }

  public static void debug(String tag, String message) {
    if (logLevel == NONE) {
      return;
    }
    doLog(DEBUG, tag, message, null);
  }

  public static void debug(String tag, String message, Throwable e) {
    if (logLevel == NONE) {
      return;
    }
    doLog(DEBUG, tag, message, new Object[]{e});
  }

  public static void debug(String message, Object param) {
    if (logLevel == NONE) {
      return;
    }
    doLog(DEBUG, null, message, new Object[]{param});
  }

  public static void debug(String message, Object param1, Object param2) {
    if (logLevel == NONE) {
      return;
    }
    doLog(DEBUG, null, message, new Object[]{param1, param2});
  }

  public static void debug(String message, Object... params) {
    if (logLevel == NONE) {
      return;
    }
    doLog(DEBUG, null, message, params);
  }

  public static void debug(String tag, String message, Object param) {
    if (logLevel == NONE) {
      return;
    }
    doLog(DEBUG, tag, message, new Object[]{param});
  }

  public static void debug(String tag, String message, Object param1, Object param2) {
    if (logLevel == NONE) {
      return;
    }
    doLog(DEBUG, tag, message, new Object[]{param1, param2});
  }

  public static void debug(String tag, String message, Object... params) {
    if (logLevel == NONE) {
      return;
    }
    doLog(DEBUG, tag, message, params);
  }

  public static void info(String message) {
    if (logLevel == NONE) {
      return;
    }
    doLog(INFO, null, message, null);
  }

  public static void info(String tag, String message) {
    if (logLevel == NONE) {
      return;
    }
    doLog(INFO, tag, message, null);
  }

  public static void info(String tag, String message, Throwable e) {
    if (logLevel == NONE) {
      return;
    }
    doLog(INFO, tag, message, new Object[]{e});
  }

  public static void info(String message, Object param) {
    if (logLevel == NONE) {
      return;
    }
    doLog(INFO, null, message, new Object[]{param});
  }

  public static void info(String message, Object param1, Object param2) {
    if (logLevel == NONE) {
      return;
    }
    doLog(INFO, null, message, new Object[]{param1, param2});
  }

  public static void info(String message, Object... params) {
    if (logLevel == NONE) {
      return;
    }
    doLog(INFO, null, message, params);
  }

  public static void info(String tag, String message, Object param) {
    if (logLevel == NONE) {
      return;
    }
    doLog(INFO, tag, message, new Object[]{param});
  }

  public static void info(String tag, String message, Object param1, Object param2) {
    if (logLevel == NONE) {
      return;
    }
    doLog(INFO, tag, message, new Object[]{param1, param2});
  }

  public static void info(String tag, String message, Object... params) {
    if (logLevel == NONE) {
      return;
    }
    doLog(INFO, tag, message, params);
  }

  public static void warning(String message) {
    if (logLevel == NONE) {
      return;
    }
    doLog(WARNING, null, message, null);
  }

  public static void warning(String tag, String message) {
    if (logLevel == NONE) {
      return;
    }
    doLog(WARNING, tag, message, null);
  }

  public static void warning(String tag, String message, Throwable e) {
    if (logLevel == NONE) {
      return;
    }
    doLog(WARNING, tag, message, new Object[]{e});
  }

  public static void warning(String message, Object param) {
    if (logLevel == NONE) {
      return;
    }
    doLog(WARNING, null, message, new Object[]{param});
  }

  public static void warning(String message, Object param1, Object param2) {
    if (logLevel == NONE) {
      return;
    }
    doLog(WARNING, null, message, new Object[]{param1, param2});
  }

  public static void warning(String message, Object... params) {
    if (logLevel == NONE) {
      return;
    }
    doLog(WARNING, null, message, params);
  }

  public static void warning(String tag, String message, Object param) {
    if (logLevel == NONE) {
      return;
    }
    doLog(WARNING, tag, message, new Object[]{param});
  }

  public static void warning(String tag, String message, Object param1, Object param2) {
    if (logLevel == NONE) {
      return;
    }
    doLog(WARNING, tag, message, new Object[]{param1, param2});
  }

  public static void warning(String tag, String message, Object... params) {
    if (logLevel == NONE) {
      return;
    }
    doLog(WARNING, tag, message, params);
  }

  public static void error(String message) {
    if (logLevel == NONE) {
      return;
    }
    doLog(ERROR, null, message, null);
  }

  public static void error(String tag, String message) {
    if (logLevel == NONE) {
      return;
    }
    doLog(ERROR, tag, message, null);
  }

  public static void error(String tag, String message, Throwable e) {
    if (logLevel == NONE) {
      return;
    }
    doLog(ERROR, tag, message, new Object[]{e});
  }

  public static void error(String message, Object param) {
    if (logLevel == NONE) {
      return;
    }
    doLog(ERROR, null, message, new Object[]{param});
  }

  public static void error(String message, Object param1, Object param2) {
    if (logLevel == NONE) {
      return;
    }
    doLog(ERROR, null, message, new Object[]{param1, param2});
  }

  public static void error(String message, Object... params) {
    if (logLevel == NONE) {
      return;
    }
    doLog(ERROR, null, message, params);
  }

  public static void error(String tag, String message, Object param) {
    if (logLevel == NONE) {
      return;
    }
    doLog(ERROR, tag, message, new Object[]{param});
  }

  public static void error(String tag, String message, Object param1, Object param2) {
    if (logLevel == NONE) {
      return;
    }
    doLog(ERROR, tag, message, new Object[]{param1, param2});
  }

  public static void error(String tag, String message, Object... params) {
    if (logLevel == NONE) {
      return;
    }
    doLog(ERROR, tag, message, params);
  }

  private static Throwable extractThrowable(Object[] params) {
    if (params == null || params.length == 0) {
      return null;
    }
    Object last = params[params.length - 1];
    return last instanceof Throwable ? (Throwable) last : null;
  }

  private static Object[] trimParams(Object[] params) {
    if (params == null || params.length == 0) {
      throw new IllegalArgumentException("params is null or empty");
    }
    Object[] trimmedParams = new Object[(params.length - 1)];
    System.arraycopy(params, 0, trimmedParams, 0, trimmedParams.length);
    return trimmedParams;
  }

  private static String formatTag(String tag) {
    return "-" + tag + ": " + Thread.currentThread().getName();
  }

  private static void doLog(int level, String tag, String message, Object[] params) {
    Throwable throwable = extractThrowable(params);
    if (throwable != null) {
      params = trimParams(params);
    }
    String formattedMsg = null;
    if (message != null && params != null) {
      formattedMsg = String.format(message, params);
    } else if (message != null) {
      formattedMsg = message;
    }
    String finalTag = tag != null ? formatTag(tag) : "";
    finalTag = "ADSFALL-" + finalTag;
    if (throwable == null) {
      androidLog(level, finalTag, formattedMsg);
    } else {
      androidLog(level, finalTag, formattedMsg, throwable);
    }
  }

  private static void androidLog(int level, String finalTag, String msgWithTrace, Throwable throwable) {
    if (msgWithTrace == null) {
      return;
    }

    if (level == ERROR) {
      Log.e(finalTag, msgWithTrace, throwable);
    } else {
      Log.w(finalTag, msgWithTrace, throwable);
    }
  }

  private static void androidLog(int level, String finalTag, String msgWithTrace) {
    if (msgWithTrace == null) {
      return;
    }
    if (level == ERROR) {
      Log.e(finalTag, msgWithTrace);
    } else {
      Log.w(finalTag, msgWithTrace);
    }
  }
}
