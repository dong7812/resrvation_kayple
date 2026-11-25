package com.kayple.booking;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByDate(LocalDate date);
    List<Reservation> findByDateOrderByHourAsc(LocalDate date);
    List<Reservation> findByPhoneOrderByDateAscHourAsc(String phone);

    @Modifying
    @Query("delete from Reservation r where r.id = :id and r.phone = :phone")
    int deleteByIdAndPhone(Long id, String phone);
}
