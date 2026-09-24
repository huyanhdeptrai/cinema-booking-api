package com.cinema.booking.cinema.controller;

import com.cinema.booking.cinema.dto.AuditoriumRequest;
import com.cinema.booking.cinema.dto.AuditoriumResponse;
import com.cinema.booking.cinema.dto.SeatResponse;
import com.cinema.booking.cinema.service.AuditoriumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/auditoriums")
@RequiredArgsConstructor
public class AdminAuditoriumController {

    private final AuditoriumService auditoriumService;

    // POST /api/admin/auditoriums: Tạo phòng và tự động sinh ghế
    @PostMapping
    public ResponseEntity<AuditoriumResponse> createAuditorium(@Valid @RequestBody AuditoriumRequest request) {
        AuditoriumResponse response = auditoriumService.createAuditorium(request); //gọi service xử lý lưu phòng và sinh các ghế từ A1 - Z
        return ResponseEntity.status(HttpStatus.CREATED).body(response); //Trả về mã HTTP 201 CREATED chuẩn restful khi tạo mới tài nguyên thành công
    }

    // GET /api/admin/auditoriums: Xem danh sách tất cả phòng
    @GetMapping
    public ResponseEntity<List<AuditoriumResponse>> getAllAuditoriums() {
        // trả về HTTP 200 OK kèm mảng JSON chứa danh sách các phòng và tổng số ghế của từng phòng
        return ResponseEntity.ok(auditoriumService.getAllAuditoriums());
    }

    // GET /api/admin/auditoriums/{id}: Xem chi tiết một phòng
    @GetMapping("/{id}")
    public ResponseEntity<AuditoriumResponse> getAuditoriumById(@PathVariable Long id) {
        // trả về HTTP 200 OK nếu tìm thấy, hoặc tự động ném ngoại lệ 404 nếu id không tồn tại
        return ResponseEntity.ok(auditoriumService.getAuditoriumById(id));
    }

    // GET /api/admin/auditoriums/{id}/seats: Xem danh sách ghế đã sinh của phòng
    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(@PathVariable Long id) {
        // trả về HTTP 200 OK kèm mảng danh sách các ghế (A1, A2, B1...) đã được sinh trong database
        // phục vụ kiểm tra dữ liệu hoặc vẽ sơ đồ ma trận ghế cho admin
        return ResponseEntity.ok(auditoriumService.getSeatsByAuditoriumId(id));
    }

    // PUT /api/admin/auditoriums/{id}: Cập nhật phòng chiếu
    @PutMapping("/{id}")
    public ResponseEntity<AuditoriumResponse> updateAuditorium(
            @PathVariable Long id,
            @Valid @RequestBody AuditoriumRequest request
    ) {
        // gọi service cập nhật và trả về HTTP 200 ok cùng thông tin phòng sau khi đổi
        return ResponseEntity.ok(auditoriumService.updateAuditorium(id, request));
    }
}
