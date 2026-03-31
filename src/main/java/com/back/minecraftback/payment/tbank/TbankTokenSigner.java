package com.back.minecraftback.payment.tbank;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Подпись запросов/уведомлений интернет-эквайринга Т‑Банка (Tinkoff Acquiring v2):
 * корневые скалярные поля + Password → сортировка по ключу → конкатенация только значений → SHA-256 (hex).
 *
 * @see <a href="https://developer.tbank.ru/eacq/intro/developer/token">Документация: токен</a>
 */
public final class TbankTokenSigner {

    private TbankTokenSigner() {
    }

    public static String sign(Map<String, String> rootParamsWithoutTokenAndPassword, String password) {
        TreeMap<String, String> sorted = new TreeMap<>(rootParamsWithoutTokenAndPassword);
        sorted.put("Password", password);
        StringBuilder sb = new StringBuilder();
        for (String value : sorted.values()) {
            sb.append(value);
        }
        return sha256Hex(sb.toString());
    }

    public static boolean constantTimeEquals(String expectedHex, String actualHex) {
        if (expectedHex == null || actualHex == null || expectedHex.length() != actualHex.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedHex.length(); i++) {
            result |= expectedHex.charAt(i) ^ actualHex.charAt(i);
        }
        return result == 0;
    }

    private static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
