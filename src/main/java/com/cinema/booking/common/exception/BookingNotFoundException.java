// 404 not found, không tìm thấy thông tin đặt vé
package com.cinema.booking.common.exception;

import org.springframework.http.HttpStatus;

public class BookingNotFoundException extends AppException {
    public BookingNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Không tìm thấy vé hoặc vé không thuộc quyền sở hữu của bạn (ID: " + id + ")");
    }
}