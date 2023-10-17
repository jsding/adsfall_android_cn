
package com.vivo.paydemo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VivoSignUtils {

    private static final String TAG = "VivoSignUtils";

    private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static final String CHARSET_UTF8 = "UTF-8";

    private final static String KEY_SIGN = "sign";
    private final static String KEY_SIGN_TYPE = "signType";

    private static final String QSTRING_EQUAL = "=";
    private static final String QSTRING_SPLIT = "&";

    /**
     * verify the payment signature
     *
     * @param para payment parameters
     * @param key  app secret
     * @return verify result
     */
    public static boolean verifySignature(Map<String, String> para, String key) {
        Map<String, String> filteredReq = paraFilter(para);
        String signature = getVivoSign(filteredReq, key);
        String respSignature = para.get(KEY_SIGN);
        if (null != respSignature && respSignature.equals(signature)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * create the payment signature
     *
     * @param para payment parameters
     * @param key  app secret
     * @return signature
     */
    public static String getVivoSign(Map<String, String> para, String key) {
        Map<String, String> filteredReq = paraFilter(para);

        String prestr = createLinkString(filteredReq, true, false); // 得到待签名字符串

        prestr = prestr + QSTRING_SPLIT + md5Hex(key);

        return md5Hex(prestr);
    }

    private static Map<String, String> paraFilter(Map<String, String> para) {
        Map<String, String> result = new HashMap<String, String>();

        if (para == null || para.size() <= 0) {
            return result;
        }

        for (String key : para.keySet()) {
            String value = para.get(key);
            if (value == null || value.equals("") || key.equalsIgnoreCase(KEY_SIGN) || key.equalsIgnoreCase
                    (KEY_SIGN_TYPE)) {
                continue;
            }
            result.put(key, value);
        }

        return result;
    }

    private static String createLinkString(Map<String, String> para, boolean sort, boolean encode) {
        List<String> keys = new ArrayList<String>(para.keySet());

        if (sort)
            Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = para.get(key);

            if (encode) {
                try {
                    value = URLEncoder.encode(value, "utf-8");
                } catch (UnsupportedEncodingException e) {
                }
            }

            if (i == keys.size() - 1) {
                sb.append(key).append(QSTRING_EQUAL).append(value);
            } else {
                sb.append(key).append(QSTRING_EQUAL).append(value).append(QSTRING_SPLIT);
            }
        }

        return sb.toString();
    }

    private static String md5Hex(String data) {
        if (data == null || data.trim().equals("")) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            byte[] md5Data = digest.digest(data.getBytes(CHARSET_UTF8));
            return byteToHexString(md5Data);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String byteToHexString(byte[] data) {
        char[] buf = new char[data.length * 2];
        int index = 0;
        for (byte b : data) {
            buf[index++] = HEX_CHAR[b >>> 4 & 0xf];
            buf[index++] = HEX_CHAR[b & 0xf];
        }
        return new String(buf);
    }

}
