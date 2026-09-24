// dữ liệu admin gửi đi khi tạo phòng
package com.cinema.booking.cinema.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuditoriumRequest(
        @NotBlank(message = "Tên phòng chiếu không được để trống")
        String name,

        @NotNull(message = "Số hàng ghế không được để trống")
        @Min(value = 1, message = "Số hàng ghế tối thiểu là 1")
        @Max(value = 26, message = "Số hàng ghế tối đa là 26 (tương ứng A-Z)")
        Integer rowCount,

        @NotNull(message = "Số ghế mỗi hàng không được để trống")
        @Min(value = 1, message = "Số ghế mỗi hàng tối thiểu là 1")
        Integer seatsPerRow
) {}