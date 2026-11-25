package com.kayple.booking;

import com.kayple.booking.Dtos.AdminReservationOut;
import com.kayple.booking.Dtos.ReservationCreate;
import com.kayple.booking.Dtos.ReservationOut;
import com.kayple.booking.Dtos.SlotOut;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    public static final List<Integer> HOURS = List.of(10, 11, 13, 14, 15, 16, 17);
    public static final int DAYS = 7;
    public static final String DUPLICATE_MESSAGE = "이미 예약된 시간입니다. 다른 시간을 선택해 주세요.";

    private final ReservationRepository repository;
    private final CancelledReservationRepository cancelledRepository;
    private final Clock clock;

    public ReservationService(ReservationRepository repository, CancelledReservationRepository cancelledRepository,
                              Clock clock) {
        this.repository = repository;
        this.cancelledRepository = cancelledRepository;
        this.clock = clock;
    }

    public static String normalizePhone(String phone) {
        if (phone == null || !phone.trim().matches(Dtos.PHONE_REGEX)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "연락처는 010-0000-0000 형식으로 입력해 주세요.");
        }
        return phone.replaceAll("\\D", "");
    }

    private void validateDate(LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || date.isAfter(today.plusDays(DAYS - 1))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "예약은 오늘부터 7일 이내에만 가능합니다.");
        }
    }

    private boolean isPast(LocalDate date, int hour) {
        return !date.atTime(hour, 0).isAfter(LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public List<SlotOut> slots(LocalDate date) {
        validateDate(date);
        Set<Integer> booked = repository.findByDate(date).stream().map(Reservation::getHour).collect(Collectors.toSet());
        return HOURS.stream().map(h -> new SlotOut(h, !booked.contains(h) && !isPast(date, h), isPast(date, h))).toList();
    }

    /**
     * 중복 방지: UNIQUE(date, hour) 제약에 INSERT 한 번으로 맡긴다 (조회+변경이 원자적).
     * 비관적 락은 빈 칸에 잠글 행이 없어 갭 락/데드락 위험이 있어 쓰지 않는다.
     */
    public ReservationOut create(ReservationCreate req) {
        validateDate(req.date());
        if (!HOURS.contains(req.hour())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "예약할 수 없는 시간입니다.");
        }
        if (isPast(req.date(), req.hour())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "이미 지난 시간은 예약할 수 없습니다.");
        }
        String name = req.name().trim();
        if (name.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "이름을 입력해 주세요.");
        }
        String phone = normalizePhone(req.phone());
        try {
            Reservation saved = repository.saveAndFlush(
                    new Reservation(req.date(), req.hour(), name, phone, LocalDateTime.now(clock)));
            return ReservationOut.of(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
        }
    }

    @Transactional(readOnly = true)
    public List<ReservationOut> findByPhone(String phone) {
        return repository.findByPhoneOrderByDateAscHourAsc(normalizePhone(phone)).stream().map(ReservationOut::of).toList();
    }

    /**
     * id + 연락처가 함께 맞아야 취소. 취소 이력 테이블에 복사한 뒤 원본을 삭제한다(한 트랜잭션).
     * 이미 없으면 그대로 성공(멱등). 지난 예약은 취소 불가.
     */
    @Transactional
    public void cancel(Long id, String phone) {
        String normalized = normalizePhone(phone);
        repository.findById(id).filter(r -> r.getPhone().equals(normalized)).ifPresent(r -> {
            if (isPast(r.getDate(), r.getHour())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "이미 지난 예약은 취소할 수 없습니다.");
            }
            // 동시에 두 번 취소해도 실제로 지운 요청만 이력을 남긴다.
            if (repository.deleteByIdAndPhone(id, normalized) == 1) {
                cancelledRepository.save(new CancelledReservation(r, LocalDateTime.now(clock)));
            }
        });
    }

    @Transactional(readOnly = true)
    public List<ReservationOut> findByDate(LocalDate date) {
        return repository.findByDateOrderByHourAsc(date).stream().map(ReservationOut::of).toList();
    }

    /** 관리자용: 예약됨 + 취소됨을 시간순으로 함께 반환 (같은 시간이면 예약됨이 먼저). */
    @Transactional(readOnly = true)
    public List<AdminReservationOut> findAllByDateForAdmin(LocalDate date) {
        return Stream.concat(
                        repository.findByDateOrderByHourAsc(date).stream().map(AdminReservationOut::booked),
                        cancelledRepository.findByDate(date).stream().map(AdminReservationOut::cancelled))
                .sorted(Comparator.comparingInt(AdminReservationOut::hour)
                        .thenComparing(o -> o.status().equals("CANCELLED")))
                .toList();
    }
}
