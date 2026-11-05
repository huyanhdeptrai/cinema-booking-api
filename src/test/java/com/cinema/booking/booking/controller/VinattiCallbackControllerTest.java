package com.cinema.booking.booking.controller;

import com.cinema.booking.booking.entity.Booking;
import com.cinema.booking.booking.service.BookingUseCase;
import com.cinema.booking.common.config.VinattiProperties;
import com.cinema.booking.common.util.VinattiSigning;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VinattiCallbackControllerTest {

    @Mock
    private BookingUseCase bookingService;

    private VinattiProperties vinattiProperties;
    private ObjectMapper objectMapper;
    private VinattiCallbackController controller;

    private final String secret = "callback-demo-secret-0123456789abcdef";

    @BeforeEach
    void setUp() {
        vinattiProperties = new VinattiProperties();
        vinattiProperties.getCallback().setSecret(secret);
        objectMapper = new ObjectMapper();
        controller = new VinattiCallbackController(bookingService, vinattiProperties, objectMapper);
    }

    private String createBearerHeader(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "Bearer " + Base64.getEncoder().encodeToString(mac.doFinal(body));
    }

    @Test
    @DisplayName("orderinfo: Trả về 00 khi chữ ký đúng và tìm thấy đơn")
    void testOrderInfoSuccess() throws Exception {
        String bodyStr = "{\"type\":\"VNT_ZMA\",\"orderCode\":\"BK-123\"}";
        byte[] rawBody = bodyStr.getBytes(StandardCharsets.UTF_8);
        String authHeader = createBearerHeader(rawBody);

        Booking booking = Booking.builder()
                .bookingCode("BK-123")
                .totalAmount(new BigDecimal("150000"))
                .build();

        when(bookingService.getOrderInfo("BK-123")).thenReturn(booking);

        ResponseEntity<Map<String, String>> response = controller.handleOrderInfo(authHeader, rawBody);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("00", response.getBody().get("responseCode"));
        assertEquals("BK-123", response.getBody().get("orderCode"));
        assertEquals("150000", response.getBody().get("transAmount"));
    }

    @Test
    @DisplayName("orderinfo: Trả về 06 khi chữ ký không hợp lệ")
    void testOrderInfoInvalidSignature() {
        String bodyStr = "{\"type\":\"VNT_ZMA\",\"orderCode\":\"BK-123\"}";
        byte[] rawBody = bodyStr.getBytes(StandardCharsets.UTF_8);

        ResponseEntity<Map<String, String>> response = controller.handleOrderInfo("Bearer invalid", rawBody);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("06", response.getBody().get("responseCode"));
    }

    @Test
    @DisplayName("orderinfo: Trả về 01 khi không tìm thấy đơn")
    void testOrderInfoNotFound() throws Exception {
        String bodyStr = "{\"type\":\"VNT_ZMA\",\"orderCode\":\"BK-999\"}";
        byte[] rawBody = bodyStr.getBytes(StandardCharsets.UTF_8);
        String authHeader = createBearerHeader(rawBody);

        when(bookingService.getOrderInfo("BK-999")).thenReturn(null);

        ResponseEntity<Map<String, String>> response = controller.handleOrderInfo(authHeader, rawBody);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("01", response.getBody().get("responseCode"));
    }

    @Test
    @DisplayName("ordersync: Xử lý thành công và trả về ACK 00")
    void testOrderSyncSuccess() throws Exception {
        String bodyStr = "{\"type\":\"VNT_ZMA\",\"responseCode\":\"00\",\"orderCode\":\"BK-123\",\"transAmount\":\"150000\",\"transId\":\"TR-1\",\"transDate\":\"05/11/2026 08:00:00\"}";
        byte[] rawBody = bodyStr.getBytes(StandardCharsets.UTF_8);
        String authHeader = createBearerHeader(rawBody);

        ResponseEntity<Map<String, String>> response = controller.handleOrderSync(authHeader, rawBody);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("00", response.getBody().get("responseCode"));
        verify(bookingService).processPaymentSync("BK-123", "00");
    }

    @Test
    @DisplayName("ordersync: Trả về 06 khi chữ ký không hợp lệ")
    void testOrderSyncInvalidSignature() {
        String bodyStr = "{\"type\":\"VNT_ZMA\",\"responseCode\":\"00\",\"orderCode\":\"BK-123\"}";
        byte[] rawBody = bodyStr.getBytes(StandardCharsets.UTF_8);

        ResponseEntity<Map<String, String>> response = controller.handleOrderSync("Bearer invalid", rawBody);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("06", response.getBody().get("responseCode"));
        verifyNoInteractions(bookingService);
    }
}
