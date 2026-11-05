package com.cinema.booking.showtime.service;

import com.cinema.booking.common.exception.ShowtimeNotFoundException;

import com.cinema.booking.cinema.entity.Auditorium;
import com.cinema.booking.cinema.entity.Seat;
import com.cinema.booking.cinema.repository.AuditoriumRepository;
import com.cinema.booking.cinema.repository.SeatRepository;
import com.cinema.booking.common.exception.ShowtimeConflictException;
import com.cinema.booking.movie.entity.Movie;
import com.cinema.booking.movie.entity.MovieStatus;
import com.cinema.booking.movie.repository.MovieRepository;
import com.cinema.booking.showtime.dto.ShowtimeRequest;
import com.cinema.booking.showtime.dto.ShowtimeResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTest {

    // mock các repository phụ thuộc để cô lập tầng service cần kiểm thử
    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @Mock
    private SeatRepository seatRepository;

    private ShowtimeService showtimeService;

    // cố định lúc 00:30 giờ Việt Nam, UTC vẫn đang là ngày hôm trước
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-21T17:30:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh")
    );

    // bắt danh sách ghế được truyền vào hàm saveAll để kiểm tra dữ liệu bên trong
    @Captor
    private ArgumentCaptor<List<ShowtimeSeat>> showtimeSeatsCaptor;

    @BeforeEach
    void setUp() {
        // truyền cùng clock cho service và dữ liệu dùng trong test
        showtimeService = new ShowtimeService(
                showtimeRepository,
                showtimeSeatRepository,
                movieRepository,
                auditoriumRepository,
                seatRepository,
                clock
        );
    }

    @Test
    @DisplayName("Tạo suất chiếu thành công: Tự tính đúng endtime và sinh đủ ghế tồn kho (AVAILABLE)")
    void createShowtime_Success() {
        // given chuẩn bị dữ liệu mẫu
        Long movieId = 1L;
        Long auditoriumId = 1L;
        LocalDateTime startTime = LocalDateTime.now(clock).plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        BigDecimal price = new BigDecimal("85000.00");

        ShowtimeRequest request = new ShowtimeRequest(movieId, auditoriumId, startTime, price);

        // phim có thời lượng 120 phút
        Movie movie = Movie.builder()
                .id(movieId)
                .title("Mai")
                .durationMinutes(120)
                .ageRating("T18")
                .releaseDate(LocalDate.now(clock))
                .status(MovieStatus.NOW_SHOWING)
                .build();

        Auditorium auditorium = Auditorium.builder()
                .id(auditoriumId)
                .name("Phòng 1")
                .rowCount(1)
                .seatsPerRow(2)
                .build();

        // phòng 1 có 2 ghế A1 và A2
        Seat seat1 = Seat.builder().id(101L).auditorium(auditorium).seatCode("A1").rowLabel("A").seatNumber(1).build();
        Seat seat2 = Seat.builder().id(102L).auditorium(auditorium).seatCode("A2").rowLabel("A").seatNumber(2).build();

        // giả lập hành vi của các repository
        when(auditoriumRepository.findByIdWithLock(auditoriumId)).thenReturn(Optional.of(auditorium));
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(showtimeRepository.existsOverlap(any(), any(), any(), any())).thenReturn(false);

        // Giả lập lưu suất chiếu thành công
        when(showtimeRepository.save(any(Showtime.class))).thenAnswer(invocation -> {
            Showtime s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        when(seatRepository.findByAuditoriumIdOrderByRowLabelAscSeatNumberAsc(auditoriumId))
                .thenReturn(List.of(seat1, seat2));

        // when gọi hàm thực thi tạo suất chiếu
        ShowtimeResponse response = showtimeService.createShowtime(request);

        // then kiểm tra kết quả
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Mai", response.movieTitle());
        assertEquals("Phòng 1", response.auditoriumName());

        // 1. kiểm tra endTime = startTime + 120 phút
        assertEquals(startTime.plusMinutes(120), response.endTime());

        // 2. kiểm tra việc tự sinh ghế tồn kho
        verify(showtimeSeatRepository, times(1)).saveAll(showtimeSeatsCaptor.capture());
        List<ShowtimeSeat> generatedSeats = showtimeSeatsCaptor.getValue();

        assertEquals(2, generatedSeats.size());
        assertEquals(ShowtimeSeatStatus.AVAILABLE, generatedSeats.get(0).getStatus());
        assertEquals(ShowtimeSeatStatus.AVAILABLE, generatedSeats.get(1).getStatus());
        assertEquals(seat1, generatedSeats.get(0).getSeat());
        assertEquals(seat2, generatedSeats.get(1).getSeat());
    }

    @Test
    @DisplayName("Tạo suất chiếu thất bại: Trùng lịch chiếu trong phòng ném ShowtimeConflictException (409 SHOWTIME_OVERLAP)")
    void createShowtime_Overlap_ThrowsException() {
        // given
        ShowtimeRequest request = new ShowtimeRequest(
                1L, 1L,
                LocalDateTime.now(clock).plusDays(1).withHour(14).withMinute(0),
                new BigDecimal("90000.00")
        );
        Auditorium auditorium = Auditorium.builder().id(1L).name("Phòng 1").build();
        Movie movie = Movie.builder().id(1L).durationMinutes(90).build();
        when(auditoriumRepository.findByIdWithLock(1L)).thenReturn(Optional.of(auditorium));
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));

        // giả lập kiểm tra bị trùng lịch (existsoverlap = true)
        when(showtimeRepository.existsOverlap(any(), any(), any(), any())).thenReturn(true);

        // when then: kỳ vọng ném ra lỗi showtimeconflictexception
        assertThrows(ShowtimeConflictException.class, () -> showtimeService.createShowtime(request));

        // Đảm bảo không có lệnh lưu dữ liệu nào được thực hiện khi bị trùng lịch
        verify(showtimeRepository, never()).save(any());
        verify(showtimeSeatRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Hủy suất chiếu thành công: Trạng thái đổi thành CANCELLED")
    void cancelShowtime_Success() {
        // given
        Auditorium auditorium = Auditorium.builder().id(1L).name("Phòng 1").build();
        Movie movie = Movie.builder().id(1L).title("Dune 2").durationMinutes(150).build();

        Showtime existingShowtime = Showtime.builder()
                .id(5L)
                .movie(movie)
                .auditorium(auditorium)
                .startTime(LocalDateTime.now(clock).plusDays(2))
                .endTime(LocalDateTime.now(clock).plusDays(2).plusMinutes(150))
                .price(new BigDecimal("100000.00"))
                .status(ShowtimeStatus.SCHEDULED)
                .build();

        when(showtimeRepository.findByIdWithLock(5L)).thenReturn(Optional.of(existingShowtime));

        // when
        ShowtimeResponse response = showtimeService.cancelShowtime(5L);

        // then
        assertNotNull(response);
        assertEquals(ShowtimeStatus.CANCELLED, response.status());
        // hủy suất phải dùng cùng khóa với luồng đặt vé
        verify(showtimeRepository).findByIdWithLock(5L);
        verify(showtimeRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Không truyền ngày thì tra cứu theo ngày Việt Nam khi UTC vẫn là hôm trước")
    void getPublicShowtimes_NoDate_UsesVietnamDate() {
        // giờ Việt Nam đã sang ngày mới nên không được lấy ngày theo UTC
        LocalDate vietnamDate = LocalDate.of(2026, 10, 22);
        when(showtimeRepository.findActiveShowtimes(
                null, vietnamDate.atStartOfDay(), vietnamDate.atTime(LocalTime.MAX)))
                .thenReturn(List.of());

        assertTrue(showtimeService.getPublicShowtimes(null, null).isEmpty());

        verify(showtimeRepository).findActiveShowtimes(
                null, vietnamDate.atStartOfDay(), vietnamDate.atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("Ngày được truyền vào được ưu tiên hơn ngày hiện tại của clock")
    void getPublicShowtimes_ExplicitDate_UsesRequestedDate() {
        // khách đã chọn ngày thì dùng ngày đó, không lấy ngày từ clock
        LocalDate requestedDate = LocalDate.of(2026, 11, 5);
        when(showtimeRepository.findActiveShowtimes(
                1L, requestedDate.atStartOfDay(), requestedDate.atTime(LocalTime.MAX)))
                .thenReturn(List.of());

        assertTrue(showtimeService.getPublicShowtimes(1L, requestedDate).isEmpty());

        verify(showtimeRepository).findActiveShowtimes(
                1L, requestedDate.atStartOfDay(), requestedDate.atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("Hủy suất không tồn tại thì trả lỗi không tìm thấy")
    void cancelShowtime_NotFound_ThrowsException() {
        // không có suất này thì trả lỗi và không đụng đến ghế
        when(showtimeRepository.findByIdWithLock(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ShowtimeNotFoundException.class,
                () -> showtimeService.cancelShowtime(999L)
        );

        verify(showtimeRepository).findByIdWithLock(999L);
        verify(showtimeRepository, never()).findById(any());
        verifyNoInteractions(showtimeSeatRepository);
    }
}
