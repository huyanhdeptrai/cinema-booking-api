// lỗi 404 khi không tìm thấy suất chiếu
package com.cinema.booking.common.exception;

import com.cinema.booking.showtime.entity.Showtime;
import org.springframework.http.HttpStatus;

public class ShowtimeNotFoundException extends AppException {
    public ShowtimeNotFoundException(long id) {
        super(HttpStatus.NOT_FOUND, "SHOWTIME_NOT_FOUND", "Không tìm thấy suất chiếu với ID: " + id);
    }
}
