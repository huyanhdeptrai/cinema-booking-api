-- chèn sẵn tài khoản quản trị viên (admin)
-- email: admin@cinema.com
-- mật khẩu mẫu: Admin@123 (lưu dưới dạng BCrypt hash)
INSERT INTO users (email, password_hash, full_name, role)
VALUES (
           'admin@cinema.com',
           '$2a$10$iZIafHeZT61pmnMSER/v4ewnWopMIJ8xgj5/8aYD/pFwkdS2YkM0S', -- hash của 'Admin@123'
           'System Administrator',
           'ADMIN'
       ) ON DUPLICATE KEY UPDATE id=id;
