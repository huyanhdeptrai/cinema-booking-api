package com.cinema.booking.cinema.repository;

import com.cinema.booking.cinema.entity.Auditorium;
import com.cinema.booking.cinema.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    // Lấy danh sách ghế của một phòng sắp xếp theo hàng và số ghế
    List<Seat> findByAuditoriumIdOrderByRowLabelAscSeatNumberAsc(Long auditoriumId);

    // Đếm số lượng ghế của một phòng
    long countByAuditoriumId(Long auditoriumId);
}