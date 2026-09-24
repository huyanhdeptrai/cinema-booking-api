package com.cinema.booking.cinema.service;

import com.cinema.booking.cinema.dto.AuditoriumRequest;
import com.cinema.booking.cinema.dto.AuditoriumResponse;
import com.cinema.booking.cinema.entity.Auditorium;
import com.cinema.booking.cinema.entity.Seat;
import com.cinema.booking.cinema.repository.AuditoriumRepository;
import com.cinema.booking.cinema.repository.SeatRepository;
import com.cinema.booking.common.exception.AuditoriumNameAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriumServiceTest {

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private AuditoriumService auditoriumService;

    // Dùng ArgumentCaptor để bắt danh sách ghế truyền vào hàm seatRepository.saveAll(...)
    @Captor
    private ArgumentCaptor<List<Seat>> seatsCaptor;

    @Test
    @DisplayName("Tạo phòng thành công: Tự động sinh đúng số lượng và mã ghế theo ma trận A1..An, B1..Bn")
    void createAuditorium_Success_GeneratesCorrectSeats() {
        // given: Giả lập yêu cầu tạo phòng 2 hàng, mỗi hàng 3 ghế (tổng 6 ghế)
        AuditoriumRequest request = new AuditoriumRequest("Phòng Chiếu 1", 2, 3);
        when(auditoriumRepository.existsByName("Phòng Chiếu 1")).thenReturn(false);
        Auditorium savedAuditorium = Auditorium.builder()
                .id(1L)
                .name("Phòng Chiếu 1")
                .rowCount(2)
                .seatsPerRow(3)
                .createdAt(LocalDateTime.now())
                .build();
        when(auditoriumRepository.save(any(Auditorium.class))).thenReturn(savedAuditorium);

        // WHEN: Gọi hàm tạo phòng
        AuditoriumResponse response = auditoriumService.createAuditorium(request);

        // THEN: Xác minh kết quả trả về
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Phòng Chiếu 1", response.name());
        assertEquals(6, response.totalSeats()); // 2 hàng * 3 ghế = 6 ghế

        // Xác minh và "bắt" danh sách ghế được truyền vào hàm saveAll
        verify(seatRepository, times(1)).saveAll(seatsCaptor.capture());
        List<Seat> generatedSeats = seatsCaptor.getValue();

        // 1. kiểm tra tổng số lượng ghế sinh ra phải đúng bằng 6
        assertEquals(6, generatedSeats.size());

        // 2. lấy danh sách mã ghế (seatCode) và kiểm tra thứ tự ma trận
        List<String> seatCodes = generatedSeats.stream().map(Seat::getSeatCode).toList();
        List<String> expectedCodes = List.of("A1", "A2", "A3", "B1", "B2", "B3");
        assertEquals(expectedCodes, seatCodes);

        // 3. kiểm tra mọi ghế đều được gắn với phòng vừa tạo
        assertTrue(generatedSeats.stream().allMatch(seat -> seat.getAuditorium().getId().equals(1L)));
    }

    @Test
    @DisplayName("Tạo phòng thất bại: Trùng tên phòng ném ngoại lệ AuditoriumNameAlreadyExistsException")
    void createAuditorium_DuplicateName_ThrowsException() {
        // given
        AuditoriumRequest request = new AuditoriumRequest("Phòng IMAX", 5, 10);
        when(auditoriumRepository.existsByName("Phòng IMAX")).thenReturn(true);

        // when then
        assertThrows(AuditoriumNameAlreadyExistsException.class, () -> auditoriumService.createAuditorium(request));

        // Đảm bảo không có lệnh lưu phòng hay lưu ghế nào được gọi khi bị trùng tên
        verify(auditoriumRepository, never()).save(any());
        verify(seatRepository, never()).saveAll(any());
    }
}