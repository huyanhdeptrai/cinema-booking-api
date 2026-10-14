package com.cinema.booking.showtime.repository;

import com.cinema.booking.showtime.entity.Showtime;
import com.cinema.booking.showtime.entity.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // 1. hàm kiểm tra trùng lịch: trong cùng 1 phòng, có suất nào đang SCHEDULED mà thời gian bị giao nhau với khoảng nghỉ 15 phút không
    // để cách 15p, thời gian bảo vệ là startThreshold=newStart-15p, endThreshold=newEnd+15p
    @Query("""
        SELECT COUNT(s) > 0 FROM Showtime s
        WHERE s.auditorium.id = :auditoriumId
          AND s.status = :status
          AND s.startTime < :endThreshold
          AND s.endTime > :startThreshold
    """)
    boolean existsOverlap(
            @Param("auditoriumId") Long auditoriumId,
            @Param("status") ShowtimeStatus status,
            @Param("startThreshold") LocalDateTime startThreshold,
            @Param("endThreshold") LocalDateTime endThreshold
            );
    // 2. Tra cứu suất chiếu công khai: lọc theo phim và khoảng thời gian trong ngày
    @Query("""
        SELECT s FROM Showtime s
        JOIN FETCH s.movie
        JOIN FETCH s.auditorium
        WHERE (:movieId IS NULL OR s.movie.id = :movieId)
          AND s.startTime BETWEEN :startOfDay AND :endOfDay
          AND s.status = 'SCHEDULED'
        ORDER BY s.startTime ASC
    """)
    List<Showtime> findActiveShowtimes(
            @Param("movieId") Long movieId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay")  LocalDateTime endOfDay
    );

    // 3. tra cứu toàn bộ suất chiếu cho admin (kèm thông tin movie và auditorium)
    @Query("""
        SELECT s FROM Showtime s
        JOIN FETCH s.movie
        JOIN FETCH s.auditorium
        ORDER BY s.startTime DESC
    """)
    List<Showtime> findAllWithDetails();

    // 4. kiểm tra phòng chiếu đã có suất chiếu nào chưa
    boolean existsByAuditoriumId(Long auditoriumId);
}