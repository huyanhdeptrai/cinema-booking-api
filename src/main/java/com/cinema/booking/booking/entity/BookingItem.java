package com.cinema.booking.booking.entity;

import com.cinema.booking.cinema.entity.Seat;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // thuộc đơn đặt vé nào
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // ghế vật lý tương ứng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // ảnh chụp mã ghế tại thời điểm mua
    @Column(name = "seat_code", nullable = false, length = 10)
    private String seatCode;

    // ảnh chụp giá vé tại thời điểm mua
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}