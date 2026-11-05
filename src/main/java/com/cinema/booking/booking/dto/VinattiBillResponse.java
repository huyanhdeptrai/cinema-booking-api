package com.cinema.booking.booking.dto;

public record VinattiBillResponse(
        String retRefNumber, // mã giao dịch do Gateway cấp
        String qrCode, // chuỗi QR mô phỏng
        String deepLink // URL trang thanh toán giả lập dành cho khách
) {
}