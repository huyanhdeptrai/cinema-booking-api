//Response trả về thông tin suất chiếu
package com.cinema.booking.showtime.dto;

import com.cinema.booking.showtime.entity.Showtime;
import com.cinema.booking.showtime.entity.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeResponse (
    Long id,
    Long movieId,
    String movieTitle,
    Long auditoriumId,
    String auditoriumName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal price,
    ShowtimeStatus status,
    LocalDateTime createdAt
) {
    public static ShowtimeResponse from(Showtime showtime) {
        return new ShowtimeResponse(
                showtime.getId(),
                // lấy trực tiếp thông tin từ quan hệ movie
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                // lấy trực tiếp thông tin từ quan hệ auditorium
                showtime.getAuditorium().getId(),
                showtime.getAuditorium().getName(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getPrice(),
                showtime.getStatus(),
                showtime.getCreatedAt()
        );
    }
}
