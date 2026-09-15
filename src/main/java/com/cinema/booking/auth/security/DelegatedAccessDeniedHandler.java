// chuẩn hoá lỗi 403 đã đăng nhập nhưng không đủ role
package com.cinema.booking.auth.security;

import com.cinema.booking.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class DelegatedAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    //hàm được spring sercurity tự động gọi khi user đã đăng nhập thành công nhưng role của user không đủ quyền để gọi api này
    //vd user với role customer nhưng lại gọi api xoá phim dành cho admin
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value()); //đặt http status code là 403
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); //định dạng kiểu nội dung trả về là json
        response.setCharacterEncoding("UTF-8"); // đặt bảng mã dành cho tiếng việt

        // tạo đối tượng errorresponse chứa chi tiết lỗi chuẩn hoá
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Bạn không có quyền thực hiện thao tác này",
                request.getRequestURI()
        );
        // chuyển đối tượng errorresponse sang chuỗi json và gửi về cho client
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}