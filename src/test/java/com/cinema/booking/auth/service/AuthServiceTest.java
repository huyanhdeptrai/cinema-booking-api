// unit test kiểm thử logic nghiệp vụ
package com.cinema.booking.auth.service;

import com.cinema.booking.auth.dto.AuthResponse;
import com.cinema.booking.auth.dto.LoginRequest;
import com.cinema.booking.auth.dto.RegisterRequest;
import com.cinema.booking.auth.entity.Role;
import com.cinema.booking.auth.entity.User;
import com.cinema.booking.auth.repository.UserRepository;
import com.cinema.booking.auth.security.JwtTokenProvider;
import com.cinema.booking.common.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    //tạo các đối tượng giả lập
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;
    //tạo instance thật của authservice
    @InjectMocks
    private AuthService authService;

    // test case 1: đăng ký tài khoản thành công
    @Test
    @DisplayName("Đăng ký thành công: Email được trim, chữ thường, gán role CUSTOMER và trả token")
    void register_Success() {
        // chuẩn bị dữ iệu và kịch bản giả lập, vd người dùng nhập ẩu
        RegisterRequest request = new RegisterRequest("  NewUser@Gmail.COM  ", "password123", " Nguyen Van A ");
        String normalizedEmail = "newuser@gmail.com";
        when(userRepository.existsByEmail(normalizedEmail)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        //giả lập đối tượng user sau khi lưu thành công vào DB
        User savedUser = User.builder()
                .id(1L)
                .email(normalizedEmail)
                .fullName("Nguyen Van A")
                .role(Role.CUSTOMER)
                .passwordHash("hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken(normalizedEmail, Role.CUSTOMER, 1L)).thenReturn("mock.jwt.token");
        // thực thi hàm cần kiểm thử
        AuthResponse response = authService.register(request);
        // kiểm tra kết quả đầu ra có như kỳ vọng
        assertNotNull(response);
        assertEquals("mock.jwt.token", response.accessToken()); // đúng token dc sinh ra
        assertEquals(normalizedEmail, response.user().email()); // email đã được chuẩn hoá về chữ thường và xoá khoảng trắng
        assertEquals(Role.CUSTOMER, response.user().role()); // role được gắn mặc định là customer
        verify(userRepository, times(1)).save(any(User.class)); //xác minh hàm save() có thực sự được gọi đúng 1 lần kh
    }

    // test 2: đăng ký thất bại
    @Test
    @DisplayName("Đăng ký thất bại: Email đã tồn tại ném ngoại lệ EmailAlreadyExistsException")
    void register_EmailAlreadyExists_ThrowsException() {
        // Given
        RegisterRequest request = new RegisterRequest("existing@gmail.com", "password123", "Nguyen Van A");
        when(userRepository.existsByEmail("existing@gmail.com")).thenReturn(true); // giả lập DB báo email đã tồn tại
        // when then kỳ vọng hàm register sẽ ném ra ngoại lệ EmailAlreadyExistsException
        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any()); // xác minh nếu email trùng, hàm save() tuyệt đối kh được chạy
    }

    // test 3: đăng nhập thành công
    @Test
    @DisplayName("Đăng nhập thành công: Trả về thông tin user và accessToken")
    void login_Success() {
        // Given
        LoginRequest request = new LoginRequest("user@gmail.com", "password123");
        User user = User.builder()
                .id(1L)
                .email("user@gmail.com")
                .fullName("Nguyen Van A")
                .role(Role.CUSTOMER)
                .passwordHash("hashedPassword")
                .build();
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user)); // giả lập tìm thấy email
        when(tokenProvider.generateToken("user@gmail.com", Role.CUSTOMER, 1L)).thenReturn("mock.jwt.token"); // giả lập sinh token thành công
        // When
        AuthResponse response = authService.login(request);
        // Then
        assertNotNull(response);
        assertEquals("mock.jwt.token", response.accessToken());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));// xác minh đã được gọi để kiểm tra mk
    }

    // test 4: đăng nhập thất bại do sai mật khẩu
    @Test
    @DisplayName("Đăng nhập thất bại: Sai mật khẩu ném ngoại lệ BadCredentialsException")
    void login_WrongPassword_ThrowsException() {
        // Given
        LoginRequest request = new LoginRequest("user@gmail.com", "wrongpass");
        doThrow(new BadCredentialsException("Sai mật khẩu")) // giả lập AuthenticationManager ném ra BadCredentialsException do mk kh khớp
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        // When Then, kỳ vọng hàm login ném thằng ngoai lệ BadCredentialsException ra ngoài để controller/filter xử lý
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}