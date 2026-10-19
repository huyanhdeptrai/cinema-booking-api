// dữ liệu client gửi lên khi đặt vé
//theo các quy tắc: Không nhận totalamount từ client, showtimeid không được null, seatids không được rỗng.
package com.cinema.booking.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BookingRequest(
        @NotNull(message = "ID suất chiếu không được để trống")
        Long showtimeId,

        @NotEmpty(message = "Danh sách ghế đặt không được để trống")
        List<Long> seatIds
) {
}