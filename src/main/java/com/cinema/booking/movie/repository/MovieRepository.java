package com.cinema.booking.movie.repository;

import com.cinema.booking.movie.entity.Movie;
import com.cinema.booking.movie.entity.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    // Tìm các phim có trạng thái nằm trong danh sách (vd now_showing hoặc coming_soon)
    List<Movie> findByStatusIn(Collection<MovieStatus> statuses);
}