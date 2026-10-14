// Response xem sơ đồ ghế của suất chiếu
package com.cinema.booking.showtime.dto;

import com.cinema.booking.showtime.entity.Showtime;
import com.cinema.booking.showtime.entity.ShowtimeSeat;
import com.cinema.booking.showtime.entity.ShowtimeSeatStatus;

//trả về thông tin từng ghế
public record ShowtimeSeatResponse (
        Long id,
        Long seatId,
        String seatCode,
        String rowLabel,
        Integer seatNumber,
        ShowtimeSeatStatus status
) {
    public static ShowtimeSeatResponse from(ShowtimeSeat showtimeSeat) {
        return new ShowtimeSeatResponse(
                showtimeSeat.getId(),
                showtimeSeat.getSeat().getId(),
                showtimeSeat.getSeat().getSeatCode(),
                showtimeSeat.getSeat().getRowLabel(),
                showtimeSeat.getSeat().getSeatNumber(),
                showtimeSeat.getStatus()
        );
    }
}
