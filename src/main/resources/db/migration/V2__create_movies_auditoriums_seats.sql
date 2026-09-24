-- bảng quản lý thông tin phim
CREATE TABLE movies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INT NOT NULL,
    age_rating VARCHAR(20) NOT NULL,
    release_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_movies_duration CHECK (duration_minutes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- bảng quản lý phòng chiếu
CREATE TABLE auditoriums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    row_count INT NOT NULL,
    seats_per_row INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_auditoriums_name UNIQUE (name),
    CONSTRAINT chk_auditoriums_row_count CHECK (row_count >= 1 AND row_count <= 26),
    CONSTRAINT chk_auditoriums_seats_per_row CHECK (seats_per_row > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng quản lý ghế ngồi
CREATE TABLE seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auditorium_id BIGINT NOT NULL,
    row_label VARCHAR(5) NOT NULL,
    seat_number INT NOT NULL,
    seat_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seats_auditorium FOREIGN KEY (auditorium_id)
    REFERENCES auditoriums(id) ON DELETE CASCADE,
    CONSTRAINT uq_seats_auditorium_seat_code UNIQUE (auditorium_id, seat_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;