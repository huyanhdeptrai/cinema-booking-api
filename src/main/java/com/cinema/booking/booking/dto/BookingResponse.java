// thông tin trả về cho client
// trả về đầy đủ thông tin: mã vé, phim gì, phòng nào, giờ chiếu, tổng tiền, trạng thái, danh sách ghế
// không bao giờ trả trực tiếp entity ra ngoài.
package com.cinema.booking.booking.dto;

import com.cinema.booking.booking.entity.Booking;
import com.cinema.booking.booking.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingCode,
        Long showtimeId,
        String movieTitle,
        String auditoriumName,
        LocalDateTime startTime,
        BigDecimal totalAmount,
        BookingStatus status,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        List<BookingItemResponse> items
) {
    public static BookingResponse from(Booking booking) {
        List<BookingItemResponse> itemResponses = booking.getItems().stream()
                .map(BookingItemResponse::from)
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getShowtime().getId(),
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getAuditorium().getName(),
                booking.getShowtime().getStartTime(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getCancelledAt(),
                itemResponses
        );
    }
}