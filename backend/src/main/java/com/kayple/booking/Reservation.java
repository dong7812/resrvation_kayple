package com.kayple.booking;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation",
        uniqueConstraints = @UniqueConstraint(name = "uk_reservation_slot", columnNames = {"date", "hour"}))
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "hour", nullable = false)
    private int hour;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 11)
    private String phone;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Reservation() {}

    public Reservation(LocalDate date, int hour, String name, String phone, LocalDateTime createdAt) {
        this.date = date; this.hour = hour; this.name = name; this.phone = phone; this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public int getHour() { return hour; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
