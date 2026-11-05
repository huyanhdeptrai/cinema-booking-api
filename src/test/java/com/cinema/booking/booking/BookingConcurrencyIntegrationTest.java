package com.cinema.booking.booking;

import com.cinema.booking.TestcontainersConfiguration;
import com.cinema.booking.auth.entity.Role;
import com.cinema.booking.auth.entity.User;
import com.cinema.booking.auth.repository.UserRepository;
import com.cinema.booking.booking.dto.BookingRequest;
import com.cinema.booking.booking.dto.BookingResponse;
import com.cinema.booking.booking.dto.VinattiBillResponse;
import com.cinema.booking.booking.entity.BookingStatus;
import com.cinema.booking.booking.repository.BookingRepository;
import com.cinema.booking.booking.service.BookingService;
import com.cinema.booking.booking.service.VinattiGatewayClient;
import com.cinema.booking.cinema.entity.Auditorium;
import com.cinema.booking.cinema.entity.Seat;
import com.cinema.booking.cinema.repository.AuditoriumRepository;
import com.cinema.booking.cinema.repository.SeatRepository;
import com.cinema.booking.common.exception.AppException;
import com.cinema.booking.common.exception.BookingCannotBeCancelledException;
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
import com.cinema.booking.showtime.service.ShowtimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class) // tự động khởi chạy 1 container mysql riêng để chạy test
public class BookingConcurrencyIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @MockBean
    private VinattiGatewayClient vinattiGatewayClient;

    @Autowired
    private ShowtimeService showtimeService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private Clock clock;

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
        showtimeSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        showtimeRepository.deleteAll();
        seatRepository.deleteAll();
        auditoriumRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        when(vinattiGatewayClient.createBill(any(), any(), any()))
                .thenReturn(new VinattiBillResponse("RET-123", "QR-123", "https://deeplink"));

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
                .releaseDate(LocalDate.now(clock))
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
                .startTime(LocalDateTime.now(clock).plusDays(1))
                .endTime(LocalDateTime.now(clock).plusDays(1).plusMinutes(120))
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

    @ParameterizedTest(name = "{0} khách cùng đặt một ghế")
    @ValueSource(ints = {2, 10, 50})
    @DisplayName("Nhiều khách tranh một ghế thì chỉ một booking được tạo")
    void testConcurrentBookingSameSeat(int numberOfCustomers) throws Exception {
        List<User> customers = new ArrayList<>(List.of(user1, user2));
        for (int index = 2; index < numberOfCustomers; index++) {
            customers.add(userRepository.save(User.builder()
                    .email("customer" + index + "@example.com")
                    .passwordHash("hashed")
                    .fullName("Khách hàng " + index)
                    .role(Role.CUSTOMER)
                    .build()));
        }

        CountDownLatch readyLatch = new CountDownLatch(numberOfCustomers);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfCustomers);
        BookingRequest request = new BookingRequest(showtime.getId(), List.of(seatA1.getId()));
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (User customer : customers) {
                results.add(executor.submit(() -> {
                    readyLatch.countDown();
                    if (!startLatch.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Không nhận được tín hiệu bắt đầu đặt vé");
                    }
                    try {
                        bookingService.createBooking(customer.getEmail(), request);
                        return true;
                    } catch (SeatAlreadyBookedException expected) {
                        return false;
                    }
                }));
            }

            assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "Các khách phải sẵn sàng trước khi bắt đầu");
            startLatch.countDown();
            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    successes++;
                }
            }

            assertEquals(1, successes, "Chỉ một khách được đặt ghế thành công");
            assertEquals(1, bookingRepository.count(), "Chỉ một booking được lưu");
        } finally {
            startLatch.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Các luồng đặt vé phải kết thúc");
        }
    }

    @Test
    @DisplayName("Hủy đồng thời chỉ thành công một lần và không làm mất ghế đã được đặt lại")
    void concurrentCancellation_OnlyOneSucceeds_AndRebookingIsPreserved() throws Exception {
        BookingRequest request = new BookingRequest(showtime.getId(), List.of(seatA1.getId()));
        BookingResponse originalBooking = bookingService.createBooking(user1.getEmail(), request);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> cancellation = () -> {
            readyLatch.countDown();
            if (!startLatch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Không nhận được tín hiệu bắt đầu hủy vé");
            }
            try {
                bookingService.cancelBooking(user1.getEmail(), originalBooking.id());
                return true;
            } catch (BookingCannotBeCancelledException expected) {
                return false;
            }
        };

        try {
            Future<Boolean> first = executor.submit(cancellation);
            Future<Boolean> second = executor.submit(cancellation);
            assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "Hai luồng phải sẵn sàng trước khi bắt đầu");
            startLatch.countDown();

            int successes = (first.get(20, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes, "Chỉ một yêu cầu được hủy booking");
        } finally {
            startLatch.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Các luồng hủy vé phải kết thúc");
        }

        BookingResponse cancelledBooking = bookingService.getBookingDetail(user1.getEmail(), originalBooking.id());
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.status());
        assertTrue(showtimeSeatRepository.findByBookingId(originalBooking.id()).isEmpty());

        BookingResponse newBooking = bookingService.createBooking(user2.getEmail(), request);
        assertThrows(BookingCannotBeCancelledException.class,
                () -> bookingService.cancelBooking(user1.getEmail(), originalBooking.id()));

        List<ShowtimeSeat> rebookedSeats = showtimeSeatRepository.findByBookingId(newBooking.id());
        assertEquals(1, rebookedSeats.size());
        assertEquals(ShowtimeSeatStatus.BOOKED, rebookedSeats.getFirst().getStatus());
        assertEquals(newBooking.id(), rebookedSeats.getFirst().getBooking().getId());
        assertEquals(BookingStatus.PENDING,
                bookingService.getBookingDetail(user2.getEmail(), newBooking.id()).status());
        assertEquals(cancelledBooking.cancelledAt(),
                bookingService.getBookingDetail(user1.getEmail(), originalBooking.id()).cancelledAt());
    }

    @Test
    @DisplayName("Admin hủy suất trước thì khách chờ commit rồi bị từ chối đặt vé")
    void cancelShowtimeFirst_BookingWaitsAndIsRejected() throws Exception {
        BookingRequest request = new BookingRequest(showtime.getId(), List.of(seatA1.getId()));

        AppException failure = runWithFirstTransactionHeld(
                () -> showtimeService.cancelShowtime(showtime.getId()),
                () -> assertThrows(AppException.class,
                        () -> bookingService.createBooking(user1.getEmail(), request))
        );

        assertEquals("SHOWTIME_NOT_AVAILABLE", failure.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, failure.getStatus());
        assertEquals(0, bookingRepository.count());
        assertEquals(ShowtimeStatus.CANCELLED,
                showtimeService.getShowtimeById(showtime.getId()).status());
        List<ShowtimeSeat> seats = showtimeSeatRepository
                .findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(showtime.getId());
        assertEquals(ShowtimeSeatStatus.AVAILABLE, seats.getFirst().getStatus());
    }

    @Test
    @DisplayName("Khách đặt trước thì admin chờ booking commit rồi mới hủy suất")
    void bookingFirst_CancelShowtimeWaitsAndExistingBookingIsPreserved() throws Exception {
        BookingRequest request = new BookingRequest(showtime.getId(), List.of(seatA1.getId()));
        AtomicReference<BookingResponse> createdBooking = new AtomicReference<>();

        var cancelledShowtime = runWithFirstTransactionHeld(
                () -> createdBooking.set(bookingService.createBooking(user1.getEmail(), request)),
                () -> showtimeService.cancelShowtime(showtime.getId())
        );

        assertEquals(ShowtimeStatus.CANCELLED, cancelledShowtime.status());
        assertEquals(ShowtimeStatus.CANCELLED,
                showtimeService.getShowtimeById(showtime.getId()).status());
        assertEquals(1, bookingRepository.count());
        assertEquals(BookingStatus.PENDING,
                bookingService.getBookingDetail(user1.getEmail(), createdBooking.get().id()).status());
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByBookingId(createdBooking.get().id());
        assertEquals(1, seats.size());
        assertEquals(ShowtimeSeatStatus.BOOKED, seats.getFirst().getStatus());
    }

    private <T> T runWithFirstTransactionHeld(
            Runnable firstOperation,
            Callable<T> secondOperation
    ) throws Exception {
        CountDownLatch firstOperationApplied = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        CountDownLatch secondOperationStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        try {
            Future<?> first = executor.submit(() -> transaction.executeWithoutResult(status -> {
                firstOperation.run();
                firstOperationApplied.countDown();
                // giữ transaction thật mở để luồng thứ hai phải chờ khóa MySQL.
                try {
                    if (!allowFirstCommit.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Không nhận được tín hiệu commit");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Transaction bị ngắt khi đang giữ khóa", interrupted);
                }
            }));

            if (!firstOperationApplied.await(20, TimeUnit.SECONDS)) {
                first.get(1, TimeUnit.SECONDS);
                fail("Thao tác đầu tiên chưa lấy được khóa suất chiếu");
            }
            Future<T> second = executor.submit(() -> {
                secondOperationStarted.countDown();
                return secondOperation.call();
            });
            assertTrue(secondOperationStarted.await(10, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(300, TimeUnit.MILLISECONDS),
                    "Thao tác thứ hai phải chờ khi transaction đầu tiên chưa commit");

            allowFirstCommit.countDown();
            first.get(20, TimeUnit.SECONDS);
            return second.get(20, TimeUnit.SECONDS);
        } finally {
            allowFirstCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Các luồng tranh khóa phải kết thúc");
        }
    }
}
