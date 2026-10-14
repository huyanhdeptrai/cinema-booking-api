package com.cinema.booking.showtime.service;

import com.cinema.booking.cinema.entity.Auditorium;
import com.cinema.booking.cinema.entity.Seat;
import com.cinema.booking.cinema.repository.AuditoriumRepository;
import com.cinema.booking.cinema.repository.SeatRepository;
import com.cinema.booking.common.exception.AuditoriumNotFoundException;
import com.cinema.booking.common.exception.MovieNotFoundException;
import com.cinema.booking.common.exception.ShowtimeConflictException;
import com.cinema.booking.common.exception.ShowtimeNotFoundException;
import com.cinema.booking.movie.entity.Movie;
import com.cinema.booking.movie.repository.MovieRepository;
import com.cinema.booking.showtime.dto.ShowtimeRequest;
import com.cinema.booking.showtime.dto.ShowtimeResponse;
import com.cinema.booking.showtime.dto.ShowtimeSeatResponse;
import com.cinema.booking.showtime.entity.Showtime;
import com.cinema.booking.showtime.entity.ShowtimeSeat;
import com.cinema.booking.showtime.entity.ShowtimeSeatStatus;
import com.cinema.booking.showtime.entity.ShowtimeStatus;
import com.cinema.booking.showtime.repository.ShowtimeRepository;
import com.cinema.booking.showtime.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final MovieRepository movieRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // 1. tạo suất chiếu mới và tự động sinh tồn kho ghế
    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeRequest request) {
        // b1 khoá phòng chiếu bằng pessimistic lock để chống tạo trùng lịch đồng thời
        Auditorium auditorium = auditoriumRepository.findByIdWithLock(request.auditoriumId()).orElseThrow(() -> new AuditoriumNotFoundException(request.auditoriumId()));

        // b2 tìm bộ phim
        Movie movie = movieRepository.findById(request.movieId()).orElseThrow(() -> new MovieNotFoundException(request.movieId()));

        // b3 tính endtime = starttime + durationminutes
        LocalDateTime startTime = request.startTime();
        LocalDateTime endTime = startTime.plusMinutes(movie.getDurationMinutes());

        //b4 kiểm tra xung đột lịch với khoảng đệm 15p
        LocalDateTime startThreshold = startTime.minusMinutes(15);
        LocalDateTime endThreshold = endTime.plusMinutes(15);

        boolean isOverlap = showtimeRepository.existsOverlap(
                auditorium.getId(),
                ShowtimeStatus.SCHEDULED,
                startThreshold,
                endThreshold
        );

        if (isOverlap) {
            throw new ShowtimeConflictException();
        }

        // b5 lưu thông tin suất chiếu
        Showtime showtime = Showtime.builder()
                .movie(movie)
                .auditorium(auditorium)
                .startTime(startTime)
                .endTime(endTime)
                .price(request.price())
                .status(ShowtimeStatus.SCHEDULED)
                .build();
        Showtime savedShowtime = showtimeRepository.save(showtime);

        // b6 nhân bản toàn bộ ghế của phòng sang bảng tồn kho ghế (showtime_seats)
        List<Seat> seats = seatRepository.findByAuditoriumIdOrderByRowLabelAscSeatNumberAsc(auditorium.getId());
        List<ShowtimeSeat> showtimeSeats = seats.stream()
                .map(seat -> ShowtimeSeat.builder()
                        .showtime(savedShowtime)
                        .seat(seat)
                        .status(ShowtimeSeatStatus.AVAILABLE)
                        .build())
                .toList();
        showtimeSeatRepository.saveAll(showtimeSeats);

        return ShowtimeResponse.from(savedShowtime);
    }

    // 2. tra cứu suất chiếu công khai ( lọc theo phim và ngày )
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getPublicShowtimes(Long movieId, LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now(VIETNAM_ZONE);
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);
        return showtimeRepository.findActiveShowtimes(movieId, startOfDay, endOfDay).stream().map(ShowtimeResponse::from).toList();
    }

    // 3. lấy danh sách toàn bộ suất chiếu cho admin
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getAllShowtimesForAdmin() {
        return showtimeRepository.findAllWithDetails().stream().map(ShowtimeResponse::from).toList();
    }

    // 4. lấy chi tiết một suất chiếu theo ID
    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(Long id) {
        Showtime showtime = showtimeRepository.findById(id).orElseThrow(() -> new ShowtimeNotFoundException(id));
        return ShowtimeResponse.from(showtime);
    }

    // 5. lấy sơ đồ ghế và trạng thái ghế của suất chiếu (phục vụ chọn ghế)
    @Transactional(readOnly = true)
    public List<ShowtimeSeatResponse> getShowtimeSeats(Long showtimeId) {
        if (!showtimeRepository.existsById(showtimeId)) {
            throw new ShowtimeNotFoundException(showtimeId);
        }
        return showtimeSeatRepository.findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(showtimeId).stream().map(ShowtimeSeatResponse::from).toList();
    }

    // 6. huỷ suất chiếu (admin)
    @Transactional
    public ShowtimeResponse cancelShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ShowtimeNotFoundException(id));
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        return ShowtimeResponse.from(showtime);
    }
}