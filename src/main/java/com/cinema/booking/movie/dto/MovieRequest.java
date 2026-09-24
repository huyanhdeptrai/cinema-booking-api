// dữ liệu admin gửi lên khi tạo hoặc sửa phim
package com.cinema.booking.movie.dto;

import com.cinema.booking.movie.entity.MovieStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MovieRequest (
        @NotBlank(message = "Tên phim không được để trống")
        String title,

        String description,

        @NotNull(message = "Thời lượng phim không được để trống")
        @Min(value = 1, message = "Thời lượng phim phải lớn hơn 0")
        Integer durationMinutes,

        @NotBlank(message = "Giới hạn độ tuổi không được để trống")
        String ageRating,

        @NotNull(message = "Ngày phát hành không được để trống")
        LocalDate releaseDate,

        @NotNull(message = "Trạng thái phim không được để trống")
        MovieStatus status
) { }