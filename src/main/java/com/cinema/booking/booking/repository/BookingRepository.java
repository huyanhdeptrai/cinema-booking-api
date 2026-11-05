package com.cinema.booking.booking.repository;

import com.cinema.booking.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

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

    // lấy showtimeId để biết cần khóa suất nào.
    @Query("""
    SELECT b.showtime.id
    FROM Booking b
    WHERE b.id = :bookingId
        AND b.user.id = :userId
          """)
    Optional<Long> findShowtimeIdByIdAndUserId(
            @Param("bookingId") Long bookingId,
            @Param("userId") Long userId
    );

    // đọc booking kèm khóa, sau khi đã khóa suất chiếu.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT b
    FROM Booking b
    WHERE b.id = :bookingId
        AND b.user.id = :userId
          """)
    Optional<Booking> findByIdAndUserIdWithLock(
            @Param("bookingId") Long bookingId,
            @Param("userId") Long userId
    );

    // Tìm đơn theo mã đặt vé (dùng cho orderinfo)
    Optional<Booking> findByBookingCode(String bookingCode);

    // Tìm đơn kèm khóa bi quan để cập nhật trạng thái an toàn (dùng cho ordersync)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingCode = :bookingCode")
    Optional<Booking> findByBookingCodeWithLock(@Param("bookingCode") String bookingCode);
}
