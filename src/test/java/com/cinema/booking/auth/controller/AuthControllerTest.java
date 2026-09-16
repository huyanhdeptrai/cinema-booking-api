// MockMvc test: kiểm thử tầng HTTP API và validation
package com.cinema.booking.auth.controller;

import com.cinema.booking.auth.dto.AuthResponse;
import com.cinema.booking.auth.dto.LoginRequest;
import com.cinema.booking.auth.dto.RegisterRequest;
import com.cinema.booking.auth.dto.UserResponse;
import com.cinema.booking.auth.entity.Role;
import com.cinema.booking.auth.security.JwtAuthenticationFilter;
import com.cinema.booking.auth.security.JwtTokenProvider;
import com.cinema.booking.auth.service.AuthService;
import com.cinema.booking.common.exception.EmailAlreadyExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // tắt filter bảo mật để tập trung test controller validation
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private AuthService authService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // testcase 1: đăng ký thành công
    @Test
    @DisplayName("API Đăng ký thành công trả về HTTP 201 và thông tin token")
    void register_Success_Returns201() throws Exception {
        // given chuẩn bị dữ liệu mẫu
        RegisterRequest request = new RegisterRequest("test@gmail.com", "password123", "Nguyen Van A");
        UserResponse userResponse = new UserResponse(1L, "test@gmail.com", "Nguyen Van A", Role.CUSTOMER, LocalDateTime.now());
        AuthResponse authResponse = AuthResponse.of("mock-token", userResponse);
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse); //giả lập AuthService xử lý và trả về authResponse
        // when then, gửi request và xác minh kết quả
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock-token"))
                .andExpect(jsonPath("$.user.email").value("test@gmail.com"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    // test case 2: validate dữ liệu vào thất bại, HTTP 400 Bad Request
    @Test
    @DisplayName("API Đăng ký với dữ liệu sai (thiếu họ tên, email sai định dạng) trả về HTTP 400")
    void register_InvalidInput_Returns400() throws Exception {
        // Request thiếu fullName và email sai cú pháp
        RegisterRequest request = new RegisterRequest("invalid-email", "123", "");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // xác minh trả về http 400
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED")) // format lỗi
                .andExpect(jsonPath("$.fieldErrors").isArray()); // chứa mảng chi tiết các trường bị lỗi
    }

    //test case 3: đăng ký email đã tồn tại, HTTP 409 Conflict
    @Test
    @DisplayName("API Đăng ký với email trùng lặp trả về HTTP 409 EMAIL_ALREADY_EXISTS")
    void register_DuplicateEmail_Returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("duplicate@gmail.com", "password123", "Nguyen Van A");
        when(authService.register(any(RegisterRequest.class))) // giả lập authService ném ngoại lệ khi email trùng
                .thenThrow(new EmailAlreadyExistsException("duplicate@gmail.com"));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // GlobalExceptionHandler bắt exeption và map thành mã http 409
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    // test case 4: đăng nhập thành công, mong đợi HTTP 200 OK
    @Test
    @DisplayName("API Đăng nhập thành công trả về HTTP 200")
    void login_Success_Returns200() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@gmail.com", "password123");
        UserResponse userResponse = new UserResponse(1L, "test@gmail.com", "Nguyen Van A", Role.CUSTOMER, LocalDateTime.now());
        AuthResponse authResponse = AuthResponse.of("mock-token", userResponse);
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);
        // when then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-token"));
    }
}