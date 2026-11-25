package com.kayple.booking;

import com.kayple.booking.Dtos.ReservationCreate;
import com.kayple.booking.Dtos.ReservationOut;
import com.kayple.booking.Dtos.SlotOut;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReservationController {
    private final ReservationService service;

    public ReservationController(ReservationService service) { this.service = service; }

    @GetMapping("/slots")
    public List<SlotOut> slots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.slots(date);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationOut create(@Valid @RequestBody ReservationCreate req) {
        return service.create(req);
    }

    @GetMapping("/reservations")
    public List<ReservationOut> mine(@RequestParam String phone) {
        return service.findByPhone(phone);
    }

    @DeleteMapping("/reservations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id, @RequestParam String phone) {
        service.cancel(id, phone);
    }
}
