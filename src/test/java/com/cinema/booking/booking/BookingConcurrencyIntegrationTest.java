package com.cinema.booking.booking;

import com.cinema.booking.TestcontainersConfiguration;
import com.cinema.booking.auth.entity.Role;
import com.cinema.booking.auth.entity.User;
import com.cinema.booking.auth.repository.UserRepository;
import com.cinema.booking.booking.dto.BookingRequest;
import com.cinema.booking.booking.repository.BookingRepository;
import com.cinema.booking.booking.service.BookingService;
import com.cinema.booking.cinema.entity.Auditorium;
import com.cinema.booking.cinema.entity.Seat;
import com.cinema.booking.cinema.repository.AuditoriumRepository;
import com.cinema.booking.cinema.repository.SeatRepository;
import com.cinema.booking.common.exception.SeatAlreadyBookedException;
import com.cinema.booking.movie.entity.Movie;
import com.cinema.booking.movie.entity.MovieStatus;
import com.cinema.booking.movie.repository.MovieRepository;
import com.cinema.booking.showtime.entity.Showtime;
import com.cinema.booking.showtime.entity.ShowtimeSeat;
import com.cinema.booking.showtime.entity.ShowtimeSeatStatus;
import com.cinema.booking.showtime.entity.ShowtimeStatus;
import com.cinema.booking.showtime.repository.ShowtimeRepository;
import com.cinema.booking.showtime.repository.ShowtimeSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(TestcontainersConfiguration.class) // tự động khởi chạy 1 container mysql riêng để chạy test
public class BookingConcurrencyIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private AuditoriumRepository auditoriumRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;
    @Autowired
    private BookingRepository bookingRepository;

    private User user1;
    private User user2;
    private Showtime showtime;
    private Seat seatA1;

    @BeforeEach
    void setUp() {
        // dọn sạch dữ liệu cũ theo thứ tự quan hệ bảng (bảng con trước, bảng cha sau)
        bookingRepository.deleteAll();
        showtimeSeatRepository.deleteAll();
        showtimeRepository.deleteAll();
        seatRepository.deleteAll();
        auditoriumRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        // 1. tạo 2 khách hàng khác nhau
        user1 = userRepository.save(User.builder()
                .email("user1@gmail.com")
                .passwordHash("hashed")
                .fullName("Khách hàng 1")
                .role(Role.CUSTOMER)
                .build());

        user2 = userRepository.save(User.builder()
                .email("user2@gmail.com")
                .passwordHash("hashed")
                .fullName("Khách hàng 2")
                .role(Role.CUSTOMER)
                .build());

        // 2. tạo 1 bộ phim
        Movie movie = movieRepository.save(Movie.builder()
                .title("Inception")
                .description("Phim hành động giả tưởng")
                .durationMinutes(120)
                .ageRating("T16")
                .releaseDate(LocalDate.now())
                .status(MovieStatus.NOW_SHOWING)
                .build());

        // 3. tạo phòng và 1 ghế A1
        Auditorium auditorium = auditoriumRepository.save(Auditorium.builder()
                .name("Phòng Test Concurrency")
                .rowCount(1)
                .seatsPerRow(1)
                .build());

        seatA1 = seatRepository.save(Seat.builder()
                .auditorium(auditorium)
                .rowLabel("A")
                .seatNumber(1)
                .seatCode("A1")
                .build());

        // 4. tạo suất chiếu ngày mai
        showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .auditorium(auditorium)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusMinutes(120))
                .price(BigDecimal.valueOf(100000))
                .status(ShowtimeStatus.SCHEDULED)
                .build());

        // 5. tạo tồn kho ghế cho suất chiếu (ban đầu ghế available)
        showtimeSeatRepository.save(ShowtimeSeat.builder()
                .showtime(showtime)
                .seat(seatA1)
                .status(ShowtimeSeatStatus.AVAILABLE)
                .build());
    }

    @Test
    @DisplayName("Hai yêu cầu đồng thời đặt cùng một ghế: Chỉ 1 yêu cầu thành công, 1 yêu cầu bị từ chối")
    void testConcurrentBookingSameSeat() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads); // đợi cả 2 luồng vào vạch xuất phát
        CountDownLatch startLatch = new CountDownLatch(1); // phát lệnh bắt đầu

        AtomicInteger successCount = new AtomicInteger(0); // đếm số người đặt thành công
        AtomicInteger conflictCount = new AtomicInteger(0); // đếm số người bị lỗi ghế đã đặt

        // cả 2 cùng gửi request đặt ghế A1 cho suất chiếu trên
        BookingRequest request = new BookingRequest(showtime.getId(), List.of(seatA1.getId()));

        // luồng 1: user 1 đặt ghế A1
        executorService.submit(() -> {
            readyLatch.countDown(); // báo đã sẵn sàng
            try {
                startLatch.await(); // đợi hiệu lệnh
                bookingService.createBooking(user1.getEmail(), request);
                successCount.incrementAndGet(); // nếu đặt thành công thì tăng biến đếm
            } catch (SeatAlreadyBookedException e) {
                conflictCount.incrementAndGet(); // bắt đúng lỗi 409 khi ghế đã bị người kia lấy
            } catch (Exception e) {
                // các lỗi khác nếu có
            }
        });

        // luồng 2: user 2 đặt ghế A1
        executorService.submit(() -> {
            readyLatch.countDown(); // báo đã sẵn sàng
            try {
                startLatch.await(); // đợi hiệu lệnh
                bookingService.createBooking(user2.getEmail(), request);
                successCount.incrementAndGet();
            } catch (SeatAlreadyBookedException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                // các lỗi khác nếu có
            }
        });

        // đợi cả 2 luồng đều sẵn sàng
        readyLatch.await();
        // nổ lệnh: Cả 2 luồng cùng lúc gọi bookingService.createBooking
        startLatch.countDown();

        // đóng thread pool và đợi tối đa 10 giây để 2 luồng hoàn tất
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // kiểm tra kết quả
        // 1. phải có đúng 1 người đặt vé thành công
        assertEquals(1, successCount.get(), "Chỉ được phép có đúng 1 booking thành công");
        // 2. phải có đúng 1 người nhận ngoại lệ SeatAlreadyBookedException
        assertEquals(1, conflictCount.get(), "Yêu cầu thứ 2 phải nhận SeatAlreadyBookedException (409)");
        // 3. trong bảng bookings chỉ tồn tại đúng 1 đơn hàng
        assertEquals(1, bookingRepository.count(), "Chỉ có 1 bản ghi Booking được lưu trong DB");
    }
}