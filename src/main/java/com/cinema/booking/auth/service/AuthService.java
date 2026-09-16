// xử lý các quy tắc nghiệp vụ cốt lõi
package com.cinema.booking.auth.service;

import com.cinema.booking.auth.dto.AuthResponse;
import com.cinema.booking.auth.dto.LoginRequest;
import com.cinema.booking.auth.dto.RegisterRequest;
import com.cinema.booking.auth.dto.UserResponse;
import com.cinema.booking.auth.entity.Role;
import com.cinema.booking.auth.entity.User;
import com.cinema.booking.auth.repository.UserRepository;
import com.cinema.booking.auth.security.JwtTokenProvider;
import com.cinema.booking.common.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    // 1. Nghiệp vụ đăng ký tài khoản
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Cắt khoảng trắng thừa hai đầu và đưa toàn bộ về chữ thường
        String email = request.email().trim().toLowerCase();

        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        // Tạo User mới với Role mặc định là CUSTOMER
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(Role.CUSTOMER)
                .build();

        // Lưu User vào cơ sở dữ liệu
        User savedUser = userRepository.save(user);

        // Đăng ký thành công thì cấp luôn token JWT
        String token = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole(), savedUser.getId());

        // Đóng gói JWT và thông tin cơ bản của User vào AuthResponse DTO trả về cho client
        return AuthResponse.of(token, UserResponse.from(savedUser));
    }

    // 2. Nghiệp vụ đăng nhập tài khoản
    public AuthResponse login(LoginRequest request) {
        // Chuẩn hóa email
        String email = request.email().trim().toLowerCase();

        // Xác thực email và mật khẩu qua Spring Security
        // Nếu sai mật khẩu hoặc user không tồn tại, hàm này tự động ném ngoại lệ (GlobalExceptionHandler sẽ bắt và trả mã 401)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        // Truy vấn DB lấy toàn bộ thông tin User để tạo token và response
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());

        // Trả về token và thông tin user cho Client
        return AuthResponse.of(token, UserResponse.from(user));
    }
}