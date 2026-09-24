package com.cinema.booking.movie.service;

import com.cinema.booking.common.exception.MovieNotFoundException;
import com.cinema.booking.movie.dto.MovieRequest;
import com.cinema.booking.movie.dto.MovieResponse;
import com.cinema.booking.movie.dto.UpdateMovieStatusRequest;
import com.cinema.booking.movie.entity.Movie;
import com.cinema.booking.movie.entity.MovieStatus;
import com.cinema.booking.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {
    // MovieService bắt buộc phải có MovieRepository và không thay đổi repository sau khi khởi tạo.
    private final MovieRepository movieRepository;

    // tạo phim mới (admin)
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        Movie movie = Movie.builder()
                .title(request.title().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .durationMinutes(request.durationMinutes())
                .ageRating(request.ageRating().trim())
                .releaseDate(request.releaseDate())
                .status(request.status())
                .build();
        Movie savedMovie = movieRepository.save(movie);
        return MovieResponse.from(savedMovie);
    }

    // lấy toàn bộ phim cho admin
    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMoviesForAdmin() {
        return movieRepository.findAll()
                .stream()
                .map(MovieResponse::from)
                .toList();
    }

    // lấy danh sách các phim đang hoạt động cho public
    // quy tắc: chỉ trả phim có trạng thái NOW_SHOWING hoặc COMING_SOON
    @Transactional(readOnly = true)
    public List<MovieResponse> getActiveMoviesForPublic() {
        List<MovieStatus> activeStatuses = List.of(MovieStatus.NOW_SHOWING, MovieStatus.COMING_SOON);
        return movieRepository.findByStatusIn(activeStatuses)
                .stream()
                .map(MovieResponse::from)
                .toList();
    }
    // lấy chi tiết một phim theo ID (admin)
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));
        return MovieResponse.from(movie);
    }

    // cập nhật thông tin phim (admin)
    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        // bước 1 tìm movie hiện tại trong database
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id)); //không tồn tại thì báo lỗi
        // bước 2 thay đổi dữ liệu entity đang được jpa quản lý
        movie.setTitle(request.title().trim());
        movie.setDescription(request.description() != null ? request.description().trim() : null);
        movie.setDurationMinutes(request.durationMinutes());
        movie.setAgeRating(request.ageRating().trim());
        movie.setReleaseDate(request.releaseDate());
        movie.setStatus(request.status());
        // bước 3 entity đến respone
        return MovieResponse.from(movie);
    }

    // đổi trạng thái phim (admin)
    @Transactional
    public MovieResponse updateMovieStatus(Long id, UpdateMovieStatusRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));
        movie.setStatus(request.status());
        return MovieResponse.from(movie);
    }
}