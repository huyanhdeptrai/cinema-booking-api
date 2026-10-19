// api quản trị cho admin
package com.cinema.booking.showtime.controller;

import com.cinema.booking.showtime.dto.ShowtimeRequest;
import com.cinema.booking.showtime.dto.ShowtimeResponse;
import com.cinema.booking.showtime.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/showtimes")
@RequiredArgsConstructor
public class AdminShowtimeController {

    private final ShowtimeService showtimeService;

    // post /api/admin/showtimes tạo suất chiếu mới
    @PostMapping
    public ResponseEntity<ShowtimeResponse> createShowtime(@Valid @RequestBody ShowtimeRequest request) {
        ShowtimeResponse response = showtimeService.createShowtime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // get /api/admin/showtimes xem danh sách toàn bộ suất chiếu
    @GetMapping
    public ResponseEntity<List<ShowtimeResponse>> getAllShowtimes() {
        return ResponseEntity.ok(showtimeService.getAllShowtimesForAdmin());
    }

    // get /api/admin/showtimes/{id} xem chi tiết suất chiếu
    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> getShowtimeById(@PathVariable Long id) {
        return ResponseEntity.ok(showtimeService.getShowtimeById(id));
    }

    // patch /api/admin/showtimes/{id}/cancel huỷ suất chiếu
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ShowtimeResponse> cancelShowtime(@PathVariable Long id) {
        return ResponseEntity.ok(showtimeService.cancelShowtime(id));
    }
}