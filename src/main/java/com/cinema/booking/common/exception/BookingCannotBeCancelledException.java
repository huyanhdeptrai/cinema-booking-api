// 409 conflict, vé không còn điều kiện huỷ
package com.cinema.booking.common.exception;

import org.springframework.http.HttpStatus;

public class BookingCannotBeCancelledException extends AppException {
    public BookingCannotBeCancelledException(String reason) {
        super(HttpStatus.CONFLICT, "BOOKING_CANNOT_BE_CANCELLED", reason);
    }
}