// ghế ngồi
package com.cinema.booking.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_seats_auditorium_seat_code", columnNames = {"auditorium_id", "seat_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    // khoá chính tự tăng
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // nhiều ghế thuộc về một phòng chiếu
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditorium_id", nullable = false)
    private Auditorium auditorium;

    // nhãn mã ghế vd A B C D
    @Column(name = "row_label", nullable = false, length = 5)
    private String rowLabel;

    // số thứ tự của ghế trong hàng
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    // mã ghế đầy đủ sau khi được ghép từ rowlabel với seatnumber
    @Column(name = "seat_code", nullable = false, length = 10)
    private String seatCode;

    // hibernate tự động lấy mốc giờ hiện tại gán vào khi insert bản ghi
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
