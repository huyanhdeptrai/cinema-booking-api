// trả về 404
package com.cinema.booking.common.exception;

import org.springframework.http.HttpStatus;

public class AuditoriumNotFoundException extends AppException {
    public AuditoriumNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "AUDITORIUM_NOT_FOUND", "Không tìm thấy phòng chiếu với ID: " + id);
    }
}