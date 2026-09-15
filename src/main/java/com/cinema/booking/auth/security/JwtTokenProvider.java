package com.cinema.booking.auth.security;

import com.cinema.booking.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;
    private SecretKey key;

    @PostConstruct
    public void init() {
        // biến chuỗi string thành mảng byte UTF 8 rồi tạo scretkey phù hợp với thuật toán
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // tạo token chứa email và role của user bằng cách đóng gói thông tin vào chuỗi JWT
    public String generateToken(String email, Role role, Long userId) {
        Date now = new Date(); // thời điểm cấp token
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs); // thời điểm hết hạn token
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    // giải mã token và lấy thông tin email của user
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    // Kiểm tra tính hợp lệ và hạn dùng của token, xác minh token có bị giả mạo hay hết hạn không
    public boolean validateToken(String token) {
        try { // nếu token bị chỉnh sửa hoặc hết hạn thì sẽ ném ra ngoại lệ
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true; //token hoàn toàn hợp lệ và còn hạn
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT token không hợp lệ hoặc đã hết hạn: {}", e.getMessage());
            return false; // từ chối request
        }
    }
}