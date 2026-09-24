package com.cinema.booking.movie.service;

import com.cinema.booking.common.exception.MovieNotFoundException;
import com.cinema.booking.movie.dto.MovieResponse;
import com.cinema.booking.movie.entity.Movie;
import com.cinema.booking.movie.entity.MovieStatus;
import com.cinema.booking.movie.repository.MovieRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    @Test
    @DisplayName("API công khai chỉ trả về danh sách phim có trạng thái hoạt động (NOW_SHOWING, COMING_SOON)")
    void getActiveMoviesForPublic_ReturnsOnlyActiveMovies() {
        // given: giả lập repository trả về 2 phim đang hoạt động
        Movie movie1 = Movie.builder()
                .id(1L)
                .title("Dune 2")
                .durationMinutes(166)
                .ageRating("T16")
                .releaseDate(LocalDate.now())
                .status(MovieStatus.NOW_SHOWING)
                .createdAt(LocalDateTime.now())
                .build();
        Movie movie2 = Movie.builder()
                .id(2L)
                .title("Deadpool & Wolverine")
                .durationMinutes(128)
                .ageRating("T18")
                .releaseDate(LocalDate.now().plusDays(7))
                .status(MovieStatus.COMING_SOON)
                .createdAt(LocalDateTime.now())
                .build();
        List<MovieStatus> activeStatuses = List.of(MovieStatus.NOW_SHOWING, MovieStatus.COMING_SOON);
        when(movieRepository.findByStatusIn(activeStatuses)).thenReturn(List.of(movie1, movie2));

        // when
        List<MovieResponse> result = movieService.getActiveMoviesForPublic();
        // then
        assertEquals(2, result.size());
        assertEquals("Dune 2", result.get(0).title());
        assertEquals(MovieStatus.NOW_SHOWING, result.get(0).status());
        assertEquals("Deadpool & Wolverine", result.get(1).title());
        assertEquals(MovieStatus.COMING_SOON, result.get(1).status());
        // Kiểm tra chắc chắn không có phim INACTIVE nào lọt vào
        assertTrue(result.stream().noneMatch(m -> m.status() == MovieStatus.INACTIVE));
    }

    @Test
    @DisplayName("Tìm phim theo ID không tồn tại ném ngoại lệ MovieNotFoundException")
    void getMovieById_NotFound_ThrowsException() {
        // given
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());
        // when then
        assertThrows(MovieNotFoundException.class, () -> movieService.getMovieById(999L));
    }
}