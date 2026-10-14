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
} 
