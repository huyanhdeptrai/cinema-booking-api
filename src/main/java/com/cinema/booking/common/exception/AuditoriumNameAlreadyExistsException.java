// trả về 409 conflict
package com.cinema.booking.common.exception;

import org.springframework.http.HttpStatus;

public class AuditoriumNameAlreadyExistsException extends AppException {
    public AuditoriumNameAlreadyExistsException(String name) {
        super(HttpStatus.CONFLICT, "AUDITORIUM_NAME_ALREADY_EXISTS", "Tên phòng chiếu đã tồn tại: " + name);
    }
}
