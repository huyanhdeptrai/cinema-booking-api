//bộ xử lý lỗi toàn cục cho toàn bộ ứng dụng, bắt tất cả các ngoại lệ chưa được xử lý ở các tầng khác
package com.cinema.booking.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    //1. xử lý các lỗi nghiệp vụ do ta chủ động ném ra (vd: EmailAlreadyExistsException)
    @ExceptionHandler(AppException.class) //bắt mọi ngoại lệ thuộc lớp AppException và các lớp con kế thừa từ nó
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        log.warn("Business error [{}]: {}", ex.getCode(), ex.getMessage()); //ghi log cảnh báo về lỗi nghiệp vụ do người dùng
        ErrorResponse response = ErrorResponse.of( //đóng gói thông tin lỗi thành đối tượng ErrorResponse
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI() // lấy đường dẫn api đang gặp lỗi
        );
        //trả về response với mã trạng thái HTTP và body chứa thông tin lỗi
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    //2. Xử lý lỗi validation dữ liệu đầu vào
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
                    return new FieldErrorDetail(fieldName, error.getDefaultMessage());
                })
                .toList();
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Dữ liệu yêu cầu không hợp lệ",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 3. Xử lý lỗi đăng nhập sai thông tin (HTTP 401: INVALID_CREDENTIALS)
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                "Thông tin đăng nhập không đúng",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 4. Xử lý lỗi không đủ quyền truy cập (HTTP 403: FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Bạn không có quyền thực hiện thao tác này",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 5. Xử lý các lỗi hệ thống không mong muốn còn lại (HTTP 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected internal server error at {}: ", request.getRequestURI(), ex);
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "Đã có lỗi xảy ra trong hệ thống, vui lòng thử lại sau",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
