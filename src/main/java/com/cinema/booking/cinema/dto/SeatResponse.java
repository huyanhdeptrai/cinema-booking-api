// thông tin ghế trả về client
package com.cinema.booking.cinema.dto;

import com.cinema.booking.cinema.entity.Seat;

public record SeatResponse(
        Long id,
        String rowLabel,
        Integer seatNumber,
        String seatCode
) {
    // hám giúp chuyển đổi nhanh từ entity sang DTO
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getSeatCode()
        );
    }
}