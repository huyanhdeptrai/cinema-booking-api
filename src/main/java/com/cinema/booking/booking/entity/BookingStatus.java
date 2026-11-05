// enum trạng thái đặt vé
package com.cinema.booking.booking.entity;

public enum BookingStatus {
    PENDING, // đơn vé mới tạo, đang chờ thanh toán qua cổng ZMA
    CONFIRMED, // đặt thành công
    CANCELLED // đã bị huỷ
}