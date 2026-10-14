// api công khai cho khách hàng
package com.cinema.booking.showtime.controller;

import com.cinema.booking.showtime.dto.ShowtimeResponse;
import com.cinema.booking.showtime.dto.ShowtimeSeatResponse;
import com.cinema.booking.showtime.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    // api1: get /api/showtimes?movieId=&date=YYYY-MM-DD tra cứu suất chếu công khai
    @GetMapping
    public ResponseEntity<List<ShowtimeResponse>> getPublicShowtimes(
            @RequestParam(required = false) Long movieId, // lấy tham so movie từ url ?movieId=...
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date // lấy tham số date từ url,tự dong chuyen chuoi yyyy-mm-dd thành localdate
    ) {
        //gọi service xử lý lọc du lieu và dong goi ket qua vào responseetity với http 200 (ok)
        return ResponseEntity.ok(showtimeService.getPublicShowtimes(movieId, date));
    }
    // api2: get /api/showtimes/{id}/seats lấy sơ đồ ghế và trạng thái của suất chiếu
    @GetMapping("/{id}/seats")
    public ResponseEntity<List<ShowtimeSeatResponse>> getShowtimeSeats(@PathVariable Long id) {
        return ResponseEntity.ok(showtimeService.getShowtimeSeats(id)); // lấy sơ đồ và trả về http 200
    }
}