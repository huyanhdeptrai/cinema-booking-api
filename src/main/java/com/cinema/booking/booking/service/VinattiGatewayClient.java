package com.cinema.booking.booking.service;

import com.cinema.booking.booking.dto.VinattiBillResponse;
import com.cinema.booking.booking.entity.Booking;
import com.cinema.booking.common.config.VinattiProperties;
import com.cinema.booking.common.exception.AppException;
import com.cinema.booking.common.util.VinattiSigning;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VinattiGatewayClient {

    private final VinattiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // gửi yêu cầu CREATE_BILL sang Vinatti Gateway
    public VinattiBillResponse createBill(Booking booking, String telco, String mobileNumber) {
        try {
            long amount = booking.getTotalAmount().longValue();

            // 1. dựng JSON Data thuần túy (lưu ý: Amount và TotalAmount bắt buộc là JSON Number)
            Map<String, Object> dataMap = new LinkedHashMap<>();
            dataMap.put("PartnerCode", "ZALO");
            dataMap.put("PartnerRefID", booking.getBookingCode());
            dataMap.put("Amount", amount);
            dataMap.put("TotalAmount", amount);
            dataMap.put("Fee", 0);
            dataMap.put("MobileNumber", mobileNumber != null ? mobileNumber : "0900000000");
            dataMap.put("Telco", telco != null ? telco.toUpperCase() : "VIETTEL");

            String rawData = objectMapper.writeValueAsString(dataMap);

            // 2. ký checksum bằng secret-key (HMAC SHA256 hex thường)
            String checksum = VinattiSigning.gatewayChecksum(rawData, properties.getSecretKey());
            String time = LocalDateTime.now().format(TIME_FORMATTER);

            // 3. đóng Envelope
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("Code", "CREATE_BILL");
            envelope.put("Data", rawData);
            envelope.put("Time", time);
            envelope.put("Checksum", checksum);

            log.info("Gửi CREATE_BILL cho đơn {}: URL={}", booking.getBookingCode(), properties.getGatewayUrl());

            // 4. gửi HTTP POST tới Gateway kèm Header AccessKey
            String responseBody = restClient.post()
                    .uri(properties.getGatewayUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("AccessKey", properties.getAccessKey())
                    .header("X-Forwarded-Proto", "https") // báo cho simulator biết đã qua HTTPS, tránh bị ép chuyển hướng
                    .body(envelope)
                    .retrieve()
                    .body(String.class);

            // 5. đọc phản hồi
            JsonNode root = objectMapper.readTree(responseBody);
            String code = root.path("code").asText();
            String message = root.path("message").asText();

            if (!"00".equals(code)) {
                log.error("Tạo hóa đơn Vinatti thất bại: code={}, message={}", code, message);
                throw new AppException(HttpStatus.BAD_GATEWAY, "PAYMENT_GATEWAY_ERROR", "Lỗi cổng thanh toán: " + message);
            }

            // phản hồi data lại là 1 chuỗi JSON lồng bên trong
            String innerDataStr = root.path("data").asText();
            JsonNode innerData = objectMapper.readTree(innerDataStr);
            return new VinattiBillResponse(
                    innerData.path("RetRefNumber").asText(),
                    innerData.path("QRCode").asText(),
                    innerData.path("DeepLink").asText()
            );

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi kết nối tới cổng thanh toán Vinatti", e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "GATEWAY_CONNECTION_ERROR", "Không thể kết nối đến cổng thanh toán");
        }
    }
}
