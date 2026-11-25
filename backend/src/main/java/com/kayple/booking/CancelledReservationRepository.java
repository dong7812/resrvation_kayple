package com.kayple.booking;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancelledReservationRepository extends JpaRepository<CancelledReservation, Long> {
    List<CancelledReservation> findByDate(LocalDate date);
}
