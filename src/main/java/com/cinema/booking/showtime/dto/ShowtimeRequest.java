// Request tạo suất chiếu, dùng cho POST /api/admin/showtimes
package com.cinema.booking.showtime.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeRequest(
        @NotNull(message = "ID phim không được để trống")
        Long movieId,

        @NotNull(message = "ID phòng chiếu không được để trống")
        Long auditoriumId,

        @NotNull(message = "Thời gian bắt đầu không được để trống")
        @Future(message = "Thời gian bắt đầu suất chiếu phải ở tương lai")
        LocalDateTime startTime,

        @NotNull(message = "Giá vé không được để trống")
        @DecimalMin(value = "0.01", message = "Giá vé phải lớn hơn 0")
        BigDecimal price
) {
}