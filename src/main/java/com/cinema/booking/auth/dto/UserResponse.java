// định hình dữ liệu thông tin user an toàn trả về cho client
package com.cinema.booking.auth.dto;

import com.cinema.booking.auth.entity.Role;
import com.cinema.booking.auth.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        LocalDateTime createdAt
) {
    // static factory method giúp chuyển đổi nhanh từ user entity sang userResponse
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
