//thuật toán tự sinh ghế
package com.cinema.booking.cinema.service;

import com.cinema.booking.cinema.dto.AuditoriumRequest;
import com.cinema.booking.cinema.dto.AuditoriumResponse;
import com.cinema.booking.cinema.dto.SeatResponse;
import com.cinema.booking.cinema.entity.Auditorium;
import com.cinema.booking.cinema.entity.Seat;
import com.cinema.booking.cinema.repository.AuditoriumRepository;
import com.cinema.booking.cinema.repository.SeatRepository;
import com.cinema.booking.common.exception.AuditoriumNameAlreadyExistsException;
import com.cinema.booking.common.exception.AuditoriumNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditoriumService {

    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;

    // 1. tạo phòng chiếu và tự động sinh ghế
    //phải đánh dấu @Transactional, nếu việc sinh 100 ghế mà bị lỗi ở ghế 99 thì toàn bộ phòng và 98 ghế trước đó sẽ rollback sạch sẽ, không để lại rác trong DB
    @Transactional
    public AuditoriumResponse createAuditorium(AuditoriumRequest request) {
        String trimmedName = request.name().trim();

        //kiểm tra xem phòng tồn tại chưa
        if (auditoriumRepository.existsByName(trimmedName)) {
            throw new AuditoriumNameAlreadyExistsException(trimmedName);
        }

        // Lưu phòng chiếu vào database trước để lấy id
        Auditorium auditorium = Auditorium.builder()
                .name(trimmedName)
                .rowCount(request.rowCount())
                .seatsPerRow(request.seatsPerRow())
                .build();
        Auditorium savedAuditorium = auditoriumRepository.save(auditorium);

        // thuật toán tự sinh ghế
        // hàng 1 tương ứng A,..., hàng 26 tương ứng z
        List<Seat> seats = new ArrayList<>();
        for (int r = 1; r <= request.rowCount(); r++) {
            char rowChar = (char) ('A' + (r - 1));
            String rowLabel = String.valueOf(rowChar);
            for (int c = 1; c <= request.seatsPerRow(); c++) {
                String seatCode = rowLabel + c; // vd A1, A2, B1
                Seat seat = Seat.builder()
                        .auditorium(savedAuditorium)
                        .rowLabel(rowLabel)
                        .seatNumber(c)
                        .seatCode(seatCode)
                        .build();
                seats.add(seat);
            }
        }

        // Lưu toàn bộ danh sách ghế vào DB
        seatRepository.saveAll(seats);

        return AuditoriumResponse.from(savedAuditorium);
    }

    // 2. lấy danh sách toàn bộ phòng chiếu
    @Transactional(readOnly = true)
    public List<AuditoriumResponse> getAllAuditoriums() {
        return auditoriumRepository.findAll()
                .stream()
                .map(AuditoriumResponse::from)
                .toList();
    }

    // 3. lấy chi tiết phòng chiếu theo ID
    @Transactional(readOnly = true)
    public AuditoriumResponse getAuditoriumById(Long id) {
        Auditorium auditorium = auditoriumRepository.findById(id)
                .orElseThrow(() -> new AuditoriumNotFoundException(id));
        return AuditoriumResponse.from(auditorium);
    }

    // 4. Lấy danh sách sơ đồ ghế của phòng chiếu
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByAuditoriumId(Long auditoriumId) {
        if (!auditoriumRepository.existsById(auditoriumId)) {
            throw new AuditoriumNotFoundException(auditoriumId);
        }
        return seatRepository.findByAuditoriumIdOrderByRowLabelAscSeatNumberAsc(auditoriumId)
                .stream()
                .map(SeatResponse::from)
                .toList();
    }

    // 5. Cập nhật phòng chiếu (PUT /api/admin/auditoriums/{id})
    // quy tắc không cho đổi kích thước nếu sau này phòng đã có suất chiếu
    @Transactional
    public AuditoriumResponse updateAuditorium(Long id, AuditoriumRequest request) {
        // tìm phòng cần sửa
        Auditorium auditorium = auditoriumRepository.findById(id)
                .orElseThrow(() -> new AuditoriumNotFoundException(id));

        // kiểm tra xem tên mới có bị trùng với một phòng khác hay không
        String trimmedName = request.name().trim();
        if (auditoriumRepository.existsByNameAndIdNot(trimmedName, id)) {
            throw new AuditoriumNameAlreadyExistsException(trimmedName);
        }

        // cập nhật tên phòng
        auditorium.setName(trimmedName);

        return AuditoriumResponse.from(auditorium);
    }
}