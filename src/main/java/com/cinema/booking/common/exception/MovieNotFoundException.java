// exception không tìm thấy phim
package com.cinema.booking.common.exception;

import org.springframework.http.HttpStatus;

public class MovieNotFoundException extends AppException {
    public MovieNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "MOVIE_NOT_FOUND", "Không tìm thấy phim với ID: " + id);
    }
}
