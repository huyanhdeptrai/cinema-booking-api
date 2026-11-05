package com.cinema.booking.booking.service;

import com.cinema.booking.booking.dto.VinattiBillResponse;
import com.cinema.booking.booking.entity.Booking;
import com.cinema.booking.common.config.VinattiProperties;
import com.cinema.booking.common.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VinattiGatewayClientTest {

    private VinattiProperties properties;
    private ObjectMapper objectMapper;
    private VinattiGatewayClient client;

    @BeforeEach
    void setUp() {
        properties = new VinattiProperties();
        properties.setGatewayUrl("http://127.0.0.1:59999/Zalo/Execute");
        properties.setAccessKey("gateway-demo-access-2026");
        properties.setSecretKey("gateway-demo-secret-2026");

        objectMapper = new ObjectMapper();
        client = new VinattiGatewayClient(properties, objectMapper);
    }

    @Test
    @DisplayName("Khi không thể kết nối tới Gateway thì ném AppException GATEWAY_CONNECTION_ERROR")
    void testCreateBillConnectionFailure() {
        Booking booking = Booking.builder()
                .bookingCode("BK-TEST-001")
                .totalAmount(BigDecimal.valueOf(100000))
                .build();

        // Simulator chưa bật ở cổng 9011 nên sẽ ném exception
        AppException exception = assertThrows(AppException.class,
                () -> client.createBill(booking, "VIETTEL", "0900000000"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("GATEWAY_CONNECTION_ERROR", exception.getCode());
    }
}
