// chuẩn hoá lỗi 401 chưa đăng nhập, token thiếu hoặc sai
package com.cinema.booking.auth.security;

import com.cinema.booking.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class DelegatedAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    //hàm được sercurity tự động gọi khi request gửi tới một API được bảo vệ (cần đang nhập), request kh có token hoặc token hết hạn
    //vì mặc định sẽ trả về trang html 401 trắng nên viết để đè lại trả về json chuẩn restful api
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value()); // đặt http status trả về là 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // báo cho client biết kiểu dữ liệu trả về là JSON
        response.setCharacterEncoding("UTF-8"); // đặt bảng mã hiển thị đúng tiếng việt

        //tạo đối tượng errorresponse chứa thông điệp lỗi có cấu trúc
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Yêu cầu xác thực tài khoản để truy cập tài nguyên này",
                request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}