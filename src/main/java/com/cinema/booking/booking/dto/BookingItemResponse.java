// Thông tin từng ghế trong vé
package com.cinema.booking.booking.dto;

import com.cinema.booking.booking.entity.BookingItem;

import java.math.BigDecimal;

public record BookingItemResponse(
        Long id,
        Long seatId,
        String seatCode,
        BigDecimal price
) {
    public static BookingItemResponse from(BookingItem item) {
        return new BookingItemResponse(
                item.getId(),
                item.getSeat().getId(),
                item.getSeatCode(),
                item.getPrice()
        );
    }
}