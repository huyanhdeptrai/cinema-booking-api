// api cập nhật phòng chỉ đổi tên
package com.cinema.booking.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAuditoriumRequest(
        @NotBlank(message = "Tên phòng chiếu không được để trống")
        @Size(max = 100, message = "Tên phòng chiếu không được vượt quá 100 ký tự")
        String name
) {
}