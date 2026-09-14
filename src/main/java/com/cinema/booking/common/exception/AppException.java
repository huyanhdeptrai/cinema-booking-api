//lớp ngoại lệ gốc cho toàn bộ lỗi trong hệ thống
package com.cinema.booking.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException { //kế thừa RuntimeException để tạo ngoại lệ tùy chỉnh
    private final HttpStatus status; //mã trạng thái HTTP (vd: 400, 404, 500)
    private final String code; //mã định danh lỗi nghiệp vụ viết hoa (vd: EMAIL_ALREADY_EXISTS,...)

    public AppException(HttpStatus status, String code, String message) {
        super(message); //truyền message lên RuntimeException để lưu vết lỗi
        this.status = status;
        this.code = code;
    }
}
