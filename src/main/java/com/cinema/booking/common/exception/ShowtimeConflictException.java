/// lỗi 409 khi bị trùng lịch chiếu
package com.cinema.booking.common.exception;

import com.cinema.booking.showtime.entity.Showtime;
import org.springframework.http.HttpStatus;

public class ShowtimeConflictException extends AppException {
    public ShowtimeConflictException() {
        super(HttpStatus.CONFLICT,"SHOWTIME_OVERLAP", "Phòng chiếu đã có suất chiếu xung đột trong khoảng thời gian này (cần cách nhau ít nhất 15 phút)");
    }
}