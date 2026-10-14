package com.cinema.booking.booking.repository;

import com.cinema.booking.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // lấy danh sách vé của khách hàng hiện tại, sắp xếp mới nhất lên đầu
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // lấy chi tiết vé nhưng bắt buộc phải đúng userid, đảm bảo khách chỉ xem được vé của mình
    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    // kiểm tra bookingcode đã tồn tại chưa, để tránh trùng mã vé khi sinh ngẫu nhiên
    boolean existsByBookingCode(String bookingCode);
}
