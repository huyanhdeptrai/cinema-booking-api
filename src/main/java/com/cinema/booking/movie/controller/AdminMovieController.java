package com.cinema.booking.movie.controller;

import com.cinema.booking.movie.dto.MovieRequest;
import com.cinema.booking.movie.dto.MovieResponse;
import com.cinema.booking.movie.dto.UpdateMovieStatusRequest;
import com.cinema.booking.movie.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/movies") // định nghĩa toàn bộ tiền tố  URL chung cho toàn bộ controller
@RequiredArgsConstructor
public class AdminMovieController {

    private final MovieService movieService;

    // POST /api/admin/movies: Tạo phim mới
    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody MovieRequest request) {
        MovieResponse response = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/admin/movies: Danh sách toàn bộ phim
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMoviesForAdmin());
    }

    // PUT /api/admin/movies/{id}: Cập nhật toàn bộ thông tin phim theo ID
    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @Valid @RequestBody MovieRequest request
    ) {
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    // PATCH /api/admin/movies/{id}/status: Đổi trạng thái phim
    @PatchMapping("/{id}/status")
    public ResponseEntity<MovieResponse> updateMovieStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMovieStatusRequest request
    ) {
        return ResponseEntity.ok(movieService.updateMovieStatus(id, request));
    }
}