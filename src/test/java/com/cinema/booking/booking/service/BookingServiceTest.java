package com.cinema.booking.booking.service;

import com.cinema.booking.auth.entity.Role;
import com.cinema.booking.auth.entity.User;
import com.cinema.booking.auth.repository.UserRepository;
import com.cinema.booking.booking.dto.BookingRequest;
import com.cinema.booking.booking.dto.BookingResponse;
import com.cinema.booking.booking.dto.VinattiBillResponse;
import com.cinema.booking.booking.entity.Booking;
import com.cinema.booking.booking.entity.BookingStatus;
import com.cinema.booking.booking.repository.BookingRepository;
import com.cinema.booking.cinema.entity.Auditorium;
import com.cinema.booking.cinema.entity.Seat;
import com.cinema.booking.common.exception.*;
import com.cinema.booking.movie.entity.Movie;
import com.cinema.booking.showtime.entity.Showtime;
import com.cinema.booking.showtime.entity.ShowtimeSeat;
import com.cinema.booking.showtime.entity.ShowtimeSeatStatus;
import com.cinema.booking.showtime.entity.ShowtimeStatus;
import com.cinema.booking.showtime.repository.ShowtimeRepository;
import com.cinema.booking.showtime.repository.ShowtimeSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VinattiGatewayClient vinattiGatewayClient;

    private BookingService bookingService;

    // cố định giờ để test không phụ thuộc lúc chạy
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-21T03:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh")
    );

    private User sampleUser;
    private Movie sampleMovie;
    private Auditorium sampleAuditorium;
    private Showtime sampleShowtime;
    private Seat seat1;
    private Seat seat2;
    private ShowtimeSeat showtimeSeat1;
    private ShowtimeSeat showtimeSeat2;

    // chạy trước mỗi test case để reset và khởi tạo lại dữ liệu sạch
    @BeforeEach
    void setUp() {
        // unit test không chạy Spring nên tự truyền clock vào service
        bookingService = new BookingService(
                bookingRepository,
                showtimeRepository,
                showtimeSeatRepository,
                userRepository,
                clock,
                vinattiGatewayClient
        );

        // giả lập thông tin khách hàng đặt vé
        sampleUser = User.builder()
                .id(1L)
                .email("customer@gmail.com")
                .fullName("Nguyen Van A")
                .role(Role.CUSTOMER)
                .build();

        // giả lập thông tin phim
        sampleMovie = Movie.builder()
                .id(1L)
                .title("Inception")
                .durationMinutes(148)
                .build();

        // giả lập thông tin phòng chiếu
        sampleAuditorium = Auditorium.builder()
                .id(1L)
                .name("Phòng 01")
                .build();

        // giả lập suất chiếu ngày mai, giá 100.000vnd
        sampleShowtime = Showtime.builder()
                .id(100L)
                .movie(sampleMovie)
                .auditorium(sampleAuditorium)
                .startTime(LocalDateTime.now(clock).plusDays(1)) // hợp lệ vì mai chiếu
                .endTime(LocalDateTime.now(clock).plusDays(1).plusMinutes(148))
                .price(BigDecimal.valueOf(100000))
                .status(ShowtimeStatus.SCHEDULED)
                .build();

        // giả lập 2 ghế vật lý trong phòng chiếu A1 va A2
        seat1 = Seat.builder().id(10L).auditorium(sampleAuditorium).rowLabel("A").seatNumber(1).seatCode("A1").build();
        seat2 = Seat.builder().id(11L).auditorium(sampleAuditorium).rowLabel("A").seatNumber(2).seatCode("A2").build();

        // trạng thái của 2 ghế trong suất chiếu này: đang available, trống, chưa ai mua
        showtimeSeat1 = ShowtimeSeat.builder().id(1L).showtime(sampleShowtime).seat(seat1).status(ShowtimeSeatStatus.AVAILABLE).build();
        showtimeSeat2 = ShowtimeSeat.builder().id(2L).showtime(sampleShowtime).seat(seat2).status(ShowtimeSeatStatus.AVAILABLE).build();
    }

    // case 1: đặt vé thành công
    @Test
    @DisplayName("Đặt vé thành công khi các ghế đều AVAILABLE và tính đúng tổng tiền")
    void createBooking_Success() {
        // given khách gửi request đặt 2 ghế có id là 10 và 11
        BookingRequest request = new BookingRequest(100L, List.of(10L, 11L));

        // giả lập kêt quả trả về từ DB:
        // 1. tìm thấy user theo email
        when(userRepository.findByEmail("customer@gmail.com")).thenReturn(Optional.of(sampleUser));
        // 2. tìm thấy suất chiếu kèm cơ chế khoá dòng pessimistic lock
        when(showtimeRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleShowtime));
        // 3. khoá và lấy thông tin 2 ghế, cả 2 đều available
        when(showtimeSeatRepository.findSeatsWithLock(100L, List.of(10L, 11L))).thenReturn(List.of(showtimeSeat1, showtimeSeat2));
        // 4. khi gọi hàm save booking, trả về chính entity booking được truyền vào
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // 5. giả lập gateway tạo hóa đơn thành công
        when(vinattiGatewayClient.createBill(any(Booking.class), any(), any()))
                .thenReturn(new VinattiBillResponse("RET123", "QR123", "https://simulator.example/checkout/xyz"));

        // when
        BookingResponse response = bookingService.createBooking("customer@gmail.com", request);

        // then
        assertNotNull(response);
        assertEquals(BookingStatus.PENDING, response.status());
        assertEquals("https://simulator.example/checkout/xyz", response.paymentUrl());

        // giá 100.000 * 2 ghế = 200.000
        assertEquals(BigDecimal.valueOf(200000), response.totalAmount());
        assertEquals(2, response.items().size());

        // Kiểm tra ghế đã đổi sang BOOKED
        assertEquals(ShowtimeSeatStatus.BOOKED, showtimeSeat1.getStatus());
        assertEquals(ShowtimeSeatStatus.BOOKED, showtimeSeat2.getStatus());

        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(showtimeSeatRepository, times(1)).saveAll(any());
        verify(vinattiGatewayClient, times(1)).createBill(any(Booking.class), eq("VIETTEL"), any());
    }

    // case 2: đặt ghế đã có người đặt -> 409 seat_already booked
    @Test
    @DisplayName("Đặt vé thất bại và ném SeatAlreadyBookedException khi có ghế không ở trạng thái AVAILABLE")
    void createBooking_SeatAlreadyBooked_ThrowsException() {
        // given giả sử ghế số 2 đã bị người khác đặt trước đó
        showtimeSeat2.setStatus(ShowtimeSeatStatus.BOOKED);

        BookingRequest request = new BookingRequest(100L, List.of(10L, 11L));

        when(userRepository.findByEmail("customer@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(showtimeRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleShowtime));
        when(showtimeSeatRepository.findSeatsWithLock(100L, List.of(10L, 11L))).thenReturn(List.of(showtimeSeat1, showtimeSeat2));

        // when then service phải phát hiện ghế đã bị đặt và ném SeatAlreadyBookedException
        assertThrows(SeatAlreadyBookedException.class, () -> bookingService.createBooking("customer@gmail.com", request)
        );

        // đảm bảo không có đơn đặt vé nào được lưu vào DB khi gặp lỗi
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(vinattiGatewayClient);
    }

    // case 3 request gửi danh sách ghế bị trùng lặp id
    @Test
    @DisplayName("Đặt vé thất bại khi danh sách seatIds gửi lên bị trùng ID")
    void createBooking_DuplicateSeatIds_ThrowsException() {
        // given request gửi cùng 1 id ghế 2 lần
        BookingRequest request = new BookingRequest(100L, List.of(10L, 10L));

        // when then phải ném ngoại lệ appexception ngay từ tầng validate dữ liệu
        assertThrows(AppException.class, () -> bookingService.createBooking("customer@gmail.com", request));
        verifyNoInteractions(vinattiGatewayClient);
    }

    // case 4 đặt vé cho suất chiếu đã bắt đầu hoặc quá giờ
    @Test
    @DisplayName("Đặt vé thất bại khi suất chiếu đã bắt đầu trong quá khứ")
    void createBooking_ShowtimeAlreadyStarted_ThrowsException() {
        // given sửa giờ bắt đầu của suất chiếu lùi về 1 tiếng trước
        sampleShowtime.setStartTime(LocalDateTime.now(clock).minusHours(1));

        BookingRequest request = new BookingRequest(100L, List.of(10L));

        when(userRepository.findByEmail("customer@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(showtimeRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleShowtime));

        // when then không cho phép đặt vé phim đang chiếu
        assertThrows(AppException.class, () -> bookingService.createBooking("customer@gmail.com", request));
        verifyNoInteractions(vinattiGatewayClient);
    }

    // test case 5 xem vé của người khác, 404 not found
    @Test
    @DisplayName("Xem chi tiết vé nhưng không đúng chủ sở hữu thì ném BookingNotFoundException")
    void getBookingDetail_NotOwner_ThrowsException() {
        // given: user 1 muốn xem vé 999, nhưng trong DB không tìm thấy vé 999 nào thuộc về user 1
        when(userRepository.findByEmail("customer@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(bookingRepository.findByIdAndUserId(999L, sampleUser.getId())).thenReturn(Optional.empty());

        // when then trả về 404 BookingNotFoundException để tránh lộ thông tin vé của khách khác
        assertThrows(BookingNotFoundException.class, () -> bookingService.getBookingDetail("customer@gmail.com", 999L));
    }

    // test case 6 huỷ vé thành công và giải phóng ghế
    @Test
    @DisplayName("Hủy vé hợp lệ trước giờ chiếu thì vé CANCELLED và ghế trở về AVAILABLE")
    void cancelBooking_Success_ReleasesSeats() {
        // given tạo 1 đơn vé hợp lệ đã confirmed cho suất chiếu ngày mai
        Booking booking = Booking.builder()
                .id(50L)
                .bookingCode("BK-12345678")
                .user(sampleUser)
                .showtime(sampleShowtime) // suất chiếu chưa diễn ra, vẫn còn ở ngày mai
                .totalAmount(BigDecimal.valueOf(100000))
                .status(BookingStatus.CONFIRMED)
                .build();

        // Ghế hiện đang được gán cho booking này
        showtimeSeat1.setStatus(ShowtimeSeatStatus.BOOKED);
        showtimeSeat1.setBooking(booking);

        when(userRepository.findByEmail("customer@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(bookingRepository.findShowtimeIdByIdAndUserId(50L, sampleUser.getId()))
                .thenReturn(Optional.of(sampleShowtime.getId()));
        when(showtimeRepository.findByIdWithLock(sampleShowtime.getId()))
                .thenReturn(Optional.of(sampleShowtime));
        when(bookingRepository.findByIdAndUserIdWithLock(50L, sampleUser.getId()))
                .thenReturn(Optional.of(booking));
        when(showtimeSeatRepository.findByBookingIdWithLock(50L)).thenReturn(List.of(showtimeSeat1));

        // when thực hiện hủy vé
        BookingResponse response = bookingService.cancelBooking("customer@gmail.com", 50L);

        // giữ thứ tự lấy khóa để tránh các luồng chờ ngược nhau
        var lockOrder = inOrder(bookingRepository, showtimeRepository, showtimeSeatRepository);
        lockOrder.verify(bookingRepository).findShowtimeIdByIdAndUserId(50L, sampleUser.getId());
        lockOrder.verify(showtimeRepository).findByIdWithLock(sampleShowtime.getId());
        lockOrder.verify(bookingRepository).findByIdAndUserIdWithLock(50L, sampleUser.getId());
        lockOrder.verify(showtimeSeatRepository).findByBookingIdWithLock(50L);

        // then
        assertEquals(BookingStatus.CANCELLED, response.status()); // trạng thái đơn đổi sang cancelled
        assertEquals(LocalDateTime.now(clock), response.cancelledAt());

        // ghế phải được giải phóng: trạng thái quay lại available và xóa liên kết với booking cũ
        assertEquals(ShowtimeSeatStatus.AVAILABLE, showtimeSeat1.getStatus());
        assertNull(showtimeSeat1.getBooking());

        // xác nhận đã lưu lại trạng thái mới của ghế vào database
        verify(showtimeSeatRepository, times(1)).saveAll(any());
    }

    //test case 7 huỷ vé khi suất chiếu đã bắt đầu, 409
    @Test
    @DisplayName("Không cho phép hủy vé khi suất chiếu đã diễn ra")
    void cancelBooking_AfterStartTime_ThrowsException() {
        // given suất chiếu đã bắt đầu cách đây 10 phút
        sampleShowtime.setStartTime(LocalDateTime.now(clock).minusMinutes(10));
        Booking booking = Booking.builder()
                .id(50L)
                .user(sampleUser)
                .showtime(sampleShowtime)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(userRepository.findByEmail("customer@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(bookingRepository.findShowtimeIdByIdAndUserId(50L, sampleUser.getId()))
                .thenReturn(Optional.of(sampleShowtime.getId()));
        when(showtimeRepository.findByIdWithLock(sampleShowtime.getId()))
                .thenReturn(Optional.of(sampleShowtime));
        when(bookingRepository.findByIdAndUserIdWithLock(50L, sampleUser.getId()))
                .thenReturn(Optional.of(booking));

        // when then chặn không cho huỷ vé sau giờ chiếu, ném BookingCannotBeCancelledException
        assertThrows(BookingCannotBeCancelledException.class, () -> bookingService.cancelBooking("customer@gmail.com", 50L));
    }

    @Test
    @DisplayName("Không cho đặt vé đúng thời điểm suất chiếu bắt đầu")
    void createBooking_AtStartTime_ThrowsException() {
        // đúng giờ bắt đầu cũng phải từ chối, không chỉ những suất đã quá giờ
        sampleShowtime.setStartTime(LocalDateTime.now(clock));
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(showtimeRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sampleShowtime));

        AppException exception = assertThrows(AppException.class,
                () -> bookingService.createBooking(sampleUser.getEmail(), new BookingRequest(100L, List.of(10L))));

        assertEquals("SHOWTIME_ALREADY_STARTED", exception.getCode());
        verifyNoInteractions(showtimeSeatRepository, bookingRepository, vinattiGatewayClient);
    }

    @Test
    @DisplayName("Không cho hủy vé đúng thời điểm suất chiếu bắt đầu")
    void cancelBooking_AtStartTime_ThrowsException() {
        // vé chỉ được hủy trước giờ bắt đầu
        sampleShowtime.setStartTime(LocalDateTime.now(clock));
        Booking booking = Booking.builder()
                .id(50L)
                .user(sampleUser)
                .showtime(sampleShowtime)
                .status(BookingStatus.CONFIRMED)
                .build();
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(bookingRepository.findShowtimeIdByIdAndUserId(50L, sampleUser.getId()))
                .thenReturn(Optional.of(sampleShowtime.getId()));
        when(showtimeRepository.findByIdWithLock(sampleShowtime.getId()))
                .thenReturn(Optional.of(sampleShowtime));
        when(bookingRepository.findByIdAndUserIdWithLock(50L, sampleUser.getId()))
                .thenReturn(Optional.of(booking));

        assertThrows(BookingCannotBeCancelledException.class,
                () -> bookingService.cancelBooking(sampleUser.getEmail(), 50L));

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verifyNoInteractions(showtimeSeatRepository);
    }

    @Test
    @DisplayName("Booking đã hủy thì không được giải phóng ghế lần nữa")
    void cancelBooking_AlreadyCancelled_DoesNotTouchSeats() {
        // giữ mốc hủy cũ và không giải phóng ghế thêm lần nữa
        LocalDateTime cancelledAt = LocalDateTime.now(clock).minusHours(1);
        Booking booking = Booking.builder()
                .id(50L)
                .user(sampleUser)
                .showtime(sampleShowtime)
                .status(BookingStatus.CANCELLED)
                .cancelledAt(cancelledAt)
                .build();
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(bookingRepository.findShowtimeIdByIdAndUserId(50L, sampleUser.getId()))
                .thenReturn(Optional.of(sampleShowtime.getId()));
        when(showtimeRepository.findByIdWithLock(sampleShowtime.getId()))
                .thenReturn(Optional.of(sampleShowtime));
        when(bookingRepository.findByIdAndUserIdWithLock(50L, sampleUser.getId()))
                .thenReturn(Optional.of(booking));

        assertThrows(BookingCannotBeCancelledException.class,
                () -> bookingService.cancelBooking(sampleUser.getEmail(), 50L));

        assertEquals(cancelledAt, booking.getCancelledAt());
        verifyNoInteractions(showtimeSeatRepository);
    }

    @Test
    @DisplayName("Không phải chủ booking thì trả lỗi trước khi lấy khóa suất hoặc ghế")
    void cancelBooking_NotOwner_DoesNotLockShowtimeOrSeats() {
        // không tìm thấy vé của user thì dừng trước khi lấy khóa
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(bookingRepository.findShowtimeIdByIdAndUserId(50L, sampleUser.getId()))
                .thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class,
                () -> bookingService.cancelBooking(sampleUser.getEmail(), 50L));

        verify(bookingRepository, never()).findByIdAndUserIdWithLock(any(), any());
        verifyNoInteractions(showtimeRepository, showtimeSeatRepository);
    }
}
