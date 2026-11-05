package com.cinema.booking.booking.controller;

import com.cinema.booking.booking.entity.Booking;
import com.cinema.booking.booking.service.BookingUseCase;
import com.cinema.booking.common.config.VinattiProperties;
import com.cinema.booking.common.util.VinattiSigning;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/vinatticonnect/v2")
@RequiredArgsConstructor
public class VinattiCallbackController {

    private final BookingUseCase bookingService;
    private final VinattiProperties vinattiProperties;
    private final ObjectMapper objectMapper;

    // 1. tra cứu backend trước khi tạo hoá đơn
    // simulator gửi: {"type":"VNT_ZMA","orderCode":"..."}
    @PostMapping(value = "/orderinfo", produces = "application/json; charset=utf-8")
    public ResponseEntity<Map<String, String>> handleOrderInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody byte[] rawBody) {

        // bước 1: xác thực chữ ký HMAC Base64 trên body thô
        String secret = vinattiProperties.getCallback().getSecret();
        if (!VinattiSigning.verifyCallback(rawBody, authHeader, secret)) {
            log.warn("Chữ ký orderinfo không hợp lệ");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("responseCode", "06"));
        }

        try {
            // bước 2: đọc mã đơn
            JsonNode root = objectMapper.readTree(rawBody);
            String orderCode = root.path("orderCode").asText(null);
            if (orderCode == null || orderCode.isBlank()) {
                return ResponseEntity.ok(Map.of("responseCode", "01"));
            }

            // bước 3: tìm đơn trong Database
            Booking booking = bookingService.getOrderInfo(orderCode);
            if (booking == null) {
                log.warn("orderinfo: Không tìm thấy đơn {}", orderCode);
                return ResponseEntity.ok(Map.of("responseCode", "01"));
            }

            // bước 4: trả về thông tin đơn (transAmount bắt buộc là String số nguyên)
            String transAmount = booking.getTotalAmount().toBigInteger().toString();
            return ResponseEntity.ok(Map.of(
                    "responseCode", "00",
                    "orderCode", booking.getBookingCode(),
                    "transAmount", transAmount
            ));
        } catch (Exception e) {
            log.error("Lỗi xử lý orderinfo", e);
            return ResponseEntity.ok(Map.of("responseCode", "01"));
        }
    }

    // 2. nhận kết quả thanh toán từ simulator
    // simulator gửi: {"type":"VNT_ZMA","responseCode":"00","orderCode":"...","transAmount":"...","transId":"...","transDate":"..."}
    @PostMapping(value = "/ordersync", produces = "application/json; charset=utf-8")
    public ResponseEntity<Map<String, String>> handleOrderSync(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody byte[] rawBody) {

        // bước 1: xác thực chữ ký HMAC Base64
        String secret = vinattiProperties.getCallback().getSecret();
        if (!VinattiSigning.verifyCallback(rawBody, authHeader, secret)) {
            log.warn("Chữ ký ordersync không hợp lệ");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("responseCode", "06"));
        }

        try {
            // bước 2: parse thông tin kết quả
            JsonNode root = objectMapper.readTree(rawBody);
            String orderCode = root.path("orderCode").asText();
            String responseCode = root.path("responseCode").asText();
            // bước 3: cập nhật DB (CONFIRMED hoặc CANCELLED + nhả ghế)
            bookingService.processPaymentSync(orderCode, responseCode);
            // bước 4: luôn trả ACK "00" để simulator biết backend đã xử lý an toàn
            return ResponseEntity.ok(Map.of("responseCode", "00"));
        } catch (Exception e) {
            log.error("Lỗi xử lý ordersync cho body: {}", new String(rawBody), e);
            // simulator quy định: mã khác 00/11 sẽ kích hoạt cơ chế retry
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("responseCode", "99"));
        }
    }
}
