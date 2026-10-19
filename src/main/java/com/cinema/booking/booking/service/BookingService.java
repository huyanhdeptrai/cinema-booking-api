package com.cinema.booking.booking.service;

import com.cinema.booking.auth.entity.User;
import com.cinema.booking.auth.repository.UserRepository;
import com.cinema.booking.booking.dto.BookingRequest;
import com.cinema.booking.booking.dto.BookingResponse;
import com.cinema.booking.booking.entity.Booking;
import com.cinema.booking.booking.entity.BookingItem;
import com.cinema.booking.booking.entity.BookingStatus;
import com.cinema.booking.booking.repository.BookingRepository;
import com.cinema.booking.common.exception.*;
import com.cinema.booking.showtime.entity.Showtime;
import com.cinema.booking.showtime.entity.ShowtimeSeat;
import com.cinema.booking.showtime.entity.ShowtimeSeatStatus;
import com.cinema.booking.showtime.entity.ShowtimeStatus;
import com.cinema.booking.showtime.repository.ShowtimeRepository;
import com.cinema.booking.showtime.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final UserRepository userRepository;

    // 1. đặt vé có chống tranh chấp đồng thời
    @Transactional
    public BookingResponse createBooking(String userEmail, BookingRequest request) {
        // b1 kiểm tra danh sách ghế không được có id trùng nhau
        Set<Long> uniqueSeatIds = new HashSet<>(request.seatIds());
        if (uniqueSeatIds.size() != request.seatIds().size()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "DUPLICATE_SEATS", "Danh sách ghế không được chứa ID trùng lặp");
        }

        // b2 tìm thông tin người dùng từ email trong token jwt
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Không tìm thấy thông tin tài khoản"));

        // b3 khoá suất chiếu pessimistic Lock
        Showtime showtime = showtimeRepository.findByIdWithLock(request.showtimeId())
                .orElseThrow(() -> new ShowtimeNotFoundException(request.showtimeId()));

        // b4 kiểm tra trạng thái và thời gian suất chiếu
        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "SHOWTIME_NOT_AVAILABLE", "Suất chiếu đã bị hủy hoặc không còn khả dụng");
        }
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "SHOWTIME_ALREADY_STARTED", "Không thể đặt vé cho suất chiếu đã bắt đầu");
        }

        // b5 khoá toàn bộ ghế theo thứ tự tăng dần của seat_id để tránh deadlock
        List<Long> sortedSeatIds = request.seatIds().stream().distinct().sorted().toList();
        List<ShowtimeSeat> lockedSeats = showtimeSeatRepository.findSeatsWithLock(showtime.getId(), sortedSeatIds);

        // Kiểm tra xem tất cả các ghế có thuộc phòng chiếu của suất này không
        if (lockedSeats.size() != sortedSeatIds.size()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_SEAT", "Một hoặc nhiều ghế không thuộc về phòng chiếu của suất này");
        }

        // b6 kiểm tra trạng thái của từng ghế: nếu chỉ có 1 ghế không available -> báo lỗi ngay
        for (ShowtimeSeat seat : lockedSeats) {
            if (seat.getStatus() != ShowtimeSeatStatus.AVAILABLE) {
                throw new SeatAlreadyBookedException();
            }
        }

        // b7 tính tổng tiền (tự tính bằng bigdecimal = giá suất * số ghế)
        BigDecimal totalAmount = showtime.getPrice().multiply(BigDecimal.valueOf(lockedSeats.size()));

        // sinh mã booking ngẫu nhiên duy nhất
        String bookingCode = "BK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        // tạo đối tượng booking
        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .user(user)
                .showtime(showtime)
                .totalAmount(totalAmount)
                .status(BookingStatus.CONFIRMED)
                .build();

        // b8 đổi trạng thái ghế sang booked, gắn booking và tạo bookingitem (snapshot giá và mã ghế)
        for (ShowtimeSeat ss : lockedSeats) {
            ss.setStatus(ShowtimeSeatStatus.BOOKED);
            ss.setBooking(booking);
            BookingItem item = BookingItem.builder()
                    .booking(booking)
                    .seat(ss.getSeat())
                    .seatCode(ss.getSeat().getSeatCode())
                    .price(showtime.getPrice())
                    .build();
            booking.getItems().add(item);
        }

        // Lưu booking (sẽ tự cascade lưu các booking items)
        Booking savedBooking = bookingRepository.save(booking);
        showtimeSeatRepository.saveAll(lockedSeats);

        return BookingResponse.from(savedBooking);
    }

    // 2. lấy danh sách vé của tôi
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Không tìm thấy thông tin tài khoản"));
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    // 3. xem chi tiết vé của tôi (kiểm tra quyền sở hữu)
    @Transactional(readOnly = true)
    public BookingResponse getBookingDetail(String userEmail, Long bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Không tìm thấy thông tin tài khoản"));
        // chỉ tìm thấy nếu vé thuộc về đúng user đó
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, user.getId())
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        return BookingResponse.from(booking);
    }

    // 4. huỷ vé hợp lệ và giải phóng ghế
    @Transactional
    public BookingResponse cancelBooking(String userEmail, Long bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Không tìm thấy thông tin tài khoản"));
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, user.getId())
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // kiểm tra điều kiện: vé chưa bị hủy trước đó
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingCannotBeCancelledException("Đơn đặt vé này đã được hủy trước đó");
        }

        // kiểm tra điều kiện: chỉ được hủy trước giờ chiếu
        if (booking.getShowtime().getStartTime().isBefore(LocalDateTime.now())) {
            throw new BookingCannotBeCancelledException("Không thể hủy vé vì suất chiếu đã bắt đầu");
        }

        // Cập nhật trạng thái Booking sang CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());

        // giải phóng ghế: đưa tất cả ghế trong vé về available và gỡ bỏ booking liên kết
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByBookingId(booking.getId());
        for (ShowtimeSeat seat : seats) {
            seat.setStatus(ShowtimeSeatStatus.AVAILABLE);
            seat.setBooking(null);
        }

        showtimeSeatRepository.saveAll(seats);

        return BookingResponse.from(booking);
    }
}