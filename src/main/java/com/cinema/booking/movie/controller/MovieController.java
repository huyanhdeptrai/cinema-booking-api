// dành cho khách tra cứu công khai
package com.cinema.booking.movie.controller;

import com.cinema.booking.movie.dto.MovieResponse;
import com.cinema.booking.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    // GET /api/movies: lấy danh sách phim đang hoạt động
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getActiveMovies() {
        return ResponseEntity.ok(movieService.getActiveMoviesForPublic());
    }

    // GET /api/movies/{id}: Xem chi tiết một bộ phim
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }
}