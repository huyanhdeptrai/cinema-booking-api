package com.cinema.booking.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

//cung cấp thông tin tổng quan tài liệu api hiển thị trên giao diện swagger ui
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Cinema Booking REST API",
                version = "1.0",
                description = "Tài liệu API hệ thống đặt vé xem phim trực tuyến"
        ),
        // Áp dụng xác thực Bearer token cho toàn bộ các API cần bảo mật
        security = @SecurityRequirement(name = "bearerAuth")
)

//định nghĩa xác thực tài khoản trên swagger ui, sẽ tạo ra nút bấm authorize
@SecurityScheme(
        name = "bearerAuth",
        description = "Nhập JWT token để xác thực (Swagger sẽ tự động gắn Bearer)",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}