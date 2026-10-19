package com.cinema.booking.cinema.repository;

import com.cinema.booking.cinema.entity.Auditorium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {
    boolean existsByName(String name); //kiểm tra có phòng chiếu nào mang tên này trong DB chưa
    boolean existsByNameAndIdNot(String name, Long id); //kiểm tra xem có phòng chiếu khác đang dùng tên này không

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auditorium a WHERE a.id = :id")
    Optional<Auditorium> findByIdWithLock(@Param("id") Long id);
}