// lấy danh sách ghế theo showtimeid, sắp xếp theo hàng và số ghế tăng dần
package com.cinema.booking.showtime.repository;

import com.cinema.booking.showtime.entity.ShowtimeSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
     // lấy toàn bộ ghế của suất chiếu, sắp xếp theo hàng (rowlabel) và số ghế (seatnumber) của entity seat
     List<ShowtimeSeat> findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(Long showtimeId);

     // 1. khóa các ghế theo danh sách seatid được truyền vào và sắp xếp tăng dần theo seat.id để tránh deadlock
     @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
     @org.springframework.data.jpa.repository.Query("SELECT ss FROM ShowtimeSeat ss WHERE ss.showtime.id = :showtimeId AND ss.seat.id IN :seatIds ORDER BY ss.seat.id ASC")
     java.util.List<ShowtimeSeat> findSeatsWithLock(
             @org.springframework.data.repository.query.Param("showtimeId") Long showtimeId,
             @org.springframework.data.repository.query.Param("seatIds") java.util.List<Long> seatIds
     );

     // 2. tìm danh sách ghế thuộc một booking, phục vụ giải phóng ghế khi hủy vé
     java.util.List<ShowtimeSeat> findByBookingId(Long bookingId);
}