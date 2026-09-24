// phòng chiếu
package com.cinema.booking.cinema.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoriums")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditorium {

    //khoá chính tự tăng cột id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ánh xạ cột tên phòng chiếu
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // số hàng ghế trong phòng
    @Column(name = "row_count", nullable = false)
    private Integer rowCount;

    // số ghế mỗi hàng
    @Column(name = "seats_per_row", nullable = false)
    private Integer seatsPerRow;

    // khi thêm mới bản ghi, hibernate tự lấy thời gian hiện tại của hệ thống gán vào đây
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
