// tầng giao tiếp trực tiếp với DB, thực hiện các thao tác CRUD với bảng User trong cơ sở dữ liệu
package com.cinema.booking.auth.repository;

import com.cinema.booking.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email); // kiểm tra tồn tại email ch

    Optional<User> findByEmail(String email); //tìm kiếm thông tin ng dùng qua email
}