// dùng cho api patch đổi trạng thái
package com.cinema.booking.movie.dto;

import com.cinema.booking.movie.entity.MovieStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMovieStatusRequest(
        @NotNull(message = "Trạng thái không được để trống")
        MovieStatus status
) {}