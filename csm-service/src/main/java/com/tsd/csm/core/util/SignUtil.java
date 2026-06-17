package com.tsd.csm.core.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * 出站调用签名工具：对参数按 key 字典序拼接后做 HMAC-SHA256（呼应 xuqiu.md 2.2）。
 */
public final class SignUtil {

    private SignUtil() {
    }

    /** 用 app_secret 对参数签名，返回小写 hex 字符串。 */
    public static String sign(Map<String, ?> params, String appSecret) {
        TreeMap<String, ?> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> {
            if (v != null) {
                sb.append(k).append('=').append(v).append('&');
            }
        });
        if (!sb.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        return hmacSha256(sb.toString(), appSecret);
    }

    public static boolean verify(Map<String, ?> params, String appSecret, String signature) {
        return sign(params, appSecret).equalsIgnoreCase(signature);
    }

    private static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("签名计算失败", e);
        }
    }
}
