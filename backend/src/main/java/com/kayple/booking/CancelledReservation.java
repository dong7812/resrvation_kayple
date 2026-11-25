package com.kayple.booking;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 취소 이력. 취소 시 reservation 행을 그대로 복사해 두고 원본은 삭제한다.
 * 원본 테이블의 UNIQUE(slot_date, slot_hour)는 "현재 예약된 칸"만 담으므로
 * 취소된 칸을 바로 다시 예약할 수 있고, 중복 방지 로직도 그대로 유지된다.
 */
@Entity
@Table(name = "reservation_cancelled", indexes = @Index(name = "idx_cancelled_date", columnList = "slot_date"))
public class CancelledReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservationId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate date;

    @Column(name = "slot_hour", nullable = false)
    private int hour;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 11)
    private String phone;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime cancelledAt;

    protected CancelledReservation() {}

    public CancelledReservation(Reservation r, LocalDateTime cancelledAt) {
        this.reservationId = r.getId(); this.date = r.getDate(); this.hour = r.getHour();
        this.name = r.getName(); this.phone = r.getPhone(); this.createdAt = r.getCreatedAt();
        this.cancelledAt = cancelledAt;
    }

    public Long getReservationId() { return reservationId; }
    public LocalDate getDate() { return date; }
    public int getHour() { return hour; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}
