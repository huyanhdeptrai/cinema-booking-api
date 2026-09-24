// cấu trúc phản hồi lỗi chuẩn hóa cho các lỗi xảy ra trong ứng dụng
package com.cinema.booking.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

// Chỉ thị cho Jackson bỏ qua bất kỳ trường nào có giá trị là null khi xuất ra JSON.
@JsonInclude(JsonInclude.Include.NON_NULL)
// dùng java record để tạo đối tượng mang dữ liệu bất biến
public record ErrorResponse(
        Instant timestamp, // mốc thời gian chính xác khi lỗi xảy ra
        int status, // mã trạng thái HTTP dạng số
        String code, // mã định danh lỗi nghiệp vụ viết hoa
        String message, // thông báo mô tả lỗi bằng ngôn ngữ tự nhiên để người đọc dễ hiểu
        String path, // đường dẫn URI của endpoint được gọi khi gặp lỗi
        List<FieldErrorDetail> fieldErrors // danh sách chi tiết các lỗi kiểm tra dữ liệu đầu vào
) {


     //Static Factory Method dùng cho các lỗi nghiệp vụ hoặc hệ thống thông thường (không có lỗi field validation).
     //Tự động lấy Instant.now() làm mốc thời gian và đặt fieldErrors là null
     //VD: 401: INVALID_CREDENTIALS, 404: BOOKING_NOT_FOUND, 409: EMAIL_ALREADY_EXISTS
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, message, path, null);
    }


     //Static Factory Method dùng riêng cho các trường hợp kiểm tra tính hợp lệ dữ liệu
     //Nhận vào danh sách chi tiết các trường bị lỗi để client biết chính xác cần sửa ô dữ liệu nào
     //VD: 400: Gửi email sai định dạng, mật khẩu để trống, số ghế bị âm...
    public static ErrorResponse of(int status, String code, String message, String path, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, code, message, path, fieldErrors);
    }
}