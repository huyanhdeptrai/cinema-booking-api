// thông tin phòng vé trả về
package com.cinema.booking.cinema.dto;

import com.cinema.booking.cinema.entity.Auditorium;

import java.time.LocalDateTime;

public record AuditoriumResponse (
        Long id,
        String name,
        Integer rowCount,
        Integer seatsPerRow,
        Integer totalSeats,
        LocalDateTime createdAt
) {
    public static AuditoriumResponse from(Auditorium auditorium) {
        int total = auditorium.getRowCount() * auditorium.getSeatsPerRow(); //tính tổng số ghế = số hàng x số ghế mỗi hàng
        return new AuditoriumResponse(
                auditorium.getId(),
                auditorium.getName(),
                auditorium.getRowCount(),
                auditorium.getSeatsPerRow(),
                total,
                auditorium.getCreatedAt()
        );
    }
}
