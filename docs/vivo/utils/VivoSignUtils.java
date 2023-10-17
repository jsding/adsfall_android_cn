/**
 * 验签工具类，调用getVivoSign可生成验签
 */

package com.vivo.unionpay.paydemo.utils;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VivoSignUtils {

    private static final String TAG = "VivoSignUtils";

    private static final char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f' };
    public static final String SIGN_ALGORITHMS = "SHA256WithRSA";
    private static final String CHARSET_UTF8 = "UTF-8";

    private final static String KEY_SIGN = "sign";
    private final static String KEY_SIGN_TYPE = "signType";

    private static final String QSTRING_EQUAL = "=";
    private static final String QSTRING_SPLIT = "&";

    /**
     * 验签
     *
     * @param para
     *            参数集
     * @param key
     *            密钥（AppSecret）
     * @return 验签结果
     */
    public static boolean verifySignature(Map<String, String> para, String key) {

        // 除去数组中的空值和签名参数
        Map<String, String> filteredReq = paraFilter(para);
        // 根据参数获取vivo签名
        String signature = getVivoSign(filteredReq, key);
        // 获取参数中的签名值
        String respSignature = para.get(KEY_SIGN);
        // 对比签名值
        if (null != respSignature && respSignature.equals(signature)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取md5签名
     *
     * @param para
     *            参与签名的要素<key,value>
     * @param key
     *            密钥(AppSecret)
     * @return 签名结果
     */
    public static String getVivoSign(Map<String, String> para, String key) {

        // 除去数组中的空值和签名参数
        Map<String, String> filteredReq = paraFilter(para);
        String prestr = createLinkString(filteredReq, true, false); // 得到待签名字符串
        // 需要对map进行sort，不需要对value进行URL编码
        prestr = prestr + QSTRING_SPLIT + md5Hex(key);
        return md5Hex(prestr);
    }

    /**
     * 获取RSA签名
     *
     * @param para
     *            参与签名的要素
     * @param key
     *            RSA密钥
     * @return签名结果
     */
    public static String getVivoRsaSign(Map<String, String> para, String key) {

        Map<String, String> filteredReq = paraFilter(para);
        String prestr = createLinkString(filteredReq, true, false); // 得到待签名字符串
        return sign(prestr, key, CHARSET_UTF8);
    }

    /**
     * 除去请求要素中的空值和签名参数
     *
     * @param para
     *            请求要素
     * @return 去掉空值与签名参数后的请求要素
     */
    private static Map<String, String> paraFilter(Map<String, String> para) {

        Map<String, String> result = new HashMap<String, String>();

        if (para == null || para.size() <= 0) {
            return result;
        }

        for (String key : para.keySet()) {
            String value = para.get(key);
            if (value == null || value.equals("") || key.equalsIgnoreCase(KEY_SIGN)
                    || key.equalsIgnoreCase(KEY_SIGN_TYPE)) {
                continue;
            }
            result.put(key, value);
        }

        return result;
    }

    /**
     * 把请求要素按照“参数=参数值”的模式用“&”字符拼接成字符串
     *
     * @param para
     *            请求要素
     * @param sort
     *            是否需要根据key值作升序排列
     * @param encode
     *            是否需要URL编码
     * @return 拼接成的字符串
     */
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

            if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
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
        for (byte b : data) { // 利用位运算进行转换
            buf[index++] = HEX_CHAR[b >>> 4 & 0xf];
            buf[index++] = HEX_CHAR[b & 0xf];
        }
        return new String(buf);
    }

    public static String sign(String content, String privateKey, String encode) {

        try {
            PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.DEFAULT));
            KeyFactory keyf = KeyFactory.getInstance("RSA");
            PrivateKey priKey = keyf.generatePrivate(priPKCS8);
            java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);

            signature.initSign(priKey);
            signature.update(content.getBytes(encode));
            byte[] signed = signature.sign();

            return replaceBlank(Base64.encodeToString(signed, Base64.DEFAULT));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String replaceBlank(String src) {

        String dest = "";
        if (src != null) {
            Pattern pattern = Pattern.compile("\t|\r|\n|\\s*");
            Matcher matcher = pattern.matcher(src);
            dest = matcher.replaceAll("");
        }
        return dest;
    }

}
