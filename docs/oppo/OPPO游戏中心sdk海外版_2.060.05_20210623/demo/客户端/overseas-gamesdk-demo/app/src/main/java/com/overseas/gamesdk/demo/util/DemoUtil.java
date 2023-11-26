package com.overseas.gamesdk.demo.util;

import android.text.TextUtils;

import java.lang.reflect.Method;

public class DemoUtil {
    public static String getRegionCurrent() {
        String region = getSystemProperties("persist.sys.oppo.region", "cn");
        if (TextUtils.isEmpty(region)) {
            region = getSystemProperties("ro.product.locale.region", "cn");
        }
        if (TextUtils.isEmpty(region)) {
            region = "cn";
        }
        return region;
    }

    private static String getSystemProperties(String key, String def) {
        Class c = getClassFromName("android.os.SystemProperties");
        return (String) invokeStatic(c, "get", new Class[]{String.class, String.class}, new Object[]{key, def});
    }

    private static Class getClassFromName(String className) {
        Class c = null;

        try {
            c = Class.forName(className);
        } catch (Throwable var3) {
        }

        return c;
    }

    private static Object invokeStatic(Class c, String methodName, Class[] paramType, Object[] paramValue) {
        if (c != null && !TextUtils.isEmpty(methodName)) {
            try {
                Method m = getMethod(c, methodName, paramType);
                if (m != null) {
                    m.setAccessible(true);
                    return m.invoke((Object) null, paramValue);
                }
            } catch (Throwable var5) {
            }

            return null;
        } else {
            return null;
        }
    }

    private static Method getMethod(Class c, String methodName, Class[] paramClass) {
        if (c != null && !TextUtils.isEmpty(methodName)) {
            Method m = null;

            try {
                m = c.getDeclaredMethod(methodName, paramClass);
            } catch (Exception var7) {
                try {
                    m = c.getMethod(methodName, paramClass);
                } catch (Exception var6) {
                    if (c.getSuperclass() == null) {
                        return m;
                    }

                    m = getMethod(c.getSuperclass(), methodName, paramClass);
                }
            }

            return m;
        } else {
            return null;
        }
    }
}
