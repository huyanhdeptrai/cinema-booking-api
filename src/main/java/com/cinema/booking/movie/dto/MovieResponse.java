// dto trả dữ liệu phim cho client
package com.cinema.booking.movie.dto;

import com.cinema.booking.movie.entity.Movie;
import com.cinema.booking.movie.entity.MovieStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovieResponse (
        Long id,
        String title,
        String description,
        Integer durationMinutes,
        String ageRating,
        LocalDate releaseDate,
        MovieStatus status,
        LocalDateTime createdAt
) {
    //chuyển từ movie entity sang movie response DTO
    public static MovieResponse from(Movie movie) {
        return new MovieResponse( // tạo movieresponse mới
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getDurationMinutes(),
                movie.getAgeRating(),
                movie.getReleaseDate(),
                movie.getStatus(),
                movie.getCreatedAt()
        );
    }
}
