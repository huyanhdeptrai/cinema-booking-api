// ném ra khi xung đột đặt ghế, 409 conflict
package com.cinema.booking.common.exception;

import org.springframework.http.HttpStatus;

public class SeatAlreadyBookedException extends AppException {
    public SeatAlreadyBookedException() {
        super(HttpStatus.CONFLICT, "SEAT_ALREADY_BOOKED", "Ít nhất một ghế đã được người khác đặt trước");
    }
}