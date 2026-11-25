package com.kayple.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class Dtos {
    private Dtos() {}

    public static final String PHONE_REGEX = "^010-?\\d{3,4}-?\\d{4}$";

    public record SlotOut(int hour, boolean available, boolean past) {}

    public record ReservationCreate(
            @NotNull(message = "날짜를 선택해 주세요.") LocalDate date,
            @NotNull(message = "시간을 선택해 주세요.") Integer hour,
            @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 20, message = "이름은 20자 이내로 입력해 주세요.") String name,
            @NotBlank(message = "연락처를 입력해 주세요.")
            @Pattern(regexp = PHONE_REGEX, message = "연락처는 010-0000-0000 형식으로 입력해 주세요.") String phone) {}

    public record ReservationOut(Long id, LocalDate date, int hour, String name, String phone) {
        static ReservationOut of(Reservation r) {
            return new ReservationOut(r.getId(), r.getDate(), r.getHour(), r.getName(), r.getPhone());
        }
    }

    public record LoginRequest(String username, String password) {}
}
