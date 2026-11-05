// tạo lớp tiện ích cho chữ ký
package com.cinema.booking.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

public final class VinattiSigning {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private VinattiSigning() {
    }

    private static byte[] hmac(byte[] data, String secret) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        return mac.doFinal(data);
    }
    // dùng khi backend gửi lệnh sang gateway (CREATE_BILL, CHECK_STATUS, REFUND)
    // checksum = lowercase_hex(HMAC_SHA256(UTF8(secret), UTF8(rawData)))
    public static String gatewayChecksum(String rawData, String secret) throws GeneralSecurityException {
        if (rawData == null || secret == null) {
            throw new IllegalArgumentException("rawData và secret không được null");
        }
        return HexFormat.of().formatHex(hmac(rawData.getBytes(StandardCharsets.UTF_8), secret));
    }

    // dùng khi backend nhận webhook từ simulator (orderinfo, ordersync, refundsync)
    // xác thực header: Authorization: Bearer <Base64(HMAC_SHA256(secret, rawBodyBytes))>
    public static boolean verifyCallback(byte[] rawBody, String authorizationHeader, String secret) {
        if (rawBody == null || authorizationHeader == null || secret == null) {
            return false;
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            return false;
        }

        try {
            byte[] expectedHmac = hmac(rawBody, secret);
            byte[] receivedHmac = Base64.getDecoder().decode(authorizationHeader.substring(7).trim());
            // MessageDigest.isEqual giúp so sánh mảng byte với thời gian cố định, chống timing attack
            return MessageDigest.isEqual(expectedHmac, receivedHmac);
        }
        catch (Exception e) {
            return false;
        }
    }
}