CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL, --mã hóa mật khẩu
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, --ngày tạo bản ghi
    CONSTRAINT uq_users_email UNIQUE (email) --ngăn chặn trùng lặp email
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; --dùng engine hỗ trợ đầy đủ tiếng việt với emoji