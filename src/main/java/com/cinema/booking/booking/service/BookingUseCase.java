package com.cinema.booking.booking.service;

import com.cinema.booking.booking.dto.BookingRequest;
import com.cinema.booking.booking.dto.BookingResponse;
import com.cinema.booking.booking.entity.Booking;

import java.util.List;

public interface BookingUseCase {

    BookingResponse createBooking(String userEmail, BookingRequest request);

    List<BookingResponse> getMyBookings(String userEmail);

    BookingResponse getBookingDetail(String userEmail, Long bookingId);

    BookingResponse cancelBooking(String userEmail, Long bookingId);

    // Lấy thông tin đơn cho Vinatti xác minh
    Booking getOrderInfo(String bookingCode);

    // Xử lý kết quả thanh toán từ Vinatti callback
    void processPaymentSync(String orderCode, String responseCode);
}
