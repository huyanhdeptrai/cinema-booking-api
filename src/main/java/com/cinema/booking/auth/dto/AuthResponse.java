package com.cinema.booking.auth.dto;

// DTO trả về cho client khi đăng nhập thành công
public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {
    public static AuthResponse of(String accessToken, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", user); // tokenType mặc định là Bearer
    }
}