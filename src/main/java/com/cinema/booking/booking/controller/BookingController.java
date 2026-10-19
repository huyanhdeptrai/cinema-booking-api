package com.cinema.booking.booking.controller;

import com.cinema.booking.booking.dto.BookingRequest;
import com.cinema.booking.booking.dto.BookingResponse;
import com.cinema.booking.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Các API đặt vé và quản lý vé dành cho khách hàng")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    private final BookingService bookingService;
    // Đặt vé (nhiều ghế)
    @PostMapping
    @Operation(summary = "Đặt vé xem phim (chọn nhiều ghế)")
    @ApiResponse(responseCode = "201", description = "Đặt vé thành công")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc suất chiếu đã bắt đầu")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ")
    @ApiResponse(responseCode = "409", description = "Ghế đã có người đặt trước (SEAT_ALREADY_BOOKED)")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BookingRequest request
    ) {
        BookingResponse response = bookingService.createBooking(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // danh sách vé của tôi
    @GetMapping("/me")
    @Operation(summary = "Lấy danh sách các vé đã đặt của khách hàng hiện tại")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(bookingService.getMyBookings(userDetails.getUsername()));
    }

    // chi tiết 1 vé của tôi
    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một vé đã đặt (yêu cầu đúng chủ sở hữu)")
    public ResponseEntity<BookingResponse> getBookingDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(bookingService.getBookingDetail(userDetails.getUsername(), id));
    }

    // hủy vé
    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Hủy vé đã đặt (chỉ được hủy trước giờ chiếu)")
    @ApiResponse(responseCode = "200", description = "Hủy vé thành công, ghế được giải phóng")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy vé hoặc vé không thuộc về bạn")
    @ApiResponse(responseCode = "409", description = "Suất chiếu đã bắt đầu, không thể hủy vé")
    public ResponseEntity<BookingResponse> cancelBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(bookingService.cancelBooking(userDetails.getUsername(), id));
    }
}
