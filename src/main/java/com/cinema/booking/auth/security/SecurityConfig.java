//cấu hình tổng hợp sercurity, liên kết các thành phần sercurity lại với nhau
package com.cinema.booking.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DelegatedAuthenticationEntryPoint authenticationEntryPoint;
    private final DelegatedAccessDeniedHandler accessDeniedHandler;

    // mã hoá mật khẩu bằng thuật toán bcrypt để băm mật khẩu trước khi lưu vào database
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // quản lý xác thực, được authservice sử dụng để thực hiện xác thực tài khoản
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    //điều phối bảo mật, thiết lập toàn bộ luật bảo mật, các endpoint công khai/hạn chế và chèn JWT filter vào luồng
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF vì ứng dụng REST API dùng token JWT stateless
                .csrf(AbstractHttpConfigurer::disable)
                // Không lưu Session trên server
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Xử lý trả JSON khi gặp lỗi 401 và 403
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // Cấu hình quyền cho từng URL
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập công khai Auth và tài liệu Swagger
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        // Cho phép xem phim và lịch chiếu công khai (cho các tuần sau)
                        .requestMatchers(HttpMethod.GET, "/api/movies/**", "/api/showtimes/**").permitAll()
                        // Các API quản trị bắt buộc quyền ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Các API khác (như booking) yêu cầu phải đăng nhập
                        .anyRequest().authenticated()
                )
                // Đặt JwtFilter trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}