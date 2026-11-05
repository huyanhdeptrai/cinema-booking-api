package com.cinema.booking.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

class VinattiSigningTest {

    @Test
    @DisplayName("Kiểm tra Checksum Gateway (Hex) khớp với dữ liệu mẫu trong tài liệu")
    void testGatewayChecksum() throws GeneralSecurityException {
        String secret = "gateway-demo-secret-2026";
        String rawData = "{\"PartnerCode\":\"ZALO\",\"PartnerRefID\":\"demo-order-001\",\"Amount\":100000,\"TotalAmount\":105000,\"Fee\":5000,\"MobileNumber\":\"0900000000\",\"Telco\":\"VIETTEL\"}";
        String expectedChecksum = "3f791301cabe563b06d3d0fc6c51ed75c2e8502fecc19042b00f09fb1fd51afd";

        String actualChecksum = VinattiSigning.gatewayChecksum(rawData, secret);

        assertEquals(expectedChecksum, actualChecksum, "Checksum tính ra phải khớp với checksum mẫu");
    }

    @Test
    @DisplayName("Kiểm tra Xác thực Callback (Base64) khớp với dữ liệu mẫu trong tài liệu")
    void testVerifyCallback() {
        String secret = "callback-demo-secret-0123456789abcdef";
        String rawBodyString = "{\"type\":\"VNT_ZMA\",\"responseCode\":\"00\",\"orderCode\":\"demo-order-001\",\"transAmount\":\"100000\",\"transId\":\"11111111111111111111111111111111\",\"transDate\":\"22/10/2026 13:00:00\"}";
        byte[] rawBodyBytes = rawBodyString.getBytes(StandardCharsets.UTF_8);
        String authHeader = "Bearer PWJhrb9BCYOqs92T6Lk2pPewQ3qAO2oHO5Y/zHaWhgA=";

        boolean isValid = VinattiSigning.verifyCallback(rawBodyBytes, authHeader, secret);

        assertTrue(isValid, "Chữ ký callback hợp lệ phải trả về true");
    }

    @Test
    @DisplayName("Callback bị chỉnh sửa dữ liệu hoặc sai secret phải trả về false")
    void testVerifyCallbackWithTamperedData() {
        String secret = "callback-demo-secret-0123456789abcdef";
        byte[] tamperedBody = "{\"type\":\"VNT_ZMA\",\"responseCode\":\"01\"}".getBytes(StandardCharsets.UTF_8);
        String authHeader = "Bearer PWJhrb9BCYOqs92T6Lk2pPewQ3qAO2oHO5Y/zHaWhgA=";

        boolean isValid = VinattiSigning.verifyCallback(tamperedBody, authHeader, secret);

        assertFalse(isValid, "Dữ liệu bị sửa phải trả về false");
    }
}