package com.cinema.booking.booking.service;

import com.cinema.booking.auth.entity.User;
import com.cinema.booking.auth.repository.UserRepository;
import com.cinema.booking.booking.dto.BookingRequest;
import com.cinema.booking.booking.dto.BookingResponse;
import com.cinema.booking.booking.dto.VinattiBillResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class BookingService implements BookingUseCase {

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final VinattiGatewayClient vinattiGatewayClient;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            ShowtimeRepository showtimeRepository,
            ShowtimeSeatRepository showtimeSeatRepository,
            UserRepository userRepository,
            Clock clock,
            VinattiGatewayClient vinattiGatewayClient,
            PlatformTransactionManager transactionManager
    ) {
        this.bookingRepository = bookingRepository;
        this.showtimeRepository = showtimeRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.vinattiGatewayClient = vinattiGatewayClient;
        this.transactionTemplate = transactionManager != null ? new TransactionTemplate(transactionManager) : null;
    }

    // Constructor dành cho Unit Test không cần PlatformTransactionManager
    public BookingService(
            BookingRepository bookingRepository,
            ShowtimeRepository showtimeRepository,
            ShowtimeSeatRepository showtimeSeatRepository,
            UserRepository userRepository,
            Clock clock,
            VinattiGatewayClient vinattiGatewayClient
    ) {
        this(bookingRepository, showtimeRepository, showtimeSeatRepository, userRepository, clock, vinattiGatewayClient, null);
    }

    // 1. đặt vé có chống tranh chấp đồng thời
    @Override
    public BookingResponse createBooking(String userEmail, BookingRequest request) {
        final BookingResponse[] baseResponse = new BookingResponse[1];
        TransactionCallback<Booking> saveAction = status -> {
            // b1 kiểm tra danh sách ghế không được có id trùng nhau
            validateUniqueSeatIds(request.seatIds());

            // b2 tìm thông tin người dùng từ email trong token jwt
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Không tìm thấy thông tin tài khoản"));

            // b3 khoá suất chiếu pessimistic Lock
            Showtime showtime = showtimeRepository.findByIdWithLock(request.showtimeId())
                    .orElseThrow(() -> new ShowtimeNotFoundException(request.showtimeId()));

            // b4 kiểm tra trạng thái và thời gian suất chiếu
            validateBookableShowtime(showtime);

            // b5 khoá toàn bộ ghế theo thứ tự tăng dần của seat_id để tránh deadlock
            List<Long> sortedSeatIds = request.seatIds().stream().distinct().sorted().toList();
            List<ShowtimeSeat> lockedSeats = showtimeSeatRepository.findSeatsWithLock(showtime.getId(), sortedSeatIds);

            validateLockedSeats(lockedSeats, sortedSeatIds.size());

            // b7 tính tiền và tạo booking từ các ghế đã kiểm tra
            Booking booking = buildBooking(user, showtime, lockedSeats.size());

            // gắn ghế và lưu thông tin từng vé
            assignSeatsToBooking(booking, lockedSeats);

            // lưu booking, các booking item được lưu theo cascade
            Booking saved = bookingRepository.save(booking);
            showtimeSeatRepository.saveAll(lockedSeats);

            // dựng response NGAY TRONG transaction, vì sau commit entity bị detached nên đọc lazy sẽ lỗi
            baseResponse[0] = BookingResponse.from(saved);
            return saved;
        };

        // Thực hiện lưu booking trong transaction và commit ngay vào database để Gateway có thể orderinfo
        Booking savedBooking = transactionTemplate != null
                ? transactionTemplate.execute(saveAction)
                : saveAction.doInTransaction(null);

        // Sau khi đơn vé đã được lưu vào database (PENDING), gọi Gateway để tạo hóa đơn
        VinattiBillResponse billResponse;
        try {
            billResponse = vinattiGatewayClient.createBill(savedBooking, "VIETTEL", null);
        } catch (Exception e) {
            log.error("Tạo hóa đơn Vinatti thất bại cho đơn {}, tiến hành hủy đơn", savedBooking.getBookingCode(), e);
            cancelAfterGatewayFailure(savedBooking.getBookingCode());
            throw e;
        }
        return baseResponse[0].withPaymentUrl(billResponse.deepLink());
    }

    // 2. lấy danh sách vé của tôi
    @Override
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
    @Override
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
    @Override
    @Transactional
    public BookingResponse cancelBooking(String userEmail, Long bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Không tìm thấy thông tin tài khoản"));

        Long showtimeId = bookingRepository
                .findShowtimeIdByIdAndUserId(bookingId, user.getId())
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // lấy khóa suất chiếu trước để tránh tranh chấp
        Showtime showtime = showtimeRepository.findByIdWithLock(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId));

        // đọc trạng thái booking sau khi đã lấy được khóa suất chiếu.
        Booking booking = bookingRepository
                .findByIdAndUserIdWithLock(bookingId, user.getId())
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingCannotBeCancelledException("Đơn đặt vé này đã được hủy trước đó");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (!showtime.getStartTime().isAfter(now)) {
            throw new BookingCannotBeCancelledException("Không thể hủy vé vì suất chiếu đã bắt đầu");
        }

        List<ShowtimeSeat> seats =
                showtimeSeatRepository.findByBookingIdWithLock(bookingId);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);

        for (ShowtimeSeat seat : seats) {
            seat.setStatus(ShowtimeSeatStatus.AVAILABLE);
            seat.setBooking(null);
        }

        showtimeSeatRepository.saveAll(seats);

        return BookingResponse.from(booking);
    }

    private void validateUniqueSeatIds(List<Long> seatIds) {
        Set<Long> uniqueSeatIds = new HashSet<>(seatIds);

        if (uniqueSeatIds.size() != seatIds.size()) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "DUPLICATE_SEATS",
                    "Danh sách ghế không được chứa ID trùng lặp"
            );
        }
    }

    private void validateBookableShowtime(Showtime showtime) {
        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "SHOWTIME_NOT_AVAILABLE",
                    "Suất chiếu không ở trạng thái mở bán vé"
            );
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!showtime.getStartTime().isAfter(now)) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "SHOWTIME_ALREADY_STARTED",
                    "Không thể đặt vé cho suất chiếu đã bắt đầu"
            );
        }
    }

    private void validateLockedSeats(List<ShowtimeSeat> lockedSeats, int requestedSeatsCount) {
        if (lockedSeats.size() != requestedSeatsCount) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "SEAT_NOT_FOUND",
                    "Một hoặc nhiều ghế không tồn tại trong suất chiếu này"
            );
        }

        for (ShowtimeSeat showtimeSeat : lockedSeats) {
            if (showtimeSeat.getStatus() != ShowtimeSeatStatus.AVAILABLE) {
                throw new SeatAlreadyBookedException();
            }
        }
    }

    private Booking buildBooking(User user, Showtime showtime, int seatCount) {
        BigDecimal totalAmount = showtime.getPrice()
                .multiply(BigDecimal.valueOf(seatCount));

        // lấy 8 ký tự từ uuid để mã vé ngắn, dễ tra cứu
        String bookingCode = "BK-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return Booking.builder()
                .bookingCode(bookingCode)
                .user(user)
                .showtime(showtime)
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .build();
    }

    private void assignSeatsToBooking(
            Booking booking,
            List<ShowtimeSeat> lockedSeats
    ) {
        for (ShowtimeSeat showtimeSeat : lockedSeats) {
            showtimeSeat.setStatus(ShowtimeSeatStatus.BOOKED);
            showtimeSeat.setBooking(booking);

            // lưu giá và mã ghế lúc đặt để giữ nguyên thông tin vé về sau
            BookingItem item = BookingItem.builder()
                    .booking(booking)
                    .seat(showtimeSeat.getSeat())
                    .seatCode(showtimeSeat.getSeat().getSeatCode())
                    .price(booking.getShowtime().getPrice())
                    .build();

            booking.getItems().add(item);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getOrderInfo(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
                .orElse(null);
    }

    @Override
    @Transactional
    public void processPaymentSync(String orderCode, String responseCode) {
        // 1. tìm đơn vé kèm khóa để tránh xung đột
        Booking booking = bookingRepository.findByBookingCodeWithLock(orderCode)
                .orElse(null);
        if (booking == null) {
            log.warn("Không tìm thấy đơn vé với mã: {}", orderCode);
            return;
        }

        // 2. kiểm tra tính Idempotent (nếu đơn đã hoàn tất trước đó thì bỏ qua, không xử lý lại)
        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.CANCELLED) {
            log.info("Đơn vé {} đã ở trạng thái {}, bỏ qua xử lý lặp", orderCode, booking.getStatus());
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);

        // 3. phân nhánh xử lý theo kết quả từ Simulator
        if ("00".equals(responseCode)) {
            // thanh toán thành công -> Đổi trạng thái vé sang CONFIRMED
            booking.setStatus(BookingStatus.CONFIRMED);
            log.info("Đơn vé {} đã thanh toán thành công", orderCode);
        } else {
            // thanh toán thất bại hoặc hết hạn 5 phút -> Hủy đơn và NHẢ GHẾ
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(now);

            // nhả lại toàn bộ ghế của đơn này về trạng thái AVAILABLE
            List<ShowtimeSeat> seats = showtimeSeatRepository.findByBookingIdWithLock(booking.getId());
            for (ShowtimeSeat seat : seats) {
                seat.setStatus(ShowtimeSeatStatus.AVAILABLE);
                seat.setBooking(null);
            }
            showtimeSeatRepository.saveAll(seats);
            log.info("Đơn vé {} thanh toán thất bại/hết hạn. Đã hủy đơn và nhả {} ghế", orderCode, seats.size());
        }

        bookingRepository.save(booking);
    }

    private void cancelAfterGatewayFailure(String bookingCode) {
        if (transactionTemplate != null) {
            transactionTemplate.executeWithoutResult(s -> processPaymentSync(bookingCode, "01"));
        } else {
            processPaymentSync(bookingCode, "01");
        }
    }
}
