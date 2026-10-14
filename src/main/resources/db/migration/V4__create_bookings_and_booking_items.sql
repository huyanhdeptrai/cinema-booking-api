-- 1. bảng lưu đơn đặt vé, đóng vai trò là bảng master lưu thông tin chung của cả giao dịch đặt vé
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_code VARCHAR(50) NOT NULL, -- mã đặt vé
    user_id BIGINT NOT NULL, -- id khách hàng
    showtime_id BIGINT NOT NULL, -- id suất chiếu
    total_amount DECIMAL(10, 2) NOT NULL, -- tổng tiền
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED', -- trạng thái đơn vé
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- thời gian tạo đơn
    cancelled_at TIMESTAMP NULL, -- thời điểm huỷ đơn
    CONSTRAINT uq_bookings_booking_code UNIQUE (booking_code),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes(id),
    CONSTRAINT chk_bookings_total_amount CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. bảng lưu chi tiết từng ghế trong đơn
CREATE TABLE booking_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL, -- id đơn đặt vé có ghế này
    seat_id BIGINT NOT NULL, -- ghế vật lý trong sơ đồ phòng chiếu
    seat_code VARCHAR(10) NOT NULL, -- mã ghế hiển thị lưu cứng khi để truy xuất nhanh khi in vé, không cần join lại bảng seat
    price DECIMAL(10, 2) NOT NULL, -- giá của ghế
    CONSTRAINT fk_booking_items_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_items_seat FOREIGN KEY (seat_id) REFERENCES seats(id),
    CONSTRAINT chk_booking_items_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- bổ sung booking_id vào bảng showtime_seats để liên kết ghế với đơn đặt
ALTER TABLE showtime_seats -- thêm cột booking_id
    ADD COLUMN booking_id BIGINT NULL,
    ADD CONSTRAINT fk_showtime_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL;