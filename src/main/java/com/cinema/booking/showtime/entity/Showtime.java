// entity này đại diện cho 1 suất chiếu
package com.cinema.booking.showtime.entity;

import com.cinema.booking.cinema.entity.Auditorium;
import com.cinema.booking.movie.entity.Movie;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // nhiều suất chiếu có thể chiếu cùng 1 bộ phim
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    // nhiều suất chiếu có thể diễn ra tại cùng một phòng chiếu
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditorium_id", nullable = false)
    private Auditorium auditorium;

    // thời gian bắt đầu suất chiếu
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // thời gian kết thúc suất chiếu
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // giá vé của suất chiếu
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // trạng thái của suất chiếu
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShowtimeStatus status = ShowtimeStatus.SCHEDULED;

    //hibernate tự động gán thời gian hiện tại khi insert bản ghi, không thể ghi đè khi update
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
