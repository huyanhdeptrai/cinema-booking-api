package com.cinema.booking.common.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends AppException { //kế thừa từ AppException để tái sử dụng cơ chế xử lý lỗi tập trung
    public EmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email đã được đăng ký: " + email);
    }
}
