// entity ánh xạ bảng movies
package com.cinema.booking.movie.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    // khoá chính
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // tên phim
    @Column(nullable = false)
    private String title;

    //mô tả phim
    @Column(columnDefinition = "TEXT")
    private String description;

    // thời lượng phim
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    // giới hạn độ tuổi
    @Column(name = "age_rating", nullable = false, length = 20)
    private String ageRating;

    // ngày phát hành
    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    // trạng thái phim
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovieStatus status;

    //thời gian tạo
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}